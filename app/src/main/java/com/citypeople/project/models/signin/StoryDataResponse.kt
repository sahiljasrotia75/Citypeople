package com.citypeople.project.models.signin

data class StoryDataResponse(val videos: ArrayList<StoryModel>)

data class StoryModel(val id: Int, val user_id: Int, val name: String, var url: String, val location:String)


