package com.spacebaar.gpstracker.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.spacebaar.gpstracker.R
import com.spacebaar.gpstracker.activity.MainActivity
import com.spacebaar.gpstracker.helper.SQLiteHandler
import com.spacebaar.gpstracker.helper.SessionManager

class MainActivity : Activity() {
    private var db: SQLiteHandler? = null
    private var session: SessionManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val txtName = findViewById<TextView>(R.id.name)
        val txtEmail = findViewById<TextView>(R.id.email)
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
        val name = user["name"]
        val email = user["email"]

        // Displaying the user details on the screen
        txtName.text = name
        txtEmail.text = email

        // Logout button click event
        btnLogout.setOnClickListener { logoutUser() }
    }

    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from SQLite users table
     */
    private fun logoutUser() {
        session!!.setLogin(false)
        db!!.deleteUsers()

        // Launching the login activity
        val intent = Intent(this@MainActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}