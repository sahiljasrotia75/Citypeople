package com.citypeople.project.retrofit

import com.citypeople.project.models.signin.ContactsResponse
import com.citypeople.project.models.signin.FriendResponse
import com.citypeople.project.models.signin.StoryDataResponse
import com.citypeople.project.models.signin.UserResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.http.*


interface RetrofitService {

    @FormUrlEncoded
    @POST("user")
    suspend fun userData(@FieldMap map: HashMap<String, String>): Response<UserResponse>

    @FormUrlEncoded
    @POST("contacts")
    suspend fun contactData( @Field("contacts[]") contacts: ArrayList<String>,
                             @Field("phone") phone: String): Response<ContactsResponse>

    @FormUrlEncoded
    @POST("friends/add")
    suspend fun friendData( @Field("ids[]") ids: ArrayList<Int>,
                             @Field("phone") phone: String): Response<FriendResponse>

    @FormUrlEncoded
    @POST("friends")
    suspend fun groupContactData(@Field("phone") phone: String): Response<ContactsResponse>

    @FormUrlEncoded
    @POST("groups/create")
    suspend fun groupData( @Field("ids[]") ids: ArrayList<Int>,
                            @Field("phone") phone: String,
        @Field("name") name: String): Response<FriendResponse>

    @FormUrlEncoded
    @POST("friendsngroups")
    suspend fun groupVideoListData(@Field("phone") phone: String): Response<ContactsResponse>

    @Multipart
    @POST("videos/upload")
    suspend fun sendVideo(@Query("friends[]") friends: ArrayList<Int>,
                          @Query("groups[]") groups: ArrayList<Int>,
                          @Query("phone") phone: String,
                          @Part video: MultipartBody.Part): Response<FriendResponse>

    @FormUrlEncoded
    @POST("videos")
    suspend fun storyListData(@Field("phone") phone: String): Response<StoryDataResponse>

}