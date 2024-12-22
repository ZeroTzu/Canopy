package com.teamname.canopy

import android.content.Context
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
import com.teamname.canopy.databinding.FragmentLoginFormBinding
import com.teamname.canopy.api.LoginRequest
import com.teamname.canopy.api.LoginResponse

import com.teamname.canopy.utils.BackendInterface
import com.teamname.canopy.utils.Constants
import com.teamname.canopy.utils.RetrofitClient
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
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

            val loginActivity =activity as LoginActivity
            //exit if email or password is invalid
            if(!(loginActivity.checkEmailInput(emailEditText) && loginActivity.checkPasswordInput(passwordEditText))){
                return@setOnClickListener
            }

            (activity as LoginActivity).firebaseAuth.signInWithEmailAndPassword(emailEditText.text.toString(),passwordEditText.text.toString())
                .addOnCompleteListener{
                    if (it.isSuccessful){
                        redirectToHome()
                    }
                    else{
                        Toast.makeText(requireContext(), it.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
        }
        binding.fragmentLoginFormGoogleCustomButtonLayout.apply {
            customButtonMaterialButton.setIconResource(R.drawable.google)
            customButtonMaterialButton.text=R.string.google_sign_in.toString()
            customButtonMaterialButton.setOnClickListener{
                var googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("80558157809-s66cksgt2kcd8bisqe3o7marf8g28a9b.apps.googleusercontent.com")
            }
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
    private fun performLogin(email:String,password:String){

        Log.e("PolarisLoginActivity","email $email, password $password")

        backendInterface.loginUser(LoginRequest(email,password)).enqueue(object :Callback<LoginResponse>{
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if(!response.isSuccessful){
                    return
                }
                val loginResponse = response.body() ?: return

                Log.i("PolarisLoginActivity", "onResponse: ${loginResponse.code}")
                if(loginResponse.status.toString()==Constants.BACK_END_SERVER_STATUS_FAILED){
                    val message =loginResponse.message!!.toString()
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    return
                }

                val uid = ""
                val username = email
                val email = email
                val password= password
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
                        redirectToHome()
                    }
                }

            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.e("LoginActivity", "Error: ${t.message}")
                Toast.makeText(requireContext(), "Login Failed due to Error", Toast.LENGTH_SHORT).show()
            }

        })
    }

    fun redirectToHome(){
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