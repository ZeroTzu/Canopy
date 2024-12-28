package com.teamname.canopy

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.TransitionManager
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.teamname.canopy.databinding.FragmentTapInAltBinding
import com.teamname.canopy.databinding.FragmentTapInBinding
import kotlin.random.Random

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentTapInAlt.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentTapInAlt : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding: FragmentTapInAltBinding
    private var handler = Handler(Looper.getMainLooper())
    private var isNFCActive = false
    private lateinit var viewModel: MainActivityViewModel


    private var showPopupRunnable = object : Runnable {
        override fun run() {
            val textView = binding.fragmentTapInAltAnimationTextView

            performHaptic(textView)
            animateWelcomeText(true)
        }
    }
    private var hidePopupRunnable = object : Runnable {
        override fun run() {
            val textView = binding.fragmentTapInAltAnimationTextView

            performHaptic(textView)
            animateWelcomeText(false)
        }
    }

    fun addVolunteerHistory(currentTimestamp: Timestamp) {
        val activity = (requireActivity() as MainActivity)
        activity.firestore.collection("users").document(activity.firebaseAuth.currentUser!!.uid).get().addOnSuccessListener { result ->

            var latestNoneCheckOut: Triple<Int, Timestamp, String>? = null
            val volunteerHistory = result.get("volunteerHistory") as ArrayList<HashMap<String, *>>


            for ((index, session) in volunteerHistory.withIndex()) {

                val sessionSession = session
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
            var newCanopyId = "123"
            if (viewModel.getCanopiesList().value!=null) {
                val canopiesCount = viewModel.getCanopiesList().value!!.size
                var random = Random.nextInt(canopiesCount)
                newCanopyId = viewModel.getCanopiesList().value!![random].canopyId
            }
            if (latestNoneCheckOut != null) {
                //check out this person
                val (index, _, _) = latestNoneCheckOut

                val timeStampDifference =  currentTimestamp.toInstant().toEpochMilli() - latestNoneCheckOut.second.toInstant().toEpochMilli()
                val timeStampDifferenceHours = timeStampDifference / (1000 * 60 * 60)
                val pointsEarned = timeStampDifferenceHours.toDouble().times(10)


                volunteerHistory[index] = HashMap<String,Any?>().apply {
                    put("tapInTimestamp",latestNoneCheckOut.second)
                    put("tapInCanopyId",latestNoneCheckOut.third)
                    put("tapOutTimestamp",currentTimestamp)
                    put("tapOutCanopyId",newCanopyId)
                }
                activity.firestore.collection("users")
                    .document(activity.firebaseAuth.currentUser!!.uid)
                    .update("volunteerHistory", volunteerHistory).also {
                        activity.firestore.collection("users")
                            .document(activity.firebaseAuth.currentUser!!.uid)
                            .update("pointsEarned", FieldValue.increment(pointsEarned.toLong()))
                    }
                    .addOnSuccessListener {
                        binding.fragmentTapInAltAnimationTextView.text = "Successfully Checked Out!"
                        handler.post(showPopupRunnable)
                        handler.postDelayed(hidePopupRunnable,5000)
                        Log.i("CanopyTapInFragment", "Successfully updated tap-out details for session $index")

                    }
                    .addOnFailureListener { e ->

                        val snackBar = Snackbar.make(requireView(), "Error checking out", Snackbar.LENGTH_LONG)
                        snackBar.show()
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
                        val canopyName= viewModel.getCanopyById(newCanopyId)?.canopyName
                        if (canopyName != null) {
                            binding.fragmentTapInAltAnimationTextView.text = "Welcome to ${viewModel.getCanopyById(newCanopyId)?.canopyName}"
                            handler.post(showPopupRunnable)
                            handler.postDelayed(hidePopupRunnable,5000)
                        }else{
                            binding.fragmentTapInAltAnimationTextView.text = "Welcome!"
                            handler.post(showPopupRunnable)
                            handler.postDelayed(hidePopupRunnable,5000)
                        }
                        Log.i("CanopyTapInFragment", "Successfully added a new session")
                    }
                    .addOnFailureListener { e ->
                        Log.e("CanopyTapInFragment", "Error adding new session", e)
                    }

            }
        }
    }

    private fun animateWelcomeText(show: Boolean) {
        // Reference the root layout
        val constraintLayout = binding.root as ConstraintLayout

        // Animate changes with TransitionManager
        TransitionManager.beginDelayedTransition(constraintLayout)

        if (show) {
            // Make the TextView visible and ensure constraints are updated
            binding.fragmentTapInAltAnimationTextView.apply {
                visibility = View.VISIBLE
                alpha = 0f
                translationY = 50f
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(1000)
                    .start()
            }
        } else {
            // Update layout to handle the removal smoothly
            binding.fragmentTapInAltAnimationTextView.apply {
                animate()
                    .alpha(0f)
                    .translationY(50f)
                    .setDuration(1000)
                    .withEndAction {
                        // Hide the TextView after animation
                        visibility = View.GONE

                        // Trigger a layout pass to adjust the position of other views
                        TransitionManager.beginDelayedTransition(constraintLayout)
                    }
                    .start()
            }
        }
    }



    private var addVolunteerHistoryRunnable = object : Runnable {
        override fun run() {
            addVolunteerHistory(Timestamp.now())
            isNFCActive = false

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        binding = FragmentTapInAltBinding.inflate(layoutInflater)
        viewModel= ViewModelProvider(requireActivity())[MainActivityViewModel::class.java]
        val constraintLayout = binding.root
        val animationDrawable = constraintLayout.background as AnimationDrawable
        animationDrawable.setEnterFadeDuration(2500)
        animationDrawable.setExitFadeDuration(5000)
        animationDrawable.start()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding.fragmentTapInAltDebugButton.setOnClickListener {
            if (!isNFCActive) {
                handler.postDelayed(addVolunteerHistoryRunnable,500)
                isNFCActive = true
            }else{
                isNFCActive = false
                handler.removeCallbacks(addVolunteerHistoryRunnable)
            }
        }
        binding.fragmentTapInAltCloseButton.setOnClickListener {
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
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FragmentTapInAlt.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FragmentTapInAlt().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}