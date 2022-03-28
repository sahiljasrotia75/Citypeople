package com.citypeople.project

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.citypeople.project.cameranew.Camera2BasicFragmentKt
import com.citypeople.project.views.UserNameActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

class SplashActivity : AppCompatActivity() {
    lateinit var mAuth: FirebaseAuth
    val TAG = "MyNotification"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        mAuth = FirebaseAuth.getInstance()
        mDelayHandler = Handler()
        mDelayHandler!!.postDelayed(mRunnable, mSplashDelay)
        generateFCMToken()

    }

    public override fun onDestroy() {
        if (mDelayHandler != null) {
            mDelayHandler!!.removeCallbacks(mRunnable)
        }

        super.onDestroy()
    }

    private var mDelayHandler: Handler? = null
    private val mSplashDelay: Long = 3000

    private val mRunnable: Runnable = Runnable {
        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            startActivity(Intent(applicationContext, HomeActivity::class.java))
            finish()
        } else {
            val intent = Intent(this, UserNameActivity::class.java)//if login
            startActivity(intent)
            finish()
        }

    }
    fun generateFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.d(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            // Get new FCM registration token
            val token = task.result
            //      sharedPref.setString(sharedPref.FCM_TOKEN, token)


        })
    }

}