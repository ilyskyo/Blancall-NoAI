// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ilyskyo.blancall.R
import com.ilyskyo.blancall.ui.common.AppIcon
import com.ilyskyo.blancall.ui.common.AppIconKind
import com.ilyskyo.blancall.ui.common.GlassCard
import com.ilyskyo.blancall.ui.common.Motion
import com.ilyskyo.blancall.ui.common.MotionFade
import com.ilyskyo.blancall.ui.common.PressTier
import com.ilyskyo.blancall.ui.common.pressClick
import com.ilyskyo.blancall.ui.theme.AccentPresets
import com.ilyskyo.blancall.ui.theme.AppPrefs
import com.ilyskyo.blancall.ui.theme.AppearanceStore
import com.ilyskyo.blancall.ui.theme.ReminderPrefs
import com.ilyskyo.blancall.ui.theme.SmoothRoundedCornerShape
import com.ilyskyo.blancall.ui.theme.ThemeManager
import com.ilyskyo.blancall.ui.theme.ThemeMode
import com.ilyskyo.blancall.ui.theme.isBlancallDark

/**
 * 设置页收起态（1 级概览）的全部零件。
 *
 * 设计依据（详见 `_review/progressive-disclosure-research.html` 第 9 节，此处只列硬约束）：
 * 1. **入口粒度不合并**：保留原分区名当入口，不抽象成「常规/高级」这类上位词
 *    （AOSP：避免模糊标题；Blackmon CHI 2002：标签气味差时首击 41% vs 70%）。
 * 2. **副行只放两样东西**：该组含哪些设置项（标题，一行截断）+ 当前值（行尾）
 *    （AOSP：辅助文本列出被隐藏设置的标题，只一行；iOS Settings：行尾当前值 + chevron；
 *    Android：显示设置状态而不是描述设置）。
 * 3. **必须有搜索**：只要存在二级页，搜索就是必做项（Android 官方「层次较深的设置请加搜索」）。
 * 4. **chevron 用自绘图标**：全站禁排版符号当图标，这里走 AppIconKind.ChevronRight。
 *
 * 这一页**不引入任何新设置项**，它只是现有分区的索引 —— 展开态仍逐字保留原布局，
 * 两态之间只是「同一份内容换一种密度」，所以不存在两份真相源。
 */
internal enum class SettingsGroup(
    /** 分区名（与展开态的分区标题、二级页标题严格同一份资源，不许各写一份文案） */
    val titleRes: Int,
    /** 该组包含的设置项标题（概览副行 + 全局搜索索引共用这一份清单） */
    val itemTitleRes: List<Int>,
) {
    APPEARANCE(
        R.string.appearance,
        listOf(
            R.string.settings_item_theme_mode,
            R.string.beige_background,
            R.string.flat_background,
            R.string.ui_language,
            R.string.ui_font,
            R.string.ui_font_scale,
            R.string.glass_tint,
            R.string.nav_glass,
            R.string.haptic_strength,
            R.string.home_subtitle,
        ),
    ),
    THEME_COLOR(
        R.string.theme_color,
        listOf(R.string.theme_color_custom, R.string.material_you),
    ),
    FEATURES(
        R.string.features,
        listOf(
            R.string.indent_first_line,
            R.string.keep_wrong_ink,
            R.string.clear_saved_ink,
            R.string.sentence_card_anim_title,
        ),
    ),
    REVIEW(
        R.string.memory_plan,
        listOf(
            R.string.plan_name_sprint,
            R.string.plan_name_standard,
            R.string.plan_name_deep,
            R.string.daily_goal,
        ),
    ),
    REMINDER(
        R.string.study_reminder,
        listOf(
            R.string.reminder_enable,
            R.string.reminder_time,
            R.string.reminder_days,
            R.string.reminder_frequency,
            R.string.reminder_quiet_hours,
        ),
    ),
    HELP(
        R.string.help_section,
        listOf(R.string.guided_tour, R.string.coach_reset, R.string.help_about),
    ),
    CONTENT(
        R.string.content_management,
        listOf(R.string.article_tags),
    ),
    DATA(
        R.string.data_section,
        listOf(
            R.string.data_records_size_title,
            R.string.export_backup,
            R.string.import_backup,
            R.string.data_archive_action,
        ),
    ),
    EXTENSIONS(
        R.string.extensions,
        listOf(
            R.string.built_in_library,
            R.string.western_thoughts,
            R.string.library_gaokao,
            R.string.content_pack_title,
            R.string.interchange_title,
        ),
    ),
    // 关于页承载品牌信息与版本号（标准版另有隐私政策入口）；收起态没有可点的子项，
    // 所以 Std 侧只列隐私政策一项，版本号仍放在行尾（见 SettingsGroupValue）。
    ABOUT(R.string.about_section, listOf(R.string.privacy_policy)),
    DONATION(R.string.donation_heading, emptyList()),
}

