package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compat.ApkAnalysisResult
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ApkImportDialog(
    isScanning: Boolean,
    analysisResult: ApkAnalysisResult?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("apk_import_dialog"),
        containerColor = CyberSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isScanning) "Inspecting 32-bit APK..." else "APK Compatibility Scan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            if (isScanning) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = NeonCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Analyzing ELF headers, .so ABIs & Manifest...",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            } else if (analysisResult != null) {
                if (!analysisResult.isValid) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = NeonRose,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Invalid APK file",
                            fontWeight = FontWeight.Bold,
                            color = NeonRose
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = analysisResult.error ?: "Could not inspect the package archive.",
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Title & Package Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberSurfaceVariant)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = analysisResult.appTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${analysisResult.packageName} • v${analysisResult.versionName}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextMuted
                                )
                            }
                        }

                        // ABI Compatibility Analysis
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (analysisResult.isPure32Bit) NeonCyan.copy(alpha = 0.1f)
                                    else NeonEmerald.copy(alpha = 0.1f)
                                )
                                .border(
                                    1.dp,
                                    if (analysisResult.isPure32Bit) NeonCyan.copy(alpha = 0.5f)
                                    else NeonEmerald.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (analysisResult.isPure32Bit) Icons.Default.Memory else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (analysisResult.isPure32Bit) NeonCyan else NeonEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (analysisResult.isPure32Bit)
                                            "PURE 32-BIT ARCH DETECTED"
                                        else "UNIVERSAL / MULTI-ARCH",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (analysisResult.isPure32Bit) NeonCyan else NeonEmerald
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (analysisResult.isPure32Bit)
                                        "Contains only 32-bit native binaries (${analysisResult.primaryAbi}). Will run inside the sandboxed ARM32 compatibility translation layer."
                                    else
                                        "Contains native code supporting multiple architectures. Will execute in isolated sandbox.",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        // Native Libraries List
                        if (analysisResult.nativeLibraries.isNotEmpty()) {
                            Column {
                                Text(
                                    text = "32-bit Native Libraries (${analysisResult.nativeLibraries.size}):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(analysisResult.nativeLibraries) { lib ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF1E293B))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = lib,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = NeonCyan
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Technical Specs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Target: Android API ${analysisResult.targetSdk}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "Size: ${analysisResult.totalSizeMb} MB",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isScanning && analysisResult?.isValid == true) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.testTag("confirm_install_apk_btn"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF041E26)
                    )
                ) {
                    Text("Install into Sandbox", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
