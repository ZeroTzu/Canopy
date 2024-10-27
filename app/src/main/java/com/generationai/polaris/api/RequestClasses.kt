package com.generationai.polaris.api
//TEMPORARY VALUES FOR TESTING
data class LoginRequest(val email:String,val password:String)

data class RegisterRequest(var username:String, var email:String, var password:String)

data class AddLocationRequest(var email:String, var latitude:Double, var longitude:Double, var altitude:Double)

data class getLocationRequest(var email:String)
data class getLocationRequestFiltered(var email:String, var startDate:String, var endDate:String)

data class getFamilyRequest(var email:String, var startDate:String, var endDate:String)
data class addFamilyRequest(var email:String, var startDate:String, var endDate:String)
