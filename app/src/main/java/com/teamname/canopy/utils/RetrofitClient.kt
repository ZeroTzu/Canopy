package com.teamname.canopy.utils

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
//    private const val BASE_URL = "https://reqres.in//"
    private const val BASE_URL = "https://g01d672bcca6461-madhacksadw.adb.us-chicago-1.oraclecloudapps.com"
    // Lazy initialization of Retrofit instance
    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    // Function to get UserService
    fun getBackendInterface(): BackendInterface {
        return instance.create(BackendInterface::class.java)
    }
    // You can add more services here for other APIs if needed in the future
}