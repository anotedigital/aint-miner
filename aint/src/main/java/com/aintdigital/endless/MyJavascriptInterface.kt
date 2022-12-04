package com.aintdigital.endless

import android.content.Context
import android.webkit.JavascriptInterface
import android.support.v4.app.NotificationManagerCompat

class MyJavascriptInterface(private val context: Context) {
    @JavascriptInterface
    fun startMiner() {
        forceRestart = true
        (context as MainActivity).startMiner()
    }

    @JavascriptInterface
    fun stopMiner() {
        forceRestart = false
        (context as MainActivity).stopMiner()
    }

    @JavascriptInterface
    fun saveAddress(addr: String) {
        address = addr
    }

    @JavascriptInterface
    fun startMinerNotification() {
        val notification = service?.createNotification("AINT miner is now mining")
        with(NotificationManagerCompat.from(this.context)) {
            if (notification != null) {
                notify(1, notification)
                isMining = true
            }
        }
    }
}