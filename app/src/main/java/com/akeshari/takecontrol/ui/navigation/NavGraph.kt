package com.akeshari.takecontrol.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hierarchy
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
import com.akeshari.takecontrol.ui.settings.SettingsScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val PERMISSION_MATRIX = "permission_matrix?group={group}"
    const val PERMISSION_MATRIX_BASE = "permission_matrix"
    const val SETTINGS = "settings"
    const val APP_DETAIL = "app_detail/{packageName}"

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
    BottomNavItem(Routes.PERMISSION_MATRIX_BASE, "Dashboard", Icons.Filled.Shield, Icons.Outlined.Shield),
    BottomNavItem(Routes.PERMISSION_MATRIX_BASE, "Matrix", Icons.Filled.GridView, Icons.Outlined.GridView),
    BottomNavItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun TakeControlNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val topLevelRoutes = setOf(Routes.DASHBOARD, Routes.PERMISSION_MATRIX_BASE, Routes.SETTINGS)
    val showBottomBar = currentDestination?.route?.let { route ->
        topLevelRoutes.any { route.startsWith(it) }
    } ?: false

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val navItems = listOf(
                        Triple("Dashboard", Routes.DASHBOARD, Icons.Filled.Shield to Icons.Outlined.Shield),
                        Triple("Matrix", Routes.PERMISSION_MATRIX_BASE, Icons.Filled.GridView to Icons.Outlined.GridView),
                        Triple("Settings", Routes.SETTINGS, Icons.Filled.Settings to Icons.Outlined.Settings)
                    )
                    navItems.forEach { (label, route, icons) ->
                        val selected = currentDestination?.route?.startsWith(route) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) icons.first else icons.second,
                                    contentDescription = label
                                )
                            },
                            label = {
                                Text(
                                    label,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
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
                        navController.navigate(Routes.permissionMatrix(groupName))
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
