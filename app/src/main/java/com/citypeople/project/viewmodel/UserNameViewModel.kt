package com.citypeople.project.viewmodel

import androidx.lifecycle.MutableLiveData
import com.citypeople.project.utilities.common.BaseViewModel

class UserNameViewModel: BaseViewModel() {

    val firstName = MutableLiveData<String>()
    val lastName = MutableLiveData<String>()
}