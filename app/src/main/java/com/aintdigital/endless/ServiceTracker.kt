package com.aintdigital.endless

import android.content.Context
import android.content.SharedPreferences

enum class ServiceState {
    STARTED,
    STOPPED,
}

private const val name = "AINTSERVICE_KEY"
private const val key = "AINTSERVICE_STATE"
var address: String = ""
var isMining: Boolean = true
var forceRestart: Boolean = true
var service: EndlessService? = null

fun setServiceState(context: Context, state: ServiceState) {
    val sharedPrefs = getPreferences(context)
    sharedPrefs.edit().let {
        it.putString(key, state.name)
        it.apply()
    }
}

fun getServiceState(context: Context): ServiceState {
    val sharedPrefs = getPreferences(context)
    val value = sharedPrefs.getString(key, ServiceState.STOPPED.name)
    if (value == ServiceState.STOPPED.name) {
        forceRestart = false
    }
    return ServiceState.valueOf(value)
}

private fun getPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(name, 0)
}
