package ir.hesabdari.shop.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.hesabdari.shop.MainViewModel
import ir.hesabdari.shop.ui.components.LocalMoneyUnit
import ir.hesabdari.shop.ui.screens.DealDetailScreen
import ir.hesabdari.shop.ui.screens.DealFormScreen
import ir.hesabdari.shop.ui.screens.DealsScreen
import ir.hesabdari.shop.ui.screens.HomeScreen
import ir.hesabdari.shop.ui.screens.InvoiceScreen
import ir.hesabdari.shop.ui.screens.ReportsScreen
import ir.hesabdari.shop.ui.screens.SettingsScreen
import ir.hesabdari.shop.ui.theme.AppTheme

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "خانه", Icons.Rounded.Home),
    Deals("deals", "معاملات", Icons.Rounded.Receipt),
    Reports("reports", "بیلان", Icons.Rounded.BarChart),
    Settings("settings", "تنظیمات", Icons.Rounded.Settings),
}

private const val FORM = "form/{id}"
private const val DETAIL = "detail/{id}"
private const val INVOICE = "invoice/{ids}"

@Composable
fun AppRoot(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    AppTheme(settings.theme) {
        CompositionLocalProvider(LocalMoneyUnit provides settings.unit.label) {
            val nav = rememberNavController()
            val entry by nav.currentBackStackEntryAsState()
            val route = entry?.destination?.route
            val showBar = Tab.entries.any { it.route == route }

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showBar) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            Tab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = route == tab.route,
                                    onClick = {
                                        nav.navigate(tab.route) {
                                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(tab.icon, null) },
                                    label = { Text(tab.label) },
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                NavHost(
                    navController = nav,
                    startDestination = Tab.Home.route,
                    modifier = Modifier.padding(padding).consumeWindowInsets(padding),
                    enterTransition = { fadeIn(tween(180)) },
                    exitTransition = { fadeOut(tween(120)) },
                    popEnterTransition = { fadeIn(tween(180)) },
                    popExitTransition = { fadeOut(tween(120)) },
                ) {
                    composable(Tab.Home.route) {
                        HomeScreen(
                            vm,
                            onAdd = { nav.navigate("form/0") },
                            onOpenDeal = { nav.navigate("detail/$it") },
                            onSeeAll = { nav.navigate(Tab.Deals.route) { launchSingleTop = true } },
                            onOpenReport = { nav.navigate(Tab.Reports.route) { launchSingleTop = true } },
                        )
                    }
                    composable(Tab.Deals.route) {
                        DealsScreen(
                            vm,
                            onAdd = { nav.navigate("form/0") },
                            onOpenDeal = { nav.navigate("detail/$it") },
                            onInvoice = { ids -> nav.navigate("invoice/" + ids.joinToString(",")) },
                        )
                    }
                    composable(Tab.Reports.route) { ReportsScreen(vm) }
                    composable(Tab.Settings.route) { SettingsScreen(vm) }
                    composable(FORM, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                        val id = e.arguments?.getLong("id") ?: 0L
                        DealFormScreen(
                            vm, id,
                            onBack = { nav.popBackStack() },
                            onSaved = { savedId ->
                                if (id == 0L) {
                                    nav.navigate("detail/$savedId") { popUpTo(FORM) { inclusive = true } }
                                } else {
                                    nav.popBackStack()
                                }
                            },
                        )
                    }
                    composable(DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                        val id = e.arguments?.getLong("id") ?: 0L
                        DealDetailScreen(
                            vm, id,
                            onBack = { nav.popBackStack() },
                            onEdit = { nav.navigate("form/$it") },
                            onInvoice = { ids -> nav.navigate("invoice/" + ids.joinToString(",")) },
                        )
                    }
                    composable(INVOICE, arguments = listOf(navArgument("ids") { type = NavType.StringType })) { e ->
                        val ids = e.arguments?.getString("ids").orEmpty().split(",").mapNotNull { it.toLongOrNull() }
                        InvoiceScreen(vm, ids, onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
