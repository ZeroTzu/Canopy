package com.generationai.polaris.api

import java.sql.Timestamp
import java.time.Instant

class LoginResponse{
    var uid:String? = null
    var username:String?=null
    var email:String?=null
    var sessionToken:String?=null
    var status:String?=null
    var code:String?=null
    var message:String?=null
}

class RegisterResponse {
    var uid:String? = null
    var username:String?=null
    var email:String?=null
    var sessionToken:String?=null
    var password:String?=null
    var status:String?=null
    var code:String?=null
    var message:String?=null
}
class AddLocationResponse {
    var code:Int?=null
    var status:String?=null
    var message:String?=null
    var latitude:Double?=null
    var longitude:Double?=null
    var altitude:Double?=null
    var timestamp:Timestamp?=null

}

class GetLocationResponse{
    var code:Int?=null
    var status:String?=null
    var message:String?=null
    var locations:ArrayList<LocationItem>?=null
}
class LocationItem{
    var latitude:Double?=null
    var longitude:Double?=null
    var altitude:Float?=null
    var timestamp:Instant?=null
}