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
    LaunchedEffect(snackbarData) {
        val autoDismissMs = when (snackbarData.visuals.duration) {
            SnackbarDuration.Short -> 3000L
            SnackbarDuration.Long -> 6000L
            SnackbarDuration.Indefinite -> Long.MAX_VALUE
        }
        if (autoDismissMs < Long.MAX_VALUE) {
            delay(autoDismissMs)
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
