package com.teamname.canopy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Timestamp
import com.teamname.canopy.api.RegisterRequest
import com.teamname.canopy.api.RegisterResponse
import com.teamname.canopy.databinding.FragmentLoginRegisterFormBinding
import com.teamname.canopy.utils.BackendInterface
import com.teamname.canopy.utils.Constants
import com.teamname.canopy.utils.RetrofitClient

import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [LoginRegisterFormFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginRegisterFormFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding: FragmentLoginRegisterFormBinding
    private lateinit var backendInterface: BackendInterface
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        backendInterface = RetrofitClient.getBackendInterface()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentLoginRegisterFormBinding.inflate(layoutInflater)

        val emailEditText = binding.fragmentLoginRegisterFormEmailTextInputLayout.editText!!
        val passwordEditText = binding.fragmentLoginRegisterFormPasswordTextInputLayout.editText!!
        val confirmPasswordEditText = binding.fragmentLoginRegisterFormConfirmPasswordTextInputLayout.editText!!
        val loginActivity = activity as LoginActivity

        binding.fragmentLoginRegisterFormLoginButton.setOnClickListener{
            //TODO: CHECK VALID INPUT, SEND API REQUEST, RECEIVE API RESPONSE, SAVE TOKEN, NAVIGATE TO LOGIN PAGE
            if(!(loginActivity.checkEmailInput(emailEditText) && loginActivity.checkPasswordInput(passwordEditText) && loginActivity.checkPasswordSame(passwordEditText,confirmPasswordEditText))){
                return@setOnClickListener
            }

            val firebaseAuth = (activity as LoginActivity).firebaseAuth
            firebaseAuth.createUserWithEmailAndPassword(emailEditText.text.toString(),passwordEditText.text.toString())
                .addOnCompleteListener{
                    var task = it
                    if(task.isSuccessful){

                        val user = HashMap<String,Any>()
                        user["userEmail"]=emailEditText.text.toString()
                        user["userId"] = it.result.user!!.uid
                        user["createdTimestamp"] = Timestamp.now()
                        user["volunteerHistory"] = ArrayList<HashMap<String,Any?>>()
                        user["pointsEarned"] = 0
                        (activity as LoginActivity).firestore.collection("users").document(firebaseAuth.uid!!).set(user).addOnCompleteListener{
                            var task2 = it
                            if (task2.isSuccessful){
                                Log.i("PolarisLoginActivity","user added to firestore")
                            }
                            else{
                                Log.i("PolarisLoginActivity","user not added to firestore")
                            }
                        }

                        redirectToLogin()
                    }
                    else {
                        Toast.makeText(
                            requireContext(),
                            it.exception.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
        binding.fragmentLoginRegisterFormCancelButton.setOnClickListener{
            requireActivity().supportFragmentManager.beginTransaction().replace(R.id.login_activity_fragmentContainerView,LoginLandingFragment()).commit()
        }
        // Inflate the layout for this fragment
        return binding.root
    }
    private fun performRegister(email:String,password:String){
        Log.i("PolarisLoginActivity","performRegister invoked")
        val username=email
        backendInterface.registerUser(RegisterRequest(username,email,password)).enqueue(object :
            Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if(!response.isSuccessful){
                    Log.e("PolarisLoginActivity","response failed:${response.body()}")
                    return
                }
                val registerResponse = response.body()?:return
                if(registerResponse.status.toString()==Constants.BACK_END_SERVER_STATUS_FAILED){
                    val message =registerResponse.message!!.toString()
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    return
                }
                Log.i("PolarisLoginActivity", "onResponse: $registerResponse")
                val uid = ""
                val username = registerResponse.username.toString()
                val email = registerResponse.email.toString()
                val password= registerResponse.password.toString()
                val sessionToken= ""
                lifecycleScope.launch{
                    if (isAdded){
                        requireActivity().dataStore.edit { preferences ->
                            run{
                                preferences[Constants.USER_ID_KEY] = uid
                                preferences[Constants.USER_NAME_KEY] = username
                                preferences[Constants.EMAIL_KEY] = email
                                preferences[Constants.PASSWORD_KEY] = password
                                preferences[Constants.SESSION_TOKEN_KEY] = sessionToken

                            }
                        }
                        redirectToLogin()
                    }
                }
            }
            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                Log.e("PolarisLoginActivity", "Error: ${t.message}")
                Toast.makeText(requireContext(), "Login Failed due to Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun redirectToLogin(){
        val intent=Intent(requireActivity(),MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment LoginRegisterFormFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            LoginRegisterFormFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}