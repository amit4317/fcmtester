package com.example.fcmtester;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class FcmService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        MessageStore.saveToken(this, token);
        MessageStore.append(this, now() + "\nNEW FCM TOKEN\n" + token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = new LinkedHashMap<>(remoteMessage.getData());
        RemoteMessage.Notification notification = remoteMessage.getNotification();

        String title = firstNonEmpty(data, "title");
        String body = firstNonEmpty(data, "body", "message");
        String imageUrl = firstNonEmpty(data, "image", "image_url", "imageUrl");

        if (notification != null) {
            if (isBlank(title)) {
                title = notification.getTitle();
            }
            if (isBlank(body)) {
                body = notification.getBody();
            }
            Uri notificationImage = notification.getImageUrl();
            if (isBlank(imageUrl) && notificationImage != null) {
                imageUrl = notificationImage.toString();
            }
            if (isBlank(data.get("deep_link")) && notification.getLink() != null) {
                data.put("deep_link", notification.getLink().toString());
            }
            if (isBlank(data.get("click_action")) && !isBlank(notification.getClickAction())) {
                data.put("click_action", notification.getClickAction());
            }
            if (isBlank(data.get("sound")) &&
                    ("fcm_test".equalsIgnoreCase(notification.getSound()) ||
                            NotificationUtil.CUSTOM_SOUND_CHANNEL_ID.equals(notification.getChannelId()))) {
                data.put("sound", "custom");
            }
        }

        if (isBlank(title)) {
            title = "FCM message";
        }
        if (isBlank(body)) {
            body = data.isEmpty()
                    ? "Message received. Open FCM Tester for details."
                    : data.toString();
        }

        StringBuilder log = new StringBuilder();
        log.append(now()).append("\nFCM MESSAGE RECEIVED");
        log.append("\nmessageId: ").append(valueOrDash(remoteMessage.getMessageId()));
        log.append("\nfrom: ").append(valueOrDash(remoteMessage.getFrom()));
        log.append("\ncollapseKey: ").append(valueOrDash(remoteMessage.getCollapseKey()));
        log.append("\nsentTime: ").append(remoteMessage.getSentTime());
        log.append("\nttl: ").append(remoteMessage.getTtl());
        log.append("\npriority: ").append(remoteMessage.getPriority());
        log.append("\noriginalPriority: ").append(remoteMessage.getOriginalPriority());

        if (notification != null) {
            log.append("\nnotification.title: ").append(valueOrDash(notification.getTitle()));
            log.append("\nnotification.body: ").append(valueOrDash(notification.getBody()));
            log.append("\nnotification.image: ").append(
                    notification.getImageUrl() == null ? "-" : notification.getImageUrl());
        }

        if (!data.isEmpty()) {
            log.append("\ndata:");
            for (Map.Entry<String, String> entry : data.entrySet()) {
                log.append("\n  ").append(entry.getKey()).append(" = ").append(entry.getValue());
            }
        }

        log.append("\n\nNORMALIZED PAYLOAD JSON (Android SDK view):\n")
                .append(prettyJson(toNormalizedJson(remoteMessage)));

        boolean silent = isTruthy(data.get("silent"))
                || isFalsey(data.get("show_notification"));

        if (silent) {
            log.append("\n\nLocal notification suppressed by payload flag.");
        }

        MessageStore.append(this, log.toString());

        if (!silent) {
            Bitmap image = downloadImage(imageUrl);
            NotificationUtil.show(this, title, body, data, image);
        }
    }

    private JSONObject toNormalizedJson(RemoteMessage message) {
        JSONObject root = new JSONObject();
        try {
            root.put("messageId", message.getMessageId());
            root.put("from", message.getFrom());
            root.put("to", message.getTo());
            root.put("collapseKey", message.getCollapseKey());
            root.put("sentTime", message.getSentTime());
            root.put("ttl", message.getTtl());
            root.put("priority", message.getPriority());
            root.put("originalPriority", message.getOriginalPriority());
            root.put("data", new JSONObject(message.getData()));

            RemoteMessage.Notification n = message.getNotification();
            if (n != null) {
                JSONObject notification = new JSONObject();
                notification.put("title", n.getTitle());
                notification.put("body", n.getBody());
                notification.put("channelId", n.getChannelId());
                notification.put("clickAction", n.getClickAction());
                notification.put("sound", n.getSound());
                notification.put("tag", n.getTag());
                notification.put("color", n.getColor());
                notification.put("imageUrl", n.getImageUrl() == null ? null : n.getImageUrl().toString());
                notification.put("link", n.getLink() == null ? null : n.getLink().toString());
                root.put("notification", notification);
            }
        } catch (Exception ignored) {
            // JSONObject failures are non-fatal for the tester.
        }
        return root;
    }

    private static String prettyJson(JSONObject object) {
        try {
            return object.toString(2);
        } catch (Exception ignored) {
            return object.toString();
        }
    }

    private Bitmap downloadImage(String imageUrl) {
        if (isBlank(imageUrl)) {
            return null;
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(7000);
            connection.setInstanceFollowRedirects(true);
            connection.connect();

            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
                return null;
            }

            try (InputStream input = connection.getInputStream()) {
                return BitmapFactory.decodeStream(input);
            }
        } catch (Exception e) {
            MessageStore.append(this, now() + "\nIMAGE DOWNLOAD FAILED\n" + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static boolean isTruthy(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim().toLowerCase(Locale.US);
        return v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("on");
    }

    private static boolean isFalsey(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim().toLowerCase(Locale.US);
        return v.equals("false") || v.equals("0") || v.equals("no") || v.equals("off");
    }

    private static String firstNonEmpty(Map<String, String> data, String... keys) {
        for (String key : keys) {
            String value = data.get(key);
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static String valueOrDash(String value) {
        return isBlank(value) ? "-" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
    }
}
