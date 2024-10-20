package com.generationai.polaris

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.generationai.polaris.api.requests.LoginRequest
import com.generationai.polaris.api.responses.LoginResponse
import com.generationai.polaris.databinding.FragmentLoginFormBinding
import com.generationai.polaris.utils.BackendInterface
import com.generationai.polaris.utils.Constants
import com.generationai.polaris.utils.RetrofitClient
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [LoginFormFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginFormFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding: FragmentLoginFormBinding
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
        binding=FragmentLoginFormBinding.inflate(layoutInflater)
        binding.fragmentLoginFormLoginButton.setOnClickListener{
            //TODO: CHECK VALID INPUT, SEND API REQUEST, RECEIVE API RESPONSE, SAVE TOKEN, NAVIGATE TO MAIN ACTIVITY
            val emailEditText = binding.fragmentLoginFormEmailTextInputLayout.editText!!
            val passwordEditText = binding.fragmentLoginFormPasswordTextInputLayout.editText!!

            //exit if email or password is invalid
            if(!(checkEmailInput(emailEditText) && checkPasswordInput(passwordEditText))){
                return@setOnClickListener
            }

            //send the API Request using coroutines
            performLogin(emailEditText.text.toString(),passwordEditText.text.toString())

        }
        binding.fragmentLoginFormCancelButton.setOnClickListener{
            requireActivity().supportFragmentManager.beginTransaction().replace(R.id.login_activity_fragmentContainerView,LoginLandingFragment()).commit()
        }

        binding.fragmentLoginFormEmailTextInputLayout.editText!!.text
        binding.fragmentLoginFormPasswordTextInputLayout.editText!!.text

        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

    }

    private fun checkEmailInput(emailEditText: EditText):Boolean {
        var isValid = false
        emailEditText.text
        if(Patterns.EMAIL_ADDRESS.matcher(emailEditText.text).matches()) {
            isValid = true
        }
        else{
            emailEditText.error = "Invalid Email"
        }
        return isValid
    }

    private fun checkPasswordInput(passwordEditText: EditText):Boolean {
        var isValid = false
        if(true){
            isValid=true
        }else{
            passwordEditText.error = "Invalid Password"
        }
        return isValid
    }
    private fun performLogin(email:String,password:String){

        backendInterface.loginUser(LoginRequest(email,password)).enqueue(object :Callback<LoginResponse>{
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if(!response.isSuccessful){
                    return
                }
                val loginResponse = response.body() ?: return
                Log.i("LoginActivity", "onResponse: ${loginResponse.name}, ${loginResponse.job}")
                //use this for actual implementation, rmb to change the values in LoginResponse as well
//                val uid = loginResponse.uid
//                val username = loginResponse.username
//                val email = loginResponse.email
//                val sessionToken= loginResponse.sessionToken
//                lifecycleScope.launch{
//                    requireActivity().dataStore.edit { preferences ->
//                        run{
//                            preferences[Constants.EMAIL_KEY] = email ?:""
//                            preferences[Constants.PASSWORD_KEY] = password
//                            preferences[Constants.SESSION_TOKEN_KEY] = sessionToken ?: ""
//                        }
//                    }
//                }
                //TEMPORARY VALUES FOR TESTING
                val uid = "12345678"
                val username = "Testis"
                val email = "test@test.com"

                lifecycleScope.launch{
                    if (isAdded){
                        requireActivity().dataStore.edit { preferences ->
                            run{
                                preferences[Constants.EMAIL_KEY] = email
                                preferences[Constants.PASSWORD_KEY] = password
                            }
                        }
                        val intent=Intent(requireActivity(),MainActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    }
                }

            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.e("LoginActivity", "Error: ${t.message}")
                Toast.makeText(requireContext(), "Login Failed due to Error", Toast.LENGTH_SHORT).show()
            }

        })
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment LoginFormFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            LoginFormFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}