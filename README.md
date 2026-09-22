# FCM Tester Pro APK — no Android Studio required

A small Android app for testing Firebase Cloud Messaging against **your own Firebase project**. It is designed to build completely in GitHub Actions. +1

## What this version tests

- FCM registration token + copy button
- Normal notification messages
- Notification + data messages
- Data-only messages
- Silent/data-only messages (`silent=true` or `show_notification=false`)
- Custom key/value data
- Topic subscribe/unsubscribe from the APK
- Topic sends from the included Node.js example
- Image notifications
- A bundled custom notification sound
- Deep-link data such as `fcmtester://order/1234`
- Android `clickAction` using `OPEN_FCM_TEST`
- Tap/open intent extras
- Message metadata: message ID, sender, TTL, collapse key, delivered/original priority
- JSON-formatted normalized payload logging

The JSON shown in the app is the message **as exposed by the Android FCM SDK**, not a byte-for-byte copy of your server's HTTP request.

---

## Updating from the first FCM Tester build

Your existing GitHub `GOOGLE_SERVICES_JSON` secret can stay exactly as it is.

Replace the repository files with this Pro project and run the new workflow.

**Important:** the first Pro build may be signed with a different debug key from the APK already installed on your phone. If Android says the update cannot be installed, uninstall the old **FCM Tester** APK and install **FCM Tester Pro**. That produces a new FCM token, so copy the new token before testing.

The Pro workflow caches its debug signing key so later Pro builds from the same repository are more likely to install as updates.

---

## 1. Firebase Android configuration

You can reuse the Firebase Android app you already created.

The repository needs the GitHub Actions secret:

`GOOGLE_SERVICES_JSON`

Its value must be the complete contents of your Firebase `google-services.json` file.

The workflow reads the Android package name from that JSON and builds the APK with the matching `applicationId`.

---

## 2. Build the APK in GitHub

1. Upload/replace the files in your GitHub repository.
2. Confirm `.github/workflows/build-apk.yml` exists.
3. Open **Actions**.
4. Select **Build FCM Tester Pro APK**.
5. Click **Run workflow**.
6. When it succeeds, open the run and scroll to **Artifacts**.
7. Download **FCMTester-Pro-debug-apk**.
8. Extract the downloaded artifact ZIP; inside is `FCMTester-Pro-debug.apk`.
9. Install it on the phone and open it once.
10. Allow notifications.

---

## 3. Basic Firebase Console test

1. Open the APK.
2. Copy the FCM token.
3. Firebase Console -> Messaging -> create a notification.
4. Enter a title/body.
5. Choose **Send test message**.
6. Paste the registration token.
7. Put the APK in the background and send.

For background notification payloads, Android/FCM normally renders the notification itself. For foreground and data-only messages, the tester handles/logs the payload itself.

---

# Payload features

## Custom data

Example data values:

```text
order_id = 1234
status = shipped
screen = order_details
```

FCM data values are strings.

## Silent / data-only

Send a **data-only** message with:

```text
silent = true
```

or:

```text
show_notification = false
```

The tester records the message but deliberately does not create a local notification.

For data-only background testing, Android delivery behavior is affected by device state and battery restrictions. Use Android FCM priority `high` when the message is time-sensitive.

## Image notification

Use either an FCM notification image or a data value:

```text
image = https://example.com/image.jpg
```

or:

```text
image_url = https://example.com/image.jpg
```

Use a publicly reachable HTTPS image. For system-rendered Android notification images, FCM has an image size limit; keep test images small.

For foreground/data messages the tester performs a short best-effort image download and uses Android BigPictureStyle. This is intentionally a test tool, not a production long-running image pipeline.

## Deep link

Use:

```text
deep_link = fcmtester://order/1234
```

When the tester creates the local notification, tapping it opens `MainActivity` with this URI and logs the received deep link.

## Android click action

For an Android notification handled by the system, send:

```text
clickAction = OPEN_FCM_TEST
```

The manifest contains an intent filter for `OPEN_FCM_TEST`. Put the app in the background, send the notification, tap it, then inspect the app log.

## Bundled custom sound

This project contains:

```text
res/raw/fcm_test.wav
```

For tester-created notifications, add this data value:

```text
sound = custom
```

For system-rendered FCM notifications on Android 8+, send:

```text
android.notification.channelId = fcm_tester_custom
android.notification.sound = fcm_test
```

Open the APK at least once before testing so it can create the custom-sound notification channel.

Android notification-channel sound settings are persistent. If you manually change/mute the channel in Android Settings, the app cannot silently override that choice.

## Notification tag

For tester-created local notifications:

```text
tag = order-1234
```

Repeated notifications with the same tag can be grouped/replaced according to Android notification behavior.

---

# Topic testing

The APK has a **Topic subscription** section.

1. Enter `news`.
2. Tap **Subscribe**.
3. Wait for the success message.
4. Send an FCM message to topic `news` from your server.

The list in the APK is a convenience list of topics successfully requested from this installation. It is not a server-side Firebase topic-management console.

---

# Node.js / Express sender included

The folder:

`node-server/`

contains a ready-to-run Firebase Admin + Express test sender.

It supports:

- token sends
- topic sends
- notification messages
- data-only messages
- silent messages
- image URL
- deep-link data
- custom sound/channel
- click action
- TTL and collapse key

See `node-server/README.md`.

The server requires a Firebase **service account private key** saved locally as:

`node-server/service-account.json`

Never commit that file. It is intentionally ignored by `.gitignore`.

---

## Example `/send` JSON

The APK has a button called **Copy sample Node /send JSON** which automatically inserts the current FCM token.

A more complete request looks like:

```json
{
  "token": "YOUR_FCM_TOKEN",
  "title": "Order update",
  "body": "Order 1234 shipped",
  "deepLink": "fcmtester://order/1234",
  "customSound": true,
  "data": {
    "order_id": "1234",
    "status": "shipped"
  }
}
```

For data-only:

```json
{
  "token": "YOUR_FCM_TOKEN",
  "dataOnly": true,
  "title": "Data test",
  "body": "FCM Tester creates the local notification",
  "data": {
    "type": "sync"
  }
}
```

For silent data-only:

```json
{
  "token": "YOUR_FCM_TOKEN",
  "dataOnly": true,
  "silent": true,
  "data": {
    "type": "background_sync",
    "job_id": "abc123"
  }
}
```

---

## App-state behavior to expect

**Notification payload, app in background:** FCM/Android normally displays it directly. `onMessageReceived()` is generally not called for that notification; if a data payload is also present, its data is delivered through the Activity launch intent when the user taps.

**Notification payload, app in foreground:** `onMessageReceived()` runs; the tester logs it and creates a local notification.

**Data-only payload:** `onMessageReceived()` handles it when Android delivers it. The tester can show a local notification or keep it silent based on the data flags.

---

## Files that matter most

```text
.github/workflows/build-apk.yml
app/src/main/java/com/example/fcmtester/MainActivity.java
app/src/main/java/com/example/fcmtester/FcmService.java
app/src/main/java/com/example/fcmtester/NotificationUtil.java
app/src/main/java/com/example/fcmtester/MessageStore.java
app/src/main/res/raw/fcm_test.wav
node-server/server.js
```
