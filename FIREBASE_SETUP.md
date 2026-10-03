# Firebase setup

1. Create a Firebase project and add the Android app with applicationId `com.pentolrebus.kasir`.
2. Enable Authentication → Email/Password.
3. Create a Realtime Database instance.
4. Publish `database.rules.json` as the RTDB rules.
5. Download `google-services.json` into `app/`.
6. Enable Cloud Messaging. No Cloud Functions are required.
7. Build with `./gradlew assembleDebug`.

The application deliberately does not use Firestore, Firebase Storage, Cloud Functions, Google Sign-In, Credential Manager Google, Phone Auth, or Google Drive.
