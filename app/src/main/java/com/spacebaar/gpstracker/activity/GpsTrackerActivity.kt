package com.spacebaar.gpstracker.activity

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.spacebaar.gpstracker.R
import com.spacebaar.gpstracker.helper.SQLiteHandler
import com.spacebaar.gpstracker.helper.SessionManager
import java.util.*

open class GpsTrackerActivity : AppCompatActivity() {
    private var defaultUploadWebsite: String? = null
    private var email: String? = null
    private var txtEmail: TextView? = null
    private var txtWebsite: TextView? = null
    private var trackingButton: Button? = null
    private var currentlyTracking = false
    private var intervalRadioGroup: RadioGroup? = null
    private var intervalInMinutes = 1
    private var db: SQLiteHandler? = null
    private var session: SessionManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gps_tracker)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.setLogo(R.mipmap.ic_launcher)
        supportActionBar!!.setDisplayUseLogoEnabled(true)
        defaultUploadWebsite = getString(R.string.default_upload_website)
        txtWebsite = findViewById(R.id.txtWebsite)
        txtEmail = findViewById(R.id.email)
        intervalRadioGroup = findViewById(R.id.intervalRadioGroup)
        trackingButton = findViewById(R.id.trackingButton)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // SqLite database handler
        db = SQLiteHandler(applicationContext)

        // session manager
        session = SessionManager(applicationContext)
        if (!session!!.isLoggedIn) {
            logoutUser()
        }

        // Fetching user details from SQLite
        val user = db!!.userDetails
        email = user["email"]
        txtEmail!!.text = email
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    MY_PERMISSION_ACCESS_COARSE_LOCATION)
        }
        val sharedPreferences = getSharedPreferences("com.spacebaar.gpstracker.activity.gpstracker.prefs", Context.MODE_PRIVATE)
        currentlyTracking = sharedPreferences.getBoolean("currentlyTracking", false)
        val firstTimeLoadingApp = sharedPreferences.getBoolean("firstTimeLoadingApp", true)
        if (firstTimeLoadingApp) {
            val editor = sharedPreferences.edit()
            editor.putBoolean("firstTimeLoadingApp", false)
            editor.putString("appID", UUID.randomUUID().toString())
            editor.apply()
        }
        intervalRadioGroup!!.setOnCheckedChangeListener { _, _ -> saveInterval() }
        trackingButton!!.setOnClickListener { view -> trackLocation(view) }
        btnLogout.setOnClickListener { logoutUser() }
        // Logout button click event
        btnLogout.setOnClickListener { logoutUser() }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == MY_PERMISSION_ACCESS_COARSE_LOCATION) { // If request is cancelled, the result arrays are empty.
            if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission was granted, yay! Do the
                // contacts-related task you need to do.
            } else {

                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private fun saveInterval() {
        if (currentlyTracking) {
            Toast.makeText(applicationContext, R.string.user_needs_to_restart_tracking, Toast.LENGTH_LONG).show()
        }
        val sharedPreferences = getSharedPreferences("com.spacebaar.gpstracker.activity.gpstracker.prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        when (intervalRadioGroup!!.checkedRadioButtonId) {
            R.id.i1 -> editor.putInt("intervalInMinutes", 1)
            R.id.i5 -> editor.putInt("intervalInMinutes", 5)
            R.id.i15 -> editor.putInt("intervalInMinutes", 15)
        }
        editor.apply()
    }

    private fun startAlarmManager() {
        Log.d(TAG, "startAlarmManager")
        val context = baseContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val gpsTrackerIntent = Intent(context, GpsTrackerAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, gpsTrackerIntent, 0)
        val sharedPreferences = getSharedPreferences("com.spacebaar.gpstracker.activity.gpstracker.prefs", Context.MODE_PRIVATE)
        intervalInMinutes = sharedPreferences.getInt("intervalInMinutes", 1)
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                intervalInMinutes * 60000.toLong(),  // 60000 = 1 minute
                pendingIntent)
    }

    private fun cancelAlarmManager() {
        Log.d(TAG, "cancelAlarmManager")
        val context = baseContext
        val gpsTrackerIntent = Intent(context, GpsTrackerAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, gpsTrackerIntent, 0)
        val alarmManager = (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        alarmManager.cancel(pendingIntent)
    }

    // called when trackingButton is tapped
    private fun trackLocation(v: View?) {
        val sharedPreferences = getSharedPreferences("com.spacebaar.gpstracker.activity.gpstracker.prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        if (!statusCheck()) {
            return
        }
        if (!saveUserSettings()) {
            return
        }
        if (!checkIfGooglePlayEnabled()) {
            return
        }
        if (currentlyTracking) {
            cancelAlarmManager()
            currentlyTracking = false
            editor.putBoolean("currentlyTracking", false)
            editor.putString("sessionID", "")
        } else {
            startAlarmManager()
            currentlyTracking = true
            editor.putBoolean("currentlyTracking", true)
            editor.putFloat("totalDistanceInMeters", 0f)
            editor.putBoolean("firstTimeGettingPosition", true)
            editor.putString("sessionID", UUID.randomUUID().toString())
        }
        editor.apply()
        setTrackingButtonState()
    }

    private fun saveUserSettings(): Boolean {
        if (textFieldsAreEmptyOrHaveSpaces()) {
            return false
        }
        val sharedPreferences = getSharedPreferences("com.spacebaar.gpstracker.activity.gpstracker.prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        when (intervalRadioGroup!!.checkedRadioButtonId) {
            R.id.i1 -> editor.putInt("intervalInMinutes", 1)
            R.id.i5 -> editor.putInt("intervalInMinutes", 5)
            R.id.i15 -> editor.putInt("intervalInMinutes", 15)
        }

        //editor.putString("userName", txtUserName.getText().toString().trim());
        editor.putString("defaultUploadWebsite", txtWebsite!!.text.toString().trim { it <= ' ' })
        editor.apply()
        return true
    }

    private fun textFieldsAreEmptyOrHaveSpaces(): Boolean {
        val tempUserName = txtEmail!!.text.toString().trim { it <= ' ' }
        val tempWebsite = txtWebsite!!.text.toString().trim { it <= ' ' }
        if (tempWebsite.isEmpty() || hasSpaces(tempWebsite) || tempUserName.isEmpty() || hasSpaces(tempUserName)) {
            Toast.makeText(this, R.string.textfields_empty_or_spaces, Toast.LENGTH_LONG).show()
            return true
        }
        return false
    }

    private fun hasSpaces(str: String): Boolean {
        return str.split(" ".toRegex()).toTypedArray().size > 1
    }

    private fun displayUserSettings() {
        val sharedPreferences = getSharedPreferences("com.spacebaar.gpstracker.activity.gpstracker.prefs", Context.MODE_PRIVATE)
        intervalInMinutes = sharedPreferences.getInt("intervalInMinutes", 1)
        when (intervalInMinutes) {
            1 -> intervalRadioGroup!!.check(R.id.i1)
            5 -> intervalRadioGroup!!.check(R.id.i5)
            15 -> intervalRadioGroup!!.check(R.id.i15)
        }
        txtWebsite!!.text = sharedPreferences.getString("defaultUploadWebsite", defaultUploadWebsite)
        txtEmail!!.text = sharedPreferences.getString("email", email)
    }

    private fun checkIfGooglePlayEnabled(): Boolean {
        return if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            true
        } else {
            Log.e(TAG, "unable to connect to google play services.")
            Toast.makeText(applicationContext, R.string.google_play_services_unavailable, Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun setTrackingButtonState() {
        if (currentlyTracking) {
            trackingButton!!.setBackgroundResource(R.drawable.green_tracking_button)
            trackingButton!!.setTextColor(Color.BLACK)
            trackingButton!!.setText(R.string.tracking_is_on)
        } else {
            trackingButton!!.setBackgroundResource(R.drawable.red_tracking_button)
            trackingButton!!.setTextColor(Color.WHITE)
            trackingButton!!.setText(R.string.tracking_is_off)
        }
    }

    private fun statusCheck(): Boolean {
        val manager = (getSystemService(Context.LOCATION_SERVICE) as LocationManager)
        return if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
            false
        } else {
            true
        }
    }

    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                .setNegativeButton("No") { dialog, id -> dialog.cancel() }
        val alert = builder.create()
        alert.show()
    }

    public override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
        displayUserSettings()
        setTrackingButtonState()
    }

    private fun logoutUser() {
        val sharedPreferences = getSharedPreferences("com.spacebaar.gpstracker.activity.gpstracker.prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        session!!.setLogin(false)
        currentlyTracking = false
        editor.putBoolean("currentlyTracking", false)
        editor.putString("sessionID", "")
        editor.apply()
        setTrackingButtonState()
        if (currentlyTracking) {
            trackingButton!!.setBackgroundResource(R.drawable.green_tracking_button)
            trackingButton!!.setTextColor(Color.BLACK)
            trackingButton!!.setText(R.string.tracking_is_on)
        } else {
            trackingButton!!.setBackgroundResource(R.drawable.red_tracking_button)
            trackingButton!!.setTextColor(Color.WHITE)
            trackingButton!!.setText(R.string.tracking_is_off)
        }
        cancelAlarmManager()
        db!!.deleteUsers()

        // Launching the login activity
        val intent = Intent(this@GpsTrackerActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "GpsTrackerActivity"
        private const val MY_PERMISSION_ACCESS_COARSE_LOCATION = 11
    }
}