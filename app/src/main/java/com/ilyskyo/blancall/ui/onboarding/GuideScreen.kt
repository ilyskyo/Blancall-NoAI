// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ilyskyo.blancall.R
import com.ilyskyo.blancall.data.repository.ArticleRepository
import com.ilyskyo.blancall.ui.common.AppIcon
import com.ilyskyo.blancall.ui.common.AppIconKind
import com.ilyskyo.blancall.ui.common.BackButton
import com.ilyskyo.blancall.ui.common.GlassCard
import com.ilyskyo.blancall.ui.navigation.navigateToTab
import com.ilyskyo.blancall.ui.theme.AppPrefs
import com.ilyskyo.blancall.ui.theme.SmoothRoundedCornerShape

/**
 * 操作地图里的一行。
 *
 * @param route        目标路由；`{id}` 会在点击时替换成最近文章的 id
 * @param tab          目标是否根部 tab（走 navigateToTab，保持全应用一致的平级切换）
 * @param needsArticle 目标需要一篇文章作例子；无文章时改去导入页
 * @param coachKeys    点击时重新标为未看过的一次性指引，现场会再演一遍
 */
data class GuideAction(
    val icon: AppIconKind,
    val title: String,
    val desc: String,
    val route: String,
    val tab: Boolean = false,
    val needsArticle: Boolean = false,
    val coachKeys: List<String> = emptyList(),
)

/** 一组操作。AI 等版本独有条目由调用方以外部区块传入，共享文件里不出现版本字样。 */
data class GuideSection(
    val title: String,
    val actions: List<GuideAction>,
)

/**
 * 使用引导 —— 操作地图。
 *
 * 与「帮助与说明」的分工：帮助页负责「读」，这里负责「做」。
 * 每一条都落到真实页面，并把对应的一次性 CoachMark 重新标为未看过，
 * 让长按、拖动这类没有可见入口的操作在现场再演示一遍。
 */
@Composable
fun GuideScreen(
    navController: NavController,
    extraSections: List<GuideSection> = emptyList(),
) {
    val context = LocalContext.current
    val articleRepo = remember {
        ArticleRepository.getInstance(context.filesDir.resolve("articles.json").absolutePath)
    }
    val articles by articleRepo.allArticles.collectAsState(initial = emptyList())
    // 首次读盘完成前 articles 也是空的；不等它加载完会先闪一下「没有文章」的提示
    var articlesLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(articleRepo) {
        articleRepo.awaitLoaded()
        articlesLoaded = true
    }
    // 演示要有一个真实例子：取最近更新的文章，练完返回后内容也还是用户刚看过的
    val firstArticleId = articles.maxByOrNull { it.updatedAt }?.id

    val go: (GuideAction) -> Unit = { action ->
        action.coachKeys.forEach { AppPrefs.unmarkCoachSeen(it) }
        when {
            action.tab -> navController.navigateToTab(action.route)
            action.needsArticle -> {
                val id = firstArticleId
                if (id == null) navController.navigate("import")
                else navController.navigate(action.route.replace("{id}", id.toString()))
            }
            else -> navController.navigate(action.route)
        }
    }

    val sections = listOf(
        GuideSection(
            title = stringResource(R.string.guide_group_start),
            actions = listOf(
                GuideAction(
                    icon = AppIconKind.Add,
                    title = stringResource(R.string.guide_import),
                    desc = stringResource(R.string.guide_import_desc),
                    route = "import",
                ),
                GuideAction(
                    icon = AppIconKind.Edit,
                    title = stringResource(R.string.guide_first_practice),
                    desc = stringResource(R.string.guide_first_practice_desc),
                    route = "reader/{id}",
                    needsArticle = true,
                ),
                GuideAction(
                    icon = AppIconKind.Insights,
                    title = stringResource(R.string.guide_data),
                    desc = stringResource(R.string.guide_data_desc),
                    route = "overview",
                    tab = true,
                ),
            ),
        ),
        GuideSection(
            title = stringResource(R.string.guide_group_hidden),
            actions = listOf(
                GuideAction(
                    icon = AppIconKind.Home,
                    title = stringResource(R.string.guide_home_card),
                    desc = stringResource(R.string.guide_home_card_desc),
                    route = "home",
                    tab = true,
                    coachKeys = listOf("home_long_press_card", "home_add_card_plus"),
                ),
                GuideAction(
                    icon = AppIconKind.Articles,
                    title = stringResource(R.string.guide_list_select),
                    desc = stringResource(R.string.guide_list_select_desc),
                    route = "list",
                    tab = true,
                    coachKeys = listOf("list_long_press_multi"),
                ),
                GuideAction(
                    icon = AppIconKind.SwapHoriz,
                    title = stringResource(R.string.guide_practice_gestures),
                    desc = stringResource(R.string.guide_practice_gestures_desc),
                    route = "practice/{id}?guide=true",
                    needsArticle = true,
                    // 只清「左右拖标题」：页面里「提示菜单」那条是它的前置，
                    // 一起清掉会让后者不再自动出现（enabled 依赖前者已看过）
                    coachKeys = listOf("practice_title_swipe"),
                ),
                GuideAction(
                    icon = AppIconKind.TrackChanges,
                    title = stringResource(R.string.guide_custom_cloze),
                    desc = stringResource(R.string.guide_custom_cloze_desc),
                    route = "custom_cloze_list/{id}",
                    needsArticle = true,
                    coachKeys = listOf("cloze_long_press_menu", "cloze_edit_long_press"),
                ),
                GuideAction(
                    icon = AppIconKind.ViewAgenda,
                    title = stringResource(R.string.guide_mask),
                    desc = stringResource(R.string.guide_mask_desc),
                    route = "reader/{id}?read=true",
                    needsArticle = true,
                    coachKeys = listOf("mask_list_long_press", "mask_long_press_split"),
                ),
                GuideAction(
                    icon = AppIconKind.Celebrate,
                    title = stringResource(R.string.guide_sentence_card),
                    desc = stringResource(R.string.guide_sentence_card_desc),
                    route = "sentence_cards",
                    coachKeys = listOf("sentence_card_gesture"),
                ),
            ),
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onClick = { navController.popBackStack() })
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.guided_tour),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    stringResource(R.string.guide_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (articlesLoaded && firstArticleId == null) {
                    Surface(
                        shape = SmoothRoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(
                                kind = AppIconKind.School,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.guide_empty_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                sections.forEach { GuideGroup(it, go) }
                extraSections.forEach { GuideGroup(it, go) }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun GuideGroup(section: GuideSection, onAction: (GuideAction) -> Unit) {
    Column {
        Text(
            section.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        GlassCard(
            backdrop = false,
            modifier = Modifier.fillMaxWidth(),
            shape = SmoothRoundedCornerShape(14.dp)
        ) {
            section.actions.forEachIndexed { index, action ->
                if (index > 0) {
                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
                Surface(
                    onClick = { onAction(action) },
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .padding(horizontal = 16.dp, vertical = 13.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(
                            kind = action.icon,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                action.title,
                                style = MaterialTheme.typography.bodyLarge,
                                // bodyLarge 在主题里是楷体；可点击行按按钮处理，用系统黑体
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                action.desc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        AppIcon(
                            AppIconKind.ChevronRight,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
