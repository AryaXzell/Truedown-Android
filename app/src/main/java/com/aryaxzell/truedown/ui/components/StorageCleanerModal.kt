package com.aryaxzell.truedown.ui.components

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aryaxzell.truedown.R
import com.aryaxzell.truedown.domain.model.toHumanReadableSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun StorageCleanerModal(
    autoClearOnExit: Boolean,
    onToggleAutoClear: (Boolean) -> Unit,
    onCacheCleared: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cacheSizeBytes by remember { mutableStateOf(0L) }
    var freeStorageBytes by remember { mutableStateOf(0L) }
    var isCleaning by remember { mutableStateOf(false) }

    fun calculateSizes() {
        scope.launch(Dispatchers.IO) {
            val totalCache = getFolderSize(context.cacheDir) + (context.externalCacheDir?.let { getFolderSize(it) } ?: 0L)
            val freeBytes = try {
                val stat = StatFs(Environment.getDataDirectory().path)
                stat.availableBlocksLong * stat.blockSizeLong
            } catch (_: Exception) {
                0L
            }
            withContext(Dispatchers.Main) {
                cacheSizeBytes = totalCache
                freeStorageBytes = freeBytes
            }
        }
    }

    LaunchedEffect(Unit) {
        calculateSizes()
    }

    fun clearCache() {
        isCleaning = true
        scope.launch(Dispatchers.IO) {
            val initialSize = cacheSizeBytes
            clearFolder(context.cacheDir)
            context.externalCacheDir?.let { clearFolder(it) }

            val freedStr = if (initialSize > 0) initialSize.toHumanReadableSize() else "0 B"
            withContext(Dispatchers.Main) {
                isCleaning = false
                val message = "Cache media sebesar $freedStr berhasil dibersihkan!"
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                onCacheCleared(freedStr)
                onDismiss()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.testTag("storage_cleaner_modal"),
        icon = {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Pembersih Cache Media",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Bersihkan file cache pratinjau media dan berkas temporary untuk menghemat ruang memori HP Anda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Storage Stat Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ukuran Cache Saat Ini",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (cacheSizeBytes > 0) cacheSizeBytes.toHumanReadableSize() else "0 B",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sisa Memori HP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = if (freeStorageBytes > 0) freeStorageBytes.toHumanReadableSize() else "...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Auto Clear Toggle
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bersihkan Cache Otomatis",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Otomatis bersihkan cache media saat aplikasi ditutup",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = autoClearOnExit,
                            onCheckedChange = onToggleAutoClear
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { clearCache() },
                enabled = !isCleaning && cacheSizeBytes > 0,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bersihkan Cache", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.action_close), fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

private fun getFolderSize(file: File?): Long {
    if (file == null || !file.exists()) return 0L
    var size = 0L
    val children = file.listFiles() ?: return 0L
    for (child in children) {
        size += if (child.isDirectory) {
            getFolderSize(child)
        } else {
            child.length()
        }
    }
    return size
}

private fun clearFolder(file: File?) {
    if (file == null || !file.exists()) return
    val children = file.listFiles() ?: return
    for (child in children) {
        if (child.isDirectory) {
            clearFolder(child)
            child.delete()
        } else {
            child.delete()
        }
    }
}
