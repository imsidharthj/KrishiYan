# Testing

## Unit tests

Plain JUnit tests for pure logic only, run with `./gradlew testDebugUnitTest` (or `just test`).

Planned: distance calculation, rental cost estimate, booking overlap check, booking status transitions.

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
