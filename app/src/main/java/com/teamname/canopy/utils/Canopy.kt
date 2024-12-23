package com.teamname.canopy.utils

import com.google.firebase.firestore.GeoPoint

data class Canopy (val canopyName: String, val canopyCoords: GeoPoint, val canopyOwner: String, val canopyAddress: String)