package com.generationai.polaris.utils

import androidx.datastore.preferences.core.stringPreferencesKey


public class Constants {
    companion object {
        const val BACKGROUND_SERVICE_CHANNEL_ID = "background_service_channel"

        const val BACKGROUND_SERVICE_RUNNING_NOTIFICATION_ID = 100

        const val BACKGROUND_SERVICE_ALIVE = 72914
        val EMAIL_KEY = stringPreferencesKey("user_email")
        val PASSWORD_KEY = stringPreferencesKey("user_password")
    }
}