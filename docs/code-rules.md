# Code rules

## Package structure

```
app/src/main/java/com/krishiyan/
  KrishiYanApp.kt        Application, creates AppContainer
  AppContainer.kt        builds Supabase client and repositories
  MainActivity.kt
  ui/theme/              colors, typography, shapes
  ui/components/         shared big buttons, equipment card, status chip
  navigation/            NavHost and bottom bar
  auth/                  AuthRepository, LoginScreen, LoginViewModel
  equipment/             model, EquipmentRepository, list, details, add/edit
  location/              LocationProvider, distance helper, LocationPicker
  map/                   MapScreen (osmdroid)
  booking/               model, BookingRepository, request, my rentals, owner requests
  profile/               ProfileScreen, owner dashboard
```

- Each feature owns its model, repository, screens and ViewModels.
- Screens never call Supabase directly. They go through a repository.
- One `ViewModel` per screen, exposing a single `StateFlow` of UI state.
- Dependencies come from `AppContainer`. No Hilt or Koin.

## Naming

- Screens: `XxxScreen`. ViewModels: `XxxViewModel`. Repositories: `XxxRepository`.
- Repository functions name the domain action: `requestBooking`, `acceptBooking`, `listMyEquipment`.
- Database columns are `snake_case`; Kotlin properties are `camelCase` with `@SerialName`.

## UI

- Colors only from `ui/theme/Color.kt`: deep green primary, light green secondary, off-white background, charcoal text.
- Spacing: 8, 16 or 24 dp. Screen padding is 16 dp.
- Corners: 16 dp for cards and buttons.
- Buttons and touch targets are at least 56 dp tall.
- Every screen has one obvious main action.
- All user-facing text lives in `res/values/strings.xml`.
- Every list has an empty state and every load has an error state with a retry.
- No decorative animations. No em dashes in copy.
