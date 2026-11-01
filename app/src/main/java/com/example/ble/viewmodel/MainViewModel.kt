package com.example.ble.viewmodel

import android.app.Application
import android.bluetooth.le.AdvertisingSetParameters
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ble.data.APPLE_PAYLOADS
import com.example.ble.data.BlePayload
import com.example.ble.data.GOOGLE_PAYLOADS
import com.example.ble.data.IOS_CRASH_PAYLOADS
import com.example.ble.data.LOVESPOUSE_PAYLOADS
import com.example.ble.data.MICROSOFT_PAYLOADS
import com.example.ble.data.PayloadCategory
import com.example.ble.data.SAMSUNG_PAYLOADS
import com.example.ble.service.BleAdvertiserService
import com.example.ble.service.BleScannerService
import com.example.ble.service.DetectedSpammer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

data class AppSettings(
    val intervalMs: Long = 100L,
    val txPower: Int = AdvertisingSetParameters.TX_POWER_MEDIUM,
    val includeDeviceName: Boolean = false,
    val randomizeMac: Boolean = false,
    val randomizePayload: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val allPayloads: List<BlePayload> = APPLE_PAYLOADS + IOS_CRASH_PAYLOADS + MICROSOFT_PAYLOADS + SAMSUNG_PAYLOADS + GOOGLE_PAYLOADS + LOVESPOUSE_PAYLOADS

    private val _selectedPayloadIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedPayloadIds: StateFlow<Set<String>> = _selectedPayloadIds.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val advertiserStatus: StateFlow<String> = BleAdvertiserService.statusFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Stopped")

    val detectedDevices: StateFlow<List<DetectedSpammer>> = BleScannerService.detectedDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scanNotificationCount: StateFlow<Int> = BleScannerService.notificationCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Payload selection
    fun togglePayload(id: String) {
        val current = _selectedPayloadIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedPayloadIds.value = current
    }

    fun selectAll() {
        _selectedPayloadIds.value = allPayloads.map { it.id }.toSet()
    }

    fun selectNone() {
        _selectedPayloadIds.value = emptySet()
    }

    fun selectCategory(category: PayloadCategory) {
        val current = _selectedPayloadIds.value.toMutableSet()
        allPayloads.filter { it.category == category }.forEach { current.add(it.id) }
        _selectedPayloadIds.value = current
    }

    fun deselectCategory(category: PayloadCategory) {
        val current = _selectedPayloadIds.value.toMutableSet()
        allPayloads.filter { it.category == category }.forEach { current.remove(it.id) }
        _selectedPayloadIds.value = current
    }

    // Advertising control
    fun startAdvertising() {
        val ctx = getApplication<Application>()
        val selected = allPayloads.filter { _selectedPayloadIds.value.contains(it.id) }
        if (selected.isEmpty()) return

        val intent = Intent(ctx, BleAdvertiserService::class.java).apply {
            action = BleAdvertiserService.ACTION_START
            putExtra(BleAdvertiserService.EXTRA_INTERVAL_MS, _settings.value.intervalMs)
            putExtra(BleAdvertiserService.EXTRA_TX_POWER, _settings.value.txPower)
            putExtra(BleAdvertiserService.EXTRA_RANDOMIZE_MAC, _settings.value.randomizeMac)
            // Pass selected payload IDs via service start and let the service fetch them
            putStringArrayListExtra("payload_ids", ArrayList(selected.map { it.id }))
        }
        ctx.startForegroundService(intent)
        _isAdvertising.value = true
    }

    fun stopAdvertising() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, BleAdvertiserService::class.java).apply {
            action = BleAdvertiserService.ACTION_STOP
        }
        ctx.startService(intent)
        _isAdvertising.value = false
    }

    // Scanner control
    fun startScanning() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, BleScannerService::class.java).apply {
            action = BleScannerService.ACTION_START
        }
        ctx.startForegroundService(intent)
        _isScanning.value = true
    }

    fun stopScanning() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, BleScannerService::class.java).apply {
            action = BleScannerService.ACTION_STOP
        }
        ctx.startService(intent)
        _isScanning.value = false
    }

    fun clearDetected() {
        BleScannerService.detectedDevices.value = emptyList()
        BleScannerService.notificationCount.value = 0
    }

    // Settings
    fun updateIntervalMs(ms: Long) {
        _settings.value = _settings.value.copy(intervalMs = ms)
    }

    fun updateTxPower(power: Int) {
        _settings.value = _settings.value.copy(txPower = power)
    }

    fun updateIncludeDeviceName(include: Boolean) {
        _settings.value = _settings.value.copy(includeDeviceName = include)
    }

    fun updateRandomizeMac(randomize: Boolean) {
        _settings.value = _settings.value.copy(randomizeMac = randomize)
    }

    fun updateRandomizePayload(randomize: Boolean) {
        _settings.value = _settings.value.copy(randomizePayload = randomize)
    }
}
