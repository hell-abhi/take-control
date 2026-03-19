package com.akeshari.takecontrol.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.akeshari.takecontrol.ui.alternatives.AlternativesScreen
import com.akeshari.takecontrol.ui.breach.BreachCheckScreen
import com.akeshari.takecontrol.ui.appdetail.AppDetailScreen
import com.akeshari.takecontrol.ui.dashboard.DashboardScreen
import com.akeshari.takecontrol.ui.matrix.PermissionMatrixScreen
import com.akeshari.takecontrol.ui.preinstall.PreInstallCheckScreen
import com.akeshari.takecontrol.ui.settings.SettingsScreen
import com.akeshari.takecontrol.ui.threats.ThreatsScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val PERMISSION_MATRIX = "permission_matrix?group={group}"
    const val PERMISSION_MATRIX_BASE = "permission_matrix"
    const val SETTINGS = "settings"
    const val APP_DETAIL = "app_detail/{packageName}"
    const val PRE_INSTALL = "pre_install?query={query}"
    const val PRE_INSTALL_BASE = "pre_install"

    fun preInstall(query: String? = null): String {
        return if (query != null) "pre_install?query=$query" else "pre_install"
    }
    const val ALTERNATIVES = "alternatives"
    const val BREACH_CHECK = "breach_check"
    const val THREATS = "threats?company={company}"
    const val THREATS_BASE = "threats"

    fun threats(company: String? = null): String {
        return if (company != null) "threats?company=$company" else "threats"
    }

    fun appDetail(packageName: String) = "app_detail/$packageName"
    fun permissionMatrix(group: String? = null): String {
        return if (group != null) "permission_matrix?group=$group" else "permission_matrix"
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// 3 core tabs only — everything else is accessible from Home's Tools section
val bottomNavItems = listOf(
    BottomNavItem(Routes.DASHBOARD, "Home", Icons.Filled.Shield, Icons.Outlined.Shield),
    BottomNavItem(Routes.THREATS_BASE, "Radar", Icons.Filled.Visibility, Icons.Outlined.Visibility),
    BottomNavItem(Routes.PERMISSION_MATRIX_BASE, "Apps", Icons.Filled.GridView, Icons.Outlined.GridView)
)

@Composable
fun TakeControlNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar on the 3 main tabs only
    val showBottomBar = currentRoute != null && (
        currentRoute == Routes.DASHBOARD ||
        currentRoute.startsWith(Routes.PERMISSION_MATRIX_BASE) ||
        currentRoute?.startsWith(Routes.THREATS_BASE) == true
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = when (item.route) {
                            Routes.DASHBOARD -> currentRoute == Routes.DASHBOARD
                            Routes.PERMISSION_MATRIX_BASE -> currentRoute?.startsWith(Routes.PERMISSION_MATRIX_BASE) == true
                            Routes.THREATS_BASE -> currentRoute?.startsWith(Routes.THREATS_BASE) == true
                            else -> false
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    item.label,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onViewAllApps = {
                        navController.navigate(Routes.permissionMatrix()) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onFixGroup = { groupName ->
                        navController.navigate(Routes.permissionMatrix(groupName)) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToRadar = { company ->
                        navController.navigate(Routes.threats(company)) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onNavigate = { route ->
                        navController.navigate(route)
                    },
                    onAppClick = { packageName ->
                        navController.navigate(Routes.appDetail(packageName))
                    }
                )
            }

            composable(
                route = Routes.PERMISSION_MATRIX,
                arguments = listOf(
                    navArgument("group") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val group = backStackEntry.arguments?.getString("group")
                PermissionMatrixScreen(
                    highlightGroup = group,
                    onAppClick = { packageName ->
                        navController.navigate(Routes.appDetail(packageName))
                    }
                )
            }

            composable(
                route = Routes.THREATS,
                arguments = listOf(
                    navArgument("company") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val company = backStackEntry.arguments?.getString("company")
                ThreatsScreen(
                    scrollToCompany = company,
                    onAppClick = { packageName ->
                        navController.navigate(Routes.appDetail(packageName))
                    }
                )
            }

            // Secondary screens (accessible from Home's Tools section)
            composable(Routes.BREACH_CHECK) {
                BreachCheckScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.ALTERNATIVES) {
                AlternativesScreen(
                    onBack = { navController.popBackStack() },
                    onLookup = { packageName ->
                        navController.navigate(Routes.preInstall(packageName))
                    }
                )
            }

            composable(
                route = Routes.PRE_INSTALL,
                arguments = listOf(
                    navArgument("query") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val query = backStackEntry.arguments?.getString("query")
                PreInstallCheckScreen(
                    onBack = { navController.popBackStack() },
                    initialQuery = query
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Routes.APP_DETAIL,
                arguments = listOf(navArgument("packageName") { type = NavType.StringType })
            ) { backStackEntry ->
                val packageName = backStackEntry.arguments?.getString("packageName") ?: return@composable
                AppDetailScreen(
                    packageName = packageName,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
