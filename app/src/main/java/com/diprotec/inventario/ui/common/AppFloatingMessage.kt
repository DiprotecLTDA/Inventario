package com.diprotec.inventario.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.diprotec.inventario.ui.theme.BrandPrimary
import com.diprotec.inventario.ui.theme.Error
import com.diprotec.inventario.ui.theme.Success
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class FloatingMessageType {
    INFO,
    SUCCESS,
    ERROR
}

data class FloatingMessageEvent(
    val text: String,
    val type: FloatingMessageType = FloatingMessageType.INFO,
    val id: Long = System.nanoTime()
)

object AppFloatingMessage {

    private val _messages = MutableSharedFlow<FloatingMessageEvent>(
        extraBufferCapacity = 1
    )

    val messages = _messages.asSharedFlow()

    fun show(
        text: String,
        type: FloatingMessageType = FloatingMessageType.INFO
    ) {
        if (text.isBlank()) return

        _messages.tryEmit(
            FloatingMessageEvent(
                text = text,
                type = type
            )
        )
    }

    fun info(text: String) {
        show(text, FloatingMessageType.INFO)
    }

    fun success(text: String) {
        show(text, FloatingMessageType.SUCCESS)
    }

    fun error(text: String) {
        show(text, FloatingMessageType.ERROR)
    }
}

@Composable
fun BoxScope.AppFloatingMessageHost(
    durationMillis: Long = 2_000L
) {
    var currentMessage by remember {
        mutableStateOf<FloatingMessageEvent?>(null)
    }

    val currentDuration by rememberUpdatedState(durationMillis)

    LaunchedEffect(Unit) {
        AppFloatingMessage.messages.collect { event ->
            currentMessage = event

            delay(currentDuration)

            if (currentMessage?.id == event.id) {
                currentMessage = null
            }
        }
    }

    AnimatedVisibility(
        visible = currentMessage != null,
        enter = slideInVertically(
            initialOffsetY = { it }
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it }
        ) + fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .windowInsetsPadding(WindowInsets.ime)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        currentMessage?.let { message ->
            FloatingMessageContent(message)
        }
    }
}

@Composable
private fun FloatingMessageContent(
    message: FloatingMessageEvent
) {
    val backgroundColor: Color =
        when (message.type) {
            FloatingMessageType.INFO -> BrandPrimary
            FloatingMessageType.SUCCESS -> Success
            FloatingMessageType.ERROR -> Error
        }

    val icon =
        when (message.type) {
            FloatingMessageType.INFO -> Icons.Filled.Info
            FloatingMessageType.SUCCESS -> Icons.Filled.CheckCircle
            FloatingMessageType.ERROR -> Icons.Filled.Error
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 13.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White
            )

            Text(
                text = message.text,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .weight(1f),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
