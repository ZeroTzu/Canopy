package com.teamname.canopy

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.teamname.canopy.databinding.FragmentVolunteerHistoryBinding
import com.teamname.canopy.utils.VolunteerSession
import com.teamname.canopy.utils.VolunteerSessionRecyclerViewAdapter
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class VolunteerHistoryFragment : Fragment() {
    private lateinit var binding : FragmentVolunteerHistoryBinding
    private lateinit var viewModel: MainActivityViewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentVolunteerHistoryBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(requireActivity())[MainActivityViewModel::class.java]
        val activity = requireActivity() as MainActivity

        activity.firestore.collection("users").document(activity.firebaseAuth.currentUser!!.uid).get().addOnSuccessListener { result ->
            var latestNoneCheckOut: Triple<Int, Timestamp, String>? = null
            val volunteerHistoryRaw = result.get("volunteerHistory") as? ArrayList<HashMap<String, *>>
            val points = result.get("pointsEarned") as? Long
            val volunteerHistory = ArrayList<VolunteerSession>()

            if (volunteerHistoryRaw == null) {
                return@addOnSuccessListener
            }
            for ((index, session) in volunteerHistoryRaw.withIndex()) {

                val sessionSession = session
                val tapInTimestamp = sessionSession["tapInTimestamp"] as? Timestamp ?: continue
                val tapInCanopyId = sessionSession["tapInCanopyId"] as? String ?: continue
                val tapOutCanopyId = sessionSession["tapOutCanopyId"] as? String
                val tapOutTimestamp = sessionSession["tapOutTimestamp"] as? Timestamp
                val tapInCanopyName = viewModel.getCanopyById(tapInCanopyId)?.canopyName
                val tapOutCanopyName = tapOutCanopyId?.let { viewModel.getCanopyById(it)?.canopyName }

                val tempVolunteerSession = VolunteerSession(tapInTimestamp.toInstant(),tapOutTimestamp?.toInstant(),tapInCanopyId,tapOutCanopyId,tapInCanopyName,tapOutCanopyName)
                volunteerHistory.add(tempVolunteerSession)
            }

            var totalHours = 0.0 as Double
            var totalPoints = 0
            var visitedCanopiesCount = 0
            var uniqueCanopyIds = mutableSetOf<String>()
            var visitedCanopyNames = mutableSetOf<String>()
            var sessionCount = 0

            val instant: Instant = Instant.now()
            val zoneId: ZoneId = ZoneId.systemDefault() // Replace with your desired time zone
            val currentMonthValue = ZonedDateTime.ofInstant(instant, zoneId).monthValue

            for (session in volunteerHistory) {
                val hours = session.calculateHours()
                if (ZonedDateTime.ofInstant(session.tappedInTimestamp,zoneId).monthValue==currentMonthValue){
                    if (hours != null ) {
                        totalHours += hours
                    }

                    if (session.tappedOutCanopy!=null){
                        sessionCount++
                        visitedCanopyNames.add(session.tappedInCanopyName!!)
                        Log.d("CanopyVolunteerHistory",session.tappedInCanopyName)
                    }
                    Log.d("CanopyVolunteerHistory",volunteerHistory.filter { it.tappedOutTimestamp != null }.map { it.tappedInCanopy}.toString())

                    if (session.tappedInCanopy !in uniqueCanopyIds){
                        visitedCanopiesCount++
                        uniqueCanopyIds.add(session.tappedInCanopy)
                    }

                }
                val points = session.getPoints()
                if (points != null) {
                    totalPoints += points
                }
            }



            volunteerHistory.reverse()


            binding.fragmentVolunteerHistoryPointsTextView.text =points.toString()
            binding.fragmentVolunteerHistoryHoursTextView.text = "${totalHours} Hours"
            binding.fragmentVolunteerHistorySessionsTextView.text = "${sessionCount} Sessions"
            val mostVisitedCanopy = visitedCanopyNames.groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }

            val tempVisitedCanopyNames = visitedCanopyNames.toMutableList()

            for (canopyName in tempVisitedCanopyNames) {
                if (canopyName == mostVisitedCanopy?.key) {
                    visitedCanopyNames.remove(canopyName)
                }
            }

            val secondMostVisitedCanopy = visitedCanopyNames.groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }

            binding.fragmentVolunteerHistoryMostTextView.text = mostVisitedCanopy?.key ?: ""
            binding.fragmentVolunteerHistorySecondMostTextView.text = secondMostVisitedCanopy?.key ?: ""


            var progressBar = binding.fragmentVolunteerHistoryHoursProgressBar
            progressBar.min = 0
            progressBar.max = 30
            progressBar.progress = totalHours.toInt()
            val adapter = VolunteerSessionRecyclerViewAdapter(volunteerHistory, onItemClick = {})
            binding.fragmentVolunteerHistoryRecyclerView.adapter = adapter
            binding.fragmentVolunteerHistoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        }

        return binding.root
    }



}