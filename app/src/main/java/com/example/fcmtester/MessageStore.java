package com.example.fcmtester;

import android.content.Context;
import android.content.SharedPreferences;

final class MessageStore {
    private static final String PREFS = "fcm_tester";
    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_TOKEN = "token";
    private static final int MAX_CHARS = 60000;

    private MessageStore() {}

    static synchronized void append(Context context, String message) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String old = prefs.getString(KEY_MESSAGES, "");
        String combined = message + "\n\n--------------------------------\n\n" + old;
        if (combined.length() > MAX_CHARS) {
            combined = combined.substring(0, MAX_CHARS);
        }
        prefs.edit().putString(KEY_MESSAGES, combined).apply();
    }

    static String getMessages(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_MESSAGES, "No messages received yet.");
    }

    static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(KEY_MESSAGES).apply();
    }

    static void saveToken(Context context, String token) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_TOKEN, token).apply();
    }

    static String getToken(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_TOKEN, "");
    }
}
