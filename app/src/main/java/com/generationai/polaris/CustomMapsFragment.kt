package com.generationai.polaris

import android.graphics.Color
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.material3.rememberTimePickerState
import com.generationai.polaris.api.LocationItem
import com.generationai.polaris.databinding.FragmentCustomMapsBinding
import com.generationai.polaris.utils.BackendInterface
import com.generationai.polaris.utils.Constants
import com.generationai.polaris.utils.RetrofitClient

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Calendar

class CustomMapsFragment : Fragment(), OnMapReadyCallback {

    private var handler: Handler = Handler(Looper.getMainLooper())
    private lateinit var locations:ArrayList<LocationItem>
    private lateinit var binding: FragmentCustomMapsBinding
    private lateinit var filterStartInstant:Instant
    private lateinit var filterEndInstant:Instant
    private lateinit var backendInterface: BackendInterface
    private lateinit var mMap: GoogleMap
    private val callback = OnMapReadyCallback { googleMap ->
        mMap=googleMap
        val sydney = LatLng(-34.0, 151.0)
        googleMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(),R.raw.map_light))
        val polyline1 = googleMap.addPolyline(
            PolylineOptions()
            .clickable(true)
            .add(
                LatLng(-35.016, 143.321),
                LatLng(-34.747, 145.592),
                LatLng(-34.364, 147.891),
                LatLng(-33.501, 150.217),
                LatLng(-32.306, 149.248),
                LatLng(-32.491, 147.309)))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-23.684, 133.903), 4f))
        addMarkers(googleMap)
    }

