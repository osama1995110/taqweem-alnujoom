package com.nujoom.calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

/** Schedules prayer notifications sent from the app screen (30 days ahead). */
public final class AlarmScheduler {

    private static final String PREFS = "prayer_alarms";

    private AlarmScheduler() { }

    public static void save(Context c, String json) {
        cancelAll(c);
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("alarms", json).apply();
        scheduleFromPrefs(c);
    }

    public static void scheduleFromPrefs(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = p.getString("alarms", "[]");
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long now = System.currentTimeMillis();
        int count = 0;
        try {
            JSONArray arr = new JSONArray(json);
            count = arr.length();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                long t = o.getLong("t");
                if (t <= now) continue;
                Intent it = new Intent(c, PrayerReceiver.class);
                it.putExtra("name", o.getString("n"));
                PendingIntent pi = PendingIntent.getBroadcast(c, 1000 + i, it,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
                if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
            }
        } catch (Exception ignored) { }
        p.edit().putInt("count", count).apply();
    }

    private static void cancelAll(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        int count = p.getInt("count", 0);
        for (int i = 0; i < count; i++) {
            PendingIntent pi = PendingIntent.getBroadcast(c, 1000 + i, new Intent(c, PrayerReceiver.class),
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) {
                am.cancel(pi);
                pi.cancel();
            }
        }
    }
}
