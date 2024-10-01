package com.example.polaris

import android.net.http.UrlRequest
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.polaris.databinding.FragmentHomeBinding
import org.chromium.net.CronetEngine
import org.chromium.net.UrlResponseInfo
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import com.example.polaris.MLServiceRequest.ResponseCallback
import android.widget.Toast
import android.util.Log

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val myBuilder = CronetEngine.Builder(context)
        cronetEngine= myBuilder.build()
        executor = Executors.newSingleThreadExecutor()
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
//
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.startServiceButton.setOnClickListener {
            val requestBuilder = cronetEngine.newUrlRequestBuilder(
            "https://catfact.ninja/fact",
            MLServiceRequest(this),
            executor)
        val request: org.chromium.net.UrlRequest = requestBuilder.build()
            request.start()

        }
        // Inflate the layout for this fragment
        return binding.root
    }
    override fun onResponseReceived(response: String) {
        Log.i("MLServiceRequest", "onResponseReceived method called: "+ response)
        activity?.runOnUiThread {
            Toast.makeText(context, response, Toast.LENGTH_LONG).show()
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
}