package com.darkmode.scrollgridstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.darkmode.scrollgridstudio.ui.StudioScreen
import com.darkmode.scrollgridstudio.ui.theme.ScrollGridTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScrollGridTheme {
                StudioScreen()
            }
        }
    }
}
