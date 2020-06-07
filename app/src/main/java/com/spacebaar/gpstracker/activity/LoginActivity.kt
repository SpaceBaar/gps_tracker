package com.spacebaar.gpstracker.activity

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.spacebaar.gpstracker.R
import com.spacebaar.gpstracker.app.AppConfig
import com.spacebaar.gpstracker.app.AppController
import com.spacebaar.gpstracker.helper.SQLiteHandler
import com.spacebaar.gpstracker.helper.SessionManager
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class LoginActivity : Activity() {
    private var inputEmail: EditText? = null
    private var inputPassword: EditText? = null
    private var pDialog: ProgressDialog? = null
    private var session: SessionManager? = null
    private var db: SQLiteHandler? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        inputEmail = findViewById(R.id.email)
        inputPassword = findViewById(R.id.password)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnLinkToRegister = findViewById<Button>(R.id.btnLinkToRegisterScreen)

        // Progress dialog
        pDialog = ProgressDialog(this)
        pDialog!!.setCancelable(false)

        // SQLite database handler
        db = SQLiteHandler(applicationContext)

        // Session manager
        session = SessionManager(applicationContext)

        // Check if user is already logged in or not
        if (session!!.isLoggedIn) {
            // User is already logged in. Take him to main activity
            val intent = Intent(this@LoginActivity, GpsTrackerActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Login button Click Event
        btnLogin.setOnClickListener {
            val email = inputEmail!!.text.toString().trim { it <= ' ' }
            val password = inputPassword!!.text.toString().trim { it <= ' ' }

            // Check for empty data in the form
            if (email.isNotEmpty() && password.isNotEmpty()) {
                // login user
                checkLogin(email, password)
            } else {
                // Prompt user to enter credentials
                Toast.makeText(applicationContext,
                        "Please enter the credentials!", Toast.LENGTH_LONG)
                        .show()
            }
        }

        // Link to Register Screen
        btnLinkToRegister.setOnClickListener {
            val i = Intent(applicationContext,
                    RegisterActivity::class.java)
            startActivity(i)
            finish()
        }
    }

    /**
     * function to verify login details in mysql db
     */
    private fun checkLogin(email: String, password: String) {
        // Tag used to cancel the request
        val tagStringReq = "req_login"
        pDialog!!.setMessage("Logging in ...")
        showDialog()
        val strReq: StringRequest = object : StringRequest(Method.POST,
                AppConfig.URL_LOGIN, Response.Listener { response ->
            Log.d(TAG, "Login Response: $response")
            hideDialog()
            try {
                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")

                // Check for error node in json
                if (!error) {
                    // user successfully logged in
                    // Create login session
                    session!!.setLogin(true)

                    // Now store the user in SQLite
                    val uid = jObj.getString("uid")
                    val user = jObj.getJSONObject("user")
                    val name = user.getString("name")
                    val emailId = user.getString("email")
                    val createdAt = user.getString("updated_at")

                    // Inserting row in users table
                    db!!.addUser(name, emailId, uid, createdAt)

                    // Launch main activity
                    val intent = Intent(this@LoginActivity,
                            GpsTrackerActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Error in login. Get the error message
                    val errorMsg = jObj.getString("error_msg")
                    Toast.makeText(applicationContext,
                            errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: JSONException) {
                // JSON error
                e.printStackTrace()
                Toast.makeText(applicationContext, "Json error: " + e.message, Toast.LENGTH_LONG).show()
            }
        }, Response.ErrorListener { error ->
            Log.e(TAG, "Login Error: " + error.message)
            Toast.makeText(applicationContext,
                    error.message, Toast.LENGTH_LONG).show()
            hideDialog()
        }) {
            override fun getParams(): Map<String, String> {
                // Posting parameters to login url
                val params: MutableMap<String, String> = HashMap()
                params["email"] = email
                params["password"] = password
                return params
            }
        }

        // Adding request to request queue
        AppController.instance?.addToRequestQueue(strReq, tagStringReq)
    }

    private fun showDialog() {
        if (!pDialog!!.isShowing) pDialog!!.show()
    }

    private fun hideDialog() {
        if (pDialog!!.isShowing) pDialog!!.dismiss()
    }

    companion object {
        private val TAG = RegisterActivity::class.java.simpleName
    }
}