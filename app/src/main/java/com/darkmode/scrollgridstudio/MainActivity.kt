package com.darkmode.scrollgridstudio

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.*
import com.darkmode.scrollgridstudio.model.LayoutStyle
import com.darkmode.scrollgridstudio.model.ScrlTemplate
import com.darkmode.scrollgridstudio.model.StudioMode
import com.darkmode.scrollgridstudio.model.StudioState
import com.darkmode.scrollgridstudio.ui.HomeHubScreen
import com.darkmode.scrollgridstudio.ui.StudioScreen
import com.darkmode.scrollgridstudio.ui.theme.ScrollGridTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContent {
            ScrollGridTheme {
                ScrollGridApp()
            }
        }
    }
}

private sealed interface AppRoute {
    data object Home : AppRoute
    data class Editor(
        val initialState: StudioState,
        val draftId: String? = null,
        val initialTemplate: ScrlTemplate? = null
    ) : AppRoute
}

@Composable
private fun ScrollGridApp() {
    var route by remember { mutableStateOf<AppRoute>(AppRoute.Home) }

    Crossfade(targetState = route, label = "app_route") { current ->
        when (current) {
            is AppRoute.Home -> {
                HomeHubScreen(
                    onStartMode = { mode, layout ->
                        route = AppRoute.Editor(
                            initialState = StudioState(
                                mode = mode,
                                layout = layout ?: LayoutStyle.GRID_2X2,
                                carouselSlides = if (mode == StudioMode.CAROUSEL) 3 else 1
                            )
                        )
                    },
                    onStartScrlTemplate = { template ->
                        route = AppRoute.Editor(
                            initialState = StudioState(
                                mode = StudioMode.CAROUSEL,
                                carouselSlides = template.slides
                            ),
                            initialTemplate = template
                        )
                    },
                    onOpenDraft = { draftId, loadedState ->
                        route = AppRoute.Editor(
                            initialState = loadedState,
                            draftId = draftId
                        )
                    }
                )
            }
            is AppRoute.Editor -> {
                StudioScreen(
                    initialState = current.initialState,
                    initialDraftId = current.draftId,
                    initialScrlTemplate = current.initialTemplate,
                    onBackHome = { route = AppRoute.Home }
                )
            }
        }
    }
}
