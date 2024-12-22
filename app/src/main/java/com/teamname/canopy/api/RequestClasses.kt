package com.teamname.canopy.api

import java.time.Instant

//TEMPORARY VALUES FOR TESTING
data class LoginRequest(val email:String,val password:String)

data class RegisterRequest(var username:String, var email:String, var password:String)

data class AddLocationRequest(var email:String, var latitude:Double, var longitude:Double, var altitude:Double)

data class GetLocationRequest(var email:String)
data class GetLocationFilteredRequest(var email:String, var startDate: Instant, var endDate:Instant)

data class GetFamilyRequest(var email:String, var startDate:String, var endDate:String)
data class GddFamilyRequest(var email:String, var startDate:String, var endDate:String)
