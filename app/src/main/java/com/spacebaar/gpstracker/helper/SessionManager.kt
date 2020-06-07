package com.spacebaar.gpstracker.helper

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.util.Log

class SessionManager(var _context: Context) {
    // Shared Preferences
    var pref: SharedPreferences
    var editor: Editor

    // Shared pref mode
    var PRIVATE_MODE = 0
    fun setLogin(isLoggedIn: Boolean) {
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)

        // commit changes
        editor.commit()
        Log.d(TAG, "User login session modified!")
    }

    val isLoggedIn: Boolean
        get() = pref.getBoolean(KEY_IS_LOGGED_IN, false)

    companion object {
        // Shared preferences file name
        private const val PREF_NAME = "AndroidHiveLogin"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"

        // LogCat tag
        private val TAG = SessionManager::class.java.simpleName
    }

    init {
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        editor = pref.edit()
    }
}