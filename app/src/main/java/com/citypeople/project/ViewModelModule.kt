package com.citypeople.project

import com.citypeople.project.viewmodel.*
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val viewmodelModule: Module = module {
    viewModel { OtpViewModel(get()) }
    viewModel { FriendViewModel(get()) }
    viewModel { GroupViewModel(get()) }
    viewModel { VideoSendViewModel(get()) }
    viewModel { StoryViewModel(get()) }
}

