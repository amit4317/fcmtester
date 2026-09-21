# FCM Tester APK — no Android Studio required

A tiny Android app for testing Firebase Cloud Messaging (FCM) against **your own Firebase project**.

It:

- Displays the device's FCM registration token.
- Has a **Copy token** button.
- Requests notification permission on Android 13+.
- Shows/logs messages delivered to `FirebaseMessagingService`.
- Shows a local notification for foreground/data messages.
- Logs extras when the app is opened from a notification.
- Builds entirely in **GitHub Actions**. You do not need Android Studio.

## 1. Create/register an Android app in Firebase

1. Open **Firebase Console** and select your project.
2. Open **Project settings** → **Your apps** → **Add app** → **Android**.
3. For the Android package name, you can use something like:

   `com.example.fcmtester`

   Any valid package name is fine. The GitHub build automatically reads the package name from your Firebase config.
4. Register the app.
5. Download **`google-services.json`**.

You do not need to install Android Studio or add an SDK manually.

## 2. Put this project in GitHub

Create an empty GitHub repository, then upload the contents of this project to it.

Important: make sure `.github/workflows/build-apk.yml` is present in the repository. That file performs the cloud APK build.

## 3. Add `google-services.json` as a GitHub Actions secret

In your GitHub repository:

1. Open **Settings**.
2. Open **Secrets and variables** → **Actions**.
3. Select **New repository secret**.
4. Name it exactly:

   `GOOGLE_SERVICES_JSON`

5. Open the `google-services.json` file you downloaded from Firebase, copy its **entire contents**, and paste it as the secret value.
6. Save the secret.

The workflow writes this secret to `app/google-services.json` only during the build.

> Firebase documents `google-services.json` as containing project identifiers rather than private server credentials, but using a repository secret keeps it out of your source repository anyway. Do **not** paste a Firebase service-account private key here.

## 4. Build the APK online

1. Open your GitHub repository.
2. Open the **Actions** tab.
3. Select **Build FCM Tester APK**.
4. Select **Run workflow**.
5. Open the completed workflow run.
6. Download **`FCMTester-debug.apk`** from the workflow artifacts/output.

The workflow uses Java 17, Android SDK 36, Gradle 9.6, Android Gradle Plugin 9.4, Firebase Android BoM 34.19.0, and the Google Services plugin 4.5.0.

## 5. Install and test

1. Copy `FCMTester-debug.apk` to your Android phone and install it.
2. If Android blocks installation, allow installation from the browser/file-manager you used.
3. Open **FCM Tester**.
4. Allow notifications when Android asks.
5. Wait until **FCM token ready** appears.
6. Tap **Copy token**.
7. In Firebase Console, open **Messaging** and create a notification.
8. Use **Send test message**, paste the token, and send it.
9. Put the FCM Tester app in the background for the simplest notification test.

If the notification arrives, your Firebase project → FCM → Android device path is working.

## Testing data messages

If your own backend sends a data payload to the token, the app logs the key/value pairs under **Received / opened messages**. If the data includes `title` and `body`, the app also uses them for its local notification.

Example payload fields:

```text
title = Test data message
body = Hello from my backend
order_id = 12345
```

## Important FCM behavior

FCM has different behavior depending on message type and app state:

- **Notification message + app in background:** Android/Firebase normally displays it automatically. `onMessageReceived()` may not run for that notification until app interaction, so the visible system notification is the main test.
- **Notification message + app in foreground:** `onMessageReceived()` runs; this tester logs it and creates a local notification.
- **Data message:** delivery to `onMessageReceived()` depends on app/device state, Android background restrictions, message priority, and payload configuration. The tester logs messages that reach the service.

## Build locally without Android Studio (optional)

If you ever have Java, Android SDK, and Gradle installed on a machine, you can also place your `google-services.json` at `app/google-services.json`, set `FCM_PACKAGE_NAME` to its Android package name, and run:

```bash
gradle :app:assembleDebug
```

But GitHub Actions is the intended path for this project.
