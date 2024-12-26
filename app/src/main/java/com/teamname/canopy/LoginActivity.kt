package com.teamname.canopy

import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.fragment.app.Fragment
import com.teamname.canopy.databinding.ActivityLoginBinding
import com.teamname.canopy.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val dataStore: DataStore<Preferences> by lazy {DataStoreManager.getInstance(this)}
    lateinit var firebaseAuth: FirebaseAuth
    lateinit var firestore: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore= FirebaseFirestore.getInstance()
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth=FirebaseAuth.getInstance()

        replaceFragment(LoginLandingFragment())

    }
    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.login_activity_fragmentContainerView, fragment)
        fragmentTransaction.commit()

    }
    private suspend fun getEmailFromDataStore(): String? {
        return dataStore.data.map { preferences ->
            preferences[Constants.EMAIL_KEY]  // Use the stringPreferencesKey to access the value
        }.firstOrNull()
    }
    private suspend fun getPasswordFromDataStore(): String? {
        return dataStore.data.map { preferences ->
            preferences[Constants.PASSWORD_KEY]  // Use the stringPreferencesKey to access the value
        }.firstOrNull()
    }

    fun checkPasswordSame(passwordEditText: EditText, confirmPasswordEditText: EditText):Boolean {
        val isSame = passwordEditText.text.toString()==confirmPasswordEditText.text.toString()
        if(!isSame){
            passwordEditText.error="Password not the same"
            confirmPasswordEditText.error="Password not the same"
        }
        return isSame
    }

    fun checkEmailInput(emailEditText: EditText):Boolean {
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

    fun checkPasswordInput(passwordEditText: EditText):Boolean {
        var isValid = false
        val password=passwordEditText.text.toString()
        if(password.length>=3){
            isValid=true
        }else{
            passwordEditText.error="Password must contain at least 3 characters"
        }
        return isValid
    }


}