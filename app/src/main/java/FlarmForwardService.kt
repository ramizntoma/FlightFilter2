package com.example.flightfilter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class FlarmForwardService : Service() {
    private val CHANNEL_ID = "flarm_service_channel"
    private val NOTIF_ID = 101

    private val tcpServerIp = "127.0.0.1"
    private val tcpServerPort = 10113
    private val udpTargetIp = "127.0.0.1"
    private val udpTargetPort = 10112

    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FlightFilter Running")
            .setContentText("Forwarding FLARM data...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
        startForeground(NOTIF_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val altitudeLimit = intent?.getIntExtra("altitudeLimit", 500) ?: 500
        val flightFilter = intent?.getBooleanExtra("flightFilter", true) ?: true
        val proximityAlert = intent?.getBooleanExtra("proximityAlert", false) ?: false

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(tcpServerIp, tcpServerPort), 3000)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val udpSocket = DatagramSocket()
                val udpAddress = InetAddress.getByName(udpTargetIp)

                while (isActive) {
                    val line = reader.readLine() ?: break
                    val relAlt = parseRelativeVertical(line)

                    if (!flightFilter || (relAlt != null && relAlt in -altitudeLimit..altitudeLimit)) {
                        val packet = DatagramPacket(line.toByteArray(), line.length, udpAddress, udpTargetPort)
                        udpSocket.send(packet)
                    }
                }

                udpSocket.close()
                socket.close()
            } catch (e: Exception) {
                Log.e("FlarmService", "Error in service", e)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "FLARM Forwarding Service",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun parseRelativeVertical(message: String): Int? {
        val clean = message.removePrefix("$").split("*")[0]
        val parts = clean.split(",")
        return if (parts.size > 4 && parts[0] == "PFLAA") parts[4].toIntOrNull() else null
    }
}
