package com.example.rehappmobile


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity : AppCompatActivity() {
    lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainAct = this
        pref = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)

        val b: Button = findViewById(R.id.buttonLogin)
        b.setOnClickListener { signIn() }

//        val token = pref.getString("token", "")
//        if (token != "") {
//            toNextActivity()
//        }
    }

    private val mRetrofit = Retrofit.Builder()
        .baseUrl("http://192.168.43.151:5001")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private fun signIn() {
        val serviceAPI = mRetrofit.create(RehServerApi::class.java)
        val email: String =
            findViewById<EditText>(R.id.textEmail).text.toString()  //.data.getString("ema
        val password: String = findViewById<EditText>(R.id.textPassword).text.toString()
        if (email != "" && password != "") {
            val call = serviceAPI.login(
                LoginClient(
                    email,
                    password
                )
            )
            call.enqueue(object : retrofit2.Callback<AuthorizationReport> {
                override fun onFailure(call: Call<AuthorizationReport>, t: Throwable) {
                    Toast.makeText(
                        mainAct.applicationContext,
                        t.message,
                        Toast.LENGTH_LONG
                    )
                        .show()
                }

                override fun onResponse(
                    call: Call<AuthorizationReport>,
                    response: Response<AuthorizationReport>
                ) {
                    if (response.body() != null) {
                        val authorizationReport = response.body() as AuthorizationReport
                        val authorizationData = authorizationReport.data as AuthorizationData
                        if (authorizationReport.status == "200") {
                            val authorizationData = authorizationReport.data as AuthorizationData
                            val editor = mainAct.pref.edit()
                            editor.putString(APP_PREFERENCES_TOKEN, authorizationData.token)
                            editor.putString(
                                APP_PREFERENCES_EMAIL,
                                findViewById<EditText>(R.id.textEmail).text.toString()
                            )
                            editor.apply()
                            mainAct.toNextActivity()
                        } else {
                            Toast.makeText(
                                mainAct.applicationContext,
                                "Something went wrong",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }

                    } else {
                        Toast.makeText(
                            mainAct.applicationContext,
                            response.errorBody().toString(),
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
            })
        }
    }

    fun toNextActivity() {
        val intent = Intent(this, ActivityBle::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    companion object {
        private lateinit var mainAct: MainActivity
        const val APP_PREFERENCES = "mysettings"
        const val APP_PREFERENCES_TOKEN = "token"
        const val APP_PREFERENCES_EMAIL = "email"
    }
}