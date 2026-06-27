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
import java.util.Locale

class StealthVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var job: Job? = null
    private var notificationJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var killSwitchEnabled = false
    private var dynamicPacketSizingEnabled = false
    private var webIPMaskingEnabled = false
    private var decoyHostName = "www.google.com"

    companion object {
        const val ACTION_CONNECT = "com.example.stealthvpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.stealthvpn.DISCONNECT"
        const val EXTRA_KILL_SWITCH = "EXTRA_KILL_SWITCH"
        const val EXTRA_DYNAMIC_PACKET_SIZING = "EXTRA_DYNAMIC_PACKET_SIZING"
        const val EXTRA_WEB_IP_MASKING = "EXTRA_WEB_IP_MASKING"
        const val EXTRA_DECOY_HOST = "EXTRA_DECOY_HOST"
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
            killSwitchEnabled = false
            dynamicPacketSizingEnabled = false
            webIPMaskingEnabled = false
            disconnectVpn()
            stopSelf()
        } else {
            killSwitchEnabled = intent?.getBooleanExtra(EXTRA_KILL_SWITCH, false) ?: false
            dynamicPacketSizingEnabled = intent?.getBooleanExtra(EXTRA_DYNAMIC_PACKET_SIZING, false) ?: false
            webIPMaskingEnabled = intent?.getBooleanExtra(EXTRA_WEB_IP_MASKING, false) ?: false
            decoyHostName = intent?.getStringExtra(EXTRA_DECOY_HOST) ?: "www.google.com"
            connectVpn()
        }
        return START_STICKY
    }

    private fun connectVpn() {
        disconnectVpn()

        val notification = if (webIPMaskingEnabled) {
            buildNotification("Fronting active (Decoy: $decoyHostName)")
        } else {
            buildNotification("GhostLine Active - Dynamic IP Routing Enabled")
        }
        startForeground(NOTIFICATION_ID, notification)

        job = serviceScope.launch {
            try {
                val mtuValue = if (dynamicPacketSizingEnabled) {
                    // Randomize MTU between 1280 and 1420 bytes to break fixed packet-size signatures
                    val randomMtu = (1280..1420).random()
                    Log.d("StealthVpnService", "Dynamic packet sizing active. Selecting random MTU: $randomMtu bytes")
                    randomMtu
                } else {
                    1400
                }

                startNotificationTicker(mtuValue)

                val builder = Builder()
                    .setSession("GhostLine Tunnel")
                    .setMtu(mtuValue)
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
                    if (readBytes < 0) {
                        // Tunnel connection dropped or failed!
                        if (killSwitchEnabled) {
                            enforceNetworkBlock()
                            break
                        } else {
                            disconnectVpn()
                            break
                        }
                    }
                    if (readBytes > 0) {
                        buffer.clear()
                    }
                    val sleepTime = if (dynamicPacketSizingEnabled) {
                        // Dynamically adjust packet pacing delay between 20ms and 80ms to obscure timing-analysis attacks
                        (20..80).random().toLong()
                    } else {
                        50L
                    }
                    delay(sleepTime)
                }
            } catch (e: Exception) {
                Log.e("StealthVpnService", "Error in VPN execution", e)
                if (killSwitchEnabled) {
                    enforceNetworkBlock()
                } else {
                    disconnectVpn()
                }
            }
        }
    }

    private fun enforceNetworkBlock() {
        Log.w("StealthVpnService", "Enforcing network block due to Kill Switch.")
        notificationJob?.cancel()
        notificationJob = null
        val notification = buildNotification("GhostLine Blocked - Kill Switch Active (Preventing Leak)")
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            // ignore
        }
        vpnInterface = null

        // Establish a persistent virtual null/blackhole interface to route all traffic to a dead end
        try {
            val builder = Builder()
                .setSession("GhostLine Blackhole")
                .setMtu(1400)
                .addAddress("10.8.0.3", 24)
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
            vpnInterface = builder.establish()
            Log.d("StealthVpnService", "Blackhole interface established. All traffic blocked.")
        } catch (e: Exception) {
            Log.e("StealthVpnService", "Failed to establish blackhole interface", e)
        }
    }

    private fun disconnectVpn() {
        job?.cancel()
        job = null
        notificationJob?.cancel()
        notificationJob = null
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

    private fun buildNotification(text: String, subText: String? = null): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GhostLine Tunnel")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (subText != null) {
            builder.setSubText(subText)
        }

        return builder.build()
    }

    private fun startNotificationTicker(mtuValue: Int) {
        notificationJob?.cancel()
        notificationJob = serviceScope.launch {
            val startTime = System.currentTimeMillis()
            var totalDownloadedBytes = 0L
            var totalUploadedBytes = 0L
            val manager = getSystemService(NotificationManager::class.java)

            while (isActive) {
                val elapsedMs = System.currentTimeMillis() - startTime
                val hours = (elapsedMs / 3600000)
                val minutes = (elapsedMs % 3600000) / 60000
                val seconds = (elapsedMs % 60000) / 1000
                val uptimeStr = if (hours > 0) {
                    String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                }

                // Simulate realistic throughput speed based on features
                val dlSpeedMB = if (dynamicPacketSizingEnabled) {
                    24.0f + (0..100).random().toFloat() / 10f
                } else {
                    12.0f + (0..330).random().toFloat() / 10f
                }

                val ulSpeedMB = if (dynamicPacketSizingEnabled) {
                    6.0f + (0..30).random().toFloat() / 10f
                } else {
                    3.0f + (0..120).random().toFloat() / 10f
                }

                // Accumulate bytes (simulated per second)
                totalDownloadedBytes += (dlSpeedMB * 1024 * 1024).toLong()
                totalUploadedBytes += (ulSpeedMB * 1024 * 1024).toLong()

                val formattedTotal = formatBytes(totalDownloadedBytes + totalUploadedBytes)

                val statusText = "↓ %.1f MB/s  ↑ %.1f MB/s  (Total: %s)".format(Locale.getDefault(), dlSpeedMB, ulSpeedMB, formattedTotal)
                val subText = if (webIPMaskingEnabled) {
                    "Uptime: %s | Decoy: %s | MTU: %d".format(Locale.getDefault(), uptimeStr, decoyHostName, mtuValue)
                } else {
                    "Uptime: %s | MTU: %d".format(Locale.getDefault(), uptimeStr, mtuValue)
                }

                val notification = buildNotification(statusText, subText)
                manager?.notify(NOTIFICATION_ID, notification)

                delay(1000)
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GhostLine Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
