package com.teamname.canopy.utils

import java.time.Instant

data class UserClass(val email:String, val uid:String) {
     var name:String?=null
     var phoneNumber:String?=null
     var joinedDate:Instant?=null
     var points:Int?=null

}