package com.citypeople.project.models.signin

import com.citypeople.project.R

data class DummyUserModel(
    var text: String,
    var image: Int
)

fun getDummyItem(): List<DummyUserModel> {
    return mutableListOf(
        DummyUserModel(
            text = "Invite",
            image = R.drawable.ic_baseline_person_24,
        ),
        DummyUserModel(
            text = "Invite",
            image = R.drawable.ic_baseline_person_24,
        ),
        DummyUserModel(
            text = "Invite",
            image = R.drawable.ic_baseline_person_24,
        ),
        DummyUserModel(
            text = "Invite",
            image = R.drawable.ic_baseline_person_24,

            ),
        DummyUserModel(
            text = "Invite",
            image = R.drawable.ic_baseline_person_24,
        ),
        DummyUserModel(
            text = "Invite",
            image = R.drawable.ic_baseline_person_24,
        ),
        DummyUserModel(
            text = "Invite",
            image = R.drawable.ic_baseline_person_24,
        ),
        DummyUserModel(
            text = "Invite",
            image = R.drawable.ic_baseline_person_24,
        )
    )
}