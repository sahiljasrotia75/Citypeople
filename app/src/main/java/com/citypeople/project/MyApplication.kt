package com.citypeople.project

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.upstream.cache.SimpleCache

import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApplication : Application() {

    companion object{
        var mInstance: MyApplication? = null
        fun getInstance() : MyApplication?{
            return mInstance
        }
        var notificationArrived = MutableLiveData<Boolean>()
        var mSessionNotification = MutableLiveData<Int?>()
        var isAppBackground = false

    }

    override fun onCreate() {
         super.onCreate()
        startKoin {   // 1
            androidContext(applicationContext)  // 3
            modules(listOf(networkModule,viewmodelModule))  // 4
        }
        mInstance = this
    }
}