package com.sheelainfotech.gpstracker.activity;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.HashMap;
import java.util.UUID;

import com.sheelainfotech.gpstracker.R;
import com.sheelainfotech.gpstracker.helper.SQLiteHandler;
import com.sheelainfotech.gpstracker.helper.SessionManager;

public class GpsTrackerActivity extends AppCompatActivity {
    private static final String TAG = "GpsTrackerActivity";

    // use the websmithing defaultUploadWebsite for testing and then check your
    // location with your browser here: https://www.websmithing.com/gpstracker/displaymap.php
    private String defaultUploadWebsite;
    private String email;

    private TextView txtEmail;
    private TextView txtWebsite;
    private Button trackingButton;

    private boolean currentlyTracking;
    private RadioGroup intervalRadioGroup;
    private int intervalInMinutes = 1;
    private AlarmManager alarmManager;
    private Intent gpsTrackerIntent;
    private PendingIntent pendingIntent;
    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    private Button btnLogout;
    private SQLiteHandler db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_gps_tracker);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.mipmap.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);

        defaultUploadWebsite = getString(R.string.default_upload_website);

        txtWebsite = (TextView)findViewById(R.id.txtWebsite);
        txtEmail = (TextView) findViewById(R.id.email);
        intervalRadioGroup = (RadioGroup)findViewById(R.id.intervalRadioGroup);
        trackingButton = (Button)findViewById(R.id.trackingButton);
        //txtUserName.setImeOptions(EditorInfo.IME_ACTION_DONE);
        btnLogout = (Button) findViewById(R.id.btnLogout);

        // SqLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // session manager
        session = new SessionManager(getApplicationContext());

        if (!session.isLoggedIn()) {
            logoutUser();
        }

        // Fetching user details from SQLite
        HashMap<String, String> user = db.getUserDetails();

        email = user.get("email");

        txtEmail.setText(email);

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSION_ACCESS_COARSE_LOCATION);

        }

        SharedPreferences sharedPreferences = this.getSharedPreferences("com.sheelainfotech.gpstracker.activity.gpstracker.prefs", Context.MODE_PRIVATE);
        currentlyTracking = sharedPreferences.getBoolean("currentlyTracking", false);

        boolean firstTimeLoadingApp = sharedPreferences.getBoolean("firstTimeLoadingApp", true);
        if (firstTimeLoadingApp) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("firstTimeLoadingApp", false);
            editor.putString("appID",  UUID.randomUUID().toString());
            editor.apply();
        }

        intervalRadioGroup.setOnCheckedChangeListener(
                new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup radioGroup, int i) {
                        saveInterval();
                    }
                });

        trackingButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                trackLocation(view);
            }
        });
        btnLogout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });
        // Logout button click event
        btnLogout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void saveInterval() {
        if (currentlyTracking) {
            Toast.makeText(getApplicationContext(), R.string.user_needs_to_restart_tracking, Toast.LENGTH_LONG).show();
        }

        SharedPreferences sharedPreferences = this.getSharedPreferences("com.sheelainfotech.gpstracker.activity.gpstracker.prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        switch (intervalRadioGroup.getCheckedRadioButtonId()) {
            case R.id.i1:
                editor.putInt("intervalInMinutes", 1);
                break;
            case R.id.i5:
                editor.putInt("intervalInMinutes", 5);
                break;
            case R.id.i15:
                editor.putInt("intervalInMinutes", 15);
                break;
        }

        editor.apply();
    }

    private void startAlarmManager() {
        Log.d(TAG, "startAlarmManager");

        Context context = getBaseContext();
        alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        gpsTrackerIntent = new Intent(context, GpsTrackerAlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(context, 0, gpsTrackerIntent, 0);

        SharedPreferences sharedPreferences = this.getSharedPreferences("com.sheelainfotech.gpstracker.activity.gpstracker.prefs", Context.MODE_PRIVATE);
        intervalInMinutes = sharedPreferences.getInt("intervalInMinutes", 1);

        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                intervalInMinutes * 60000, // 60000 = 1 minute
                pendingIntent);
    }

    private void cancelAlarmManager() {
        Log.d(TAG, "cancelAlarmManager");

        Context context = getBaseContext();
        Intent gpsTrackerIntent = new Intent(context, GpsTrackerAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, gpsTrackerIntent, 0);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    // called when trackingButton is tapped
    protected void trackLocation(View v) {
        SharedPreferences sharedPreferences = this.getSharedPreferences("com.sheelainfotech.gpstracker.activity.gpstracker.prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (!statusCheck()) {
            return;
        }
        if (!saveUserSettings()) {
            return;
        }

        if (!checkIfGooglePlayEnabled()) {
            return;
        }


        if (currentlyTracking) {
            cancelAlarmManager();

            currentlyTracking = false;
            editor.putBoolean("currentlyTracking", false);
            editor.putString("sessionID", "");
        } else {
            startAlarmManager();

            currentlyTracking = true;
            editor.putBoolean("currentlyTracking", true);
            editor.putFloat("totalDistanceInMeters", 0f);
            editor.putBoolean("firstTimeGettingPosition", true);
            editor.putString("sessionID",  UUID.randomUUID().toString());
        }

        editor.apply();
        setTrackingButtonState();
    }

    private boolean saveUserSettings() {
        if (textFieldsAreEmptyOrHaveSpaces()) {
            return false;
        }

        SharedPreferences sharedPreferences = this.getSharedPreferences("com.sheelainfotech.gpstracker.activity.gpstracker.prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        switch (intervalRadioGroup.getCheckedRadioButtonId()) {
            case R.id.i1:
                editor.putInt("intervalInMinutes", 1);
                break;
            case R.id.i5:
                editor.putInt("intervalInMinutes", 5);
                break;
            case R.id.i15:
                editor.putInt("intervalInMinutes", 15);
                break;
        }

        //editor.putString("userName", txtUserName.getText().toString().trim());
        editor.putString("defaultUploadWebsite", txtWebsite.getText().toString().trim());

        editor.apply();

        return true;
    }

    private boolean textFieldsAreEmptyOrHaveSpaces() {
        String tempUserName = txtEmail.getText().toString().trim();
        String tempWebsite = txtWebsite.getText().toString().trim();

        if (tempWebsite.length() == 0 || hasSpaces(tempWebsite) || tempUserName.length() == 0 || hasSpaces(tempUserName)) {
            Toast.makeText(this, R.string.textfields_empty_or_spaces, Toast.LENGTH_LONG).show();
            return true;
        }

        return false;
    }

    private boolean hasSpaces(String str) {
        return ((str.split(" ").length > 1));
    }

    private void displayUserSettings() {
        SharedPreferences sharedPreferences = this.getSharedPreferences("com.sheelainfotech.gpstracker.activity.gpstracker.prefs", Context.MODE_PRIVATE);
        intervalInMinutes = sharedPreferences.getInt("intervalInMinutes", 1);

        switch (intervalInMinutes) {
            case 1:
                intervalRadioGroup.check(R.id.i1);
                break;
            case 5:
                intervalRadioGroup.check(R.id.i5);
                break;
            case 15:
                intervalRadioGroup.check(R.id.i15);
                break;
        }

        txtWebsite.setText(sharedPreferences.getString("defaultUploadWebsite", defaultUploadWebsite));
        txtEmail.setText(sharedPreferences.getString("email", email));
    }

    private boolean checkIfGooglePlayEnabled() {
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            return true;
        } else {
            Log.e(TAG, "unable to connect to google play services.");
            Toast.makeText(getApplicationContext(), R.string.google_play_services_unavailable, Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void setTrackingButtonState() {
        if (currentlyTracking) {
            trackingButton.setBackgroundResource(R.drawable.green_tracking_button);
            trackingButton.setTextColor(Color.BLACK);
            trackingButton.setText(R.string.tracking_is_on);
        } else {
            trackingButton.setBackgroundResource(R.drawable.red_tracking_button);
            trackingButton.setTextColor(Color.WHITE);
            trackingButton.setText(R.string.tracking_is_off);
        }
    }


   public boolean statusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
            return false;

        }

       else{

            return  true;
        }
    }
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        displayUserSettings();
        setTrackingButtonState();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void logoutUser() {
        SharedPreferences sharedPreferences = this.getSharedPreferences("com.sheelainfotech.gpstracker.activity.gpstracker.prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        session.setLogin(false);
        currentlyTracking = false;
        editor.putBoolean("currentlyTracking", false);
        editor.putString("sessionID", "");

        editor.apply();
        setTrackingButtonState();

        if (currentlyTracking) {
            trackingButton.setBackgroundResource(R.drawable.green_tracking_button);
            trackingButton.setTextColor(Color.BLACK);
            trackingButton.setText(R.string.tracking_is_on);
        } else {
            trackingButton.setBackgroundResource(R.drawable.red_tracking_button);
            trackingButton.setTextColor(Color.WHITE);
            trackingButton.setText(R.string.tracking_is_off);
        }
        cancelAlarmManager();

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(GpsTrackerActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
