package com.generationai.polaris.api.requests
//TEMPORARY VALUES FOR TESTING
data class LoginRequest(val email:String,val password:String)

data class RegisterRequest(var username:String, var email:String, var password:String)