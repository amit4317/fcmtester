package com.example.fcmtester;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final Pattern TOPIC_PATTERN = Pattern.compile("[a-zA-Z0-9-_.~%]{1,900}");

    private TextView statusView;
    private TextView tokenView;
    private TextView topicsView;
    private TextView messagesView;
    private EditText topicInput;
    private String currentToken = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        NotificationUtil.ensureChannels(this);
        requestNotificationPermissionIfNeeded();
        recordIntent(getIntent());
        refreshToken();
        refreshTopics();
        refreshMessages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshMessages();
        refreshTopics();
        if (currentToken.isEmpty()) {
            String saved = MessageStore.getToken(this);
            if (!saved.isEmpty()) {
                setToken(saved);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        recordIntent(intent);
        refreshMessages();
    }

    private View buildUi() {
        int p = dp(16);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p, p, p);
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("FCM Tester Pro");
        title.setTextSize(27);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Test tokens, notification + data payloads, data-only/silent messages, topics, images, custom sound, click actions and deep links.");
        subtitle.setTextSize(15);
        subtitle.setPadding(0, dp(8), 0, dp(14));
        root.addView(subtitle);

        statusView = new TextView(this);
        statusView.setText("Getting FCM token…");
        statusView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(statusView);

        tokenView = new TextView(this);
        tokenView.setText("Token not available yet");
        tokenView.setTextIsSelectable(true);
        tokenView.setTypeface(Typeface.MONOSPACE);
        tokenView.setTextSize(13);
        tokenView.setPadding(0, dp(8), 0, dp(10));
        root.addView(tokenView);

        LinearLayout tokenButtons = new LinearLayout(this);
        tokenButtons.setOrientation(LinearLayout.HORIZONTAL);

        Button copy = new Button(this);
        copy.setText("Copy token");
        copy.setOnClickListener(v -> copyToken());
        tokenButtons.addView(copy, new LinearLayout.LayoutParams(0, dp(52), 1));

        Button refresh = new Button(this);
        refresh.setText("Refresh token");
        refresh.setOnClickListener(v -> refreshToken());
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(0, dp(52), 1);
        refreshParams.setMarginStart(dp(8));
        tokenButtons.addView(refresh, refreshParams);
        root.addView(tokenButtons);

        Button sample = new Button(this);
        sample.setText("Copy sample Node /send JSON");
        sample.setOnClickListener(v -> copySampleJson());
        LinearLayout.LayoutParams sampleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        sampleParams.topMargin = dp(8);
        root.addView(sample, sampleParams);

        TextView topicTitle = sectionTitle("Topic subscription");
        root.addView(topicTitle);

        topicInput = new EditText(this);
        topicInput.setSingleLine(true);
        topicInput.setHint("Topic name, e.g. news");
        topicInput.setText("news");
        root.addView(topicInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));

        LinearLayout topicButtons = new LinearLayout(this);
        topicButtons.setOrientation(LinearLayout.HORIZONTAL);

        Button subscribe = new Button(this);
        subscribe.setText("Subscribe");
        subscribe.setOnClickListener(v -> changeTopic(true));
        topicButtons.addView(subscribe, new LinearLayout.LayoutParams(0, dp(52), 1));

        Button unsubscribe = new Button(this);
        unsubscribe.setText("Unsubscribe");
        unsubscribe.setOnClickListener(v -> changeTopic(false));
        LinearLayout.LayoutParams unsubscribeParams = new LinearLayout.LayoutParams(0, dp(52), 1);
        unsubscribeParams.setMarginStart(dp(8));
        topicButtons.addView(unsubscribe, unsubscribeParams);
        root.addView(topicButtons);

        topicsView = new TextView(this);
        topicsView.setTypeface(Typeface.MONOSPACE);
        topicsView.setTextSize(13);
        topicsView.setPadding(0, dp(8), 0, 0);
        root.addView(topicsView);

        TextView keysTitle = sectionTitle("Payload controls supported by this tester");
        root.addView(keysTitle);

        TextView keys = new TextView(this);
        keys.setTextIsSelectable(true);
        keys.setTypeface(Typeface.MONOSPACE);
        keys.setTextSize(12);
        keys.setText(
                "data.image / image_url = https://...\n" +
                "data.deep_link = fcmtester://message/123\n" +
                "data.sound = custom\n" +
                "data.silent = true\n" +
                "data.show_notification = false\n" +
                "data.tag = same-tag\n\n" +
                "For system-rendered background notifications, use:\n" +
                "android.notification.channelId = fcm_tester_custom\n" +
                "android.notification.sound = fcm_test\n" +
                "android.notification.clickAction = OPEN_FCM_TEST"
        );
        root.addView(keys);

        TextView receivedTitle = sectionTitle("Received / opened messages");
        root.addView(receivedTitle);

        messagesView = new TextView(this);
        messagesView.setTextIsSelectable(true);
        messagesView.setTypeface(Typeface.MONOSPACE);
        messagesView.setTextSize(12);
        messagesView.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.addView(messagesView);

        Button clear = new Button(this);
        clear.setText("Clear message log");
        clear.setOnClickListener(v -> {
            MessageStore.clear(this);
            refreshMessages();
        });
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        clearParams.topMargin = dp(10);
        root.addView(clear, clearParams);

        TextView note = new TextView(this);
        note.setText("Note: the JSON log is a normalized view of what the Android FCM SDK exposes. It is not the byte-for-byte original HTTP request sent by your server. For reliable data-only testing, send Android priority=high.");
        note.setPadding(0, dp(18), 0, dp(24));
        root.addView(note);

        return scroll;
    }

    private TextView sectionTitle(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(18);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(0, dp(22), 0, dp(8));
        return view;
    }

    private void refreshToken() {
        statusView.setText("Getting FCM token…");
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                String message = task.getException() == null
                        ? "Unknown error"
                        : task.getException().getMessage();
                statusView.setText("Could not get token: " + message);
                return;
            }
            String token = task.getResult();
            MessageStore.saveToken(this, token);
            setToken(token);
        });
    }

    private void setToken(String token) {
        currentToken = token == null ? "" : token;
        tokenView.setText(currentToken.isEmpty() ? "Token not available yet" : currentToken);
        statusView.setText(currentToken.isEmpty()
                ? "FCM token not available"
                : "FCM token ready — copy it and send a test message");
    }

    private void copyToken() {
        if (currentToken.isEmpty()) {
            Toast.makeText(this, "Token is not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }
        copyToClipboard("FCM token", currentToken);
        Toast.makeText(this, "FCM token copied", Toast.LENGTH_SHORT).show();
    }

    private void copySampleJson() {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "tester_demo");
            data.put("order_id", "1234");
            data.put("deep_link", "fcmtester://message/1234");
            data.put("sound", "custom");

            JSONObject root = new JSONObject();
            root.put("token", currentToken.isEmpty() ? "PASTE_FCM_TOKEN" : currentToken);
            root.put("title", "FCM Tester Pro");
            root.put("body", "Node.js test notification");
            root.put("data", data);

            copyToClipboard("FCM sample JSON", root.toString(2));
            Toast.makeText(this, "Sample JSON copied", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not create sample JSON", Toast.LENGTH_SHORT).show();
        }
    }

    private void changeTopic(boolean subscribe) {
        String topic = normalizeTopic(topicInput.getText().toString());
        if (topic == null) {
            Toast.makeText(this, "Invalid topic. Use letters, numbers, - _ . ~ %", Toast.LENGTH_LONG).show();
            return;
        }

        if (subscribe) {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            MessageStore.addTopic(this, topic);
                            MessageStore.append(this, now() + "\nSUBSCRIBED TO TOPIC\n" + topic);
                            Toast.makeText(this, "Subscribed to " + topic, Toast.LENGTH_SHORT).show();
                        } else {
                            logTopicError("subscribe", topic, task.getException());
                        }
                        refreshTopics();
                        refreshMessages();
                    });
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            MessageStore.removeTopic(this, topic);
                            MessageStore.append(this, now() + "\nUNSUBSCRIBED FROM TOPIC\n" + topic);
                            Toast.makeText(this, "Unsubscribed from " + topic, Toast.LENGTH_SHORT).show();
                        } else {
                            logTopicError("unsubscribe", topic, task.getException());
                        }
                        refreshTopics();
                        refreshMessages();
                    });
        }
    }

    private void logTopicError(String action, String topic, Exception exception) {
        String message = exception == null ? "Unknown error" : exception.getMessage();
        MessageStore.append(this, now() + "\nTOPIC " + action.toUpperCase(Locale.US)
                + " FAILED\n" + topic + "\n" + message);
        Toast.makeText(this, "Topic operation failed: " + message, Toast.LENGTH_LONG).show();
    }

    private void refreshTopics() {
        if (topicsView == null) {
            return;
        }
        List<String> topics = MessageStore.getTopics(this);
        if (topics.isEmpty()) {
            topicsView.setText("Saved subscriptions: none");
        } else {
            topicsView.setText("Saved subscriptions: " + join(topics));
        }
    }

    private void refreshMessages() {
        if (messagesView != null) {
            messagesView.setText(MessageStore.getMessages(this));
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private void recordIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        boolean hasInterestingData = intent.getData() != null
                || intent.getAction() != null
                || (intent.getExtras() != null && !intent.getExtras().isEmpty());
        if (!hasInterestingData) {
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append(now()).append("\nAPP OPENED / INTENT RECEIVED");
        text.append("\naction = ").append(String.valueOf(intent.getAction()));

        Uri data = intent.getData();
        if (data != null) {
            text.append("\ndeepLink = ").append(data);
        }

        if (intent.getCategories() != null && !intent.getCategories().isEmpty()) {
            text.append("\ncategories = ").append(intent.getCategories());
        }

        if (intent.getExtras() != null && !intent.getExtras().isEmpty()) {
            text.append("\nextras:");
            for (String key : intent.getExtras().keySet()) {
                Object value = intent.getExtras().get(key);
                text.append("\n  ").append(key).append(" = ").append(String.valueOf(value));
            }
        }
        MessageStore.append(this, text.toString());
    }

    private String normalizeTopic(String raw) {
        if (raw == null) {
            return null;
        }
        String topic = raw.trim();
        if (topic.startsWith("/topics/")) {
            topic = topic.substring("/topics/".length());
        }
        return TOPIC_PATTERN.matcher(topic).matches() ? topic : null;
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text));
    }

    private String join(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(values.get(i));
        }
        return sb.toString();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
    }
}
