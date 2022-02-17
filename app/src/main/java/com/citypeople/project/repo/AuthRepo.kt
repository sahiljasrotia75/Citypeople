package com.citypeople.project.repo


import android.app.Application
import com.citypeople.project.models.signin.ContactsResponse
import com.citypeople.project.models.signin.UserResponse
import com.citypeople.project.retrofit.Resources
import com.citypeople.project.retrofit.RetrofitService
import com.citypeople.project.retrofit.safeApiCall
import org.json.JSONObject
import com.citypeople.project.models.signin.FriendResponse
import com.citypeople.project.models.signin.StoryDataResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File


class AuthRepo(private val retrofitService :RetrofitService) {

    suspend fun requestUser(data: HashMap<String, String>): Resources<UserResponse> {
        return safeApiCall {
            retrofitService.userData(data)
        }
    }

    suspend fun contactList(jsonObject: JSONObject): Resources<ContactsResponse> {
        return safeApiCall {
            retrofitService.contactData(
                jsonObject["contacts"] as ArrayList<String>,
                jsonObject["phone"] as String
            )
        }
    }

    suspend fun addFriend(jsonObject: JSONObject): Resources<FriendResponse> {
        return safeApiCall {
            retrofitService.friendData(
                jsonObject["ids"] as ArrayList<Int>,
                jsonObject["phone"] as String
            )
        }
    }

    suspend fun groupContactList(jsonObject: JSONObject): Resources<ContactsResponse> {
        return safeApiCall {
            retrofitService.groupContactData(jsonObject["phone"] as String)
        }
    }

    suspend fun addGroup(jsonObject: JSONObject): Resources<FriendResponse> {
        return safeApiCall {
            retrofitService.groupData(
                jsonObject["ids"] as ArrayList<Int>,
                jsonObject["phone"] as String,
                jsonObject["name"] as String
            )
        }
    }

    suspend fun getVideoList(jsonObject: JSONObject): Resources<ContactsResponse> {
        return safeApiCall {
            retrofitService.groupVideoListData(jsonObject["phone"] as String)
        }
    }

    suspend fun sendVideo(jsonObject: JSONObject, videoFile: File): Resources<FriendResponse> {

        val fileBody: RequestBody = videoFile.asRequestBody("video/mp4".toMediaTypeOrNull())
        val videoPart: MultipartBody.Part =
            MultipartBody.Part.createFormData("video", videoFile.name, fileBody)

        return safeApiCall {
            retrofitService.sendVideo(
                jsonObject["friends"] as ArrayList<Int>,
                jsonObject["groups"] as ArrayList<Int>,
                jsonObject["phone"] as String,
                jsonObject["location"] as String,
                videoPart
            )
        }
    }

    suspend fun getStoryLstList(jsonObject: JSONObject): Resources<StoryDataResponse> {
        return safeApiCall {
            retrofitService.storyListData(jsonObject["phone"] as String)
        }
    }

}