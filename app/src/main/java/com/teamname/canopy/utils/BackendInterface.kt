package com.teamname.canopy.utils


import com.teamname.canopy.api.AddLocationRequest
import com.teamname.canopy.api.AddLocationResponse
import com.teamname.canopy.api.GetLocationResponse
import com.teamname.canopy.api.LoginRequest
import com.teamname.canopy.api.RegisterRequest
import com.teamname.canopy.api.LoginResponse
import com.teamname.canopy.api.RegisterResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import java.time.Instant

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
    fun getLocation(@Path ("email") email: String): Call<GetLocationResponse>

    @GET("/ords/admin/api/polaris/getLocation/{email}/{startTime}/{endTime}")
    fun getLocationFiltered(
        @Path ("email") email: String,
        @Path ("startTime") longitude: Instant,
        @Path ("endTime") latitude: Instant): Call<ResponseBody>
//
//    @POST("/api/users")
//    fun getUser(@Path("uid") uid: String,@Path()): Call<UserClass>
}