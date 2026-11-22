package com.darkmode.scrollgridstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.darkmode.scrollgridstudio.ui.StudioScreen
import com.darkmode.scrollgridstudio.ui.theme.ScrollGridTheme
import com.darkmode.scrollgridstudio.ui.theme.ThemePreference

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences = remember {
                getSharedPreferences("scrollgrid_preferences", MODE_PRIVATE)
            }
            var themePreference by remember {
                mutableStateOf(ThemePreference.fromStorage(preferences.getString("theme_preference", "SYSTEM")))
            }
            val darkTheme = when (themePreference) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
            ScrollGridTheme(themePreference) {
                StudioScreen(
                    themePreference = themePreference,
                    onThemePreferenceChange = { selected ->
                        themePreference = selected
                        preferences.edit().putString("theme_preference", selected.name).apply()
                    }
                )
            }
        }
    }
}
