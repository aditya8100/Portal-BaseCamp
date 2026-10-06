package com.portalhomebase.app.data

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("homebase", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = sp.getString("server_url", "") ?: ""
        set(v) = sp.edit().putString("server_url", v.trim()).apply()

    var token: String
        get() = sp.getString("token", "") ?: ""
        set(v) = sp.edit().putString("token", v.trim()).apply()

    var themeMode: String
        get() = sp.getString("theme_mode", "auto") ?: "auto"
        set(v) = sp.edit().putString("theme_mode", v.trim()).apply()
}
