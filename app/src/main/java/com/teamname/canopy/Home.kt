package com.teamname.canopy
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.teamname.canopy.databinding.FragmentHomeBinding
import com.teamname.canopy.utils.Canopy
import com.teamname.canopy.utils.CanopyCustomAdapter
import com.teamname.canopy.utils.CanopyRecyclerViewAdapter
import com.teamname.canopy.utils.Constants
import com.teamname.canopy.utils.MLServiceRequest.ResponseCallback
import com.teamname.canopy.utils.THLDataPoint
import kotlinx.coroutines.selects.select
import org.chromium.net.CronetEngine
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Home.newInstance] factory method to
 * create an instance of this fragment.
 */
class Home : Fragment(),ResponseCallback, OnMapReadyCallback{
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding: FragmentHomeBinding
    private lateinit var cronetEngine: CronetEngine
    private lateinit var executor: Executor
    private lateinit var notificationManager: NotificationManager
    private lateinit var activityManager: ActivityManager
    private lateinit var mainViewModel: MainActivityViewModel
    private lateinit var selectedCanopy : MutableLiveData<Canopy?>
    private lateinit var dataPointViews: ArrayList<RelativeLayout>
    private var mMap: GoogleMap? = null

    private val getTemperatureDataRunnable =  object :Runnable {
        override fun run() {
            for (view in dataPointViews){
                val thl = generateTHLData()
                updateDataPointView(thl,view)
            }
            handler.postDelayed(this, 5000)
        }
    }
    private val handler = Handler (Looper.getMainLooper())
    private fun updateDataPointView(thl: THLDataPoint,relativeLayout: RelativeLayout) {
        relativeLayout.findViewById<TextView>(R.id.data_point_text).text = "${thl.temperature}°C, ${thl.humidity}%, ${thl.lightIntensity}µmol/m²/s"
    }
    private fun generateTHLData(): THLDataPoint {
        val random = java.util.Random()

        // Simulate slight temperature fluctuations between -1.0 and 1.0 degrees Celsius
        val temperatureChange = -0.5 + random.nextDouble() * 1.0  // Random number between -1.0 and 1.0
        val temperature = 24.3 + temperatureChange

        // Simulate slight humidity fluctuations between -5% and 5%
        val humidityChange = -2 + random.nextDouble() * 4  // Random number between -5 and 5
        val humidity = 60 + humidityChange

        // Simulate slight light intensity fluctuations between -50 and 50 µmol/m²/s
        val lightIntensityChange = -30 + random.nextDouble() * 60  // Random number between -50 and 50
        val lightIntensity = 800 + lightIntensityChange

        //round off
        val temperatureRounded = String.format("%.1f", temperature).toDouble()
        val humidityRounded = String.format("%.0f", humidity).toDouble()
        val lightIntensityRounded = String.format("%.0f", lightIntensity).toDouble()
        return THLDataPoint(temperatureRounded, humidityRounded, lightIntensityRounded)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val myBuilder = CronetEngine.Builder(context)
        handler.post(getTemperatureDataRunnable)
        notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        activityManager = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        mainViewModel = ViewModelProvider(requireActivity())[MainActivityViewModel::class.java]
        createNotificationChannel()
        cronetEngine= myBuilder.build()
        executor = Executors.newSingleThreadExecutor()
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        dataPointViews = ArrayList()
        selectedCanopy = MutableLiveData<Canopy?>()

        selectedCanopy.observe(this){
            if (it!=null) {


                for (view in dataPointViews){
                    binding.fragmentHomeFacilityMapContainer.removeView(view)
                }

                var tempDataPointViews=ArrayList<RelativeLayout>()
                for (canopyIndoorMapPoint in it.canopyIndoorMapPoints!!){
                    val facilityMapContainer: FrameLayout = binding.fragmentHomeFacilityMapContainer
                    val dataPointView = layoutInflater.inflate(R.layout.data_point_view, facilityMapContainer, false)
                    var layoutParams: FrameLayout.LayoutParams = dataPointView.layoutParams as FrameLayout.LayoutParams

                    layoutParams.leftMargin = canopyIndoorMapPoint.marginStart.toInt()
                    layoutParams.topMargin = canopyIndoorMapPoint.topMargin.toInt()

                    var temperature = 24.3
                    var humidity = 60
                    var lightIntensity = 800

                    dataPointView.layoutParams = layoutParams
                    dataPointView.findViewById<TextView>(R.id.data_point_text).text = "$temperature°C, $humidity%, ${lightIntensity}aµmol/m²/s"
                    facilityMapContainer.addView(dataPointView)
                    tempDataPointViews.add(dataPointView as RelativeLayout)
                }
                dataPointViews=tempDataPointViews

                mMap?.clear()
                mMap?.addMarker(MarkerOptions().position(LatLng(it.canopyCoords.latitude, it.canopyCoords.longitude)).title(it.canopyName))
                mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.canopyCoords.latitude, it.canopyCoords.longitude), 18f))
            }
        }

        mainViewModel.getCanopiesList().observe(this) { canopiesList ->
            val mutableCanopiesList = canopiesList.toMutableList()
            Log.i("CanopyHomeFragment", "mutableCanopiesList: $mutableCanopiesList")

            if (mutableCanopiesList != null) {
                Log.i("CanopyHomeFragment", "mutableCanopiesList is not null")
                for (canopy in mutableCanopiesList) {
                    Log.i("CanopyHomeFragment", "Name: ${canopy.canopyName}, " +
                            "Coords: (${canopy.canopyCoords.latitude}, ${canopy.canopyCoords.longitude}), " +
                            "Owner: ${canopy.canopyOwner}, " +
                            "Address: ${canopy.canopyAddress}")
                }
            } else {
                Log.i("CanopyHomeFragment", "No data found in canopiesList.")
            }

            var searchBar = binding.searchBar
            var searchView = binding.fragmentHomeSearchView
            var recyclerView = binding.fragmentHomeSearchViewRecyclerView
            val canopyAdapter = CanopyRecyclerViewAdapter(mutableCanopiesList) { tempSelectedCanopy ->
                // Handle item click
                selectedCanopy.value=tempSelectedCanopy
                searchBar.setText(tempSelectedCanopy.canopyName)
                searchView.hide() // Hide SearchView when an item is selected
                binding.fragmentHomeFacilityMapContainer.visibility = View.VISIBLE
            }
            recyclerView.layoutManager=LinearLayoutManager(requireContext())
            recyclerView.adapter=canopyAdapter
            searchView.editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // No action needed here
                }
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val query = s.toString()
                    val filteredList = mutableCanopiesList.filter {
                        it.canopyName.contains(query, ignoreCase = true) ||
                        it.canopyAddress.contains(query, ignoreCase = true)
                    }
                    canopyAdapter.updateList(filteredList)
                }
                override fun afterTextChanged(s: Editable?) {
                    // No action needed here
                }
            })
            binding.fragmentHomeTapInMaterialButton.setOnClickListener {
                var fragmentTransaction = parentFragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.nav_host_fragment, TapInFragment())
                fragmentTransaction.commit()
                performHaptic(it)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        googleMap.apply {
            mapType = GoogleMap.MAP_TYPE_NORMAL
            uiSettings.isRotateGesturesEnabled = true
            uiSettings.isTiltGesturesEnabled = true
            uiSettings.isScrollGesturesEnabled = true

            isBuildingsEnabled = true
        }
        val marker = MarkerOptions().position(LatLng(1.335295037891502, 103.77556193766127))
        googleMap.addMarker(marker)

        val cameraPosition = CameraPosition.Builder()
            .target(LatLng(1.335295037891502, 103.77556193766127))
            .zoom(18f)
            .tilt(45f)
            .build()
        val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)
        googleMap.moveCamera(cameraUpdate)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        val rootView = binding.root

        binding.fragmentHomeToggleIndoorMapMaterialButton.setOnClickListener {
            if (binding.fragmentHomeFacilityMapContainer.visibility==View.GONE) {
                Log.i("PolarisHome", "button Pressed ")
                binding.fragmentHomeFacilityMapContainer.visibility = View.VISIBLE
            }
            else{
                binding.fragmentHomeFacilityMapContainer.visibility = View.GONE
            }
        }
        binding.fragmentHomeTapInMaterialButton.setOnClickListener {

        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.fragment_home_map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    override fun onResponseReceived(response: String) {
        Log.i("MLServiceRequest", "onResponseReceived method called: $response")
        activity?.runOnUiThread {
            Toast.makeText(context, response, Toast.LENGTH_LONG).show()
        }
    }
    fun performHaptic(view: View){
        val successful = view.isActivated
        when (successful){
            true -> view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            false -> view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        }
    }
    private suspend fun logoutUser(){
        requireActivity().dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Home.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Home().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
    private fun createNotificationChannel() {

        val notificationChannelId = Constants.BACKGROUND_SERVICE_CHANNEL_ID
        val channelName = resources.getString(R.string.notification_channel_name)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(notificationChannelId, channelName, importance)
        channel.enableLights(true)
        channel.description = resources.getString(R.string.notification_channel_description)
        notificationManager.createNotificationChannel(channel)
    }
    private fun isServiceRunning(serviceClass:Class<Service>) : Boolean{
        return false
    }
    fun updateServiceStatus(isRunning: Boolean) {
        Log.i("PolarisHome", "updateServiceStatus method called")
        var currentlyShownState=false


    }
}