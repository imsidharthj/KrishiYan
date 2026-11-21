"""
End-to-end check of KrishiYan against the real Supabase project, acting as two separate users.

Needs two users created in Supabase (Authentication > Users > Add user, Auto Confirm on):
  tester.owner@krishiyan.test and tester.renter@krishiyan.test, password KrishiTest-2026!
Run from the repo root:  python3 supabase/tests/e2e.py
It removes the equipment, photo and bookings it creates.
"""
import base64, json, sys, time, urllib.request, urllib.error, datetime as dt

props = dict(l.strip().split("=", 1) for l in open("local.properties") if "=" in l and not l.startswith("#"))
URL, KEY = props["SUPABASE_URL"].rstrip("/"), props["SUPABASE_KEY"]
PHOTO = base64.b64decode("/9j/4AAQSkZJRgABAQAAAAAAAAD/2wBDAAMCAgICAgMCAgIDAwMDBAYEBAQEBAgGBgUGCQgKCgkICQkKDA8MCgsOCwkJDRENDg8QEBEQCgwSExIQEw8QEBD/2wBDAQMDAwQDBAgEBAgQCwkLEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBD/wAARCAAwAEADAREAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAj/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFgEBAQEAAAAAAAAAAAAAAAAAAAYH/8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAwDAQACEQMRAD8AlxFM0AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAf//Z")  # a tiny green JPEG
PASSWORD = "KrishiTest-2026!"
results = []

