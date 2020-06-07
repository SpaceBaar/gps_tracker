package com.spacebaar.gpstracker.activity

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.loopj.android.http.AsyncHttpResponseHandler
import com.loopj.android.http.RequestParams
import com.spacebaar.gpstracker.R
import com.spacebaar.gpstracker.helper.SQLiteHandler
import com.spacebaar.gpstracker.helper.SessionManager
import cz.msebera.android.httpclient.Header
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

open class LocationService : Service(), ConnectionCallbacks, OnConnectionFailedListener, LocationListener {
    private var defaultUploadWebsite: String? = null
    private var currentlyProcessingLocation = false
    private var googleApiClient: GoogleApiClient? = null
    override fun onCreate() {
        super.onCreate()
        defaultUploadWebsite = getString(R.string.default_upload_website)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // if we are currently trying to get a location and the alarm manager has called this again,
        // no need to start processing a new location.
        if (!currentlyProcessingLocation) {
            currentlyProcessingLocation = true
            startTracking()
        }
        return START_NOT_STICKY
    }

    private fun startTracking() {
        Log.d(TAG, "startTracking")
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            googleApiClient = GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build()
            if (!googleApiClient!!.isConnected || !googleApiClient!!.isConnecting) {
                googleApiClient!!.connect()
            }
        } else {
            Log.e(TAG, "unable to connect to google play services.")
        }
    }

    private fun sendLocationDataToWebsite(location: Location) {
        // formatted for mysql datetime format
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        dateFormat.timeZone = TimeZone.getDefault()
        val date = Date(location.time)
        val sharedPreferences = getSharedPreferences("com.spacebaar.gpstracker.activity.gpstracker.prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        var totalDistanceInMeters = sharedPreferences.getFloat("totalDistanceInMeters", 0f)
        val firstTimeGettingPosition = sharedPreferences.getBoolean("firstTimeGettingPosition", true)
        if (firstTimeGettingPosition) {
            editor.putBoolean("firstTimeGettingPosition", false)
        } else {
            val previousLocation = Location("")
            previousLocation.latitude = sharedPreferences.getFloat("previousLatitude", 0f).toDouble()
            previousLocation.longitude = sharedPreferences.getFloat("previousLongitude", 0f).toDouble()
            val distance = location.distanceTo(previousLocation)
            totalDistanceInMeters += distance
            editor.putFloat("totalDistanceInMeters", totalDistanceInMeters)
        }
        editor.putFloat("previousLatitude", location.latitude.toFloat())
        editor.putFloat("previousLongitude", location.longitude.toFloat())
        editor.apply()
        val requestParams = RequestParams()
        requestParams.put("latitude", location.latitude.toString())
        requestParams.put("longitude", location.longitude.toString())
        val speedInMilesPerHour = location.speed * 2.2369
        requestParams.put("speed", speedInMilesPerHour.toInt().toString())
        try {
            requestParams.put("date", URLEncoder.encode(dateFormat.format(date), "UTF-8"))
        } catch (ignored: UnsupportedEncodingException) {
        }
        requestParams.put("locationmethod", location.provider)
        if (totalDistanceInMeters > 0) {
            requestParams.put("distance", String.format("%.1f", totalDistanceInMeters / 1609)) // in miles,
        } else {
            requestParams.put("distance", "0.0") // in miles
        }

        // SqLite database handler
        val db = SQLiteHandler(applicationContext)

        // session manager
        val session = SessionManager(applicationContext)

        // Fetching user details from SQLite
        val user = db.userDetails
        val email = user["email"]
        requestParams.put("username", email)
        requestParams.put("phonenumber", sharedPreferences.getString("appID", "")) // uuid
        requestParams.put("sessionid", sharedPreferences.getString("sessionID", "")) // uuid
        val accuracyInFeet = location.accuracy * 3.28
        requestParams.put("accuracy", accuracyInFeet.toInt().toString())
        val altitudeInFeet = location.altitude * 3.28
        requestParams.put("extrainfo", altitudeInFeet.toInt().toString())
        requestParams.put("eventtype", "android")
        val direction = location.bearing
        requestParams.put("direction", direction.toInt().toString())
        val uploadWebsite = sharedPreferences.getString("defaultUploadWebsite", defaultUploadWebsite)
        LoopjHttpClient[uploadWebsite, requestParams, object : AsyncHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<Header>, responseBody: ByteArray) {
                LoopjHttpClient.debugLoopJ(TAG, "sendLocationDataToWebsite - success", uploadWebsite, requestParams, responseBody, headers, statusCode, null)
                stopSelf()
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, errorResponse: ByteArray, e: Throwable) {
                LoopjHttpClient.debugLoopJ(TAG, "sendLocationDataToWebsite - failure", uploadWebsite, requestParams, errorResponse, headers, statusCode, e)
                stopSelf()
            }
        }]
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onLocationChanged(location: Location) {
        Log.e(TAG, "position: " + location.latitude + ", " + location.longitude + " accuracy: " + location.accuracy)

        // we have our desired accuracy of 500 meters so lets quit this service,
        // onDestroy will be called and stop our location updates
        if (location.accuracy < 500.0f) {
            stopLocationUpdates()
            sendLocationDataToWebsite(location)
        }
    }

    private fun stopLocationUpdates() {
        if (googleApiClient != null && googleApiClient!!.isConnected) {
            googleApiClient!!.disconnect()
        }
    }

    /**
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    override fun onConnected(bundle: Bundle?) {
        Log.d(TAG, "onConnected")
        val locationRequest = LocationRequest.create()
        locationRequest.interval = 1000 // milliseconds
        locationRequest.fastestInterval = 1000 // the fastest rate in milliseconds at which your app can handle location updates
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient, locationRequest, this)
        } else {
            stopSelf()
        }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.e(TAG, "onConnectionFailed")
        stopLocationUpdates()
        stopSelf()
    }

    override fun onConnectionSuspended(i: Int) {
        Log.e(TAG, "GoogleApiClient connection has been suspend")
    }

    companion object {
        private const val TAG = "LocationService"
    }
}