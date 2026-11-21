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
