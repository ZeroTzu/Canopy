package com.teamname.canopy

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.teamname.canopy.databinding.FragmentProfileBinding
import com.teamname.canopy.utils.UserClass
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import com.teamname.canopy.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding: FragmentProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var userClass: UserClass
    private lateinit var firestore: FirebaseFirestore
    private lateinit var mainViewModel: MainActivityViewModel
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
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        mainViewModel= ViewModelProvider(requireActivity())[MainActivityViewModel::class.java]
        firestore = FirebaseFirestore.getInstance()
        firebaseAuth = (activity as MainActivity).firebaseAuth
        binding.fragmentProfileCloseButton.setOnClickListener {
            var fragmentTransaction = parentFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.nav_host_fragment, Home())
            fragmentTransaction.commit()
        }

        binding.fragmentProfileUidTextView.text=firebaseAuth.currentUser?.uid.toString()
        binding.fragmentProfileEmailAddressTextView.text=firebaseAuth.currentUser?.email.toString()

        if(mainViewModel.userClass.value?.name!=null){
            binding.fragmentProfileNameTextView.text=mainViewModel.userClass.value?.name.toString()
            binding.fragmentProfileNameMainTextView.text=mainViewModel.userClass.value?.name.toString()

        }
        else{
            binding.fragmentProfileNameTextView.text="Unknown Name"
            binding.fragmentProfileNameMainTextView.text="Unknown Name"

        }
        if(mainViewModel.userClass.value?.phoneNumber!=null){
            binding.fragmentProfilePhoneNumberTextView.text=mainViewModel.userClass.value?.phoneNumber.toString()
        }
        else{
            binding.fragmentProfilePhoneNumberTextView.text="Unknown Phone Number"
        }
        if(mainViewModel.userClass.value?.joinedDate!=null){
            val joinedDateDateTime = LocalDateTime.ofInstant(mainViewModel.userClass.value?.joinedDate, ZoneOffset.UTC).format(
                DateTimeFormatter.ofPattern("dd MMM uuuu").withZone(ZoneOffset.ofHours(8)))
            binding.fragmentProfileJoinedDateTextView.text=joinedDateDateTime
        }
        else{
            binding.fragmentProfileJoinedDateTextView.text="Unknown Joined Date"
        }

        binding.fragmentProfileNameTextView.text=mainViewModel.userClass.value?.name.toString()
        binding.fragmentProfilePhoneNumberTextView.text=mainViewModel.userClass.value?.phoneNumber.toString()


        binding.fragmentProfileNameTextView.setOnClickListener {
            //open dialogue view
            val dialogView = layoutInflater.inflate(R.layout.dialog_layout_text_input, null)
            val textInputLayout = dialogView.findViewById<TextInputLayout>(R.id.dialog_layout_text_input_name_textInputLayout)
            textInputLayout.hint="Name"
            textInputLayout.editText?.setText(mainViewModel.userClass.value?.name.toString())
            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
            materialAlertDialogBuilder.setTitle("Name")
            materialAlertDialogBuilder.setPositiveButton("OK") { dialog, which ->
                updateFireStoreUserProfile("name",textInputLayout.editText?.text.toString(),dialog,materialAlertDialogBuilder)
            }
            materialAlertDialogBuilder.setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            materialAlertDialogBuilder.setView(dialogView)
            materialAlertDialogBuilder.show()
        }

        binding.fragmentProfilePhoneNumberTextView.setOnClickListener {
            //open dialogue view
            val dialogView = layoutInflater.inflate(R.layout.dialog_layout_text_input, null)
            val textInputLayout = dialogView.findViewById<TextInputLayout>(R.id.dialog_layout_text_input_name_textInputLayout)
            textInputLayout.hint="Phone Number"
            textInputLayout.editText?.setText(mainViewModel.userClass.value?.phoneNumber.toString())
            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
            materialAlertDialogBuilder.setTitle("Phone Number")
            materialAlertDialogBuilder.setPositiveButton("OK") { dialog, which ->
                updateFireStoreUserProfile("phoneNumber",textInputLayout.editText?.text.toString(),dialog,materialAlertDialogBuilder)
            }
            materialAlertDialogBuilder.setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            materialAlertDialogBuilder.setView(dialogView)
            materialAlertDialogBuilder.show()
        }

        // Inflate the layout for this fragment

        return binding.root
    }
    fun updateFireStoreUserProfile(field:String,value:Any,dialogInterface: DialogInterface,materialAlertDialogBuilder: MaterialAlertDialogBuilder){

        firestore.collection("users").document(firebaseAuth.currentUser?.uid.toString()).update(field,value)
            .addOnSuccessListener {
                materialAlertDialogBuilder.setMessage("Done")
            }.addOnFailureListener {
                materialAlertDialogBuilder.setMessage("Failed to update name")
            }
        dialogInterface.dismiss()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}