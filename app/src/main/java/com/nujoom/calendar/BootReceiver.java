package com.nujoom.calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Restores prayer notifications after the phone restarts. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        AlarmScheduler.scheduleFromPrefs(c);
    }
}
