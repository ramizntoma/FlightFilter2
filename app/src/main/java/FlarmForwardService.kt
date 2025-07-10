package com.example.flightfilter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.InetAddresses
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.*

class FlarmForwardService : Service() {

    private var serviceJob: Job? = null
    private var socket: Socket? = null

    private val tcpServerIp = "127.0.0.1"
    private val tcpServerPort = 10113

    private val udpTargetIp = "127.0.0.1"
    private val udpTargetPort = 10112

    private val altitudeLimit = 500  // You can make this configurable later

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundWithNotification()

        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                socket = Socket()
                socket?.connect(InetSocketAddress(tcpServerIp, tcpServerPort), 3000)

                val reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                val udpSocket = DatagramSocket()
                val udpAddress = InetAddress.getByName(udpTargetIp)

                while (isActive && socket?.isClosed == false) {
                    val line = reader.readLine() ?: break
                    val relativeVertical = parseRelativeVertical(line)

                    if (relativeVertical != null && relativeVertical in -altitudeLimit..altitudeLimit) {
                        val sendData = line.toByteArray()
                        val packet = DatagramPacket(sendData, sendData.size, udpAddress, udpTargetPort)
                        udpSocket.send(packet)
                    }
                }

                udpSocket.close()
            } catch (e: Exception) {
                Log.e("FlarmService", "Error in service: ${e.message}", e)
            } finally {
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel()
        try {
            socket?.close()
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundWithNotification() {
        val channelId = "flarm_forward_channel"
        val channelName = "FLARM Forwarding Service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                channelId, channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(chan)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("FLARM Forwarding Running")
            .setContentText("Filtering and forwarding data in background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    private fun parseRelativeVertical(message: String): Int? {
        val clean = message.removePrefix("$").split("*")[0]
        val parts = clean.split(",")
        return if (parts.size > 4 && parts[0] == "PFLAA") {
            parts[4].toIntOrNull()
        } else {
            null
        }
    }
}
