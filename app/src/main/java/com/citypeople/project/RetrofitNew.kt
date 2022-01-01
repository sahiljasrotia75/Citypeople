package com.citypeople.project

import com.citypeople.project.BuildConfig.DEBUG
import com.citypeople.project.repo.AuthRepo
import com.citypeople.project.retrofit.RetrofitService
import com.citypeople.project.utilities.extensions.gson
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GetTokenResult
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.koin.dsl.module
import retrofit2.Retrofit
import okhttp3.MediaType.Companion.get
import okhttp3.Interceptor

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


val retrofitModule = module {    // 1

    single {
        retrofit(Constants.baseUrl)  // 4
    }

    single {
        AuthRepo(get())
    }

    single {
        get<Retrofit>().create(RetrofitService::class.java)   // 5
    }
}

var httpClient: OkHttpClient = OkHttpClient.Builder()
    .addInterceptor(Interceptor { chain ->
        val ongoing = chain.request().newBuilder()
        ongoing.addHeader("Authorization", "Bearer " + getCurrentUserToken { token -> token })
        chain.proceed(ongoing.build())
    })
    .build()


private fun retrofit(baseUrl: String) = Retrofit.Builder()
    .callFactory(OkHttpClient.Builder().build())
    .baseUrl(baseUrl)
    // .client(httpClient)
    .addConverterFactory(GsonConverterFactory.create(gson))
    // .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))  // 6
    .build()


private fun getCurrentUserToken(callback: (token: String) -> Unit) {
    val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    val currentUser = mAuth.currentUser
    currentUser?.getIdToken(false)
        ?.addOnSuccessListener(OnSuccessListener<GetTokenResult> { result ->
            val idToken = result.token
            val expiryTime = result.expirationTimestamp
            val tsLong = System.currentTimeMillis() / 1000
            if (expiryTime - tsLong > 0) {
                callback(idToken.toString())
            } else {
                currentUser.getIdToken(true)
                    .addOnSuccessListener(OnSuccessListener<GetTokenResult> { result ->
                        val idToken = result.token
                        callback(idToken.toString())
                    })
            }

        })

}

val networkModule = module {
    val connectTimeout: Long = 40// 20s
    val readTimeout: Long = 40 // 20s

    fun provideHttpClient(): OkHttpClient {
        val okHttpClientBuilder = OkHttpClient.Builder()
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
        if (DEBUG) {
            val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            okHttpClientBuilder.addInterceptor(httpLoggingInterceptor)
        }
        okHttpClientBuilder.addInterceptor(Interceptor { chain ->
            val ongoing = chain.request().newBuilder()
            ongoing.addHeader(
                "Authorization",
                "Bearer " + getCurrentUserToken { token -> token })
            chain.proceed(ongoing.build())
        })
        okHttpClientBuilder.build()
        return okHttpClientBuilder.build()
    }

    fun provideRetrofit(client: OkHttpClient, baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(client)
            .build()
    }

    single { provideHttpClient() }
    single {
        val baseUrl = Constants.baseUrl
        provideRetrofit(get(), baseUrl)
    }
    single {
        AuthRepo(get())
    }

    single {
        get<Retrofit>().create(RetrofitService::class.java)
    }// 5
}