//    private var getUserLocationHistoryRunnable= Runnable {
//\    }

    fun getDefaultFilterInstants(){
        // Start of the day
        val calendar= Calendar.getInstance()
        calendar.set(Calendar.HOUR,0)
        calendar.set(Calendar.MINUTE,0)
        calendar.set(Calendar.SECOND,0)
        calendar.set(Calendar.MILLISECOND,0)
        filterStartInstant=calendar.time.toInstant()

        // End of the day (23:59:59.999)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        filterEndInstant = calendar.time.toInstant()
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding=FragmentCustomMapsBinding.inflate(layoutInflater)
        backendInterface= RetrofitClient.getBackendInterface()

        // populate filter instants with default values
        getDefaultFilterInstants()

//        handler.post()


        binding.customMapsStartFilterTextInputLayout.editText?.apply {
            inputType = InputType.TYPE_NULL // Disable keyboard
            setOnClickListener {
                if (childFragmentManager.findFragmentByTag("start_date_tag") == null
                    && childFragmentManager.findFragmentByTag("end_date_tag") == null) { // Check if dialog is already showing
                    val finalCalendar: Calendar = Calendar.getInstance()
                    val zonedInstant = filterStartInstant.atZone(ZoneId.systemDefault())

                    val calendar: Calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, zonedInstant.get(ChronoField.HOUR_OF_DAY))
                        set(Calendar.MINUTE, zonedInstant.get(ChronoField.MINUTE_OF_HOUR))
                        set(Calendar.DAY_OF_MONTH, zonedInstant.get(ChronoField.DAY_OF_MONTH))
                        set(Calendar.MONTH, zonedInstant.get(ChronoField.MONTH_OF_YEAR) - 1) // Adjust for 0-based month
                        set(Calendar.YEAR, zonedInstant.get(ChronoField.YEAR))
                    }

                    val tempCalendar = Calendar.getInstance()
                    val materialDatePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select start date")
                        .setSelection(filterStartInstant.toEpochMilli())
                        .setCalendarConstraints(CalendarConstraints.Builder().setEnd(tempCalendar.timeInMillis).build())
                        .build()

                    materialDatePicker.show(childFragmentManager, "start_date_tag")
                    materialDatePicker.addOnPositiveButtonClickListener { selectedDate ->
                        // Set date in `finalCalendar`
                        finalCalendar.timeInMillis = selectedDate

                        val materialTimePicker = MaterialTimePicker.Builder()
                            .setTitleText("Select start time")
                            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                            .setMinute(calendar.get(Calendar.MINUTE))
                            .build()

                        materialTimePicker.show(childFragmentManager, "start_time_tag")
                        materialTimePicker.addOnPositiveButtonClickListener {
                            // Set time in `finalCalendar`
                            finalCalendar.set(Calendar.HOUR_OF_DAY, materialTimePicker.hour)
                            finalCalendar.set(Calendar.MINUTE, materialTimePicker.minute)

                            // Create `filterStartInstant` in UTC
                            filterStartInstant = finalCalendar.toInstant()

                            // Format and display the selected date and time
                            val formattedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                .format(filterStartInstant.atZone(ZoneId.systemDefault()).toLocalDateTime())
                            binding.customMapsStartFilterTextInputLayout.editText?.setText(formattedDate)

                            getLocationHistoryBetweenDates(filterStartInstant, filterEndInstant)
                        }
                    }

                }
            }
        }


        // End date picker
        binding.customMapsEndFilterTextInputLayout.editText?.apply {
            inputType = InputType.TYPE_NULL // Disable keyboard
            setOnClickListener {
                if (childFragmentManager.findFragmentByTag("start_date_tag") == null
                    && childFragmentManager.findFragmentByTag("end_date_tag") == null) { // Check if dialog is already showing

                    val finalCalendar: Calendar = Calendar.getInstance()
                    val zonedInstant = filterStartInstant.atZone(ZoneId.systemDefault())

                    val calendar: Calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, zonedInstant.get(ChronoField.HOUR_OF_DAY))
                        set(Calendar.MINUTE, zonedInstant.get(ChronoField.MINUTE_OF_HOUR))
                        set(Calendar.DAY_OF_MONTH, zonedInstant.get(ChronoField.DAY_OF_MONTH))
                        set(Calendar.MONTH, zonedInstant.get(ChronoField.MONTH_OF_YEAR) - 1) // Adjust for 0-based month
                        set(Calendar.YEAR, zonedInstant.get(ChronoField.YEAR))
                    }

                    val tempCalendar = Calendar.getInstance()
                    val materialDatePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select start date")
                        .setSelection(filterStartInstant.toEpochMilli())
                        .setCalendarConstraints(CalendarConstraints.Builder().setEnd(tempCalendar.timeInMillis).build())
                        .build()

                    materialDatePicker.show(childFragmentManager, "start_date_tag")
                    materialDatePicker.addOnPositiveButtonClickListener { selectedDate ->
                        // Set date in `finalCalendar`
                        finalCalendar.timeInMillis = selectedDate

                        val materialTimePicker = MaterialTimePicker.Builder()
                            .setTitleText("Select start time")
                            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                            .setMinute(calendar.get(Calendar.MINUTE))
                            .build()

                        materialTimePicker.show(childFragmentManager, "start_time_tag")
                        materialTimePicker.addOnPositiveButtonClickListener {

                            finalCalendar.set(Calendar.HOUR_OF_DAY, materialTimePicker.hour)
                            finalCalendar.set(Calendar.MINUTE, materialTimePicker.minute)

                            filterEndInstant = finalCalendar.toInstant()

                            // Format and display the selected date and time
                            val formattedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                .format(filterEndInstant.atZone(ZoneId.systemDefault()).toLocalDateTime())
                            binding.customMapsEndFilterTextInputLayout.editText?.setText(formattedDate)

                            getLocationHistoryBetweenDates(filterStartInstant, filterEndInstant)
                        }
                    }
                }
            }
        }

        binding.customMapsClearFiltersButton.setOnClickListener{
            binding.customMapsStartFilterTextInputLayout.editText?.setText("")
            binding.customMapsEndFilterTextInputLayout.editText?.setText("")
            getDefaultFilterInstants()
        }
        binding.customMapsFiltersLinearLayout.visibility = View.GONE
        binding.customMapsFiltersButton.setOnClickListener {
            if (binding.customMapsFiltersLinearLayout.visibility == View.GONE){
                binding.customMapsFiltersLinearLayout.visibility = View.VISIBLE
            } else {
                binding.customMapsFiltersLinearLayout.visibility = View.GONE
            }
        }
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        binding.customMapsNightModeToggleSwitchCompat.setOnCheckedChangeListener { _, isChecked ->
            mapFragment?.getMapAsync { googleMap ->
                val mapStyleResource = if (isChecked) {
                    R.raw.map_night // Night mode style
                } else {
                    R.raw.map_light // Light mode style
                }
                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), mapStyleResource))
            }
        }

        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }
    fun getLocationHistoryBetweenDates(startInstant:Instant,endInstant: Instant){
        var locations=ArrayList<LocationItem>()
        backendInterface.getLocationFiltered("tida@gmail.com", startInstant, endInstant).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val responseBody: ResponseBody = response.body() ?: return
                val jsonString = responseBody.string()  // Use .string() to get the actual content
                Log.i("CustomMapsFragment", "Response: $jsonString")
                var isSuccessful = 0
                try {
                    val jsonObject = JSONObject(jsonString)
                    isSuccessful = 2
                } catch (e: Exception) {
                    // Failed to parse as object, fallback to error handling
                }
                try {
                    jsonString.replace("}{","},{")
                    val jsonArray = JSONArray(jsonString)
                    if (jsonArray.length() == 0) isSuccessful = 3
                    else isSuccessful = 1
                } catch (e: Exception) {
                    // Failed to parse as array, continue to check if it's an object
                }


                when (isSuccessful) {
                    1 -> {
                        Toast.makeText(context, "Found Locations", Toast.LENGTH_SHORT).show()
                        val jsonArray = JSONArray(jsonString)
                        val locationItems = ArrayList<LocationItem>()
                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            //format the server timestamp into iso 8601
                            val timestampString = jsonObject.getString("timestamp")
                                .replace(" AM", "")
                                .replace(" PM", "")
                                .replace(" UTC", "")
                            Log.i("CustomMapsFragment", "Timestamp String: $timestampString")
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX")

// Parse to LocalDateTime, then convert to Instant
                            val parsedInstant = LocalDateTime.parse(timestampString, formatter)
                                .toInstant(ZoneOffset.UTC)
                            val locationItem = LocationItem().apply {
                                latitude = jsonObject.getDouble("latitude")
                                longitude = jsonObject.getDouble("longitude")
                                altitude = jsonObject.getDouble("altitude").toFloat()
                                timestamp = parsedInstant
                            }
                            locationItems.add(locationItem)
                        }
                        setMapMarkers(locationItems)
                    }
                    2 -> {
                        val jsonObject = JSONObject(jsonString)
                        val status = jsonObject.getString("status")
                        val code = jsonObject.getInt("code")
                        if (status == Constants.BACK_END_SERVER_STATUS_FAILED) {
                            if (code == Constants.BACK_END_SERVER_CODE_NO_LOCATION_RETURNED) {
                                Toast.makeText(context, "Server Returned Error", Toast.LENGTH_SHORT).show()
                                return
                            }
                        }
                    }
                    3 -> {
                        clearMap()
                        Toast.makeText(context, "No Results Found", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(context, "Case not caught", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // Handle network failure
            }
        })


    }
    override fun onMapReady(p0: GoogleMap) {
        TODO("Not yet implemented")


    }
    fun addMarkers(googleMap: GoogleMap){
        googleMap.addMarker(MarkerOptions().position(LatLng(-34.0, 151.0)).title("Marker in Sydney"))
    }
    fun setMapMarkers(locations:ArrayList<LocationItem>){

        Log.i("CustomMapsFragment", "Adding Locations: $locations")
        var mapFragment = parentFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mMap.let {
            googleMap ->
            var tempPolyLineOptions = PolylineOptions()
                .clickable(true)
                .color(Color.BLUE)
                .width(10f)
            for (location in locations){
                Log.i("CustomMapsFragment", "Latitude: ${location.latitude}, Longitude: ${location.longitude}")
                tempPolyLineOptions.add(LatLng(location.latitude!!.toDouble(),location.longitude!!.toDouble()))
            }
            googleMap.clear()
            googleMap.addPolyline(tempPolyLineOptions)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(locations[0].latitude!!.toDouble(),locations[0].longitude!!.toDouble()), 10f))
            Log.i("CustomMapsFragment", "Locations added: $locations")

        }

    }
    fun clearMap(){
        mMap.clear()
    }
    override fun onPause() {

        super.onPause()
    }
//    fun addMarkers(locations:ArrayList<LocationItem>){
//        for (location in locations){
//            val latLng=LatLng(location.latitude!!,location.longitude!!)
//            mMap.addMarker(MarkerOptions().position(latLng).title("Marker in Sydney"))
//        }
//    }
}