package com.citypeople.project.retrofit

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.json.JSONException
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.CancellationException



inline fun <T>safeApiCall(call: () -> Response<T>): Resources<T> {
       runCatching {
            call.invoke()
        }.onSuccess {
            return if(it.isSuccessful){
                Resources.success( it.body())
            }else{
                return try {
                    if (it.code()==401){
                        Resources.error(ErrorResponse(it.code(),""))
                    }else{
                        val fromJson = Gson().fromJson(
                            it.errorBody()?.string(),
                            ErrorResponse::class.java
                        )
                        if (fromJson!=null){
                            Resources.error(ErrorResponse(fromJson.code,fromJson.message))
                        }else Resources.error(ErrorResponse())
                    }
                }catch (e:Exception){
               //     Timber.e(e)
                    Resources.error(ErrorResponse())
                }
            }
        }.onFailure {e->
            return when (e) {
                is JSONException -> Resources.error(
                    ErrorResponse(900,"Internal Error occurred")
                )
                is CancellationException->{
                    Resources.error(
                        ErrorResponse(499,"")
                    )
                }
                is Failure.NetworkConnection->{
                    Resources.error(
                        ErrorResponse(903, e.networkErrorMessage)
                    )
                }
                else -> Resources.error(
                    ErrorResponse(901)
                )
            }
        }
    return Resources.error(ErrorResponse(902,"An Error occurred"))
}


data class Error(
    @SerializedName("error")
    val error: String?="Something went wrong!"
)

data class ErrorResponse(
    @SerializedName("code")
    val code: Int=0,
    @SerializedName("message")
    val message: String="Something went wrong!",
    @SerializedName("error")
    val error: Error?= Error(),
    @SerializedName("valid")
    val valid: Boolean=false
)

suspend fun safeMediaUpload(call: suspend () -> Response<Void>): Resources<Void> {
    runCatching {
        call.invoke()
    }.onSuccess {
        return if(it.isSuccessful&&it.message()=="OK"){
            Resources.success(null)
        }else{
            Resources.error(ErrorResponse(it.code(),"Something went wrong!"))
        }
    }.onFailure {e->
        return when (e) {
            is JSONException -> Resources.error(
                ErrorResponse(900,"Internal Error occurred")
            )
            is CancellationException->{
                Resources.error(
                    ErrorResponse(499,"")
                )
            }
            is Failure.NetworkConnection->{
                Resources.error(
                    ErrorResponse(903, e.networkErrorMessage)
                )
            }
            else -> Resources.error(
                ErrorResponse(901,"Something went wrong")
            )
        }
    }
    return Resources.error(ErrorResponse(902,"An Error occurred"))
}

sealed class Failure(val errorMessage: String, val error:Int=-1): IOException() {
    data class NetworkConnection    (
        val networkErrorMessage:String="No Internet connection",val errorCode: Int=-1
    ) : Failure(networkErrorMessage,errorCode)
}