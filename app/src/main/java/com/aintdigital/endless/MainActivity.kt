package com.aintdigital.endless

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient


class MainActivity : AppCompatActivity() {

    private var mywebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mywebView = findViewById(R.id.webview) as WebView
        mywebView!!.setWebChromeClient(WebChromeClient())

        mywebView!!.setWebViewClient(object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    view.context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    )
                    true
                } else {
                    false
                }
            }
        })

        if (getServiceState(this) == ServiceState.STOPPED) {
            mywebView!!.loadUrl("https://aint.digital/?app=true")
        } else {
            mywebView!!.loadUrl("https://aint.digital/?run=true&app=true")
        }
        val webSettings = mywebView!!.getSettings()
        webSettings.javaScriptEnabled = true
        webSettings.databaseEnabled = true
        webSettings.domStorageEnabled = true
        mywebView!!.addJavascriptInterface(MyJavascriptInterface(this), "MyJavascriptInterface")
    }

    public fun startMiner() {
        actionOnService(Actions.START)
    }

    public fun stopMiner() {
        actionOnService(Actions.STOP)
    }

    private fun actionOnService(action: Actions) {
        if (getServiceState(this) == ServiceState.STOPPED && action == Actions.STOP) return
        Intent(this, EndlessService::class.java).also {
            it.action = action.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                log("Starting the service in >=26 Mode")
                startForegroundService(it)
                return
            }
            log("Starting the service in < 26 Mode")
            startService(it)
        }
    }
}
