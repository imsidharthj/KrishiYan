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
