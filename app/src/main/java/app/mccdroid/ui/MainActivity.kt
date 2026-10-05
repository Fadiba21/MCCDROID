@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.mccdroid.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import app.mccdroid.core.AppPrefs
import app.mccdroid.core.Notifier
import app.mccdroid.core.Profile
import app.mccdroid.ui.screens.AutomationScreen
import app.mccdroid.ui.screens.ConfigScreen
import app.mccdroid.ui.screens.ConsoleScreen
import app.mccdroid.ui.screens.FilesScreen
import app.mccdroid.ui.screens.HomeScreen
import app.mccdroid.ui.screens.ModsScreen
import app.mccdroid.ui.screens.MoreScreen
import app.mccdroid.ui.screens.NotificationsScreen
import app.mccdroid.ui.screens.SettingsScreen

enum class Sub { FILES, NOTIFY, MODS, SETTINGS }

/** Status navigasi yang dipakai bersama semua layar. */
class Nav {
    var tab by mutableIntStateOf(0)
    var sub by mutableStateOf<Sub?>(null)
    var profileId by mutableStateOf<String?>(null)

    fun pick(profiles: List<Profile>): Profile? = profiles.firstOrNull { it.id == profileId } ?: profiles.firstOrNull()
}

private data class Page(val tab: Int, val sub: Sub?)

class MainActivity : ComponentActivity() {
    private val nav = Nav()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                    .launch("android.permission.POST_NOTIFICATIONS")
            } catch (_: Exception) {
            }
        }
        setContent { McTheme { AppRoot(nav) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        val id = i?.getStringExtra(Notifier.EXTRA_PROFILE) ?: return
        nav.profileId = id
        nav.sub = null
        nav.tab = 1
    }
}

private data class TabDef(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val TABS = listOf(
    TabDef("Beranda", Icons.Rounded.Home),
    TabDef("Terminal", Icons.Rounded.Terminal),
    TabDef("Konfig", Icons.Rounded.Tune),
    TabDef("Otomasi", Icons.Rounded.Bolt),
    TabDef("Lainnya", Icons.Rounded.MoreHoriz),
)

@Composable
fun AppRoot(nav: Nav) {
    val view = LocalView.current
    SideEffect { view.keepScreenOn = AppPrefs.keepScreenOn }
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0

    BackHandler(enabled = nav.sub != null) { nav.sub = null }
    BackHandler(enabled = nav.sub == null && nav.tab != 0) { nav.tab = 0 }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (nav.sub == null && !imeVisible) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    TABS.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = nav.tab == i,
                            onClick = { nav.tab = i },
                            icon = { Icon(t.icon, t.label) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
    ) { pad ->
        Box(
            Modifier
                .padding(pad)
                .consumeWindowInsets(pad)
                .imePadding()
                .fillMaxSize(),
        ) {
            AnimatedContent(
                targetState = Page(nav.tab, nav.sub),
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 24 }) togetherWith fadeOut(tween(120))
                },
                label = "page",
            ) { page ->
                when (page.sub) {
                    Sub.FILES -> FilesScreen(nav)
                    Sub.NOTIFY -> NotificationsScreen(nav)
                    Sub.MODS -> ModsScreen(nav)
                    Sub.SETTINGS -> SettingsScreen(nav)
                    null -> when (page.tab) {
                        0 -> HomeScreen(nav)
                        1 -> ConsoleScreen(nav)
                        2 -> ConfigScreen(nav)
                        3 -> AutomationScreen(nav)
                        else -> MoreScreen(nav)
                    }
                }
            }
        }
    }
}
