// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ilyskyo.blancall.ui.common.BottomNavBar
import com.ilyskyo.blancall.ui.home.HomeScreen
import com.ilyskyo.blancall.ui.import.ImportScreen
import com.ilyskyo.blancall.ui.list.ListScreen
import com.ilyskyo.blancall.ui.practice.PracticeScreen
import com.ilyskyo.blancall.ui.reader.ReaderScreen
import com.ilyskyo.blancall.ui.settings.SettingsScreen
import com.ilyskyo.blancall.ui.settings.HelpScreen
import com.ilyskyo.blancall.ui.statistics.OverviewScreen
import com.ilyskyo.blancall.ui.statistics.StatisticsScreen
import com.ilyskyo.blancall.ui.theme.AppPrefs
import com.ilyskyo.blancall.ui.viewmodel.BlancallMode
import com.ilyskyo.blancall.ui.viewmodel.SectionMode

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val predictiveBack by AppPrefs.predictiveBackFlow.collectAsState()
    // 底部导航栏开关（设置中可开：底部显示 首页/我的文章/数据）
    val bottomNavEnabled by AppPrefs.bottomNavEnabledFlow.collectAsState()
    // 当前路由（用于底部导航栏高亮）
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    // 底部导航栏仅在三个根页面显示（子页面如阅读/练习不显示）
    val rootRoutes = listOf("home", "list", "overview")
    // 按「根 tab 归属」匹配，覆盖各根页面的直接子路由（如 statistics/xxx），
    // 保证进入子页面时底部导航栏仍可见且高亮正确，用户可随时点其它 tab 跳出。
    val currentTab = when {
        currentRoute == "home" || currentRoute?.startsWith("home/") == true -> 0
        currentRoute == "list" || currentRoute?.startsWith("list/") == true -> 1
        currentRoute == "overview" || currentRoute?.startsWith("statistics/") == true -> 2
        else -> -1
    }
    // 首页在底部导航模式下不再显示左下角入口按钮（入口已迁移到导航栏）。
    // 注意：仅依赖开关状态，不依赖当前路由——否则进入设置等子页面时首页重组会导致入口按钮闪现
    val hideHomeEntry = bottomNavEnabled

    // tab 切换：回到根页面栈，避免子页面残留
    fun selectTab(index: Int) {
        val route = rootRoutes[index]
        if (route == currentRoute) return
        // 用目标 route 自身做 popUpTo（而非 startDestinationId）：
        // 1) 兼容「从子页面/统计页等任意深度返回根 tab」——把目标 tab 之上的所有页面弹出；
        // 2) 关键：去掉 restoreState，避免回到 startDestination(home) 时状态被恢复、
        //    NavHost 不再重组该 destination 导致的「点了首页无反应」问题。
        navController.navigate(route) {
            popUpTo(route) {
                saveState = true
                inclusive = false
            }
            launchSingleTop = true
            restoreState = false
        }
    }

    // ── 处理通知点击跳转 ──
    // 合并为单一消费：collect 即消费，避免双重导航；
    // Channel(CONFLATED) 保证冷启动期间投递的路由也能在收集开始后被收到，
    // 且消费后即从缓冲移除，配置变更（旋转）不会重复触发。
    LaunchedEffect(Unit) {
        NavigationDispatcher.pendingRoute.collect { route ->
            navController.navigate(route) {
                popUpTo("home") { inclusive = false }
            }
        }
    }

    // ══════════════════════════════════════════════════════
    // 导航过渡动画
    // 原则：enableEdgeToEdge() 已提供系统级预测性返回动画，
    // Compose 只做辅助淡出，避免双动画叠加导致返回变慢。
    // ══════════════════════════════════════════════════════

    // enterTransition：前进导航 → 新页面从右侧滑入
    val enterSlide: (AnimatedContentTransitionScope<*>.() -> EnterTransition) = if (predictiveBack) {
        { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) +
          fadeIn(tween(200)) }
    } else {
        { EnterTransition.None }
    }

    // exitTransition：前进导航 → 旧页面向右滑出（快速，不加缩放）
    val exitSlide: (AnimatedContentTransitionScope<*>.() -> ExitTransition) = if (predictiveBack) {
        { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(200)) }
    } else {
        { ExitTransition.None }
    }

    // popExitTransition：系统返回 / 预测性返回 → 当前页面退出
    // 只做快速淡出，因为 enableEdgeToEdge() 已处理系统级滑动+缩放动画
    val popExitSlide: (AnimatedContentTransitionScope<*>.() -> ExitTransition) = if (predictiveBack) {
        { fadeOut(tween(200)) }
    } else {
        { ExitTransition.None }
    }

    // popEnterTransition：系统返回 → 上一页从左侧滑入
    val popEnterSlide: (AnimatedContentTransitionScope<*>.() -> EnterTransition) = if (predictiveBack) {
        { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(250)) }
    } else {
        { EnterTransition.None }
    }

    // 底部导航模式：根页面 tab 切换无动画（点哪页立即显示哪页，如普通 App 底部导航）
    val noneTransition: (AnimatedContentTransitionScope<*>.() -> EnterTransition) = { EnterTransition.None }
    val noneExitTransition: (AnimatedContentTransitionScope<*>.() -> ExitTransition) = { ExitTransition.None }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable(
            "home",
            enterTransition = if (bottomNavEnabled) noneTransition else enterSlide,
            exitTransition = if (bottomNavEnabled) noneExitTransition else exitSlide,
            popExitTransition = if (bottomNavEnabled) noneExitTransition else popExitSlide,
            popEnterTransition = if (bottomNavEnabled) noneTransition else popEnterSlide
        ) {
            HomeScreen(navController, hideEntryButtons = hideHomeEntry)
        }

        composable(
            "import",
            enterTransition = enterSlide,
            exitTransition = exitSlide,
            popExitTransition = popExitSlide,
            popEnterTransition = popEnterSlide
        ) {
            ImportScreen(navController)
        }

        composable(
            "list",
            enterTransition = if (bottomNavEnabled) noneTransition else enterSlide,
            exitTransition = if (bottomNavEnabled) noneExitTransition else exitSlide,
            popExitTransition = if (bottomNavEnabled) noneExitTransition else popExitSlide,
            popEnterTransition = if (bottomNavEnabled) noneTransition else popEnterSlide
        ) {
            ListScreen(navController)
        }

        composable(
            route = "reader/{articleId}",
            arguments = listOf(
                navArgument("articleId") { type = NavType.LongType }
            ),
            enterTransition = enterSlide,
            exitTransition = exitSlide,
            popExitTransition = popExitSlide,
            popEnterTransition = popEnterSlide
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getLong("articleId") ?: 0L
            ReaderScreen(navController, articleId)
        }

        composable(
            route = "practice/{articleId}?mode={mode}&resume={resume}&sectionMode={sectionMode}",
            arguments = listOf(
                navArgument("articleId") { type = NavType.LongType },
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("resume") {
                    type = NavType.StringType
                    defaultValue = "false"
                },
                navArgument("sectionMode") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            ),
            enterTransition = enterSlide,
            exitTransition = exitSlide,
            popExitTransition = popExitSlide,
            popEnterTransition = popEnterSlide
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getLong("articleId") ?: 0L
            val modeStr = backStackEntry.arguments?.getString("mode") ?: ""
            val resumeStr = backStackEntry.arguments?.getString("resume") ?: "false"
            val sectionModeStr = backStackEntry.arguments?.getString("sectionMode") ?: ""
            val initialMode = try { BlancallMode.valueOf(modeStr) } catch (_: IllegalArgumentException) { null }
            val initialSectionMode = try { SectionMode.valueOf(sectionModeStr) } catch (_: IllegalArgumentException) { null }
            PracticeScreen(
                navController, listOf(articleId), initialMode,
                resume = resumeStr == "true",
                initialSectionMode = initialSectionMode
            )
        }

        composable(
            route = "cross/{articleIds}",
            arguments = listOf(
                navArgument("articleIds") { type = NavType.StringType }
            ),
            enterTransition = enterSlide,
            exitTransition = exitSlide,
            popExitTransition = popExitSlide,
            popEnterTransition = popEnterSlide
        ) { backStackEntry ->
            val idsStr = backStackEntry.arguments?.getString("articleIds") ?: ""
            val ids = idsStr.split(",").mapNotNull { it.toLongOrNull() }
            if (ids.isEmpty()) {
                // ids 为空时不进入练习（避免 PracticeScreen 永远"准备中"），返回首页
                LaunchedEffect(Unit) {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            } else {
                PracticeScreen(navController, ids)
            }
        }

        composable(
            "overview",
            enterTransition = if (bottomNavEnabled) noneTransition else enterSlide,
            exitTransition = if (bottomNavEnabled) noneExitTransition else exitSlide,
            popExitTransition = if (bottomNavEnabled) noneExitTransition else popExitSlide,
            popEnterTransition = if (bottomNavEnabled) noneTransition else popEnterSlide
        ) {
            OverviewScreen(navController)
        }

        composable(
            "settings",
            enterTransition = enterSlide,
            exitTransition = exitSlide,
            popExitTransition = popExitSlide,
            popEnterTransition = popEnterSlide
        ) {
            SettingsScreen(navController)
        }

        composable(
            "help",
            enterTransition = enterSlide,
            exitTransition = exitSlide,
            popExitTransition = popExitSlide,
            popEnterTransition = popEnterSlide
        ) {
            HelpScreen(navController)
        }

        composable(
            route = "statistics/{articleId}",
            arguments = listOf(
                navArgument("articleId") { type = NavType.LongType }
            ),
            enterTransition = enterSlide,
            exitTransition = exitSlide,
            popExitTransition = popExitSlide,
            popEnterTransition = popEnterSlide
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getLong("articleId") ?: 0L
            StatisticsScreen(navController, articleId)
        }
    }
        } // close weight Box

        // ── 底部导航栏（设置中开启后显示，仅在三个根页面） ──
        if (bottomNavEnabled && currentTab >= 0) {
            BottomNavBar(
                currentTab = currentTab,
                onSelect = { selectTab(it) }
            )
        }
    } // close Column
    } // close Box
}
