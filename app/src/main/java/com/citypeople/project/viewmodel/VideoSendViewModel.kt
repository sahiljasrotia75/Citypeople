package com.citypeople.project.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.citypeople.project.models.signin.ContactsResponse
import com.citypeople.project.models.signin.FriendResponse
import com.citypeople.project.repo.AuthRepo
import com.citypeople.project.retrofit.ErrorResponse
import com.citypeople.project.retrofit.Resources
import com.citypeople.project.utilities.common.BaseViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File


class VideoSendViewModel(private val authRepo: AuthRepo) : BaseViewModel() {

    private val _videoList = MutableLiveData<Resources<ContactsResponse>>()
    var videoList: LiveData<Resources<ContactsResponse>>? = _videoList

    private val _sendVideo = MutableLiveData<Resources<FriendResponse>>()
    var sendVideo: LiveData<Resources<FriendResponse>>? = _sendVideo

    fun groupContacts(jsonObject: JSONObject) {
        viewModelScope.launch {
            runCatching {
                _videoList.postValue(Resources.loading())
                authRepo.getVideoList(jsonObject)
            }.onSuccess {
                _videoList.postValue(it)
            }.onFailure {
                _videoList.postValue(
                    Resources.error(
                        ErrorResponse(903, it.message.toString()), null
                    )
                )
            }
        }
    }

    fun sendVideo(jsonObject: JSONObject, videoFile: File) {

        viewModelScope.launch {
            runCatching {
                _sendVideo.postValue(Resources.loading())
                authRepo.sendVideo(jsonObject, videoFile)
            }.onSuccess {
                _sendVideo.postValue(it)
            }.onFailure {
                _sendVideo.postValue(
                    Resources.error(
                        ErrorResponse(903, it.message.toString()), null
                    )
                )
            }
        }
    }

}