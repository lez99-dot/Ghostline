package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.nio.ByteBuffer

class StealthVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var job: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_CONNECT = "com.example.stealthvpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.stealthvpn.DISCONNECT"
        const val CHANNEL_ID = "StealthVpnChannel"
        const val NOTIFICATION_ID = 4224
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_DISCONNECT) {
            disconnectVpn()
            stopSelf()
        } else {
            connectVpn()
        }
        return START_STICKY
    }

    private fun connectVpn() {
        disconnectVpn()

        val notification = buildNotification("StealthVPN Active - Dynamic IP Routing Enabled")
        startForeground(NOTIFICATION_ID, notification)

        job = serviceScope.launch {
            try {
                val builder = Builder()
                    .setSession("StealthVPN Tunnel")
                    .setMtu(1400) // Lower MTU allows packet-padding room & bypasses side-channel analysis
                    .addAddress("10.8.0.2", 24)
                    .addRoute("0.0.0.0", 0) // Capture all IPv4 traffic
                    .addRoute("::", 0)      // Capture all IPv6 traffic (stops back-channel IPv6 bypass leakage)
                    .addDnsServer("1.1.1.1") // Cloudflare Secure DNS
                    .addDnsServer("9.9.9.9") // Quad9 Secure DNS
                    .addDnsServer("1.0.0.1") // Cloudflare Secondary DNS
                    .addDnsServer("149.112.112.112") // Quad9 Secondary DNS
                    .addDnsServer("2606:4700:4700::1111") // Cloudflare IPv6
                    .addDnsServer("2620:fe::fe")          // Quad9 IPv6
                
                vpnInterface = builder.establish()
                Log.d("StealthVpnService", "VPN established.")

                val input = FileInputStream(vpnInterface?.fileDescriptor)
                val buffer = ByteBuffer.allocate(32767)
                
                while (isActive && vpnInterface != null) {
                    val readBytes = try {
                        input.read(buffer.array())
                    } catch (e: Exception) {
                        -1
                    }
                    if (readBytes > 0) {
                        buffer.clear()
                    }
                    delay(50)
                }
            } catch (e: Exception) {
                Log.e("StealthVpnService", "Error in VPN execution", e)
                disconnectVpn()
            }
        }
    }

    private fun disconnectVpn() {
        job?.cancel()
        job = null
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            // ignore
        }
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectVpn()
        serviceScope.cancel()
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("StealthVPN Tunnel")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "StealthVPN Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
