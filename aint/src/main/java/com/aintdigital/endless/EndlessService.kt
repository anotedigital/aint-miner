package com.aintdigital.endless

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.support.v4.app.NotificationManagerCompat
import android.widget.Toast
import com.github.kittinunf.fuel.Fuel
import com.aintdigital.endless.R
import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONTokener
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class EndlessService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false
    private var lastNotification : Int = 0

    override fun onBind(intent: Intent): IBinder? {
        log("Some component want to bind with the service")
        // We don't provide binding, so return null
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log("onStartCommand executed with startId: $startId")
        if (intent != null) {
            val action = intent.action
            log("using an intent with action $action")
            when (action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
                else -> log("This should never happen. No action in the received intent")
            }
        } else {
            log(
                "with a null intent. It has been probably restarted by the system."
            )
        }
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

    override fun onCreate() {
        service = this
        super.onCreate()
        log("The service has been created".toUpperCase())
        val notification = createNotification("AINT miner is now running")
        startForeground(1, notification)
//        val notification = (baseContext as MainActivity).createNotification("AINT miner is now mining")
//        with(NotificationManagerCompat.from(applicationContext)) {
//            notify(1, notification)
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        log("The service has been destroyed".toUpperCase())
        Toast.makeText(this, "AINT miner stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        if (forceRestart) {
            val restartServiceIntent = Intent(applicationContext, EndlessService::class.java).also {
                it.setPackage(packageName)
            };
            val restartServicePendingIntent: PendingIntent = PendingIntent.getService(this, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
            applicationContext.getSystemService(Context.ALARM_SERVICE);
            val alarmService: AlarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
            alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent);
        }
    }
    
    private fun startService() {
        if (isServiceStarted) return
        log("Starting the foreground service task")
        Toast.makeText(this, "Starting AINT miner", Toast.LENGTH_SHORT).show()
        isServiceStarted = true
        setServiceState(this, ServiceState.STARTED)

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock").apply {
                    acquire()
                }
            }

        // we're starting a loop in a coroutine
        GlobalScope.launch(Dispatchers.IO) {
            while (isServiceStarted) {
                delay(1 * 60 * 1000)
                launch(Dispatchers.IO) {
                    if (isMining) {
                        pingMiningServer()
                    }
                }
            }
            log("End of the loop for the service")
        }
    }

    private fun stopService() {
        log("Stopping the foreground service")
        Toast.makeText(this, "AINT miner stopping", Toast.LENGTH_SHORT).show()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            log("Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
        setServiceState(this, ServiceState.STOPPED)
    }

    fun pingMiningServer() {
        try {
            Fuel.get("https://mobile.anote.digital/mine/" + address)
                .response { _, resp, result ->
                    val (bytes, error) = result
                    if (bytes != null) {
                        val jsonObject = JSONTokener(String(bytes)).nextValue() as JSONObject
                        val cf = jsonObject.getString("cycle_finished")
                        log("[response bytes] ${String(bytes)}")
                        val time = Calendar.getInstance().time
                        val formatter = SimpleDateFormat("d")
                        val current = formatter.format(time)
                        if (cf == "true" && this.lastNotification != current.toInt()) {
                            this.lastNotification = current.toInt()
                            val notification = createNotification("Please restart the mining!")
                            startForeground(1, notification)
//                            val notification = (baseContext as MainActivity).createNotification("AINT miner is now mining")
//                            with(NotificationManagerCompat.from(applicationContext)) {
//                                notify(1, notification)
//                            }
                            isMining = false
                        }
                    } else {
                        log("[response error] ${error?.message}")
                    }
                }
        } catch (e: Exception) {
            log("Error making the request: ${e.message}")
        }
    }

    fun createNotification(text: String): Notification {
        val notificationChannelId = "AINT SERVICE CHANNEL"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                notificationChannelId,
                "AINT Service notifications channel",
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = "AINT Service channel"
                it.enableLights(true)
                it.lightColor = Color.RED
                it.enableVibration(true)
                it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
            this,
            notificationChannelId
        ) else Notification.Builder(this)

        return builder
            .setContentTitle("AINT Miner")
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("Ticker text")
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .build()
    }
}
