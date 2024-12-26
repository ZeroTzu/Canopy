package com.teamname.canopy

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.TwoLineListItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.teamname.canopy.databinding.ActivityMainBinding
import com.teamname.canopy.ui.theme.PolarisTheme
import com.teamname.canopy.utils.Constants
import com.teamname.canopy.utils.UserClass
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import com.teamname.canopy.utils.Canopy
import com.teamname.canopy.utils.CanopyCustomAdapter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore by preferencesDataStore(name = "user_prefs")

object DataStoreManager {
    private lateinit var dataStoreInstance: DataStore<Preferences>
    fun getInstance(context: Context): DataStore<Preferences> {
        if (!::dataStoreInstance.isInitialized) {
            dataStoreInstance = context.dataStore // Initialize it once
        }
        return dataStoreInstance
    }
}

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var serviceStatusReceiver: BroadcastReceiver
    private var isBackgroundServiceRunning: Boolean = false
    private val pingInterval: Long = 2000
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var userClass: UserClass
    private val viewModel = viewModels<MainActivityViewModel>()
    lateinit var firebaseAuth: FirebaseAuth
    lateinit var firestore: FirebaseFirestore
    private val dataStore: DataStore<Preferences> by lazy {DataStoreManager.getInstance(this)}
    private lateinit var userDocumentId:String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(baseContext)
        firebaseAuth=FirebaseAuth.getInstance()
        firestore=FirebaseFirestore.getInstance()
        var db = Firebase.firestore
        db.collection("canopies")
            .get()
            .addOnSuccessListener {
                result ->
                var tempCanopyList = ArrayList<Canopy>()
                for (canopy in result) {
                    val name = canopy.data["canopyName"]
                    val coords = canopy.data["canopyCoords"] as GeoPoint
                    val owner = canopy.data["canopyOwner"]
                    val address = canopy.data["canopyAddress"]
                    val indoorMapPoints = canopy.data["indoorMapPoints"] as ArrayList<HashMap<String, *>>

                    val latitude = coords.latitude
                    val longitude = coords.longitude

                    val canopyIndoorMapPoints = ArrayList<Canopy.CanopyIndoorPoint>()
                    for (point in indoorMapPoints) {
                        val topMarginString = point["marginTop"].toString()
                        val marginStartString = point["marginStart"].toString()

                        Log.i("CanopyMainActivity" , topMarginString + " "+ marginStartString)
                        val topMargin = topMarginString!!.toLong()
                        val marginStart = marginStartString!!.toLong()
                        Log.i("CanopyMainActivity" , latitude.toString() + " "+ longitude.toString() + " " + name.toString() + " " +owner.toString() + " " + address.toString() + " " + topMargin.toString() + " " + marginStart.toString())

                        val indoorMapPoint = Canopy.CanopyIndoorPoint(topMargin!!, marginStart!!)
                        canopyIndoorMapPoints.add(indoorMapPoint)
                    }
                    val canopies = Canopy(name.toString(), GeoPoint(latitude, longitude), owner.toString(), address.toString(), canopyIndoorMapPoints)
                    tempCanopyList.add(canopies)
                    Log.i("CanopyMainActivity" , latitude.toString() + " "+ longitude.toString() + " " + name.toString() + " " +owner.toString() + " " + address.toString() + " " + indoorMapPoints.toString())

                }
                viewModel.value.setCanopiesList(tempCanopyList)
                val canopyList = viewModel.value.getCanopiesList().value

                if (canopyList != null) {
                    for (canopy in canopyList) {
                        Log.i("CanopyMainActivity", "Name: ${canopy.canopyName}, " +
                                "Coords: (${canopy.canopyCoords.latitude}, ${canopy.canopyCoords.longitude}), " +
                                "Owner: ${canopy.canopyOwner}, " +
                                "Address: ${canopy.canopyAddress}")
                    }
                } else {
                    Log.i("CanopyMainActivity", "No data found in canopiesList.")
                }

            }
            .addOnFailureListener {
                exception ->
                Log.i("CanopyMainActivity" , exception.toString())
            }

        //check if user is logged in under firebase
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            reload()
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //Request permissions (e.g. Camera, Audio etc.)
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }
        replaceFragment(Home())

        val drawerLayout=binding.mainActivityDrawerLayout
        val toolbar = binding.mainActivityMaterialToolbar
        toolbar.setNavigationOnClickListener { drawerLayout.open() }
        binding.mainActivitySidePanel.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.side_panel_home -> {
                    for (i in 0 until binding.mainActivitySidePanel.menu.size()) {
                        binding.mainActivitySidePanel.menu.getItem(i).isChecked = false
                    }
                    it.isChecked = true
                    replaceFragment(Home())
                    drawerLayout.close()
                }

                R.id.side_panel_home -> {
                    for (i in 0 until binding.mainActivitySidePanel.menu.size()) {
                        binding.mainActivitySidePanel.menu.getItem(i).isChecked = false
                    }
                    it.isChecked = true
                    replaceFragment(Settings())
                    drawerLayout.close()
                }

                R.id.side_panel_volunteer_history -> {
                    for (i in 0 until binding.mainActivitySidePanel.menu.size()) {
                        binding.mainActivitySidePanel.menu.getItem(i).isChecked = false
                    }
                    it.isChecked = true
                    replaceFragment(VolunteerHistoryFragment())
                    drawerLayout.close()
                }

                R.id.side_panel_profile -> {
                    for (i in 0 until binding.mainActivitySidePanel.menu.size()) {
                        binding.mainActivitySidePanel.menu.getItem(i).isChecked = false
                    }
                    it.isChecked = true
                    replaceFragment(Command())
                    drawerLayout.close()
                }


                R.id.side_panel_manage_family -> {}
                R.id.side_panel_logout -> {
                    drawerLayout.close()
                    lifecycleScope.launch {
                        logoutUser()
                    }
                }

            }
            true
        }
        serviceStatusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == PolarisBackgroundService.Actions.SERVICE_IMAGE_TAKEN.toString()) {
                    val imagePath = intent.getStringExtra("imagePath")
                    Log.i("PolarisMainActivity", "=imageReceiver received imagePath: $imagePath")
                    val currentFragment =
                        supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                    if (currentFragment is Home) {
                        //change the imageview to show the image
//                        currentFragment.updateImage(imagePath)
                    }
                }
                else if (intent.action == PolarisBackgroundService.Actions.SERVICE_LOCATION_TAKEN.toString()) {
                    val location: Location? = intent.getParcelableExtra("location",Location::class.java)
                    val currentFragment=supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                    if (location!=null){
                        val locationTime=location.time
                        val locationLongitude=location.longitude
                        val locationLatitude=location.longitude
                        val locationAltitude=location.altitude
                        val locationSpeed=location.speed
                        val locationAccuracy=location.accuracy
                        val locationBearing=location.bearing
                        Log.i("PolarisMainActivity", "serviceStatusReceiver received location Value: [ longitude: $locationLongitude, latitude: $locationLatitude, altitude: $locationAltitude, bearing: $locationBearing,time:$locationTime, speed: $locationSpeed, accuracy: $locationAccuracy]")
                    }else{
                        Log.i("PolarisMainActivity", "serviceStatusReceiver received location Value: null")
                    }
                    if (currentFragment is Home){
                        return
                    }
                }
                else if (intent.action == PolarisBackgroundService.Actions.SERVICE_STATUS_RESPONSE.toString()) {
                    val isServiceRunning = intent.getBooleanExtra("isRunning", false)
                    handleServiceStatus(isServiceRunning)
                    Log.i("PolarisMainActivity", "serviceStatusReceiver received isRunning Value: $isServiceRunning")
                    val currentFragment=supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                    if (currentFragment is Home){
                        currentFragment.updateServiceStatus(isServiceRunning)
                    }
                }
                else{
                    Log.i("PolarisMainActivity", "serviceStatusReceiver received unknown action: ${intent.action}")
                }
            }
        }

        val intentFilter = IntentFilter().apply{
            addAction(PolarisBackgroundService.Actions.SERVICE_STATUS_RESPONSE.toString())
            addAction(PolarisBackgroundService.Actions.SERVICE_IMAGE_TAKEN.toString())
            addAction(PolarisBackgroundService.Actions.SERVICE_LOCATION_TAKEN.toString())

        }
        registerReceiver(serviceStatusReceiver,intentFilter, RECEIVER_EXPORTED)
    }
    private fun reload(){
        val intent = Intent(this@MainActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun pingService() {
        // Send a broadcast to the service to check if it's running
        val intent = Intent(PolarisBackgroundService.Actions.CHECK_SERVICE_STATUS.toString())
        sendBroadcast(intent)
        Log.i("PolarisMainActivity", "ping Ran")
    }
    private fun handleServiceStatus(isRunning: Boolean) {
        Log.i("PolarisMainActivity", "isRunning: $isRunning")
        if (viewModel.value.isSertviceRunning.value!=isRunning) {
            viewModel.value.setIsBackgroundServiceRunning(isRunning)
            Log.i("PolarisMainActivity", "isBackgroundServiceRunning: ${viewModel.value.isSertviceRunning.value}")
        }

    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.nav_host_fragment, fragment)
        fragmentTransaction.commit()

    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        )
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(
                    baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startCamera()
            }
        }

    private suspend fun getEmailFromDataStore(): String? {
        return dataStore.data.map { preferences ->
            preferences[Constants.EMAIL_KEY]  // Use the stringPreferencesKey to access the value
        }.firstOrNull()
    }
    suspend fun logoutUser(){

        val intent = Intent(this@MainActivity, LoginActivity::class.java)
        var mainViewModel = ViewModelProvider(this@MainActivity)[MainActivityViewModel::class.java]
        if(mainViewModel.isSertviceRunning.value){
            //Stop the background service before logging out
            Intent(this, PolarisBackgroundService::class.java).also {
                it.action = PolarisBackgroundService.Actions.STOP.toString()
                this@MainActivity.startService(it)
            }
        }
        firebaseAuth.signOut()
        //start login activity and stop the MainActivity so that user cant navigate back
        startActivity(intent)
        this@MainActivity.finish()
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private suspend fun getPasswordFromDataStore(): String? {
        return dataStore.data.map { preferences ->
            preferences[Constants.PASSWORD_KEY]  // Use the stringPreferencesKey to access the value
        }.firstOrNull()
    }
    private fun captureVideo() {}

    private fun startCamera() {}

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }


    companion object {
        private const val TAG = "Polaris"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.FOREGROUND_SERVICE_CAMERA,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION,
                Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
                Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PolarisTheme {
        Greeting("Android")
    }
}

class MainActivityViewModel : ViewModel() {
    var isSertviceRunning = mutableStateOf(false)
        private set
    fun setIsBackgroundServiceRunning(isRunning: Boolean) {
        isSertviceRunning.value = isRunning
    }

    private var canopiesList = MutableLiveData<List<Canopy>>()

    fun setCanopiesList(canopyList: List<Canopy>) {
        canopiesList.value = canopyList
    }
    fun getCanopiesList(): MutableLiveData<List<Canopy>> {
        return canopiesList
    }
}

