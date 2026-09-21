# Optional Node.js / Express sender

1. In Firebase Console open **Project settings -> Service accounts** and generate a private key.
2. Save it here as `service-account.json`.
3. Never commit that file.
4. Run:

```bash
npm install
npm start
```

The server listens on `http://localhost:3000`.

## Basic notification

```bash
curl -X POST http://localhost:3000/send \
  -H "Content-Type: application/json" \
  -d '{
    "token":"YOUR_FCM_TOKEN",
    "title":"Hello",
    "body":"Basic notification"
  }'
```

## Notification + custom data + deep link + bundled custom sound

```bash
curl -X POST http://localhost:3000/send \
  -H "Content-Type: application/json" \
  -d '{
    "token":"YOUR_FCM_TOKEN",
    "title":"Order update",
    "body":"Tap to inspect the deep link",
    "deepLink":"fcmtester://order/1234",
    "customSound":true,
    "data":{
      "order_id":"1234",
      "status":"shipped"
    }
  }'
```

## Data-only but visible locally

```bash
curl -X POST http://localhost:3000/send \
  -H "Content-Type: application/json" \
  -d '{
    "token":"YOUR_FCM_TOKEN",
    "dataOnly":true,
    "title":"Data only",
    "body":"The tester created this notification",
    "data":{"type":"data_only"}
  }'
```

## Silent data-only

```bash
curl -X POST http://localhost:3000/send \
  -H "Content-Type: application/json" \
  -d '{
    "token":"YOUR_FCM_TOKEN",
    "dataOnly":true,
    "silent":true,
    "data":{"type":"silent_sync","job_id":"abc123"}
  }'
```

No local notification is shown. Open FCM Tester Pro and check the message log.

## Image notification

Use a publicly reachable HTTPS image URL:

```bash
curl -X POST http://localhost:3000/send \
  -H "Content-Type: application/json" \
  -d '{
    "token":"YOUR_FCM_TOKEN",
    "title":"Image test",
    "body":"Expand the notification",
    "imageUrl":"https://YOUR-DOMAIN.EXAMPLE/test.jpg"
  }'
```

## System click-action test

Set the tester in the background and send:

```json
{
  "token": "YOUR_FCM_TOKEN",
  "title": "Click action",
  "body": "Tap me",
  "clickAction": "OPEN_FCM_TEST"
}
```

The Android manifest contains an activity intent filter for `OPEN_FCM_TEST`.

## Topic test

First subscribe to `news` inside the APK, then:

```bash
curl -X POST http://localhost:3000/send-topic \
  -H "Content-Type: application/json" \
  -d '{
    "topic":"news",
    "title":"Topic message",
    "body":"Sent to every subscriber"
  }'
```
