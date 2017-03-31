package com.sheelainfotech.gpstracker.activity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;

public class GpsTrackerBootReceiver extends BroadcastReceiver {
    private static final String TAG = "GpsTrackerBootReceiver";

    public void onReceive(Context context, Intent intent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, GpsTrackerAlarmReceiver.class), 0);
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.sheelainfotech.gpstracker.prefs", 0);
        int intervalInMinutes = sharedPreferences.getInt("intervalInMinutes", 1);
        if (Boolean.valueOf(sharedPreferences.getBoolean("currentlyTracking", false)).booleanValue()) {
            alarmManager.setRepeating(2, SystemClock.elapsedRealtime(), (long) (60000 * intervalInMinutes), pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
        }
    }
}
