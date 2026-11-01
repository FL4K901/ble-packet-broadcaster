package com.example.ble.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.ble.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DetectedSpammer(
    val macAddress: String,
    val type: String,
    val rssi: Int,
    val timestamp: String,
)

class BleScannerService : Service() {

    companion object {
        const val ACTION_START = "com.example.ble.ACTION_START_SCAN"
        const val ACTION_STOP = "com.example.ble.ACTION_STOP_SCAN"
        const val NOTIFICATION_CHANNEL_ID = "ble_scanner_channel"
        const val NOTIFICATION_ID = 1002
        private const val TAG = "BleScannerService"

        @Volatile var isRunning = false
        val detectedDevices = MutableStateFlow<List<DetectedSpammer>>(emptyList())
        val notificationCount = MutableStateFlow(0)
    }

    private var scanner: BluetoothLeScanner? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            processResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { processResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error: $errorCode")
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        scanner = bluetoothManager.adapter?.bluetoothLeScanner
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification("Scanning for BLE spam..."))
                startScanning()
            }
            ACTION_STOP -> {
                stopScanning()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startScanning() {
        isRunning = true
        detectedDevices.value = emptyList()
        notificationCount.value = 0

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner?.startScan(null, settings, scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start scan", e)
        }
    }

    private fun stopScanning() {
        isRunning = false
        try {
            scanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop scan", e)
        }
    }

    private fun processResult(result: ScanResult) {
        val mac = result.device.address ?: "Unknown"
        val rssi = result.rssi
        val record = result.scanRecord ?: return
        val manufacturerData = record.manufacturerSpecificData

        val spamType = detectSpamType(manufacturerData) ?: return

        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = DetectedSpammer(
            macAddress = mac,
            type = spamType,
            rssi = rssi,
            timestamp = timestamp
        )

        val current = detectedDevices.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.macAddress == mac && it.type == spamType }
        if (existingIndex >= 0) {
            current[existingIndex] = entry
        } else {
            current.add(0, entry)
            notificationCount.value++
            updateNotification("Detected: $spamType @ $mac")
        }
        // Keep last 100 entries
        if (current.size > 100) current.removeAt(current.size - 1)
        detectedDevices.value = current
    }

    private fun detectSpamType(manufacturerData: android.util.SparseArray<ByteArray>?): String? {
        if (manufacturerData == null) return null

        // Check Apple (0x004C)
        val appleData = manufacturerData.get(0x004C)
        if (appleData != null && appleData.isNotEmpty()) {
            return detectAppleType(appleData)
        }

        // Check Samsung (0x0075)
        val samsungData = manufacturerData.get(0x0075)
        if (samsungData != null && samsungData.isNotEmpty()) {
            return detectSamsungType(samsungData)
        }

        // Check Microsoft (0x0006)
        val msData = manufacturerData.get(0x0006)
        if (msData != null && msData.isNotEmpty()) {
            if (msData.size >= 3 && msData[0] == 0x03.toByte() && msData[1] == 0x00.toByte()) {
                return "Microsoft Swift Pair"
            }
        }

        return null
    }

    private fun detectAppleType(data: ByteArray): String {
        if (data.isEmpty()) return "Apple Unknown"
        return when (data[0].toInt() and 0xFF) {
            0x07 -> {
                if (data.size >= 4) {
                    when (data[3].toInt() and 0xFF) {
                        0x02 -> "Apple AirPods"
                        0x0E -> "Apple AirPods Pro"
                        0x0A -> "Apple AirPods Max"
                        0x10 -> "Apple Beats Flex"
                        0x06 -> "Apple Beats Solo 3"
                        0x09 -> "Apple Beats Studio 3"
                        0x0B -> "Apple Powerbeats Pro"
                        0x0C -> "Apple Beats Headphones"
                        else -> "Apple Proximity Pairing (Flipper Zero?)"
                    }
                } else "Apple Proximity Pairing"
            }
            0x0B -> "Apple TV Setup"
            0x0F -> "Apple Handoff (Possible iOS Crash Attempt)"
            0x12 -> "Apple AirTag"
            0x27 -> "Apple TV Color Balance"
            0x09 -> "Apple New Device / Migration"
            else -> "Apple Unknown (0x${data[0].toInt().and(0xFF).toString(16).uppercase()}) - Possible Flipper Zero"
        }
    }

    private fun detectSamsungType(data: ByteArray): String {
        if (data.size < 4) return "Samsung Easy Setup (Unknown)"
        return when (data[3].toInt() and 0xFF) {
            0x02 -> "Samsung Galaxy Buds2 Pro"
            0x01 -> "Samsung Galaxy Buds Live"
            0x00 -> "Samsung Galaxy Buds Pro"
            0x05 -> "Samsung Galaxy Buds2"
            0x09 -> "Samsung Galaxy Buds FE"
            0x06 -> "Samsung Galaxy Watch 4"
            0x07 -> "Samsung Galaxy Watch 5"
            else -> "Samsung Easy Setup (Possible Flipper Zero)"
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "BLE Scanner",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "BLE spam detector service" }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("BLE Spam Detector")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScanning()
    }
}
