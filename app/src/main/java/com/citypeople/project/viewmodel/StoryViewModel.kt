package com.citypeople.project.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.citypeople.project.models.signin.FriendResponse
import com.citypeople.project.models.signin.StoryDataResponse
import com.citypeople.project.repo.AuthRepo
import com.citypeople.project.retrofit.ErrorResponse
import com.citypeople.project.retrofit.Resources
import com.citypeople.project.utilities.common.BaseViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

 class StoryViewModel(var authRepo :AuthRepo) : BaseViewModel() {
    private val _storyList = MutableLiveData<Resources<StoryDataResponse>>()
    var storyList: LiveData<Resources<StoryDataResponse>>? = _storyList

    private val _sendVideo = MutableLiveData<Resources<FriendResponse>>()
    var sendVideo: LiveData<Resources<FriendResponse>>? = _sendVideo

    fun stories(jsonObject: JSONObject) {
        viewModelScope.launch {
            runCatching {
                _storyList.postValue(Resources.loading())
                authRepo.getStoryLstList(jsonObject)
            }.onSuccess {
                _storyList.postValue(it)
            }.onFailure {
                _storyList.postValue(
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