package com.pocketclone.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pocketclone.app.ui.screens.ArticleListScreen
import com.pocketclone.app.ui.screens.ReaderScreen
import com.pocketclone.app.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    data object ArticleList : Screen("articles")
    data object Reader : Screen("reader/{articleId}") {
        fun createRoute(articleId: Long) = "reader/$articleId"
    }
    data object Settings : Screen("settings")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.ArticleList.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.ArticleList.route) {
            ArticleListScreen(
                onArticleClick = { articleId ->
                    navController.navigate(Screen.Reader.createRoute(articleId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.Reader.route,
            arguments = listOf(
                navArgument("articleId") { type = NavType.LongType }
            )
        ) {
            ReaderScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
