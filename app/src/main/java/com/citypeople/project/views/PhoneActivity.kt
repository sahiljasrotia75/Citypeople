package com.citypeople.project.views

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.citypeople.project.HomeActivity
import com.citypeople.project.R
import com.citypeople.project.camera_preview.CameraPreview
import com.citypeople.project.databinding.ActivityPhoneBinding
import com.citypeople.project.viewmodel.PhoneViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import java.util.concurrent.TimeUnit


class PhoneActivity : AppCompatActivity(), PhoneListeners {
    private var firstName: String? = ""
    private var lastName: String? = ""
    private lateinit var resendToken: ForceResendingToken
    lateinit var bindingObj: ActivityPhoneBinding
    lateinit var mViewModel: PhoneViewModel
    private var mAuth: FirebaseAuth? = null
    lateinit var callBack: OnVerificationStateChangedCallbacks
    private var storedVerificationId: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingObj = DataBindingUtil.setContentView(this, R.layout.activity_phone)
        mViewModel = ViewModelProvider(this).get(PhoneViewModel::class.java)
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val ii = intent
        firstName = ii.getStringExtra("firstName")
        lastName = ii.getStringExtra("lastName")
        bindingObj.viewModel = mViewModel
        bindingObj.listeners = this
        bindingObj.lifecycleOwner = this
        bindingObj.executePendingBindings()
        mAuth = FirebaseAuth.getInstance()
        optSend()
        // of our FirebaseAuth.

    }

    override fun onClickContinue() {
        when {
            TextUtils.isEmpty(bindingObj.etPhone.text.toString().trim()) -> {
                Toast.makeText(this, "Invalid phone number.", Toast.LENGTH_SHORT).show();
            }
            bindingObj.etPhone.text.toString().trim().length != 10 -> {
                Toast.makeText(this, "Type valid  phone number.", Toast.LENGTH_SHORT).show();
            }
            else -> {
                val countryCode = bindingObj.countryCode.selectedCountryCodeWithPlus
                val etphone = bindingObj.etPhone.text.toString().trim()
                val pp = countryCode + etphone
                sendVerificationCode(pp)
            }
        }
    }

    private fun sendVerificationCode(phone: String) {
        // this method is used for getting
        // OTP on user phone number.
        val options = PhoneAuthOptions.newBuilder(mAuth!!)
            .setPhoneNumber(phone) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(callBack) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)

    }

    private fun optSend() {
        callBack = object : OnVerificationStateChangedCallbacks() {


            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT).show();
            }

            override fun onCodeSent(
                verificationId: String,
                token: ForceResendingToken
            ) {
                Log.d(TAG, "onCodeSent:$verificationId")
                // Save verification ID and resending token so we can use them later
                // storedVerificationId = verificationId
                // resendToken = token
                val phone = bindingObj.etPhone.text.toString().trim()
                val intent = Intent(applicationContext, OtpActivity::class.java)//if login
                intent.putExtra("countryCode", bindingObj.countryCode.selectedCountryCodeWithPlus);
                intent.putExtra("phoneNumber", phone);
                intent.putExtra("verificationId", verificationId);
                intent.putExtra("firstName", firstName);
                intent.putExtra("lastName", lastName);
                startActivity(intent)

            }
        }

    }

    /*   override fun onStart() {
           super.onStart()
           val currentUser = mAuth?.currentUser
           if (currentUser!=null){
              sendToMain()
           }
       }*/

    private fun sendToMain() {
        startActivity(Intent(applicationContext, HomeActivity::class.java))
        finish()
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        // inside this method we are checking if
        // the code entered is correct or not.
        mAuth!!.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // if the code is correct and the task is successful
                    // we are sending our user to new activity.
                    sendToMain()
                } else {
                    // if the code is not correct then we are
                    // displaying an error message to the user.
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(
                            this,
                            (task.exception as FirebaseAuthInvalidCredentialsException).message.toString(),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
    }


    override fun onBack() {
        finish()
    }

}


interface PhoneListeners {
    fun onClickContinue()
    fun onBack()
}