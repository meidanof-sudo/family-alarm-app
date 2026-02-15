package com.familyalarm.data

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "family_alarm_prefs"
        private const val KEY_FAMILY_CODE = "family_code"
        private const val KEY_MEMBER_NAME = "member_name"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var familyCode: String?
        get() = prefs.getString(KEY_FAMILY_CODE, null)
        set(value) = prefs.edit().putString(KEY_FAMILY_CODE, value).apply()

    var memberName: String?
        get() = prefs.getString(KEY_MEMBER_NAME, null)
        set(value) = prefs.edit().putString(KEY_MEMBER_NAME, value).apply()

    val isInFamily: Boolean
        get() = !familyCode.isNullOrBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