def call(method, path, token=None, body=None, raw=None, ctype="application/json", prefer=None):
    headers = {"apikey": KEY, "Content-Type": ctype}
    if token: headers["Authorization"] = "Bearer " + token
    if prefer: headers["Prefer"] = prefer
    data = raw if raw is not None else (json.dumps(body).encode() if body is not None else None)
    req = urllib.request.Request(URL + path, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            text = r.read().decode() or "null"
            return r.status, json.loads(text) if text.strip()[:1] in "[{\"n0123456789tf" else text
    except urllib.error.HTTPError as e:
        text = e.read().decode()
        try: return e.code, json.loads(text)
        except Exception: return e.code, text

def check(name, ok, detail=""):
    results.append(ok)
    print(("PASS " if ok else "FAIL ") + name + (f"  ({detail})" if detail and not ok else ""))

def rpc(fn, token, **args): return call("POST", f"/rest/v1/rpc/{fn}", token, args)

def login(email):
    s, b = call("POST", "/auth/v1/token?grant_type=password", body={"email": email, "password": PASSWORD})
    if s != 200: sys.exit(f"Cannot log in {email}: {s} {b}")
    return b["access_token"], b["user"]["id"]

owner_t, owner_id = login("tester.owner@krishiyan.test")
renter_t, renter_id = login("tester.renter@krishiyan.test")
check("both test users log in with the app's public key", True)

for t, uid, name, phone in [(owner_t, owner_id, "Test Owner", "9111111111"), (renter_t, renter_id, "Test Renter", "9222222222")]:
    s, b = call("POST", "/rest/v1/profiles", t, {"id": uid, "name": name, "phone": phone, "village": "Testpur"},
                prefer="resolution=merge-duplicates,return=minimal")
    check(f"{name} saves profile", s in (200, 201, 204), f"{s} {b}")

s, b = call("POST", "/rest/v1/profiles", renter_t, {"id": owner_id, "name": "Hacker", "phone": "9000000000", "village": "X"})
check("renter cannot write owner's profile", s >= 400, f"{s} {b}")

LAT, LNG = 25.1234, 76.8765
s, b = call("POST", "/rest/v1/equipment", owner_t, {
    "owner_id": owner_id, "name": f"E2E tractor {int(time.time())}", "category": "tractor", "brand_model": "Test",
    "description": "end to end test", "price": 700, "price_unit": "hour", "lat": LAT, "lng": LNG}, prefer="return=representation")
check("owner lists equipment", s == 201, f"{s} {b}")
eq_id = b[0]["id"]
path = f"{owner_id}/{eq_id}/0.jpg"

try:
    s, b = call("POST", f"/storage/v1/object/equipment-photos/{path}", owner_t, raw=PHOTO, ctype="image/jpeg")
    check("owner uploads photo to own folder", s == 200, f"{s} {b}")
    s, b = call("POST", "/rest/v1/equipment_images", owner_t, {"equipment_id": eq_id, "storage_path": path, "position": 0})
    check("owner saves photo row", s == 201, f"{s} {b}")

    s, b = call("POST", f"/storage/v1/object/equipment-photos/{owner_id}/{eq_id}/evil.jpg", renter_t, raw=PHOTO, ctype="image/jpeg")
    check("renter cannot upload into owner's folder", s >= 400, f"{s} {b}")
    s, b = call("POST", "/rest/v1/equipment", renter_t, {**{"owner_id": owner_id, "name": "fake", "category": "tractor",
                "price": 1, "price_unit": "day", "lat": 1, "lng": 1}})
    check("renter cannot list equipment as the owner", s >= 400, f"{s} {b}")

    s, b = rpc("nearby_equipment", renter_t, p_lat=LAT + 0.02, p_lng=LNG, p_radius_km=10)
    mine = [x for x in b if x["id"] == eq_id] if s == 200 else []
    check("renter finds it in nearby search", len(mine) == 1, f"{s} {b}")
    if mine:
        m = mine[0]
        check("search shows only approximate location", (m["approx_lat"], m["approx_lng"]) == (25.12, 76.88), str(m))
        check("search returns distance and photo", m["distance_km"] == 2 and m["photo_path"] == path, str(m))
    s, b = rpc("nearby_equipment", None, p_lat=LAT, p_lng=LNG, p_radius_km=10)
    check("logged-out caller cannot search", s >= 400 or b == [], f"{s} {b}")

    s, b = call("GET", f"/rest/v1/equipment?id=eq.{eq_id}&select=lat,lng", renter_t)
    check("renter cannot read exact location from the table", s == 200 and b == [], f"{s} {b}")
    s, b = call("GET", f"/rest/v1/profiles?id=eq.{owner_id}", renter_t)
    check("renter cannot read owner's phone from profiles", s == 200 and b == [], f"{s} {b}")

    s, b = rpc("equipment_details", renter_t, p_equipment_id=eq_id)
    check("renter opens details with owner's first name only", s == 200 and b and b[0]["owner_first_name"] == "Test"
          and "phone" not in json.dumps(b), f"{s} {b}")

    s, b = call("POST", "/storage/v1/object/sign/equipment-photos", renter_t, {"expiresIn": 600, "paths": [path]})
    signed = b[0].get("signedURL") if s == 200 and b else None
    if signed:
        try:
            with urllib.request.urlopen(URL + "/storage/v1" + signed if signed.startswith("/") else signed, timeout=30) as r:
                check("renter downloads the photo", r.status == 200 and r.read() == PHOTO)
        except Exception as e:
            check("renter downloads the photo", False, str(e))
    else:
        check("renter gets a photo link", False, f"{s} {b}")

    ist = dt.timezone(dt.timedelta(hours=5, minutes=30))
    day = (dt.datetime.now(ist) + dt.timedelta(days=2)).replace(minute=0, second=0, microsecond=0)
    at = lambda h: day.replace(hour=h).isoformat()
    s, b1 = rpc("request_booking", renter_t, p_equipment_id=eq_id, p_start=at(9), p_end=at(12), p_message="e2e")
    check("renter requests a booking", s == 200, f"{s} {b1}")
    s, b2 = rpc("request_booking", renter_t, p_equipment_id=eq_id, p_start=at(11), p_end=at(14), p_message="")
    check("renter requests an overlapping booking", s == 200, f"{s} {b2}")
    s, b = rpc("request_booking", owner_t, p_equipment_id=eq_id, p_start=at(15), p_end=at(16), p_message="")
    check("owner cannot book own equipment", s >= 400, f"{s} {b}")

    s, b = call("GET", f"/rest/v1/bookings?id=eq.{b1}&select=estimated_cost,status", renter_t)
    check("server works out the rent (3 h x 700 = 2100)", s == 200 and b and float(b[0]["estimated_cost"]) == 2100 and b[0]["status"] == "pending", f"{s} {b}")

    s, b = rpc("booking_contact", renter_t, p_booking_id=b1)
    check("no exact location or phone before acceptance", s == 200 and b == [], f"{s} {b}")
    s, b = rpc("accept_booking", renter_t, p_booking_id=b1)
    check("renter cannot accept own request", s >= 400, f"{s} {b}")

    s, b = call("GET", f"/rest/v1/bookings?owner_id=eq.{owner_id}&status=eq.pending&select=id,equipment(name)", owner_t)
    check("owner sees both pending requests", s == 200 and len(b) == 2, f"{s} {b}")
    s, b = rpc("booking_renter", owner_t, p_booking_id=b1)
    check("owner sees renter's name and phone", s == 200 and b and b[0]["renter_phone"] == "9222222222", f"{s} {b}")

    s, b = rpc("accept_booking", owner_t, p_booking_id=b1)
    check("owner accepts the first request", s in (200, 204), f"{s} {b}")
    s, b = rpc("accept_booking", owner_t, p_booking_id=b2)
    check("accepting the clashing request is refused", s >= 400 and "dates_already_booked" in json.dumps(b), f"{s} {b}")
    s, b = rpc("reject_booking", owner_t, p_booking_id=b2)
    check("owner rejects the clashing request", s in (200, 204), f"{s} {b}")

    s, b = rpc("my_rentals", renter_t)
    st = {x["id"]: x["status"] for x in b} if s == 200 else {}
    check("renter sees accepted and rejected in My rentals", st.get(b1) == "accepted" and st.get(b2) == "rejected", f"{s} {b}")
    check("My rentals includes the equipment name", s == 200 and all(x["equipment_name"].startswith("E2E") for x in b if x["id"] in (b1, b2)))

    s, b = rpc("booking_contact", renter_t, p_booking_id=b1)
    check("renter gets exact location and owner phone after acceptance",
          s == 200 and b and abs(b[0]["lat"] - LAT) < 1e-9 and b[0]["owner_phone"] == "9111111111", f"{s} {b}")
    s, b = rpc("request_booking", renter_t, p_equipment_id=eq_id, p_start=at(10), p_end=at(11), p_message="")
    check("new request over the accepted time is refused", s >= 400 and "dates_unavailable" in json.dumps(b), f"{s} {b}")

    s, b = rpc("cancel_booking", renter_t, p_booking_id=b1)
    check("renter cancels before the start", s in (200, 204), f"{s} {b}")
    s, b = rpc("cancel_booking", owner_t, p_booking_id=b1)
    check("owner cannot cancel through the renter's action", s >= 400, f"{s} {b}")

    s, b = call("PATCH", f"/rest/v1/equipment?id=eq.{eq_id}", owner_t, {"is_available": False}, prefer="return=minimal")
    check("owner marks equipment unavailable", s == 204, f"{s} {b}")
    s, b = rpc("nearby_equipment", renter_t, p_lat=LAT, p_lng=LNG, p_radius_km=10)
    check("hidden equipment disappears from search", s == 200 and not any(x["id"] == eq_id for x in b), f"{s} {b}")
finally:
    call("DELETE", f"/storage/v1/object/equipment-photos/{path}", owner_t)
    s, _ = call("DELETE", f"/rest/v1/equipment?id=eq.{eq_id}", owner_t)
    print(f"cleanup: removed test equipment, its photo and bookings ({s})")

print(f"\n{sum(results)} of {len(results)} checks passed")
sys.exit(0 if all(results) else 1)
