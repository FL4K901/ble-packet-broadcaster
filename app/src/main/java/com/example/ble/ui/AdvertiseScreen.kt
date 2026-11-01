package com.example.ble.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ble.data.BlePayload
import com.example.ble.data.PayloadCategory
import com.example.ble.ui.theme.AccentApple
import com.example.ble.ui.theme.AccentCrash
import com.example.ble.ui.theme.AccentGoogle
import com.example.ble.ui.theme.AccentLovespouse
import com.example.ble.ui.theme.AccentMicrosoft
import com.example.ble.ui.theme.AccentSamsung
import com.example.ble.ui.theme.Surface2
import com.example.ble.ui.theme.Surface3
import com.example.ble.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvertiseScreen(viewModel: MainViewModel) {
    val selectedIds by viewModel.selectedPayloadIds.collectAsState()
    val isAdvertising by viewModel.isAdvertising.collectAsState()
    val status by viewModel.advertiserStatus.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val groupedPayloads = viewModel.allPayloads.groupBy { it.category }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            // Status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAdvertising) Surface3.copy(alpha = 0.8f) else Surface2
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isAdvertising) Color(0xFF00E676) else Color(0xFF616161))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isAdvertising) "ADVERTISING" else "STOPPED",
                            color = if (isAdvertising) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = status,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Interval: ${settings.intervalMs}ms",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        item {
            // Quick selection buttons
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(onClick = { viewModel.selectAll() }) { Text("Select All") }
                FilledTonalButton(onClick = { viewModel.selectNone() }) { Text("Select None") }
                FilledTonalButton(
                    onClick = { viewModel.selectCategory(PayloadCategory.APPLE) },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = AccentApple.copy(0.3f))
                ) { Text("Apple", color = AccentApple) }
                FilledTonalButton(
                    onClick = { viewModel.selectCategory(PayloadCategory.SAMSUNG) },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = AccentSamsung.copy(0.3f))
                ) { Text("Samsung", color = AccentSamsung) }
                FilledTonalButton(
                    onClick = { viewModel.selectCategory(PayloadCategory.GOOGLE) },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = AccentGoogle.copy(0.3f))
                ) { Text("Google", color = AccentGoogle) }
            }
        }

        // Payload groups by category
        groupedPayloads.forEach { (category, payloads) ->
            item(key = "header_${category.name}") {
                CategoryHeader(category = category)
            }
            items(payloads, key = { it.id }) { payload ->
                PayloadItem(
                    payload = payload,
                    isSelected = selectedIds.contains(payload.id),
                    onToggle = { viewModel.togglePayload(payload.id) }
                )
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            // Big START/STOP button
            Button(
                onClick = {
                    if (isAdvertising) viewModel.stopAdvertising()
                    else viewModel.startAdvertising()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAdvertising) Color(0xFFE53935) else Color(0xFF1B5E20),
                ),
                enabled = isAdvertising || selectedIds.isNotEmpty()
            ) {
                Icon(
                    imageVector = if (isAdvertising) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (isAdvertising) "STOP ADVERTISING" else "START ADVERTISING",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun CategoryHeader(category: PayloadCategory) {
    val color = categoryColor(category)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = category.displayName,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        if (category == PayloadCategory.IOS_CRASH) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "⚠ Experimental",
                color = AccentCrash,
                fontSize = 11.sp
            )
        }
        if (category == PayloadCategory.GOOGLE) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "EOL",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
    HorizontalDivider(color = color.copy(alpha = 0.3f))
}

@Composable
fun PayloadItem(
    payload: BlePayload,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val color = categoryColor(payload.category)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.12f) else Surface2
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = color,
                    checkmarkColor = Color.White
                )
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payload.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (payload.description.isNotEmpty()) {
                    Text(
                        text = payload.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

fun categoryColor(category: PayloadCategory): Color = when (category) {
    PayloadCategory.APPLE -> AccentApple
    PayloadCategory.MICROSOFT -> AccentMicrosoft
    PayloadCategory.SAMSUNG -> AccentSamsung
    PayloadCategory.GOOGLE -> AccentGoogle
    PayloadCategory.LOVESPOUSE -> AccentLovespouse
    PayloadCategory.IOS_CRASH -> AccentCrash
}
