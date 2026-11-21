# Build, run and release

## One-time setup (Linux)

1. Android Studio is unpacked in `~/android-studio`. Start it with `~/android-studio/bin/studio`.
   On first start choose "Standard" setup and point the SDK at `~/Android/Sdk`.
2. Add to `~/.zshrc`, then open a new terminal:
   ```
   export ANDROID_HOME=$HOME/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/platform-tools:$HOME/android-studio/bin
   ```
3. In Android Studio: File > Open > this folder. Wait for Gradle sync to finish.

## Run on your phone

### USB
1. On the phone: Settings > About phone > tap **Build number** 7 times. Developer options is now on.
2. Settings > System > Developer options > turn on **USB debugging**.
3. Plug the phone in with a data cable. Accept the "Allow USB debugging?" prompt on the phone.
4. Check: `adb devices` lists your phone as `device` (not `unauthorized`).
5. In Android Studio pick your phone in the device dropdown and press the green Run button.
   From the terminal: `./gradlew installDebug`.

### Wireless (Android 11+)
1. Phone and laptop on the same Wi-Fi.
2. Developer options > **Wireless debugging** > on > **Pair device with pairing code**.
3. `adb pair <ip>:<pairing-port>` and type the code, then `adb connect <ip>:<port>` (port shown on the Wireless debugging screen).
4. Run as above.

## Command line

- `./gradlew assembleDebug` builds `app/build/outputs/apk/debug/app-debug.apk`.
- `./gradlew installDebug` installs it on the connected phone.
- `./gradlew testDebugUnitTest` runs unit tests.

## Supabase setup (once per project)

1. Create a free project at supabase.com (region: South Asia, Mumbai).
2. SQL Editor: run each file in `supabase/migrations/` in number order.
3. Authentication > Emails > SMTP Settings: Gmail SMTP (`smtp.gmail.com`, port 465, Gmail address, Gmail app password), sender name `KrishiYan`.
4. Authentication > Emails > Templates: in **Confirm sign up** and **Magic link or OTP**, subject `Your KrishiYan login code`, body:
   ```
   <h2>Your KrishiYan login code</h2>
   <p>Enter this code in the app: <strong>{{ .Token }}</strong></p>
   ```
5. Project Settings > API Keys: copy the publishable key into `local.properties`:
   ```
   SUPABASE_URL=https://<project>.supabase.co
   SUPABASE_KEY=sb_publishable_...
   ```
