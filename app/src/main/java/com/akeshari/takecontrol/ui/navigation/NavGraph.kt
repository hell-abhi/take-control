package com.akeshari.takecontrol.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.akeshari.takecontrol.ui.appdetail.AppDetailScreen
import com.akeshari.takecontrol.ui.applist.AppListScreen
import com.akeshari.takecontrol.ui.dashboard.DashboardScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val APP_LIST = "app_list"
    const val APP_DETAIL = "app_detail/{packageName}"

    fun appDetail(packageName: String) = "app_detail/$packageName"
}

@Composable
fun TakeControlNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onViewAllApps = { navController.navigate(Routes.APP_LIST) },
                onAppClick = { packageName ->
                    navController.navigate(Routes.appDetail(packageName))
                }
            )
        }

        composable(Routes.APP_LIST) {
            AppListScreen(
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
