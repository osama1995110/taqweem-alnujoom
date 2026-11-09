package com.nujoom.calendar;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Shows the "time for prayer" notification. */
public class PrayerReceiver extends BroadcastReceiver {

    private static final String CHANNEL = "prayer_times";

    @Override
    public void onReceive(Context c, Intent intent) {
        String name = intent.getStringExtra("name");
        if (name == null) name = "";
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "مواقيت الصلاة",
                    NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("تنبيه عند دخول وقت كل صلاة");
            ch.enableVibration(true);
            nm.createNotificationChannel(ch);
        }

        PendingIntent open = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, CHANNEL)
                : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_notif)
                .setContentTitle("حان الآن موعد صلاة " + name)
                .setContentText("حسب التوقيت المحلي لموقعك")
                .setContentIntent(open)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER);
        if (Build.VERSION.SDK_INT < 26) {
            b.setDefaults(Notification.DEFAULT_ALL).setPriority(Notification.PRIORITY_HIGH);
        }
        try {
            nm.notify((int) (System.currentTimeMillis() % 100000), b.build());
        } catch (SecurityException ignored) { }
    }
}
