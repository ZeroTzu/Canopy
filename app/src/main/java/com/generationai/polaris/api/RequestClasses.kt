package com.generationai.polaris.api
//TEMPORARY VALUES FOR TESTING
data class LoginRequest(val email:String,val password:String)

data class RegisterRequest(var username:String, var email:String, var password:String)

data class AddLocationRequest(var email:String, var latitude:Double, var longitude:Double, var altitude:Double)

