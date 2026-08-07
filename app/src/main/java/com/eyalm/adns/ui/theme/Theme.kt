package com.eyalm.adns.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import com.eyalm.adns.data.appearance.AppearanceRepository
import com.eyalm.adns.data.appearance.ColorSchemePreference
import com.eyalm.adns.data.appearance.DarkModePreference
import com.eyalm.adns.data.appearance.resolveDarkTheme

@Composable
fun AdnsTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) {
        AppearanceRepository.getInstance(context.applicationContext)
    }
    val preferences by repository.state.collectAsState()
    val darkTheme = resolveDarkTheme(
        preference = preferences.darkMode,
        systemDark = isSystemInDarkTheme(),
    )
    val baseColorScheme = if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        preferences.colorScheme == ColorSchemePreference.SystemDynamic
    ) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (darkTheme) {
        AdnsDarkColorScheme
    } else {
        AdnsLightColorScheme
    }

    val colorScheme = if (preferences.darkMode == DarkModePreference.Oled) {
        baseColorScheme.copy(
            background = Color(0xFF000000),
            surface = Color(0xFF000000),
            surfaceDim = Color(0xFF000000),
            surfaceContainerLowest = Color(0xFF000000),
            surfaceContainerLow = Color(0xFF0A0B0E),
            surfaceContainer = Color(0xFF121418),
            surfaceContainerHigh = Color(0xFF1B1D22),
            surfaceContainerHighest = Color(0xFF24262C),
        )
    } else {
        baseColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
