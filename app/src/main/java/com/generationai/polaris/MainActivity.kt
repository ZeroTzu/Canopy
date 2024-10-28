package com.generationai.polaris

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
import android.view.View
import android.widget.Toast
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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.generationai.polaris.databinding.ActivityMainBinding
import com.generationai.polaris.ui.theme.PolarisTheme
import com.generationai.polaris.utils.Constants
import com.generationai.polaris.utils.UserClass
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
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

    private val dataStore: DataStore<Preferences> by lazy {DataStoreManager.getInstance(this)}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //check if the user is logged in
        lifecycleScope.launch{
            val email=getEmailFromDataStore()
            val password=getPasswordFromDataStore()
            Log.i("PolarisMainActivity", "onCreate: email: $email, password: $password")
            if (email==null || password==null){
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
            else{
                userClass= UserClass(email,password)
            }
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
        toolbar.setNavigationOnClickListener { v: View? -> drawerLayout.open() }

        binding.mainActivitySidePanel.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.home -> {
                    it.isChecked=true
                    replaceFragment(Home())
                    drawerLayout.close()
                }
                R.id.settings -> {
                    it.isChecked=true
                    replaceFragment(Settings())
                    drawerLayout.close()
                }
                R.id.history -> {
                    it.isChecked=true
                    replaceFragment(History())
                    drawerLayout.close()
                }
                R.id.command -> {
                    it.isChecked=true
                    replaceFragment(Command())
                    drawerLayout.close()
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
                        currentFragment.updateImage(imagePath)
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
    private suspend fun logoutUser(){
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
}

