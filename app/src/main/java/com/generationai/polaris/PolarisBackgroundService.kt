package com.generationai.polaris

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.generationai.polaris.api.AddLocationRequest
import com.generationai.polaris.api.AddLocationResponse
import com.generationai.polaris.utils.BackendInterface
import com.generationai.polaris.utils.Constants
import com.generationai.polaris.utils.RetrofitClient
import com.generationai.polaris.utils.UserClass
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat

class PolarisBackgroundService : LifecycleService(){

    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val handler = Handler(Looper.getMainLooper())
    private val pingInterval: Long = 2000
    private lateinit var backendInterface: BackendInterface
    private val dataStore: DataStore<Preferences> by lazy {DataStoreManager.getInstance(this)}
    private lateinit var user:UserClass
    private val sendServiceStatusRunnable = object : Runnable {
        override fun run() {
            sendServiceStatus(true)
            // Schedule the next ping after 2 seconds
            handler.postDelayed(this, pingInterval)
        }
    }
    private val captureImageRunnable = object : Runnable {
        override fun run() {
            takePicture() // Capture a picture
            handler.postDelayed(this, 5000) //
        }
    }
    private val captureLocationRunnable = object : Runnable {
        override fun run() {
            getCurrentLocation() // get the current location
            handler.postDelayed(this, 5000) //
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("PolarisBackgroundService", "onStartCommand method called")
        when(intent?.action){
            Actions.START.toString() -> start()
            Actions.STOP.toString() -> stopSelf()
        }
        return super.onStartCommand(intent, flags, startId);
    }

    override fun onDestroy() {
        Log.i("PolarisBackgroundService", "onDestroy()")
        super.onDestroy()
        handler.removeCallbacks(captureImageRunnable)
        handler.removeCallbacks(sendServiceStatusRunnable)
        handler.removeCallbacks(captureLocationRunnable)
        sendServiceStatus(false)

        unregisterReceiver(serviceStatusReceiver)
    }
    override fun onCreate() {
        Log.i("PolarisBackgroundService", "onCreate()")
        super.onCreate()
    }

    private fun createNotification() :Notification{
        val notification = NotificationCompat.Builder(this, "PolarisBackgroundService")
            .setContentTitle(resources.getString(R.string.background_service_title))
            .setContentText(resources.getString(R.string.background_service_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setChannelId(Constants.BACKGROUND_SERVICE_CHANNEL_ID)
            .build()
        return notification
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            // CameraProvider
            cameraProvider = cameraProviderFuture.get()

            // Set up ImageCapture UseCase
            imageCapture = ImageCapture.Builder()
                .setTargetRotation(Surface.ROTATION_0) // Adjust as needed
                .build()

            // Bind the camera lifecycle
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, // Pass lifecycle owner
                    cameraSelector,
                    imageCapture
                )
                handler.post(captureImageRunnable)
            // Now capture a picture
            } catch (exc: Exception) {
                Log.e("PolarisBackgroundService", "Use case binding failed", exc)
            }
        },ContextCompat.getMainExecutor(this))
    }
    private fun start(){
        startForeground(Constants.BACKGROUND_SERVICE_RUNNING_NOTIFICATION_ID, createNotification())
        //Check for Camera and Microphone, and Location permissions stop the service if any of the permissions are not granted
        val cameraPermission = checkSelfPermission(android.Manifest.permission.CAMERA)
        val microphonePermission = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
        val locationPermission = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
        backendInterface = RetrofitClient.getBackendInterface()


        lifecycleScope.launch {

            val email=getEmailFromDataStore().toString()
            val password=getPasswordFromDataStore().toString()
            user= UserClass(email,password)
        }
        if (cameraPermission == PackageManager.PERMISSION_GRANTED &&
            microphonePermission == PackageManager.PERMISSION_GRANTED &&
            locationPermission == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }else{
            stopSelf()
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val intentFilter = IntentFilter(Actions.CHECK_SERVICE_STATUS.toString())
        registerReceiver(serviceStatusReceiver,intentFilter, RECEIVER_EXPORTED)
        handler.post(sendServiceStatusRunnable)
        handler.post(captureLocationRunnable)
    }

    private val serviceStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Actions.CHECK_SERVICE_STATUS.toString()) {
                Log.i("PolarisBackgroundService", "Received ${Actions.CHECK_SERVICE_STATUS} ping")
                sendServiceStatus(false)
            }
        }
    }
    private fun getCurrentLocation(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //TODO Make a system to request for permissions and relaunch the service
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                // Got last known location. In some rare situations this can be null.
                if (location!=null){
                    sendCurrentLocation(location)
                }
            }
    }
    private fun sendCurrentLocation(location:Location){
        val locationIntent = Intent(Actions.SERVICE_LOCATION_TAKEN.toString()).apply {
            putExtra("location", location)
        }
        val latitude =location.latitude
        val longitude =location.longitude
        val altitude =location.altitude
        val addLocationRequest = AddLocationRequest(user.email,latitude,longitude,altitude)
        backendInterface.addLocation(addLocationRequest).enqueue(object: Callback<AddLocationResponse>{
            override fun onResponse(call: Call<AddLocationResponse>, response: Response<AddLocationResponse>)
            {
                val addLocationResponse = response.body()?:return

                Log.i("PolarisBackgroundService","addLocation Callback: ${addLocationResponse.status.toString()}")
            }

            override fun onFailure(call: Call<AddLocationResponse>, e: Throwable) {
                    Log.i("PolarisBackgroundService","addLocation Callback Failure: $e")
            }
        })
        sendBroadcast(locationIntent)
    }
    private suspend fun getEmailFromDataStore(): String? {
        return dataStore.data.map { preferences ->
            preferences[Constants.EMAIL_KEY]  // Use the stringPreferencesKey to access the value
        }.firstOrNull()
    }
    private suspend fun getPasswordFromDataStore(): String? {
        return dataStore.data.map { preferences ->
            preferences[Constants.PASSWORD_KEY]  // Use the stringPreferencesKey to access the value
        }.firstOrNull()
    }
    private fun sendServiceStatus(isRunning:Boolean) {
        // Send a broadcast back to the activity with the service status
        val statusIntent = Intent(Actions.SERVICE_STATUS_RESPONSE.toString()).apply {
            putExtra("isRunning", isRunning)
        }
        sendBroadcast(statusIntent)
        Log.i("PolarisBackgroundService", "status sent" +
                "")

    }

    private fun sendServiceImage(imagePath:String) {
        // Send a broadcast back to the activity with the service status
        val imageIntent = Intent(Actions.SERVICE_IMAGE_TAKEN.toString()).apply {
            putExtra("imagePath", imagePath)
        }
        sendBroadcast(imageIntent)
        Log.i("PolarisBackgroundService", "image sent")
    }
    private fun sendLocation(email:String, latitude:Float, longitude:Float){

    }
    private fun takePicture() {
        // Create output file options (save the image to storage)
        val file = File(externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        // Take picture and handle the result
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("PolarisBackgroundService", "Image saved: ${file.absolutePath}")
                    sendServiceImage(file.absolutePath)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("PolarisBackgroundService", "Image capture failed", exc)
                }
            }
        )
    }
    enum class Actions(private val action: String) {
        START("com.generationai.polaris.action.START"),
        STOP("com.generationai.polaris.action.STOP"),
        CHECK_SERVICE_STATUS("com.generationai.polaris.action.CHECK_SERVICE_STATUS"),
        SERVICE_STATUS_RESPONSE("com.generationai.polaris.action.SERVICE_STATUS_RESPONSE"),
        SERVICE_IMAGE_TAKEN("com.generationai.polaris.action.SERVICE_IMAGE_TAKEN"),
        SERVICE_LOCATION_TAKEN("com.generationai.polaris.action.SERVICE_LOCATION_TAKEN")
    }



}