package work.shreyaan.dwell

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B5E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF98F2DD),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF42628A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E3FF),
    onSecondaryContainer = Color(0xFF001B3C),
    tertiary = Color(0xFF9B4056),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9DF),
    onTertiaryContainer = Color(0xFF3F0017),
    background = Color(0xFFFAFCF8),
    onBackground = Color(0xFF181D1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF181D1A),
    surfaceVariant = Color(0xFFE0E9E3),
    onSurfaceVariant = Color(0xFF414942),
    outline = Color(0xFF717971),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7DDAC6),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005145),
    onPrimaryContainer = Color(0xFF98F2DD),
    secondary = Color(0xFFABC7F7),
    onSecondary = Color(0xFF113354),
    secondaryContainer = Color(0xFF294B70),
    onSecondaryContainer = Color(0xFFD5E3FF),
    tertiary = Color(0xFFFFB1C1),
    onTertiary = Color(0xFF5F112A),
    tertiaryContainer = Color(0xFF7D2940),
    onTertiaryContainer = Color(0xFFFFD9DF),
    background = Color(0xFF101513),
    onBackground = Color(0xFFE0E4DF),
    surface = Color(0xFF181D1A),
    onSurface = Color(0xFFE0E4DF),
    surfaceVariant = Color(0xFF414942),
    onSurfaceVariant = Color(0xFFC0C9C1),
    outline = Color(0xFF8A938B),
)

private val DwellShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

@Composable
fun DwellTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = colors.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        shapes = DwellShapes,
        content = content,
    )
}
