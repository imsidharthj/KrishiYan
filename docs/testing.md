# Testing

## Unit tests

Plain JUnit tests for pure logic only, run with `./gradlew testDebugUnitTest` (or `just test`).

Done: distance calculation (`DistanceTest`), rental cost estimate (`RentalCostTest`), booking overlap check (`OverlapTest`), booking status transitions (`BookingStatusTest`).

## Manual checklist

Run on a real phone before calling a step done. Each line is pass or fail.

### Skeleton
- [ ] App opens without crashing and shows the Home tab.
- [ ] Bottom bar shows Home, Map, My rentals, Profile.
- [ ] Tapping each tab shows its screen title.
- [ ] Back from a non-Home tab returns to Home, then closes the app.

### Login
- [ ] First open shows the KrishiYan login screen with an Email box.
- [ ] Invalid email shows "Enter a valid email".
- [ ] Send code: an email "Your KrishiYan login code" arrives with a number.
- [ ] A wrong code shows "Wrong or expired code".
- [ ] The right code opens "About you" on first login.
- [ ] Empty name, a 9-digit phone, or empty village each show their error.
- [ ] Save and continue opens Home.
- [ ] Force close and reopen: goes straight to Home.
- [ ] Profile > Log out returns to login. Logging in again skips "About you".
- [ ] Airplane mode on Send code shows "No internet. Try again".

### Add equipment
- [ ] Profile > Add equipment opens the form.
- [ ] Publish with nothing filled shows "Add at least one photo".
- [ ] Gallery adds photos (max 5); the cross removes one. Camera adds a photo.
- [ ] My location asks for permission, then shows "Location set".
- [ ] Pick on map loads the map; Use this place shows "Location set".
- [ ] Not available on some days: adding a date range shows a chip; tapping it removes it.
- [ ] Publish shows "Equipment published" and returns to Profile.
- [ ] Supabase has the row in equipment, its photos in equipment_images and Storage, and blocked dates if added.

### Home
- [ ] With no saved place, Home asks "Where are you?" with My location and Search village or town.
- [ ] My location shows nearby equipment and the town name on the chip.
- [ ] Location chip > type 3 letters shows place suggestions; picking one reloads the list and the chip shows its name.
- [ ] Nonsense text shows "No place found. Check the spelling".
- [ ] Pick on map still works.
- [ ] Each card shows photo, name, "x km away", price per hour or day, and Available now or Busy now.
- [ ] Search text, category chips and 5/10/25/50 km chips each reload the list.
- [ ] A far place shows "No equipment near you yet".
- [ ] Pull down reloads.
- [ ] Airplane mode shows "No internet. Try again"; Try again works once back online.
- [ ] Reopening the app keeps the last place.

### Map
- [ ] Map tab shows the place, search box, type chips and distance chips at the top.
- [ ] Green pins show listings; the blue dot is the chosen place.
- [ ] Tapping a pin shows its card; tapping empty map hides it.
- [ ] Type, search and distance change the pins. Home then shows "Showing: ..." with Clear.
- [ ] "© OpenStreetMap contributors" shows on the Map tab and the pin picker.
- [ ] Searching a place opens the pin picker at that place; moving the map updates the address under the pin.

### Equipment details
- [ ] Tapping a card on Home, or the card on the Map, opens the details screen.
- [ ] Photos swipe, with a "1 / 3" counter when there are several.
- [ ] Shows type, name, price, owner first name and village, "About x km away", brand and year, description.
- [ ] Calendar: past days grey, free days green, blocked or booked days grey with a line through. Arrows change month.
- [ ] Rental terms are listed.
- [ ] Someone else's listing shows Request booking; your own shows "This is your listing".

### Booking request
- [ ] Details > Request booking opens the form with the equipment name and price.
- [ ] Picking a start date fills start and end (one hour or one day later). Today defaults to the next full hour.
- [ ] Past days cannot be picked. End date picker starts at the start date.
- [ ] The estimate updates live, e.g. "₹2,400, 3 hours × ₹800". 2 h 10 min on an hourly price counts as 3 hours.
- [ ] Choosing busy days shows "These dates are not free. Check the calendar" and Send is refused.
- [ ] Send request shows "Request sent. The owner will reply soon" and goes back to details.
- [ ] Supabase bookings table has the new row with status pending and the same estimated cost.

### Owner requests
- [ ] Run `supabase/seed/demo_requests.sql` to get two overlapping requests per listing you own.
- [ ] Profile shows "Booking requests (n)".
- [ ] Each request shows equipment, renter name and village, dates, rent and message.
- [ ] Accept removes the card. Accepting the overlapping one shows "You already accepted another booking for these dates".
- [ ] Reject asks to confirm, then removes the card.
- [ ] Empty list shows "No booking requests right now". Pull down reloads.
- [ ] The accepted day is greyed out in the listing's calendar.

### My rentals
- [ ] Tabs Pending, Confirmed, Active, Past show counts; each empty tab has a clear message.
- [ ] A new request shows under Pending as "Waiting for owner".
- [ ] After the owner accepts (or the SQL in the step 10 notes), it moves to Confirmed with owner name and phone, Call, WhatsApp and Open location in maps.
- [ ] Call opens the dialler with +91 number; WhatsApp opens a chat with a ready message; maps opens the exact spot.
- [ ] During the rental it shows under Active; after the end under Past.
- [ ] Cancel booking asks to confirm and moves it to Past as Cancelled. Not offered once an accepted rental has started.
- [ ] Pull down reloads; opening the tab reloads.
