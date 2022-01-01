package com.citypeople.project.views

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.citypeople.project.R
import com.citypeople.project.databinding.ActivityUsernameBinding
import com.citypeople.project.viewmodel.PhoneViewModel
import com.citypeople.project.viewmodel.UserNameViewModel

class UserNameActivity : AppCompatActivity(), UserNameListeners {

    lateinit var bindingObj: ActivityUsernameBinding
    lateinit var mViewModel: UserNameViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingObj = DataBindingUtil.setContentView(this, R.layout.activity_username)
        mViewModel = ViewModelProvider(this).get(UserNameViewModel::class.java)
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        bindingObj.listeners = this

       val ss = SpannableString(getString(R.string.terms_check))
        val boldSpan = StyleSpan(Typeface.BOLD)
        val boldSpan2 = StyleSpan(Typeface.BOLD)
        val foregroundSpan = ForegroundColorSpan(resources.getColor(R.color.appprimarycolor))
        val foregroundSpan2 = ForegroundColorSpan(resources.getColor(R.color.appprimarycolor))
        ss.setSpan(boldSpan2, 68, 84, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        ss.setSpan(foregroundSpan, 68, 84, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        ss.setSpan(boldSpan, 100, 118, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        ss.setSpan(foregroundSpan2, 100, 118, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        bindingObj.privacyText.apply {
            text = ss
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = Color.TRANSPARENT
        }

    }

    override fun onClickSignIn() {
        if(bindingObj.firstNameEditText.text.toString().trim().isEmpty()){
            Toast.makeText(this,"Enter first name",Toast.LENGTH_SHORT).show()
        }else if (bindingObj.lastNameEditText.text.toString().trim().isEmpty()){
            Toast.makeText(this,"Enter last name",Toast.LENGTH_SHORT).show()
        }else{
            val intent = Intent(this, PhoneActivity::class.java)//if login
            intent.putExtra("firstName", bindingObj.firstNameEditText.text.toString().trim());
            intent.putExtra("lastName", bindingObj.lastNameEditText.text.toString().trim());
            startActivity(intent)
        }

    }

    override fun onBack() {
        finish()
    }

}

interface UserNameListeners {
    fun onClickSignIn()
    fun onBack()
}
