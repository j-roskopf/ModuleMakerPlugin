package com.joetr.modulemaker.ui.theme

import androidx.compose.runtime.Composable
import org.jetbrains.jewel.bridge.theme.SwingBridgeTheme

@Composable
fun WidgetTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    SwingBridgeTheme {
        content()
    }
}
