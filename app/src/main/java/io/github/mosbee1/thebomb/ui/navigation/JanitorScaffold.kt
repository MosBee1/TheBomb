package io.github.mosbee1.thebomb.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.mosbee1.thebomb.R
import io.github.mosbee1.thebomb.ui.MainViewModel
import io.github.mosbee1.thebomb.ui.home.HomeScreen
import io.github.mosbee1.thebomb.ui.kept.KeptScreen
import io.github.mosbee1.thebomb.ui.settings.SettingsScreen
import io.github.mosbee1.thebomb.ui.stats.StatsScreen

/** Bottom-bar tabs. Deliberately four: Home, Defused, Stats, Settings. */
sealed class JanitorTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    data object Home : JanitorTab("home", R.string.tab_home, Icons.Rounded.CleaningServices)
    data object Kept : JanitorTab("kept", R.string.tab_kept, Icons.Rounded.PushPin)
    data object Stats : JanitorTab("stats", R.string.tab_stats, Icons.Rounded.Insights)
    data object Settings : JanitorTab("settings", R.string.tab_settings, Icons.Rounded.Settings)

    companion object {
        val all = listOf(Home, Kept, Stats, Settings)
    }
}

@Composable
fun JanitorScaffold(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                JanitorTab.all.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = JanitorTab.Home.route,
        ) {
            composable(JanitorTab.Home.route) { HomeScreen(mainViewModel) }
            composable(JanitorTab.Kept.route) { KeptScreen(mainViewModel) }
            composable(JanitorTab.Stats.route) { StatsScreen() }
            composable(JanitorTab.Settings.route) { SettingsScreen(mainViewModel) }
        }
    }
}
