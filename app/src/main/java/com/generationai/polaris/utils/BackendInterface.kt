package com.generationai.polaris.utils


import com.generationai.polaris.api.AddLocationRequest
import com.generationai.polaris.api.AddLocationResponse
import com.generationai.polaris.api.LoginRequest
import com.generationai.polaris.api.RegisterRequest
import com.generationai.polaris.api.LoginResponse
import com.generationai.polaris.api.RegisterResponse
import retrofit2.Call
import retrofit2.http.*

interface BackendInterface {

    @POST("/ords/admin/api/polaris/registerUser")
    fun registerUser(@Body user: RegisterRequest): Call<RegisterResponse>

    //Actual implmentation, rmb to change the values in LoginResponse as well
//    @POST("loginUser")
//    fun loginUser(@Body loginRequest: LoginRequest): Call<LoginResponse>


    //TEMPORARY IMPLEMENTATION FOR TESTING
    @POST("/ords/admin/api/polaris/loginUser")
    fun loginUser(@Body loginRequest: LoginRequest): Call<LoginResponse>

    @POST("/ords/admin/api/polaris/addLocation")
    fun addLocation(@Body loginRequest: AddLocationRequest): Call<AddLocationResponse>

    @GET("/ords/admin/api/polaris/getLocation/{email}")
    fun getLocation(@Path ("email") email: String): Call<AddLocationResponse>

    @GET("/ords/admin/api/polaris/getLocation/{email}/{latitude}/{longitude}")
    fun getFilteredLocation(
        @Path ("email") email: String,
        @Path ("longitude") longitude: Float,
        @Path ("latitude") latitude: Float): Call<AddLocationResponse>

//
//    @POST("/api/users")
//    fun getUser(@Path("uid") uid: String,@Path()): Call<UserClass>
}