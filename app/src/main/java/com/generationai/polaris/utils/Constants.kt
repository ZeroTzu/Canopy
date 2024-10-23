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


        val BACK_END_SERVER_STATUS_SUCCESS = "success"
        val BACK_END_SERVER_STATUS_FAILED = "error"
        val BACK_END_SERVER_CODE_WRONG_PASSWORD =-20003
        val BACK_END_SERVER_CODE_EMAIL_DOES_NOT_EXIST =-20002
        val BACK_END_SERVER_CODE_EMAIL_ALREADY_EXISTS =-20001

    }
}