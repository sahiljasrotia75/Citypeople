package com.citypeople.project

import android.app.Application
import android.content.Context
import com.google.android.exoplayer2.upstream.cache.SimpleCache

import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
//import com.danikula.videocache.HttpProxyCacheServer

class MyApplication : Application() {

    companion object{
        var mInstance: MyApplication? = null
         var simpleCache: SimpleCache?= null
        fun getInstance() : MyApplication?{
            return mInstance
        }
    }

    override fun onCreate() {
         super.onCreate()
        startKoin {   // 1
            // androidLogger(Level.DEBUG)  // 2
            androidContext(applicationContext)  // 3
            modules(listOf(networkModule,viewmodelModule))  // 4
        }
        mInstance = this
    }

    /*private var proxy: HttpProxyCacheServer? = null

    fun getProxy(context: Context): HttpProxyCacheServer? {
        val app: MyApplication = context.applicationContext as MyApplication
        return app.proxy ?: app.newProxy().also { app.proxy = it }
    }

    private fun newProxy(): HttpProxyCacheServer? {
        return HttpProxyCacheServer(this)
    }*/
}