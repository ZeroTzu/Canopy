package com.generationai.polaris

import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.fragment.app.Fragment
import com.generationai.polaris.databinding.ActivityLoginBinding
import com.generationai.polaris.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val dataStore: DataStore<Preferences> by lazy {DataStoreManager.getInstance(this)}
    private lateinit var firebaseAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

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