/** 分区名。标准版没有 AI 分组，每个分区都有对应的字符串资源。 */
@Composable
internal fun settingsGroupTitle(group: SettingsGroup): String =
    stringResource(group.titleRes)

/** 概览行的副行：把该组含的设置项标题串成一行。`map` 是内联的，才能在内部调 stringResource。 */
@Composable
private fun settingsGroupItemLine(group: SettingsGroup): String =
    group.itemTitleRes.map { stringResource(it) }.joinToString(" · ")

/**
 * 行尾当前值（iOS 惯例：标题—值—chevron 同一行）。
 *
 * 只显示该组**最有用的一格状态**，不是全部：概览不是仪表盘，塞满值会让整页变成一片噪音。
 * 没有天然单值的组（帮助 / 内容管理 / 数据 / 赞赏）干脆不放 —— 空着比写「—」干净。
 */
@Composable
private fun SettingsGroupValue(group: SettingsGroup, versionName: String) {
    when (group) {
        SettingsGroup.APPEARANCE -> {
            val mode by ThemeManager.themeMode.collectAsState()
            SettingsValueText(
                when (mode) {
                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                }
            )
        }

        // 主题色没有可本地化的名字（AccentPresets.name 是不翻译的中文字面量），
        // 与其显示中文色名，不如直接给一颗当前色的小圆点 —— 颜色本身就是它的值。
        SettingsGroup.THEME_COLOR -> AccentDot()

        SettingsGroup.FEATURES -> {
            val indent by AppPrefs.autoIndentEnabledFlow.collectAsState()
            val ink by AppPrefs.keepInkEnabledFlow.collectAsState()
            val tapExit by AppPrefs.sentenceTapExitEnabledFlow.collectAsState()
            val on = (if (indent) 1 else 0) + (if (ink) 1 else 0) + (if (tapExit) 1 else 0)
            SettingsValueText(stringResource(R.string.settings_count_on, on))
        }

        SettingsGroup.REVIEW -> {
            val templateId by AppPrefs.reviewTemplateFlow.collectAsState()
            SettingsValueText(
                when (templateId) {
                    "sprint" -> stringResource(R.string.plan_name_sprint)
                    "deep" -> stringResource(R.string.plan_name_deep)
                    else -> stringResource(R.string.plan_name_standard)
                }
            )
        }

        SettingsGroup.REMINDER -> {
            val enabled by ReminderPrefs.enabledFlow.collectAsState()
            SettingsSwitchValue(enabled)
        }

        SettingsGroup.EXTENSIONS -> {
            val libraries by AppPrefs.builtInLibraryKeysFlow.collectAsState()
            SettingsValueText(stringResource(R.string.settings_count_on, libraries.size))
        }

        SettingsGroup.ABOUT -> SettingsValueText("v$versionName")

        SettingsGroup.HELP, SettingsGroup.CONTENT, SettingsGroup.DATA, SettingsGroup.DONATION -> Unit
    }
}

@Composable
private fun SettingsValueText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SettingsSwitchValue(on: Boolean) {
    SettingsValueText(
        stringResource(if (on) R.string.settings_value_on else R.string.settings_value_off)
    )
}

/** 当前强调色圆点：与「主题色」卡里那 6 颗色球同一套取色规则（自定义 > 动态取色 > 预设）。 */
@Composable
private fun AccentDot() {
    val appearance by AppearanceStore.state.collectAsState()
    val isDark = isBlancallDark()
    val color = when {
        appearance.materialYou -> MaterialTheme.colorScheme.primary
        appearance.useCustomAccent -> Color(appearance.customArgb)
        else -> AccentPresets.getOrNull(appearance.accentIndex)
            ?.let { if (isDark) it.primaryDark else it.primaryLight }
            ?: MaterialTheme.colorScheme.primary
    }
    Box(
        Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
    )
}

