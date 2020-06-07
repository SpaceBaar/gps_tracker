package com.spacebaar.gpstracker.activity

import android.content.Context
import android.content.Intent
import androidx.legacy.content.WakefulBroadcastReceiver

// make sure we use a WakefulBroadcastReceiver so that we acquire a partial wakelock
class GpsTrackerAlarmReceiver : WakefulBroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.startService(Intent(context, LocationService::class.java))
    }

    companion object {
        private const val TAG = "GpsTrackerAlarmReceiver"
    }
}