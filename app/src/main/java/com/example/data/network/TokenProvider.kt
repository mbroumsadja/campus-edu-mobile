package com.example.data.network

import android.content.Context
import android.content.SharedPreferences

class TokenProvider(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("campus_edu_prefs", Context.MODE_PRIVATE)

    fun getAccessToken(): String? {
        return prefs.getString("access_token", null)
    }

    fun setAccessToken(token: String?) {
        prefs.edit().putString("access_token", token).apply()
    }
}