/**
 * 收起态的 1 级概览。
 *
 * 结构：一次性提示卡（可关）→ 搜索框 → 每个分区一行（同一张卡内连续排列）。
 * 入口放在**同一张卡**里而不是一组一张卡：每组一张卡的间距会让概览凭空多出上百 dp 高度，
 * 而它们本来就是同一个「目录」，分组容器的语义只该有一个。
 */
@Composable
internal fun SettingsCompactOverview(
    query: String,
    onQueryChange: (String) -> Unit,
    hintVisible: Boolean,
    onDismissHint: () -> Unit,
    onOpenGroup: (SettingsGroup) -> Unit,
) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: ""
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsLayoutHint(visible = hintVisible, onDismiss = onDismissHint)
        SettingsSearchField(query = query, onQueryChange = onQueryChange)
        if (query.isBlank()) {
            SettingsGroupIndexCard(versionName = versionName, onOpenGroup = onOpenGroup)
        } else {
            SettingsSearchResults(query = query.trim(), onOpenGroup = onOpenGroup)
        }
    }
}

/**
 * 「双击标题可切换布局」的一次性引导。
 *
 * 为什么必须存在：双击是**不可发现**的手势（NN/g 2016：隐藏交互让可发现性下降 >20%），
 * 而这一版把默认形态换成了用户没见过的紧凑布局。所以双击只能是捷径，
 * 真正的保底出口是标题栏那颗常驻胶囊；这条提示负责把捷径教出去，
 * 文案里同时点明胶囊，避免用户以为「只有双击这一条路」。
 */
