package com.citypeople.project.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.citypeople.project.utilities.FieldValidators
import com.citypeople.project.utilities.common.BaseViewModel

class PhoneViewModel: BaseViewModel() {

    val phone = MutableLiveData<String>()
    val phoneError = Transformations.switchMap(phone, FieldValidators::phoneError)


}