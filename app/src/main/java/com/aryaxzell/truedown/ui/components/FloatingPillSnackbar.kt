package com.aryaxzell.truedown.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun FloatingPillSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { snackbarData ->
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value != SwipeToDismissBoxValue.Settled) {
                    snackbarData.dismiss()
                    true
                } else {
                    false
                }
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {},
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            FloatingPillSnackbar(snackbarData = snackbarData)
        }
    }
}

@Composable
fun FloatingPillSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(snackbarData) {
        val baseMs = when (snackbarData.visuals.duration) {
            SnackbarDuration.Short -> 4000
            SnackbarDuration.Long -> 10000
            SnackbarDuration.Indefinite -> Int.MAX_VALUE
        }
        if (baseMs < Int.MAX_VALUE) {
            val a11yManager = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
            val flags = if (snackbarData.visuals.actionLabel != null) {
                android.view.accessibility.AccessibilityManager.FLAG_CONTENT_CONTROLS or android.view.accessibility.AccessibilityManager.FLAG_CONTENT_TEXT
            } else {
                android.view.accessibility.AccessibilityManager.FLAG_CONTENT_TEXT
            }
            val timeout = a11yManager?.getRecommendedTimeoutMillis(baseMs, flags)?.toLong() ?: baseMs.toLong()
            delay(timeout)
            snackbarData.dismiss()
        }
    }

    val pillShape = RoundedCornerShape(32.dp)
    Snackbar(
        snackbarData = snackbarData,
        modifier = modifier
            .widthIn(max = 520.dp)
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                shape = pillShape
            ),
        shape = pillShape,
        containerColor = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        actionColor = MaterialTheme.colorScheme.inversePrimary,
        actionContentColor = MaterialTheme.colorScheme.inversePrimary,
        dismissActionContentColor = MaterialTheme.colorScheme.inverseOnSurface
    )
}
