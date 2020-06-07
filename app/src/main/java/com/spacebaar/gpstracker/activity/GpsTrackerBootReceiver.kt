package com.spacebaar.gpstracker.activity

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

class GpsTrackerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(context, 0, Intent(context, GpsTrackerAlarmReceiver::class.java), 0)
        val sharedPreferences = context.getSharedPreferences("com.spacebaar.gpstracker.prefs", 0)
        val intervalInMinutes = sharedPreferences.getInt("intervalInMinutes", 1)
        if (sharedPreferences.getBoolean("currentlyTracking", false)) {
            alarmManager.setRepeating(2, SystemClock.elapsedRealtime(), 60000 * intervalInMinutes.toLong(), pendingIntent)
        } else {
            assert(alarmManager != null)
            alarmManager.cancel(pendingIntent)
        }
    }
}