package com.example.fcmtester;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
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

        StringBuilder log = new StringBuilder();
        log.append(now()).append("\nFCM MESSAGE RECEIVED");
        if (remoteMessage.getMessageId() != null) {
            log.append("\nmessageId: ").append(remoteMessage.getMessageId());
        }
        if (remoteMessage.getFrom() != null) {
            log.append("\nfrom: ").append(remoteMessage.getFrom());
        }

        String title = "FCM message";
        String body = "Message received. Open FCM Tester for details.";

        RemoteMessage.Notification notification = remoteMessage.getNotification();
        if (notification != null) {
            if (notification.getTitle() != null && !notification.getTitle().isEmpty()) {
                title = notification.getTitle();
            }
            if (notification.getBody() != null && !notification.getBody().isEmpty()) {
                body = notification.getBody();
            }
            log.append("\nnotification.title: ").append(notification.getTitle());
            log.append("\nnotification.body: ").append(notification.getBody());
        }

        Map<String, String> data = remoteMessage.getData();
        if (!data.isEmpty()) {
            log.append("\ndata:");
            for (Map.Entry<String, String> entry : data.entrySet()) {
                log.append("\n  ").append(entry.getKey()).append(" = ").append(entry.getValue());
            }
            if (notification == null) {
                if (data.get("title") != null && !data.get("title").isEmpty()) {
                    title = data.get("title");
                }
                if (data.get("body") != null && !data.get("body").isEmpty()) {
                    body = data.get("body");
                } else {
                    body = data.toString();
                }
            }
        }

        MessageStore.append(this, log.toString());
        NotificationUtil.show(this, title, body, data);
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
    }
}
