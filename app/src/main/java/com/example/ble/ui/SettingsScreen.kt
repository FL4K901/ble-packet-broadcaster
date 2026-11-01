package com.example.ble.ui

import android.bluetooth.le.AdvertisingSetParameters
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ble.ui.theme.Purple80
import com.example.ble.ui.theme.Surface2
import com.example.ble.viewmodel.MainViewModel
import kotlin.math.roundToInt

private val TX_POWER_OPTIONS = listOf(
    "Low" to AdvertisingSetParameters.TX_POWER_LOW,
    "Medium" to AdvertisingSetParameters.TX_POWER_MEDIUM,
    "High" to AdvertisingSetParameters.TX_POWER_HIGH,
    "Ultra High" to AdvertisingSetParameters.TX_POWER_MAX
)

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // Advertising Interval
        SettingsCard(title = "Advertising Interval") {
            Text(
                text = "${settings.intervalMs} ms",
                color = Purple80,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(4.dp))
            Slider(
                value = settings.intervalMs.toFloat(),
                onValueChange = { viewModel.updateIntervalMs(it.roundToInt().toLong()) },
                valueRange = 20f..1000f,
                steps = 49,
                colors = SliderDefaults.colors(
                    thumbColor = Purple80,
                    activeTrackColor = Purple80
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("20ms (Fast)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Text("1000ms (Slow)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }

        // TX Power
        SettingsCard(title = "TX Power Level") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TX_POWER_OPTIONS.forEach { (label, value) ->
                    FilterChip(
                        selected = settings.txPower == value,
                        onClick = { viewModel.updateTxPower(value) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Purple80.copy(alpha = 0.3f),
                            selectedLabelColor = Purple80
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Toggles
        SettingsCard(title = "Options") {
            SettingsToggle(
                label = "Include Device Name",
                subtitle = "Broadcasts device name in advertisement",
                checked = settings.includeDeviceName,
                onCheckedChange = { viewModel.updateIncludeDeviceName(it) }
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            SettingsToggle(
                label = "Randomize MAC Address",
                subtitle = "New random MAC each advertising cycle",
                checked = settings.randomizeMac,
                onCheckedChange = { viewModel.updateRandomizeMac(it) }
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            SettingsToggle(
                label = "Randomize Payload Bytes",
                subtitle = "For iOS crash mode — randomizes data each cycle",
                checked = settings.randomizePayload,
                onCheckedChange = { viewModel.updateRandomizePayload(it) }
            )
        }

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ℹ️ About",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "BLE Spammer is an educational tool for studying Bluetooth Low Energy advertisement protocols. " +
                        "For use in authorized testing environments only.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "⚠️ iOS Crash mode is experimental and may not work on iOS 18+.",
                    color = Color(0xFFFF5722),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface2),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SettingsToggle(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Purple80
            )
        )
    }
}
