package com.citypeople.project.utilities

import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.regex.Pattern


object FieldValidators
{
    var firebasetoken = ""
    fun getToken(){

    }
    var specialChar: Pattern = Pattern.compile("[$&+,:;=\\\\?@#|/'<>.^*()%!-]")
    var alphabetRegex = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")
    val emailError="Req. Minimum 8 chars with one special character and one alphabet"
     fun setEmailError(textValue: String?):LiveData<String>
     {
        val liveData=MutableLiveData<String>()
        if(textValue.isNullOrEmpty())return liveData.apply { value="Please enter your email" }
        // validating
        liveData.value= isValidEmail(textValue)!!

         liveData.value?.let { Log.e("", it) }
        return liveData
    }
     fun setPasswordError(textValue: String?):LiveData<String>{

        val liveData=MutableLiveData<String>()
         if(textValue.isNullOrEmpty())return liveData.apply { value= "Please enter a password" }
        return when {
            TextUtils.isEmpty(textValue) -> liveData.apply { value="Required minimum 8 characters"}
            textValue.length<8 -> liveData.apply { value="Required minimum 8 characters" }
            else -> liveData.apply { value=null }
        }
    }
    fun phoneError(textValue: String?):LiveData<String>{

        val liveData=MutableLiveData<String>()
        if(textValue.isNullOrEmpty())return liveData.apply { value= "Please enter a phone number" }
        return when {
            TextUtils.isEmpty(textValue) -> liveData.apply { value="Please enter a phone number"}
            textValue.length<7 || textValue.length>15 -> liveData.apply { value="Invalid phone number" }
            else -> liveData.apply { value=null }
        }
    }
    fun setUsernameError(textValue: String?):LiveData<String>{

        val liveData=MutableLiveData<String>()
        if(textValue.isNullOrEmpty())
            return liveData.apply { value="Please enter your user name" }

        return when {
            TextUtils.isEmpty(textValue)
            -> liveData.apply { value="Please enter your user name"}
            else -> liveData.apply { value=null }
        }
    }
    fun setPetNameError(textValue: String?):LiveData<String>{

        val liveData=MutableLiveData<String>()
        if(textValue.isNullOrEmpty())
            return liveData.apply { value="Please enter your Pet's name" }

        return when {
            TextUtils.isEmpty(textValue)
            -> liveData.apply { value="Please enter your Pet's name"}
            else -> liveData.apply { value=null }
        }
    }
    fun setBioError(textValue: String?):LiveData<String>{

        val liveData=MutableLiveData<String>()
        if(textValue.isNullOrEmpty())
            return liveData.apply { value="Please enter your bio" }

        return when {
            TextUtils.isEmpty(textValue)
            -> liveData.apply { value="Please enter your bio"}
            else -> liveData.apply { value=null }
        }
    }
    fun isValidEmail(textValue: String?):String?{

       return when{
           textValue.isNullOrEmpty()-> "Email must not be empty"
           !android.util.Patterns.EMAIL_ADDRESS.matcher(textValue).find() -> "Invalid email"
           else -> null
       }
    }
    fun isValidPassword(textValue: String?):String?
    {

        return when{
            TextUtils.isEmpty(textValue) -> "Old Password must not be empty"
            textValue?.length!!<8 -> "Required minimum 8 characters"
            else -> null
        }
    }

}