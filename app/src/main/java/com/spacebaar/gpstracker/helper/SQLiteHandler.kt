package com.spacebaar.gpstracker.helper

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.util.*

class SQLiteHandler(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    // Creating Tables
    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_LOGIN_TABLE = ("CREATE TABLE " + TABLE_USER + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
                + KEY_EMAIL + " TEXT UNIQUE," + KEY_UID + " TEXT,"
                + KEY_CREATED_AT + " TEXT" + ")")
        db.execSQL(CREATE_LOGIN_TABLE)
        Log.d(TAG, "Database tables created")
    }

    // Upgrading database
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER")

        // Create tables again
        onCreate(db)
    }

    /**
     * Storing user details in database
     */
    fun addUser(name: String?, email: String?, uid: String?, created_at: String?) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_NAME, name) // Name
        values.put(KEY_EMAIL, email) // Email
        values.put(KEY_UID, uid) // Email
        values.put(KEY_CREATED_AT, created_at) // Created At

        // Inserting Row
        val id = db.insert(TABLE_USER, null, values)
        db.close() // Closing database connection
        Log.d(TAG, "New user inserted into SQLite: $id")
    }// Move to first row
    // return user

    /**
     * Getting user data from database
     */
    val userDetails: HashMap<String, String>
        get() {
            val user = HashMap<String, String>()
            val selectQuery = "SELECT  * FROM $TABLE_USER"
            val db = this.readableDatabase
            val cursor = db.rawQuery(selectQuery, null)
            // Move to first row
            cursor.moveToFirst()
            if (cursor.count > 0) {
                user["name"] = cursor.getString(1)
                user["email"] = cursor.getString(2)
                user["uid"] = cursor.getString(3)
                user["created_at"] = cursor.getString(4)
            }
            cursor.close()
            db.close()
            // return user
            Log.d(TAG, "Fetching user from SQLite: $user")
            return user
        }

    /**
     * Re crate database Delete all tables and create them again
     */
    fun deleteUsers() {
        val db = this.writableDatabase
        // Delete All Rows
        db.delete(TABLE_USER, null, null)
        db.close()
        Log.d(TAG, "Deleted all user info from SQLite")
    }

    companion object {
        private val TAG = SQLiteHandler::class.java.simpleName

        // All Static variables
        // Database Version
        private const val DATABASE_VERSION = 1

        // Database Name
        private const val DATABASE_NAME = "android_api"

        // Login table name
        private const val TABLE_USER = "user"

        // Login Table Columns names
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_UID = "uid"
        private const val KEY_CREATED_AT = "created_at"
    }
}