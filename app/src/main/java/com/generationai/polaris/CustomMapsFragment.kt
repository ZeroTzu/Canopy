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
import androidx.compose.ui.graphics.vector.addPathNodes
import com.generationai.polaris.api.GetLocationResponse
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
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date

class CustomMapsFragment : Fragment(), OnMapReadyCallback {

    private var handler: Handler = Handler(Looper.getMainLooper())
    private lateinit var locations:ArrayList<LocationItem>
    private lateinit var binding: FragmentCustomMapsBinding
    private lateinit var filterStartInstant:Instant
    private lateinit var filterEndInstant:Instant
    private lateinit var backendInterface: BackendInterface
    private val callback = OnMapReadyCallback { googleMap ->
        val sydney = LatLng(-34.0, 151.0)
        googleMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
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
        polyline1.remove()
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

                    val calendar:Calendar = Calendar.getInstance()

                    val materialDatePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select start date")
                        .setSelection(filterStartInstant.toEpochMilli())
                        .setCalendarConstraints(CalendarConstraints.Builder().setEnd(calendar.timeInMillis).build())
                        .build()

                    materialDatePicker.show(childFragmentManager, "start_date_tag")
                    materialDatePicker.addOnPositiveButtonClickListener { selectedDate ->
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis=selectedDate
                        }

                        val materialTimePicker = MaterialTimePicker.Builder()
                            .setTitleText("Select start time")
                            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                            .setMinute(calendar.get(Calendar.MINUTE))
                            .build()

                        materialTimePicker.show(childFragmentManager, "start_time_tag")
                        materialTimePicker.addOnPositiveButtonClickListener {
                            calendar.set(Calendar.HOUR_OF_DAY, materialTimePicker.hour)
                            calendar.set(Calendar.MINUTE, materialTimePicker.minute)
                            filterStartInstant = calendar.time.toInstant()

                            val formattedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                .format(filterStartInstant.atZone(ZoneId.systemDefault()).toLocalDateTime())
                            binding.customMapsStartFilterTextInputLayout.editText?.setText(formattedDate)
                            getLocationHistoryBetweenDates(filterStartInstant,filterEndInstant)
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

                    val calendar:Calendar = Calendar.getInstance()

                    val materialDatePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select start date")
                        .setSelection(filterEndInstant.toEpochMilli())
                        .setCalendarConstraints(CalendarConstraints.Builder().setEnd(calendar.timeInMillis).build())
                        .build()

                    materialDatePicker.show(childFragmentManager, "start_date_tag")
                    materialDatePicker.addOnPositiveButtonClickListener { selectedDate ->
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis=selectedDate
                        }

                        val materialTimePicker = MaterialTimePicker.Builder()
                            .setTitleText("Select start time")
                            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                            .setMinute(calendar.get(Calendar.MINUTE))
                            .build()

                        materialTimePicker.show(childFragmentManager, "start_time_tag")
                        materialTimePicker.addOnPositiveButtonClickListener {
                            calendar.set(Calendar.HOUR_OF_DAY, materialTimePicker.hour)
                            calendar.set(Calendar.MINUTE, materialTimePicker.minute)
                            filterEndInstant = calendar.time.toInstant()

                            val formattedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                .format(filterEndInstant.atZone(ZoneId.systemDefault()).toLocalDateTime())

                            binding.customMapsEndFilterTextInputLayout.editText?.setText(formattedDate)
                            getLocationHistoryBetweenDates(filterStartInstant,filterEndInstant)
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }
    fun getLocationHistoryBetweenDates(startInstant:Instant,endInstant: Instant){
        var locations=ArrayList<LocationItem>()
        backendInterface.getLocationFiltered("tida@gmail.com",startInstant,endInstant).enqueue(object: Callback<GetLocationResponse>{
            override fun onResponse(
                call: Call<GetLocationResponse>,
                response: Response<GetLocationResponse>
            ) {
                val getLocationResponse:GetLocationResponse=response.body()?:return
                if (getLocationResponse.status==Constants.BACK_END_SERVER_STATUS_FAILED) {
                    if (getLocationResponse.code==Constants.BACK_END_SERVER_CODE_NO_LOCATION_RETURNED){
                        Toast.makeText(context,"No Location History Found",Toast.LENGTH_SHORT).show()
                        return
                    }
                    Toast.makeText(context,"Back End Error",Toast.LENGTH_SHORT).show()
                }
                val tempVal=getLocationResponse
                if (tempVal.locations==null) return
                Log.i("PolarisCustomMapsFragment", "locations: ${tempVal.locations}")
                setMapMarkers(tempVal.locations?:return)
            }

            override fun onFailure(p0: Call<GetLocationResponse>, p1: Throwable) {

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
        var mapFragment = parentFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync{googleMap ->
            var tempPolyLineOptions = PolylineOptions()
                .clickable(true)
                .color(Color.BLUE)
                .width(10f)
            for (location in locations){
                tempPolyLineOptions.add(LatLng(location.latitude!!.toDouble(),location.longitude!!.toDouble()))
            }
            googleMap.clear()
            googleMap.addPolyline(tempPolyLineOptions)

        }
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