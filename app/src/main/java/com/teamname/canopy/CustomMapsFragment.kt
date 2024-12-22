package com.teamname.canopy

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.location.Location
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
import androidx.core.content.ContextCompat
import com.teamname.canopy.api.LocationItem
import com.teamname.canopy.databinding.FragmentCustomMapsBinding
import com.teamname.canopy.utils.BackendInterface
import com.teamname.canopy.utils.Constants
import com.teamname.canopy.utils.RetrofitClient

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CustomMapsFragment : Fragment(), OnMapReadyCallback {

    private var handler: Handler = Handler(Looper.getMainLooper())
    private lateinit var locations:ArrayList<LocationItem>
    private lateinit var lastLocations:ArrayList<LocationItem>
    private lateinit var binding: FragmentCustomMapsBinding
    private lateinit var filterStartInstant:Instant
    private lateinit var filterEndInstant:Instant
    private lateinit var backendInterface: BackendInterface
    private lateinit var mMap: GoogleMap
    private val captureImageRunnable = object : Runnable {
        override fun run() {

            getLocationHistoryBetweenDates(filterStartInstant, filterEndInstant)
            handler.postDelayed(this, 5000) //
        }
    }
    private val callback = OnMapReadyCallback { googleMap ->
        mMap=googleMap
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(1.3521, 103.8198), 11f))
        locations=ArrayList()
        lastLocations=ArrayList()
        handler.post(captureImageRunnable)
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
                        .setTheme(R.style.AppThemeOverlay_MaterialCalendar)
                        .build()

                    materialDatePicker.show(childFragmentManager, "start_date_tag")
                    materialDatePicker.addOnPositiveButtonClickListener { selectedDate ->
                        // Set date in `finalCalendar`
                        finalCalendar.timeInMillis = selectedDate

                        val materialTimePicker = MaterialTimePicker.Builder()
                            .setTitleText("Select start time")
                            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                            .setMinute(calendar.get(Calendar.MINUTE))
                            .setTheme(R.style.AppThemeOverlay_MaterialTimePicker)
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
                        .setTheme(R.style.AppThemeOverlay_MaterialCalendar)
                        .build()

                    materialDatePicker.show(childFragmentManager, "start_date_tag")
                    materialDatePicker.addOnPositiveButtonClickListener { selectedDate ->
                        // Set date in `finalCalendar`
                        finalCalendar.timeInMillis = selectedDate

                        val materialTimePicker = MaterialTimePicker.Builder()
                            .setTitleText("Select start time")
                            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                            .setMinute(calendar.get(Calendar.MINUTE))
                            .setTheme(R.style.AppThemeOverlay_MaterialTimePicker)
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
    fun filterLocations(locations: ArrayList<LocationItem>, minDistance: Float): ArrayList<LocationItem> {
        val filteredLocations = ArrayList<LocationItem>()
        if (locations.isEmpty()) return filteredLocations

        // Add the first location by default
        filteredLocations.add(locations[0])

        // Iterate and add only locations far enough from the last added point
        for (i in 1 until locations.size) {
            val lastAddedLocation = filteredLocations.last()
            val currentLocation = locations[i]

            val results = FloatArray(1)
            Location.distanceBetween(
                lastAddedLocation.latitude!!.toDouble(), lastAddedLocation.longitude!!.toDouble(),
                currentLocation.latitude!!.toDouble(), currentLocation.longitude!!.toDouble(),
                results
            )

            // Check if the distance is greater than the specified minimum
            if (results[0] > minDistance) {
                filteredLocations.add(currentLocation)
            }
        }
        return filteredLocations
    }

    fun getLocationHistoryBetweenDates(startInstant:Instant,endInstant: Instant){
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
                        val jsonArray = JSONArray(jsonString)

                        if (locations!=ArrayList<LocationItem>()) lastLocations=locations
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
                            locations.add(locationItem)
                        }
                        val filteredLocations=filterLocations(locations, 100f)
                        setMapMarkers(filteredLocations)

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
    fun setMapMarkers(locations: ArrayList<LocationItem>) {
        Log.i("CustomMapsFragment", "Adding Locations: $locations")
        val mapFragment = parentFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mMap.let { googleMap ->
            // Clear the map first to avoid duplicates
            googleMap.clear()

            // Create polyline options
            val tempPolyLineOptions = PolylineOptions()
                .clickable(true)
                .color(Color.BLUE)
                .width(10f)

            // Iterate through locations to add markers and polyline
            for (i in locations.indices) {
                val location = locations[i]
                Log.i("CustomMapsFragment", "Latitude: ${location.latitude}, Longitude: ${location.longitude}")
                tempPolyLineOptions.add(LatLng(location.latitude!!.toDouble(), location.longitude!!.toDouble()))

                if (i == 0) { // Latest position
                    // Create a larger red marker
                    val personIcon = createCustomMarker(R.drawable.baseline_person_pin_24, Color.RED, 10f) // Scale up by 1.5x
                    val markerOptions = MarkerOptions()
                        .position(LatLng(location.latitude!!.toDouble(), location.longitude!!.toDouble()))
                        .title("As of ${formatTimestamp(location.timestamp!!.toEpochMilli())}") // Set title
                        .icon(personIcon)

                    googleMap.addMarker(markerOptions)
                }
            }

            // Add polyline to the map
            googleMap.addPolyline(tempPolyLineOptions)
            Log.i("CustomMapsFragment", "Locations added: $locations")
        }
    }

    private fun createCustomMarker(drawableId: Int, color: Int, scale: Float): BitmapDescriptor {
        // Create a bitmap from the vector drawable and change its color
        val drawable = ContextCompat.getDrawable(requireContext(), drawableId)?.mutate() // Ensure the drawable can be modified
        drawable?.setColorFilter(color, PorterDuff.Mode.SRC_IN)

        // Set the size based on the scale factor
        val bitmap = Bitmap.createBitmap(((drawable?.intrinsicWidth ?: (0 * scale))).toInt(), ((drawable?.intrinsicHeight
            ?: (0 * scale))).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable?.setBounds(0, 0, canvas.width, canvas.height)
        drawable?.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }





    fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val date = Date(timestamp)
        return dateFormat.format(date)
    }


        fun vectorToBitmap(vectorResId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(requireContext(), vectorResId)

        // Create a bitmap with the appropriate size
        val bitmap = Bitmap.createBitmap(drawable!!.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // Set the bounds for the drawable and draw it
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }
    fun clearMap(){
        mMap.clear()
    }
    override fun onPause() {
        handler.removeCallbacks(captureImageRunnable)
        super.onPause()
    }
    override fun onResume() {
        handler.post(captureImageRunnable)
        super.onResume()
    }
    override fun onDestroy() {
        handler.removeCallbacks(captureImageRunnable)
        super.onDestroy()
    }

//    fun addMarkers(locations:ArrayList<LocationItem>){
//        for (location in locations){
//            val latLng=LatLng(location.latitude!!,location.longitude!!)
//            mMap.addMarker(MarkerOptions().position(latLng).title("Marker in Sydney"))
//        }
//    }
}