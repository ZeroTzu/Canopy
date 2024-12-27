package com.teamname.canopy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            val volunteerHistoryRaw = result.get("volunteerHistory") as ArrayList<HashMap<String, *>>
            val volunteerHistory = ArrayList<VolunteerSession>()

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
                    }
                }
                val points = session.getPoints()
                if (points != null) {
                    totalPoints += points
                }
            }

            visitedCanopiesCount = volunteerHistory
                .filter { it.tappedOutTimestamp != null }
                .map { it.tappedInCanopy }
                .distinct().size


            binding.fragmentVolunteerHistoryPointsTextView.text =totalPoints.toString()
            binding.fragmentVolunteerHistoryHoursTextView.text = "Over ${totalHours} HOURS"
            binding.fragmentVolunteerHistoryCanopiesTextView.text = "Across ${visitedCanopiesCount} CANOPIES"
            binding.fragmentVolunteerHistorySessionsTextView.text = "In ${sessionCount} SESSIONS"

            var encourageStatement = ""
            if (totalHours>10){
                encourageStatement = "KEEP IT UP!"
            }
            else if (totalHours>5){
                encourageStatement = "KEEP GOING!"
            }
            else{
                encourageStatement = "YOU CAN DO IT!"
            }

            val adapter = VolunteerSessionRecyclerViewAdapter(volunteerHistory, onItemClick = {})
            binding.fragmentVolunteerHistoryRecyclerView.adapter = adapter
            binding.fragmentVolunteerHistoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        }

        return binding.root
    }



}