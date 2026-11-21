# Principles

- Make the code say what the system is doing. A reader should not need a comment to find the important behavior.
- Name state changes after the exact domain action: `acceptBooking`, `rejectBooking`, `cancelBooking`, not `updateBooking(status)`.
- Add abstractions only when they genuinely clarify. Similar-looking code is not a reason to generalize.
- Keep repository functions small and explicit: `listNearbyEquipment(lat, lng, radiusKm)`, `requestBooking(...)`.
- Keep design proportional to the problem. No layers just to look "clean".
- Use configuration only for secrets or values that change between environments. Everything else goes in constants in code.
