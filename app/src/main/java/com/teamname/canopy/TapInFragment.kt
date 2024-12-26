package com.teamname.canopy

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.teamname.canopy.databinding.FragmentTapInBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [TapInFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TapInFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private var handler = Handler(Looper.getMainLooper())
    private var isNFCActive = false
    //binding
    private lateinit var binding: FragmentTapInBinding
    private var buttonAnimationRunnable = object : Runnable {
        override fun run() {
            val imageView = requireActivity().findViewById<View>(R.id.fragment_tap_in_animation_imageView)

            imageView.animate().scaleX(4f).scaleY(4f).alpha(0f).setDuration(1000)
                .withEndAction {
                    imageView.animate().scaleX(1f).scaleY(1f).alpha(1f)
                }
            performHaptic(imageView)
            handler.postDelayed(this,1500)
        }
    }
    private var addVolunteerHistoryRunnable = object : Runnable {
        override fun run() {
            addVolunteerHistory(Timestamp.now())
            handler.removeCallbacks(buttonAnimationRunnable)
            isNFCActive = false

        }
    }
    fun addVolunteerHistory(timestamp: Timestamp) {
        val activity = (requireActivity() as MainActivity)
        activity.firestore.collection("users").document(activity.firebaseAuth.currentUser!!.uid).get().addOnSuccessListener { result ->

            var latestNoneCheckOut: Triple<Int, Timestamp, String>? = null

            Log.i("CanopyTapInFragment", "volunteerHistory: ${result["volunteerHistory"]}")
            val volunteerHistory = result.get("volunteerHistory") as ArrayList<HashMap<String, *>>


            for ((index, session) in volunteerHistory.withIndex()) {

                val sessionSession = session as HashMap<String, *>
                val tapInTimestamp = sessionSession["tapInTimestamp"] as? Timestamp ?: continue
                val tapInCanopyId = sessionSession["tapInCanopyId"] as? String ?: continue
                val tapOutCanopyId = sessionSession["tapOutCanopyId"] as? String
                val tapOutTimestamp = sessionSession["tapOutTimestamp"] as? Timestamp

                if (tapOutTimestamp != null && tapOutCanopyId != null) {
                    latestNoneCheckOut = null
                    continue
                }

                if (latestNoneCheckOut == null || latestNoneCheckOut.second < tapInTimestamp) {
                    latestNoneCheckOut = Triple(index, tapInTimestamp, tapInCanopyId)
                }
            }
            val currentTimestamp = Timestamp.now()
            val newCanopyId = "123"

            if (latestNoneCheckOut != null) {
                //check out this person
                val (index, _, _) = latestNoneCheckOut


                volunteerHistory[index] = HashMap<String,Any?>().apply {
                    put("tapInTimestamp",latestNoneCheckOut.second)
                    put("tapInCanopyId",latestNoneCheckOut.third)
                    put("tapOutTimestamp",currentTimestamp)
                    put("tapOutCanopyId",newCanopyId)
                }
                activity.firestore.collection("users")
                    .document(activity.firebaseAuth.currentUser!!.uid)
                    .update("volunteerHistory", volunteerHistory)
                    .addOnSuccessListener {
                        Log.i("CanopyTapInFragment", "Successfully updated tap-out details for session $index")
                    }
                    .addOnFailureListener { e ->
                        Log.e("CanopyTapInFragment", "Error updating tap-out details", e)
                    }
            } else {
                Log.i("CanopyTapInFragment", "No session found to update. Adding a new session.")

                // Logic to add a new session

                volunteerHistory.add(
                    HashMap<String,Any?>().apply {
                        put("tapInTimestamp",currentTimestamp)
                        put("tapInCanopyId",newCanopyId)
                    }
                )
                activity.firestore.collection("users")
                    .document(activity.firebaseAuth.currentUser!!.uid)
                    .update("volunteerHistory", volunteerHistory,
                    )
                    .addOnSuccessListener {
                        Log.i("CanopyTapInFragment", "Successfully added a new session")
                    }
                    .addOnFailureListener { e ->
                        Log.e("CanopyTapInFragment", "Error adding new session", e)
                    }

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentTapInBinding.inflate(inflater, container, false)

        binding.fragmentTapInMaterialButton.setOnClickListener {
            if (!isNFCActive) {
                handler.postDelayed(addVolunteerHistoryRunnable,3000)
                isNFCActive = true
                handler.post(buttonAnimationRunnable)
            }else{
                isNFCActive = false
                handler.removeCallbacks(buttonAnimationRunnable)
                handler.removeCallbacks(addVolunteerHistoryRunnable)
            }
        }
        binding.fragmentTapInCloseButton.setOnClickListener {
            var fragmentTransaction = parentFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.nav_host_fragment, Home())
            fragmentTransaction.commit()
        }
        return binding.root
    }
    fun performHaptic(view: View){
        val successful = view.isActivated
        when (successful){
            true -> view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            false -> view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        }
    }
    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            TapInFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}