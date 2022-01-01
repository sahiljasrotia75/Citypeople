package com.citypeople.project.utilities.extensions

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken



val gson = Gson()

//convert a data class to a map
fun <T> T.serializeToMap(): HashMap<String, String> {
    return convert()
}

//convert a map to a data class
inline fun <reified T> HashMap<String, String>.toDataClass(): T {
    return convert()
}

//convert an object of type I to type O
inline fun <I, reified O> I.convert(): O {
    val json = gson.toJson(this)
    return gson.fromJson(json, object : TypeToken<HashMap<String , String>>() {}.type)
}
