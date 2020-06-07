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

class RegisterActivity : Activity() {
    private var inputFullName: EditText? = null
    private var inputEmail: EditText? = null
    private var inputPassword: EditText? = null
    private var pDialog: ProgressDialog? = null
    private var db: SQLiteHandler? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        inputFullName = findViewById(R.id.name)
        inputEmail = findViewById(R.id.email)
        inputPassword = findViewById(R.id.password)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnLinkToLogin = findViewById<Button>(R.id.btnLinkToLoginScreen)

        // Progress dialog
        pDialog = ProgressDialog(this)
        pDialog!!.setCancelable(false)

        // Session manager
        val session = SessionManager(applicationContext)

        // SQLite database handler
        db = SQLiteHandler(applicationContext)

        // Check if user is already logged in or not
        if (session.isLoggedIn) {
            // User is already logged in. Take him to main activity
            val intent = Intent(this@RegisterActivity,
                    MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Register Button Click event
        btnRegister.setOnClickListener {
            val name = inputFullName!!.text.toString().trim { it <= ' ' }
            val email = inputEmail!!.text.toString().trim { it <= ' ' }
            val password = inputPassword!!.text.toString().trim { it <= ' ' }
            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                registerUser(name, email, password)
            } else {
                Toast.makeText(applicationContext,
                        "Please enter your details!", Toast.LENGTH_LONG)
                        .show()
            }
        }

        // Link to Login Screen
        btnLinkToLogin.setOnClickListener {
            val i = Intent(applicationContext,
                    LoginActivity::class.java)
            startActivity(i)
            finish()
        }
    }

    /**
     * Function to store user in MySQL database will post params(tag, name,
     * email, password) to register url
     */
    private fun registerUser(name: String, email: String,
                             password: String) {
        // Tag used to cancel the request
        val tagStringReq = "req_register"
        pDialog!!.setMessage("Registering ...")
        showDialog()
        val strReq: StringRequest = object : StringRequest(Method.POST,
                AppConfig.URL_REGISTER, Response.Listener { response ->
            Log.d(TAG, "Register Response: $response")
            hideDialog()
            try {
                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                if (!error) {
                    // User successfully stored in MySQL
                    // Now store the user in SQLite
                    val uid = jObj.getString("uid")
                    val user = jObj.getJSONObject("user")
                    val fullName = user.getString("name")
                    val emailId = user.getString("email")
                    val createdAt = user
                            .getString("created_at")

                    // Inserting row in users table
                    db!!.addUser(fullName, emailId, uid, createdAt)
                    Toast.makeText(applicationContext, "User successfully registered. Try login now!", Toast.LENGTH_LONG).show()

                    // Launch login activity
                    val intent = Intent(
                            this@RegisterActivity,
                            LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {

                    // Error occurred in registration. Get the error
                    // message
                    val errorMsg = jObj.getString("error_msg")
                    Toast.makeText(applicationContext,
                            errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }, Response.ErrorListener { error ->
            Log.e(TAG, "Registration Error: " + error.message)
            Toast.makeText(applicationContext,
                    error.message, Toast.LENGTH_LONG).show()
            hideDialog()
        }) {
            override fun getParams(): Map<String, String> {
                // Posting params to register url
                val params: MutableMap<String, String> = HashMap()
                params["name"] = name
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