package com.teamname.canopy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.teamname.canopy.databinding.FragmentVolunteerHistoryBinding
import com.teamname.canopy.utils.VolunteerSession
import com.teamname.canopy.utils.VolunteerSessionRecyclerViewAdapter

class VolunteerHistoryFragment : Fragment() {
    private lateinit var binding : FragmentVolunteerHistoryBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentVolunteerHistoryBinding.inflate(layoutInflater)

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
                val tempVolunteerSession = VolunteerSession(tapInTimestamp.toInstant(),tapOutTimestamp?.toInstant(),tapInCanopyId,tapOutCanopyId)

                volunteerHistory.add(tempVolunteerSession)
            }

            val adapter = VolunteerSessionRecyclerViewAdapter(volunteerHistory, onItemClick = {})
            binding.fragmentVolunteerHistoryRecyclerView.adapter = adapter
            binding.fragmentVolunteerHistoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        }

        return binding.root
    }



}