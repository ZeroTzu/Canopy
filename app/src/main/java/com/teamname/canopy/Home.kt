package com.teamname.canopy
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.teamname.canopy.databinding.FragmentHomeBinding
import com.teamname.canopy.utils.Constants
import com.teamname.canopy.utils.MLServiceRequest.ResponseCallback
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
class Home : Fragment(),ResponseCallback {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding: FragmentHomeBinding
    private lateinit var cronetEngine: CronetEngine
    private lateinit var executor: Executor
    private lateinit var notificationManager: NotificationManager
    private lateinit var activityManager: ActivityManager
    private lateinit var mainViewModel: MainActivityViewModel
    private val latch = CountDownLatch(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val myBuilder = CronetEngine.Builder(context)
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

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        val rootView = binding.root

        // Find the FrameLayout in the parent view
        val facilityMapContainer = rootView.findViewById<FrameLayout>(R.id.facility_map_container)

        // Inflate the data point view without attaching it to the container yet
        val datapointView = inflater.inflate(R.layout.data_point_view, null)

        // prepare layout parameters of the data point view
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(100, 200, 0, 0)  // adjust the left and top margins
        }

        // Apply the layout parameters to the inflated datapoint view
        datapointView.layoutParams = params

        // Add the data point view to the FrameLayout
        facilityMapContainer.addView(datapointView)

        //
//        binding.startServiceButton.setOnClickListener {
//            val context = this.context as? Context
//            if (context!=null){
//                Log.i("PolarisHome", "button Pressed ")
//                val intent = Intent(PolarisBackgroundService.Actions.CHECK_SERVICE_STATUS.toString())
//                requireActivity().sendBroadcast(intent)
//                try {
//                    // Wait for the response, blocking the thread
//                    // After the latch is counted down, decide to start or stop the service
//                    if (!mainViewModel.isSertviceRunning.value) {
//                        Log.i("PolarisHome", "starting Background Service ")
//                        Intent(context, PolarisBackgroundService::class.java).also {
//                            it.action = PolarisBackgroundService.Actions.START.toString()
//                            context.startService(it)
//                            mainViewModel.setIsBackgroundServiceRunning(true)
//                        }
//                    } else {
//                        Log.i("PolarisHome", "stopping Background Service ")
//                        Intent(context, PolarisBackgroundService::class.java).also {
//                            it.action = PolarisBackgroundService.Actions.STOP.toString()
//                            context.startService(it)
//                            mainViewModel.setIsBackgroundServiceRunning(false)
//                        }
//
//                    }
//                } catch (e: InterruptedException) {
//                    e.printStackTrace()
//                }
//            }
//        }
//        binding.debugButton1.setOnClickListener{ view ->
//            val requestBuilder = cronetEngine.newUrlRequestBuilder(
//                "https://catfact.ninja/fact",
//                MLServiceRequest(this),
//                executor
//            )
//            val request: org.chromium.net.UrlRequest = requestBuilder.build()
//            request.start()  // Start the request
//            performHaptic(view)
//        }
////        binding.debugButton2.setOnClickListener{
////
////        }
//        binding.homeFragmentLogoutButton.setOnClickListener{
//            lifecycleScope.launch {
//                (activity as? MainActivity)?.logoutUser()
//            }
//        }
        // Inflate the layout for this fragment
        return binding.root
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
        // Count down the latch to unblock the waiting thread
//        latch.countDown()
//        when (binding.startServiceButton.text){
//            resources.getString(R.string.start_service)->currentlyShownState=true
//            resources.getString(R.string.stop_service)->currentlyShownState=false
//        }
//        if (isRunning) {
//            // Change button text to "Stop Service"
//            Log.i("PolarisHome", "updateServiceStatus: Changing button to show Stop Service ")
//            binding.startServiceButton.text=resources.getString(R.string.stop_service)
//            binding.startServiceButton.icon=ResourcesCompat.getDrawable(resources,R.drawable.baseline_stop_24,requireContext().theme)
//        } else {
//            // Change button text to "Start Service"
//            Log.i("PolarisHome", "updateServiceStatus: Changing button to show Start Service ")
//            binding.startServiceButton.text=resources.getString(R.string.start_service)
//            binding.startServiceButton.icon=ResourcesCompat.getDrawable(resources,R.drawable.baseline_play_arrow_24,requireContext().theme)
//        }

    }
//    fun updateImage(imagePath: String?){
//        try{
//            if (imagePath!=null){
////                binding.homeFragmentTopBarTextView.text=imagePath
//                binding.debugHomeFragmentImageView.setImageURI(android.net.Uri.parse(imagePath))
//                Log.i("PolarisHome", "updateImage success: $imagePath")
//            }else{
//                Log.i("PolarisHome", "updateImage failed due to no path: $imagePath")
//            }
//        }catch (e:Exception){
//            Log.e("PolarisHome", "updateImage failed: ${e.message}")
//        }
//    }
}