package com.generationai.polaris.utils

import androidx.datastore.preferences.core.stringPreferencesKey


public class Constants {
    companion object {
        const val BACKGROUND_SERVICE_CHANNEL_ID = "background_service_channel"

        const val BACKGROUND_SERVICE_RUNNING_NOTIFICATION_ID = 100

        const val BACKGROUND_SERVICE_ALIVE = 72914
        val USER_ID_KEY = stringPreferencesKey("user_id")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val EMAIL_KEY = stringPreferencesKey("user_email")
        val PASSWORD_KEY = stringPreferencesKey("user_password")
        val SESSION_TOKEN_KEY = stringPreferencesKey("user_session_token")
    }
}