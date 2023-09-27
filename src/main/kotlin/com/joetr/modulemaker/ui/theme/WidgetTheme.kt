package com.joetr.modulemaker.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.joetr.modulemaker.ui.theme.intellij.SwingColor
import kotlinx.serialization.json.JsonNull.content

private val DarkGreenColorPalette = darkColors(
    primary = blue200,
    primaryVariant = blue700,
    secondary = teal200,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    error = Color.Red
)

private val LightGreenColorPalette = lightColors(
    primary = blue500,
    primaryVariant = blue700,
    secondary = teal200,
    onPrimary = Color.White,
    onSurface = Color.Black
)

@Composable
fun WidgetTheme(
    darkTheme: Boolean = false,
    content: @Composable()
    () -> Unit
) {
    val colors = if (darkTheme) DarkGreenColorPalette else LightGreenColorPalette
    val swingColor = SwingColor()

    MaterialTheme(
        colors = colors.copy(
            background = swingColor.background,
            onBackground = swingColor.onBackground,
            surface = swingColor.background,
            onSurface = swingColor.onBackground
        ),
        typography = typography,
        shapes = shapes
    ) {
        Surface {
            content()
        }
    }
}
