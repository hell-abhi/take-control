package com.akeshari.takecontrol.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.akeshari.takecontrol.ui.appdetail.AppDetailScreen
import com.akeshari.takecontrol.ui.dashboard.DashboardScreen
import com.akeshari.takecontrol.ui.matrix.PermissionMatrixScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val PERMISSION_MATRIX = "permission_matrix"
    const val APP_DETAIL = "app_detail/{packageName}"

    fun appDetail(packageName: String) = "app_detail/$packageName"
}

@Composable
fun TakeControlNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onViewAllApps = { navController.navigate(Routes.PERMISSION_MATRIX) },
                onAppClick = { packageName ->
                    navController.navigate(Routes.appDetail(packageName))
                }
            )
        }

        composable(Routes.PERMISSION_MATRIX) {
            PermissionMatrixScreen(
                onAppClick = { packageName ->
                    navController.navigate(Routes.appDetail(packageName))
                },
                onBack = { navController.popBackStack() }
            )
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
