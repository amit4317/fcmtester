package com.example.fcmtester;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class MessageStore {
    private static final String PREFS = "fcm_tester";
    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_TOPICS = "topics";
    private static final int MAX_CHARS = 120000;

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

    static synchronized void addTopic(Context context, String topic) {
        Set<String> topics = new HashSet<>(getTopicSet(context));
        topics.add(topic);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putStringSet(KEY_TOPICS, topics).apply();
    }

    static synchronized void removeTopic(Context context, String topic) {
        Set<String> topics = new HashSet<>(getTopicSet(context));
        topics.remove(topic);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putStringSet(KEY_TOPICS, topics).apply();
    }

    static List<String> getTopics(Context context) {
        List<String> result = new ArrayList<>(getTopicSet(context));
        Collections.sort(result);
        return result;
    }

    private static Set<String> getTopicSet(Context context) {
        Set<String> saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getStringSet(KEY_TOPICS, Collections.emptySet());
        return saved == null ? Collections.emptySet() : new HashSet<>(saved);
    }
}
