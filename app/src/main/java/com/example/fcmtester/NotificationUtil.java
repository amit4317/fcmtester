package com.example.fcmtester;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import java.util.Locale;
import java.util.Map;

final class NotificationUtil {
    static final String DEFAULT_CHANNEL_ID = "fcm_tester";
    static final String CUSTOM_SOUND_CHANNEL_ID = "fcm_tester_custom";

    private NotificationUtil() {}

    static void ensureChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager.getNotificationChannel(DEFAULT_CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                    DEFAULT_CHANNEL_ID,
                    "FCM test messages",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Normal notifications received by FCM Tester");
            manager.createNotificationChannel(channel);
        }

        if (manager.getNotificationChannel(CUSTOM_SOUND_CHANNEL_ID) == null) {
            Uri soundUri = customSoundUri(context);
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            NotificationChannel channel = new NotificationChannel(
                    CUSTOM_SOUND_CHANNEL_ID,
                    "FCM custom sound",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("FCM test messages that use the bundled custom sound");
            channel.setSound(soundUri, attributes);
            manager.createNotificationChannel(channel);
        }
    }

    static void show(
            Context context,
            String title,
            String body,
            Map<String, String> data,
            Bitmap image
    ) {
        ensureChannels(context);

        boolean customSound = isCustomSound(data);
        String channelId = customSound ? CUSTOM_SOUND_CHANNEL_ID : DEFAULT_CHANNEL_ID;

        Intent openIntent = buildOpenIntent(context, data);
        int requestCode = (int) (System.currentTimeMillis() & 0x7fffffff);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, channelId);
        } else {
            builder = new Notification.Builder(context)
                    .setPriority(Notification.PRIORITY_HIGH);
            if (customSound) {
                builder.setSound(customSoundUri(context));
            }
        }

        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (image != null) {
            builder.setLargeIcon(image);
            builder.setStyle(new Notification.BigPictureStyle()
                    .bigPicture(image)
                    .bigLargeIcon((Bitmap) null)
                    .setBigContentTitle(title)
                    .setSummaryText(body));
        } else {
            builder.setStyle(new Notification.BigTextStyle().bigText(body));
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String tag = data == null ? null : trimToNull(data.get("tag"));
        if (tag != null) {
            manager.notify(tag, requestCode, builder.build());
        } else {
            manager.notify(requestCode, builder.build());
        }
    }

    private static Intent buildOpenIntent(Context context, Map<String, String> data) {
        String deepLink = firstNonEmpty(data, "deep_link", "link", "url");
        Intent intent;

        if (deepLink != null) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(deepLink), context, MainActivity.class);
        } else {
            intent = new Intent(context, MainActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                intent.putExtra("data." + entry.getKey(), entry.getValue());
            }
        }
        intent.putExtra("opened_from_local_notification", true);
        return intent;
    }

    private static boolean isCustomSound(Map<String, String> data) {
        if (data == null) {
            return false;
        }
        String sound = trimToNull(data.get("sound"));
        if (sound != null && (sound.equalsIgnoreCase("custom") || sound.equalsIgnoreCase("fcm_test"))) {
            return true;
        }
        String channel = trimToNull(data.get("channel"));
        return channel != null && channel.toLowerCase(Locale.US).contains("custom");
    }

    private static Uri customSoundUri(Context context) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + context.getPackageName() + "/" + R.raw.fcm_test);
    }

    private static String firstNonEmpty(Map<String, String> data, String... keys) {
        if (data == null) {
            return null;
        }
        for (String key : keys) {
            String value = trimToNull(data.get(key));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
