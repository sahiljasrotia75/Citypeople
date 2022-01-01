package com.citypeople.project.models.signin

data class DummyModel(
    val categories: List<Category>
)

class MediaObject(var title: String? = null,
                  var media_url: String? = null,
                  var thumbnail: String? = null,
                  var description: String? = null) {

}
