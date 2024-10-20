package com.generationai.polaris.utils


import com.generationai.polaris.api.requests.LoginRequest
import com.generationai.polaris.api.requests.RegisterRequest
import com.generationai.polaris.api.responses.LoginResponse
import com.generationai.polaris.api.responses.RegisterResponse
import retrofit2.Call
import retrofit2.http.*

interface BackendInterface {

    @POST("registerUser")
    fun registerUser(@Body user: RegisterRequest): Call<RegisterResponse>


    //Actual implmentation, rmb to change the values in LoginResponse as well
//    @POST("loginUser")
//    fun loginUser(@Body loginRequest: LoginRequest): Call<LoginResponse>
//
//
    //TEMPORARY IMPLEMENTATION FOR TESTING
    @POST("/api/users")
    fun loginUser(@Body loginRequest: LoginRequest): Call<LoginResponse>

    @GET("user/{uid}")
    fun getUser(@Path("uid") uid: String): Call<UserClass>
}