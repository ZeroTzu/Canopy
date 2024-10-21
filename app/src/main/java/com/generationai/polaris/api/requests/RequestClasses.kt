package com.generationai.polaris.api.requests

//Actual values, rmb to change temp values in LoginFormFragment,RequestClasses as well
//data class LoginRequest(val email:String,val password:String)

//TEMPORARY VALUES FOR TESTING
data class LoginRequest(val name:String,val job:String)

data class RegisterRequest(var username:String, var email:String, var password:String)