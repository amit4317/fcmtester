package com.example.fcmtester;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView statusView;
    private TextView tokenView;
    private TextView messagesView;
    private String currentToken = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        NotificationUtil.ensureChannel(this);
        requestNotificationPermissionIfNeeded();
        recordIntentExtras(getIntent());
        refreshToken();
        refreshMessages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshMessages();
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
        recordIntentExtras(intent);
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
        title.setText("FCM Tester");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("This APK is connected to the Firebase project from google-services.json. Copy the token below and send FCM to it.");
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

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Button copy = new Button(this);
        copy.setText("Copy token");
        copy.setOnClickListener(v -> copyToken());
        buttons.addView(copy, new LinearLayout.LayoutParams(0, dp(52), 1));

        Button refresh = new Button(this);
        refresh.setText("Refresh");
        refresh.setOnClickListener(v -> refreshToken());
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(0, dp(52), 1);
        refreshParams.setMarginStart(dp(8));
        buttons.addView(refresh, refreshParams);

        root.addView(buttons);

        TextView receivedTitle = new TextView(this);
        receivedTitle.setText("Received / opened messages");
        receivedTitle.setTextSize(18);
        receivedTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        receivedTitle.setPadding(0, dp(22), 0, dp(8));
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
        note.setText("Tip: for an easy test, open this app once, allow notifications, then put it in the background and send a Firebase Console test notification to the token above.");
        note.setPadding(0, dp(18), 0, dp(24));
        root.addView(note);

        return scroll;
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
        currentToken = token;
        tokenView.setText(token);
        statusView.setText("FCM token ready — copy it and send a test message");
    }

    private void copyToken() {
        if (currentToken.isEmpty()) {
            Toast.makeText(this, "Token is not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("FCM token", currentToken));
        Toast.makeText(this, "FCM token copied", Toast.LENGTH_SHORT).show();
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

    private void recordIntentExtras(Intent intent) {
        if (intent == null || intent.getExtras() == null || intent.getExtras().isEmpty()) {
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append(now()).append("\nAPP OPENED WITH EXTRAS");
        for (String key : intent.getExtras().keySet()) {
            Object value = intent.getExtras().get(key);
            text.append("\n").append(key).append(" = ").append(String.valueOf(value));
        }
        MessageStore.append(this, text.toString());
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
    }
}
