package com.citypeople.project.views

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.citypeople.project.BaseActivity
import com.citypeople.project.HomeActivity
import com.citypeople.project.R
import com.citypeople.project.camera_preview.CameraPreview
import com.citypeople.project.databinding.ActivityOtpBinding
import com.citypeople.project.retrofit.Status
import com.citypeople.project.utilities.common.BaseViewModel
import com.citypeople.project.viewmodel.OtpViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import org.koin.android.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit


class OtpActivity : BaseActivity(), OtpListeners {

    private var timeCount: Int = 0
    private var myText: String? = null
    lateinit var bindingObj: ActivityOtpBinding
    val mViewModel by viewModel<OtpViewModel>()
    private var mAuth: FirebaseAuth? = null
    private var verificationId: String? = null
    private var firstName: String? = ""
    private var lastName: String? = ""
    var flag: Boolean = false
    var cTimer: CountDownTimer? = null
    private var myPhoneNumber: String? = ""
    lateinit var callBack: PhoneAuthProvider.OnVerificationStateChangedCallbacks


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingObj = DataBindingUtil.setContentView(this, R.layout.activity_otp)
        //   mViewModel = ViewModelProvider(this).get(OtpViewModel::class.java)
        bindingObj.listeners = this
        bindingObj.lifecycleOwner = this
        bindingObj.executePendingBindings()
        bindingObj.otpView.setOtpCompletionListener {
            myText = it
        }
        mAuth = FirebaseAuth.getInstance();
        val ii = intent
        val phone = ii.getStringExtra("phoneNumber")
        val countryCode = ii.getStringExtra("countryCode")
        verificationId = ii.getStringExtra("verificationId")
        firstName = ii.getStringExtra("firstName")
        lastName = ii.getStringExtra("lastName")
        myPhoneNumber = countryCode + phone
        bindingObj.enterTxt.text =
            "Enter the six digit code we've sent by text to $myPhoneNumber. Change"
        startTimer()
        optSend()
        apiObserver()
    }

    override fun bindViewModel(): BaseViewModel {
        return mViewModel
    }

    //start timer function
    private fun startTimer() {
        cTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeCount = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                timeCount = 0
                cancelTimer()
            }
        }
        (cTimer as CountDownTimer).start()
    }


    //cancel timer
    fun cancelTimer() {
        cTimer?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelTimer()
    }

    private fun resendCode(phone: String) {
        // this method is used for getting
        // OTP on user phone number.
        flag = true
        startTimer()
        val options = PhoneAuthOptions.newBuilder(mAuth!!)
            .setPhoneNumber(phone) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(callBack) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)

    }

    private fun optSend() {
        callBack = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {


            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                flag = false
                cancelTimer()
                signInWithCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                flag = false
                cancelTimer()
                Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT).show();
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                flag = false
                Log.d(ContentValues.TAG, "onCodeSent:$verificationId")
                Toast.makeText(applicationContext, "Otp has been sent!", Toast.LENGTH_SHORT).show();
                // Save verification ID and resending token so we can use them later
                // storedVerificationId = verificationId
                // resendToken = token

            }

            override fun onCodeAutoRetrievalTimeOut(p0: String) {
                super.onCodeAutoRetrievalTimeOut(p0)
                Log.e("TimeOut", p0)
            }

        }

    }


    override fun onClickContinue() {
        val otp = bindingObj.otpView.text.toString().trim()
        if (otp.isNotEmpty()) {
            val credential: PhoneAuthCredential = PhoneAuthProvider.getCredential(
                verificationId.toString(), otp
            )
            signInWithCredential(credential)
        } else {
            Toast.makeText(this, "Enter otp", Toast.LENGTH_SHORT).show();
        }
    }

    override fun onResendCode() {

        if (!flag && timeCount == 0) {
            resendCode(myPhoneNumber.toString())
        } else {
            Toast.makeText(this, "Please wait for ${timeCount} seconds", Toast.LENGTH_SHORT).show();
        }
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        // inside this method we are checking if
        // the code entered is correct or not.
        mAuth!!.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // if the code is correct and the task is successful
                    // we are sending our user to new activity.
                    mViewModel.signIn(myPhoneNumber, firstName, lastName)
//                    val i = Intent(this, HomeActivity::class.java)
//                    startActivity(i)
//                    finish()
                } else {
                    // if the code is not correct then we are
                    // displaying an error message to the user.
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(this, "Invalid OTP", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    fun apiObserver() {
        mViewModel.generateUser?.observe(this, Observer {
            when (it.status) {
                Status.LOADING -> mViewModel.loader.postValue(true)
                Status.ERROR -> {
                    mViewModel.loader.postValue(false)
                    it.message?.let { msg ->
                        Toast.makeText(this, msg.message, Toast.LENGTH_SHORT).show()
                    } ?: Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
                }
                Status.SUCCESS -> {
                    mViewModel.loader.postValue(false)
                    it.data?.apply {
                        val i = Intent(applicationContext, HomeActivity::class.java)
                        startActivity(i)
                        finish()
                    }
                }
            }
        })

    }

    override fun onBack() {
        finish()
    }
}

interface OtpListeners {
    fun onClickContinue()
    fun onResendCode()
    fun onBack()
}

