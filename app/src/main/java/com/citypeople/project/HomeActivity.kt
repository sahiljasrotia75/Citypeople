package com.citypeople.project

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.citypeople.project.cameranew.Camera2BasicFragmentKt


class HomeActivity : AppCompatActivity() {

    lateinit var mAuth: FirebaseAuth

    var mFriendId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //preventStatusBarExpansion(this)
        setContentView(R.layout.activity_home)
        mAuth = FirebaseAuth.getInstance()
        // In Activity's onCreate() for instance
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            val w = window
//            w.setFlags(
//                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
//                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
//            )
//        }
        // In Activity's onCreate() for instance

        extractIntent()

        if (null == savedInstanceState) {
            Camera2BasicFragmentKt.newInstance()?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container, it)
                    .commit()
            }
        }
    }

    private fun extractIntent() {
        if (intent.extras == null || intent.extras?.containsKey(Constants.INTENT_DATA) == false)
            return

        mFriendId = intent.extras?.getInt(Constants.INTENT_DATA, 0) ?: 0
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
      //  showFullscreenFlag()
        if (hasFocus){
         //   hideSystemUI()
        }

    }

    fun hideSystemUI(){
        showFullscreenFlag()
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN)


    }

    fun showFullscreenFlag(){
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    override fun onBackPressed() {
        //super.onBackPressed()
    }
}