@Composable
private fun SettingsLayoutHint(visible: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(MotionFade.alpha(MotionFade.enter)) +
            expandVertically(Motion.contentSize()),
        exit = fadeOut(MotionFade.alpha(MotionFade.exit)) +
            shrinkVertically(Motion.contentSize()),
    ) {
        GlassCard(
            backdrop = false,
            modifier = Modifier.fillMaxWidth(),
            shape = SmoothRoundedCornerShape(14.dp),
            // 8% 的强调色在浅色底上刚好是一层「提示」，压到深色底上会被吃掉：
            // 同一张卡在深色里要加倍才看得出边界（卡片本身还有 1dp 描边兜底）
            containerColor = MaterialTheme.colorScheme.primary.copy(
                alpha = if (isBlancallDark()) 0.16f else 0.08f
            ),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 10.dp, end = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                DoubleTapGlyph(
                    modifier = Modifier.padding(top = 3.dp).size(26.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_layout_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.settings_layout_hint_sub),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // 关闭用图标而不是文字按钮：卡片高度由文案行数决定，文字按钮竖排在
                // 三四行文案旁边永远对不齐重心；右上角的 × 是所有一次性提示的通用位置。
                val dismissLabel = stringResource(R.string.settings_hint_got_it)
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .pressClick(tier = PressTier.Icon, onClick = onDismiss)
                        // 语义必须挂在可点击的这一层：描述留在图标上会变成两个节点
                        // （图标一个、可点的一个），读屏聚焦到图标上双击不会触发关闭
                        .semantics(mergeDescendants = true) {
                            contentDescription = dismissLabel
                            role = Role.Button
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(
                        kind = AppIconKind.Close,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * 自绘「双击」字形：两组「落点 + 涟漪」并排，且左小右大 —— 读起来是「刚点了第二下」，
 * 而不是一个静止的靶心或信号波纹。物象取自水面涟漪（一次点按只会在触点周围荡开一圈）。
 * 全站图标自绘，不用 ★ ✓ 之类的排版符号。
 */
@Composable
private fun DoubleTapGlyph(modifier: Modifier, tint: Color) {
    Canvas(modifier) {
        val strokeWidth = size.minDimension * 0.075f
        val dotRadius = size.minDimension * 0.085f
        // (横向位置, 涟漪半径系数, 不透明度)：左侧是上一拍（已散开、更淡），右侧是这一拍
        listOf(
            Triple(0.27f, 0.19f, 0.30f),
            Triple(0.70f, 0.30f, 0.60f),
        ).forEach { (fx, radiusFactor, alpha) ->
            val cx = size.width * fx
            val cy = size.height * 0.60f
            drawCircle(color = tint, radius = dotRadius, center = Offset(cx, cy))
            drawCircle(
                color = tint.copy(alpha = alpha),
                radius = size.minDimension * radiusFactor,
                center = Offset(cx, cy),
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

@Composable
private fun SettingsSearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                stringResource(R.string.settings_search_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingIcon = {
            AppIcon(
                kind = AppIconKind.SearchHint,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = if (query.isEmpty()) null else {
            {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .pressClick(tier = PressTier.Icon) { onQueryChange("") },
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(
                        kind = AppIconKind.Close,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        singleLine = true,
        shape = SmoothRoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        // 描边走主题的 outlineVariant（与卡片同一条线），不用 M3 默认的 onSurface 深灰：
        // 默认值在这套浅色玻璃界面上像一根 1dp 黑框，比任何卡片都抢眼
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun SettingsGroupIndexCard(versionName: String, onOpenGroup: (SettingsGroup) -> Unit) {
    SettingsOverviewCard {
        SettingsGroup.entries.forEachIndexed { index, group ->
            SettingsOverviewRow(
                title = settingsGroupTitle(group),
                subtitle = settingsGroupItemLine(group),
                onClick = { onOpenGroup(group) },
                trailing = { SettingsGroupValue(group, versionName) },
            )
            if (index != SettingsGroup.entries.lastIndex) {
                SettingsOverviewDivider()
            }
        }
    }
}

/** 一条命中：分组名命中时 isTitle=true（副行不重复写组名），项名命中时副行写它属于哪一组。 */
private data class SettingsSearchHit(
    val group: SettingsGroup,
    val title: String,
    val isTitle: Boolean,
)

/**
 * 搜索结果。
 *
 * 逐项命中而不是只报「哪一组里有」：Android 官方要求深层设置提供搜索，AOSP 要求结果
 * 带标题 + 辅助文本 + 所在层级。这里用「项标题 + 所属分组」实现面包屑，
 * 不做「直接跳到该项」—— 二级页是整组渲染的，跳项要额外做锚点测量，收益不抵复杂度。
 */
@Composable
private fun SettingsSearchResults(query: String, onOpenGroup: (SettingsGroup) -> Unit) {
    val hits = mutableListOf<SettingsSearchHit>()
    SettingsGroup.entries.forEach { group ->
        val groupTitle = settingsGroupTitle(group)
        if (groupTitle.contains(query, ignoreCase = true)) {
            hits += SettingsSearchHit(group, groupTitle, isTitle = true)
        }
        group.itemTitleRes.forEach { itemRes ->
            val itemTitle = stringResource(itemRes)
            if (itemTitle.contains(query, ignoreCase = true)) {
                hits += SettingsSearchHit(group, itemTitle, isTitle = false)
            }
        }
    }

    if (hits.isEmpty()) {
        Text(
            stringResource(R.string.settings_search_empty),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
            textAlign = TextAlign.Center,
        )
        return
    }

    SettingsOverviewCard {
        hits.forEachIndexed { index, hit ->
            SettingsOverviewRow(
                title = hit.title,
                subtitle = if (hit.isTitle) settingsGroupItemLine(hit.group)
                else settingsGroupTitle(hit.group),
                onClick = { onOpenGroup(hit.group) },
                trailing = {},
                highlightSubtitle = !hit.isTitle,
            )
            if (index != hits.lastIndex) {
                SettingsOverviewDivider()
            }
        }
    }
}

@Composable
private fun SettingsOverviewCard(content: @Composable ColumnScope.() -> Unit) {
    GlassCard(
        backdrop = false,
        modifier = Modifier.fillMaxWidth(),
        shape = SmoothRoundedCornerShape(14.dp),
        content = content,
    )
}

@Composable
private fun SettingsOverviewDivider() {
    HorizontalDivider(
        Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    )
}

/**
 * 概览 / 搜索结果共用的一行：标题 + 一行副行 + 行尾值 + chevron。
 *
 * 整行用 `PressTier.Plain`（只压暗、不缩放）：一整行缩 3% 会同时离开左右边距，
 * 读起来像布局错位，这一点与全站其它整行条目保持一致。
 */
@Composable
private fun SettingsOverviewRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
    highlightSubtitle: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressClick(tier = PressTier.Plain, onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (highlightSubtitle) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        trailing()
        Spacer(Modifier.width(6.dp))
        AppIcon(
            kind = AppIconKind.ChevronRight,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
