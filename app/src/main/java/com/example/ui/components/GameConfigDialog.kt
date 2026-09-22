package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.GameEntity
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GameConfigDialog(
    game: GameEntity,
    onSave: (GameEntity) -> Unit,
    onDelete: (GameEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOs by remember { mutableStateOf(game.virtualAndroidVersion) }
    var selectedMemory by remember { mutableIntStateOf(game.memoryLimitMb) }

    val osOptions = listOf(
        "Android 4.4 (KitKat - API 19)",
        "Android 5.1 (Lollipop - API 22)",
        "Android 7.0 (Nougat - API 24)",
        "Android 9.0 (Pie - API 28)"
    )

    val memoryOptions = listOf(512, 1024, 2048)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("game_config_dialog"),
        containerColor = CyberSurface,
        titleContentColor = TextPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sandbox Environment Settings",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Configure 32-bit runtime parameters for ${game.title}:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                // Virtual OS Spoofing selection
                Column {
                    Text(
                        text = "VIRTUAL ANDROID SPOOFING:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    osOptions.forEach { os ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedOs = os.substringBefore(" -") }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedOs.startsWith(os.substringBefore(" -")),
                                onClick = { selectedOs = os.substringBefore(" -") },
                                colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = os,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // 32-bit Memory allocation selection
                Column {
                    Text(
                        text = "32-BIT VIRTUAL MEMORY (RAM):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        memoryOptions.forEach { mem ->
                            val isSelected = selectedMemory == mem
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else CyberSurfaceVariant)
                                    .clickable { selectedMemory = mem }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (mem >= 1024) "${mem / 1024} GB" else "$mem MB",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonCyan else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Delete Game Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDelete(game) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Delete",
                        tint = NeonRose,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete from 32-bit Sandbox",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonRose
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        game.copy(
                            virtualAndroidVersion = selectedOs,
                            memoryLimitMb = selectedMemory
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color(0xFF041E26)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Settings", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
