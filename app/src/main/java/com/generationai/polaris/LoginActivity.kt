package com.generationai.polaris

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.fragment.app.Fragment
import com.generationai.polaris.databinding.ActivityLoginBinding
import com.generationai.polaris.utils.Constants
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val dataStore: DataStore<Preferences> by lazy {DataStoreManager.getInstance(this)}

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
}