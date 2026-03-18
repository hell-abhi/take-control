package com.akeshari.takecontrol.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
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
import com.akeshari.takecontrol.ui.appdetail.AppDetailScreen
import com.akeshari.takecontrol.ui.dashboard.DashboardScreen
import com.akeshari.takecontrol.ui.matrix.PermissionMatrixScreen
import com.akeshari.takecontrol.ui.preinstall.PreInstallCheckScreen
import com.akeshari.takecontrol.ui.settings.SettingsScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val PERMISSION_MATRIX = "permission_matrix?group={group}"
    const val PERMISSION_MATRIX_BASE = "permission_matrix"
    const val SETTINGS = "settings"
    const val APP_DETAIL = "app_detail/{packageName}"
    const val PRE_INSTALL = "pre_install"

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

val bottomNavItems = listOf(
    BottomNavItem(Routes.DASHBOARD, "Dashboard", Icons.Filled.Shield, Icons.Outlined.Shield),
    BottomNavItem(Routes.PERMISSION_MATRIX_BASE, "Matrix", Icons.Filled.GridView, Icons.Outlined.GridView),
    BottomNavItem(Routes.PRE_INSTALL, "Check", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun TakeControlNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute != null && (
        currentRoute == Routes.DASHBOARD ||
        currentRoute.startsWith(Routes.PERMISSION_MATRIX_BASE) ||
        currentRoute == Routes.SETTINGS ||
        currentRoute == Routes.PRE_INSTALL
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
                            Routes.SETTINGS -> currentRoute == Routes.SETTINGS
                            Routes.PRE_INSTALL -> currentRoute == Routes.PRE_INSTALL
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

            composable(Routes.PRE_INSTALL) {
                PreInstallCheckScreen()
            }

            composable(Routes.SETTINGS) {
                SettingsScreen()
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
