package com.citypeople.project.models.signin

import com.citypeople.project.R

data class StoryDataResponse(val videos: ArrayList<StoryModel>)

data class StoryModel(val id: Int, val user_id: Int, val name: String, var url: String, val location:String)

fun getDummyUser(): List<StoryModel> {
    return mutableListOf(
        StoryModel(
            name = "Invite",
            url = "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_960_720.png",
            id = 1,
            user_id = 11,
            location = ""
        ),
        StoryModel(
            name = "Invite",
            url = "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_960_720.png",
            id = 2,
            user_id = 22,
            location = ""
        ),
        StoryModel(
            name = "Invite",
            url ="https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_960_720.png",
            id = 3,
            user_id = 33,
            location = ""
        ),
        StoryModel(
            name = "Invite",
            url = "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_960_720.png",
            id = 4,
            user_id = 44,
            location = ""
        ),
        StoryModel(
            name = "Invite",
            url = "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_960_720.png",
            id = 5,
            user_id = 55,
            location = ""
        ),
        StoryModel(
            name = "Invite",
            url = "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_960_720.png",
            id = 6,
            user_id = 66,
            location = ""
        ),
        StoryModel(
            name = "Invite",
            url = "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_960_720.png",
            id = 7,
            user_id = 77,
            location = ""
        ),
        StoryModel(
            name = "Invite",
            url = "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_960_720.png",
            id = 8,
            user_id = 88,
            location = ""
        ),
    )
}


