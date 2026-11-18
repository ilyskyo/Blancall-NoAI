// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
 * 1. **入口粒度不合并成「常规/高级」这类上位词**：入口名仍是用户能对上号的名词
 *    （AOSP：避免模糊标题；Blackmon CHI 2002：标签气味差时首击 41% vs 70%）。
 *    但**同族的低频分区必须并进同一个入口** —— 2026-11-14 用户原话：
 *    「『关于』和『支持』单独弄了一个，这肯定不行啊」。一级是目录，不是分区清单。
 * 2. **副行只放两样东西**：该入口含哪几项（最多 3 个标题 + 「另 N 项」）+ 行尾当前值
 *    （AOSP：辅助文本列出被隐藏设置的标题，只一行；iOS Settings：行尾当前值 + chevron）。
 *    全列出来等于把二级页复制到一级，用户原话是「不能像现在这样一下子全放在里面」。
 * 3. **必须有搜索，且必须能模糊命中**：只要存在二级页，搜索就是必做项（Android 官方
 *    「层次较深的设置请加搜索」）。中文用户不会逐字敲全名（「主色」要能搜到「主题色」），
 *    所以匹配走「包含 → 子序列」两级，见 [settingsMatchScore]。
 * 4. **chevron / 搜索图标全部自绘**：全站禁排版符号当图标，这里走 AppIconKind。
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
            R.string.handwriting_symbol_bar,
            R.string.handwriting_symbols,
            R.string.handwriting_auto_commit_weak,
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
    // 关于页承载品牌信息与版本号；标准版另有隐私政策入口，所以副行不是空的。
    // 版本号仍放在行尾（见 SettingsEntryValue）。
    ABOUT(R.string.about_section, listOf(R.string.privacy_policy)),
    DONATION(R.string.donation_heading, emptyList()),
}

/**
 * 概览的**一级入口**：若干分区合并成一个可点目录行。
 *
 * 为什么需要这一层、而不是直接把 [SettingsGroup] 当入口：
 * 2026-11-14 的实机反馈是「一级列了 11 行，『关于』和『支持』还各占一行」。
 * 分区是**二级页的排版单位**，没人规定它必须等于一级的入口数 ——
 * 一级只该回答「我想改哪一类东西」，同类低频分区（帮助 / 关于 / 赞赏）在这一层合并，
 * 进去之后仍是各自独立的分区卡片，展开态的 13 个分区也一个字没动。
 */
internal enum class SettingsEntry(
    /** 入口名（取字符串资源，标准版没有 AI 分组）。 */
    val titleRes: Int,
    /** 这个入口进去之后要渲染哪几个分区（顺序 = 展开态的阅读顺序） */
    val groups: List<SettingsGroup>,
) {
    APPEARANCE(
        R.string.appearance,
        listOf(SettingsGroup.APPEARANCE, SettingsGroup.THEME_COLOR),
    ),
    STUDY(
        R.string.settings_entry_study,
        listOf(SettingsGroup.REVIEW, SettingsGroup.REMINDER),
    ),
    READING(
        R.string.settings_entry_reading,
        listOf(SettingsGroup.FEATURES),
    ),
    CONTENT(
        R.string.settings_entry_content,
        listOf(SettingsGroup.CONTENT, SettingsGroup.DATA, SettingsGroup.EXTENSIONS),
    ),
    ABOUT(
        R.string.settings_entry_about,
        listOf(SettingsGroup.HELP, SettingsGroup.ABOUT, SettingsGroup.DONATION),
    ),
    ;

    /** 该入口含的全部设置项标题（副行与搜索索引共用）。 */
    val itemTitleRes: List<Int> get() = groups.flatMap { it.itemTitleRes }
}

/** 分属哪个一级入口。搜索命中一个分区后要靠它回到正确的二级页。 */
internal fun entryOf(group: SettingsGroup): SettingsEntry? =
    SettingsEntry.entries.firstOrNull { it.groups.contains(group) }

/** 入口名。 */
@Composable
internal fun settingsEntryTitle(entry: SettingsEntry): String =
    stringResource(entry.titleRes)

/** 分区名。 */
@Composable
internal fun settingsGroupTitle(group: SettingsGroup): String =
    stringResource(group.titleRes)

/** 副行的前半段：最多 3 个标题，用间隔点串起来。 */
@Composable
private fun entryHeadline(entry: SettingsEntry): String =
    entry.itemTitleRes.take(PREVIEW_ITEM_COUNT).map { stringResource(it) }.joinToString(" · ")

/** 副行的后半段：「另 N 项」。没有超出时为空串。 */
@Composable
private fun entryOverflow(entry: SettingsEntry): String {
    val extra = entry.itemTitleRes.size - PREVIEW_ITEM_COUNT
    return if (extra > 0) stringResource(R.string.settings_group_more, extra) else ""
}

/** 概览行的完整副行（前 3 项 + 另 N 项）。 */
@Composable
private fun settingsEntryItemLine(entry: SettingsEntry): String {
    val head = entryHeadline(entry)
    if (head.isEmpty()) return ""
    val tail = entryOverflow(entry)
    return if (tail.isEmpty()) head else "$head $tail"
}

/** 某一分区的完整副行（搜索命中的是分区名时用它）。 */
@Composable
private fun settingsGroupItemLine(group: SettingsGroup): String {
    val titles = group.itemTitleRes.map { stringResource(it) }
    if (titles.isEmpty()) return ""
    val head = titles.take(PREVIEW_ITEM_COUNT).joinToString(" · ")
    return if (titles.size > PREVIEW_ITEM_COUNT) {
        "$head " + stringResource(R.string.settings_group_more, titles.size - PREVIEW_ITEM_COUNT)
    } else {
        head
    }
}

/**
 * 行尾当前值（iOS 惯例：标题—值—chevron 同一行）。
 *
 * 只显示该入口**最有用的一格状态**，不是全部：概览不是仪表盘，塞满值会让整页变成一片噪音。
 * 没有天然单值的入口（阅读与练习 / 内容与数据）宁可不放 —— 空着比写「—」干净。
 */
@Composable
private fun SettingsEntryValue(entry: SettingsEntry, versionName: String) {
    when (entry) {
        SettingsEntry.APPEARANCE -> {
            val mode by ThemeManager.themeMode.collectAsState()
            SettingsValueText(
                when (mode) {
                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                }
            )
        }

        SettingsEntry.STUDY -> {
            val templateId by AppPrefs.reviewTemplateFlow.collectAsState()
            SettingsValueText(
                when (templateId) {
                    "sprint" -> stringResource(R.string.plan_name_sprint)
                    "deep" -> stringResource(R.string.plan_name_deep)
                    else -> stringResource(R.string.plan_name_standard)
                }
            )
        }

        SettingsEntry.READING -> {
            val indent by AppPrefs.autoIndentEnabledFlow.collectAsState()
            val ink by AppPrefs.keepInkEnabledFlow.collectAsState()
            val tapExit by AppPrefs.sentenceTapExitEnabledFlow.collectAsState()
            val on = (if (indent) 1 else 0) + (if (ink) 1 else 0) + (if (tapExit) 1 else 0)
            SettingsValueText(stringResource(R.string.settings_count_on, on))
        }

        SettingsEntry.CONTENT -> {
            val libraries by AppPrefs.builtInLibraryKeysFlow.collectAsState()
            SettingsValueText(stringResource(R.string.settings_count_on, libraries.size))
        }

        SettingsEntry.ABOUT -> SettingsValueText("v$versionName")
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
 * 结构：一次性提示卡（可关）→ 搜索框 → 每个入口一行（同一张卡内连续排列）。
 * 入口放在**同一张卡**里而不是一组一张卡：每组一张卡的间距会让概览凭空多出上百 dp 高度，
 * 而它们本来就是同一个「目录」，分组容器的语义只该有一个。
 */
@Composable
internal fun SettingsCompactOverview(
    query: String,
    onQueryChange: (String) -> Unit,
    hintVisible: Boolean,
    onDismissHint: () -> Unit,
    onOpenEntry: (SettingsEntry) -> Unit,
) {
    val versionName = rememberVersionName()

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsLayoutHint(visible = hintVisible, onDismiss = onDismissHint)
        SettingsSearchField(query = query, onQueryChange = onQueryChange)
        if (query.isBlank()) {
            SettingsEntryIndexCard(versionName = versionName, onOpenEntry = onOpenEntry)
        } else {
            SettingsSearchResults(query = query.trim(), onOpenEntry = onOpenEntry)
        }
    }
}

@Composable
private fun rememberVersionName(): String {
    val context = LocalContext.current
    return remember {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: ""
    }
}

/**
 * 「长按标题可切换布局」的一次性引导。
 *
 * 为什么必须存在：长按是**不可发现**的手势（NN/g 2016：隐藏交互让可发现性下降 >20%），
 * 而这一版把默认形态换成了用户没见过的紧凑布局。所以长按只能是捷径，
 * 真正的保底出口是读屏用户能直接触发的 onLongClick 动作；这条提示负责把手势教出去。
 *
 * 用户 2026-11-14：「长按过一次，再长按回来之后，这个提示就应该自动消失」——
 * 所以「学会了」的判据不是点了 ×，而是**真的切换过一次布局**（见 SettingsScreen 的
 * switchLayout）。× 只是给「我不用这个手势」的人一条退路。
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
            shape = SmoothRoundedCornerShape(16.dp),
            // 8% 的强调色在浅色底上刚好是一层「提示」，压到深色底上会被吃掉：
            // 同一张卡在深色里要加倍才看得出边界（卡片本身还有 1dp 描边兜底）
            containerColor = MaterialTheme.colorScheme.primary.copy(
                alpha = if (isBlancallDark()) 0.16f else 0.08f
            ),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 12.dp, end = 6.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LongPressGlyph(
                    modifier = Modifier.size(26.dp),
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
                //
                // 用户 2026-11-14：「叉叉不在水平正中央的水平线上，而是稍微偏上了一点」——
                // 旧实现把 × 挂在 Top 对齐的 Row 上，卡片两行文字把它顶成了「贴着第一行」。
                // 现在显式 CenterVertically：× 的圆心与整张卡的垂直中线重合（52dp 卡身时
                // 偏差 < 1dp，剩下那点是字形本身的视觉重心，不是布局）。
                val dismissLabel = stringResource(R.string.settings_hint_got_it)
                Box(
                    Modifier
                        .align(Alignment.CenterVertically)
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
 * 自绘「长按」字形：一个正在被按住的触点（实心）+ 两圈从它荡开的涟漪。
 *
 * 旧版是两个并排的双击涟漪 —— 文案早就改成长按了，图形还在讲「点两下」，
 * 手势与图示对不上等于没教。同心圆是「按住不放，波纹一圈圈散开」的物象，
 * 与点按（只荡开一圈、且立刻消失）能区分开。全站图标自绘，不用排版符号。
 */
@Composable
private fun LongPressGlyph(modifier: Modifier, tint: Color) {
    Canvas(modifier) {
        val strokeWidth = size.minDimension * 0.072f
        val dotRadius = size.minDimension * 0.115f
        val cx = size.width * 0.5f
        // 触点略低于中心：视觉重心落在提示卡的第一行文字上，而不是浮在正中
        val cy = size.height * 0.56f
        drawCircle(color = tint, radius = dotRadius, center = Offset(cx, cy))
        // 内圈更实（刚荡开）、外圈更淡（已经散远）：两圈同色不同 alpha，读得出先后
        drawCircle(
            color = tint.copy(alpha = 0.55f),
            radius = size.minDimension * 0.27f,
            center = Offset(cx, cy),
            style = Stroke(width = strokeWidth),
        )
        drawCircle(
            color = tint.copy(alpha = 0.24f),
            radius = size.minDimension * 0.42f,
            center = Offset(cx, cy),
            style = Stroke(width = strokeWidth * 0.8f),
        )
    }
}

/**
 * 搜索框。
 *
 * 不用 M3 `OutlinedTextField`：它的描边由库内部的 container 自己画，圆角只能改 shape，
 * 但**高度被库锁在 56dp、圆角在视觉上被文字基线拉平** —— 用户 2026-11-14 的原话是
 * 「边框没有按照规范使用超椭圆曲线，圆角太小了」。这里改成自绘：
 * - 轮廓 `SmoothRoundedCornerShape(22.dp)`（全站同一套超椭圆，n=3），52dp 高 ≈ 药丸，
 *   但四角是连续曲率而不是圆弧；
 * - 描边/底色/光标全部走主题，聚焦时描边过渡到主题色（[MotionFade.color]，不是硬切）；
 * - 整框可点（点空白也聚焦），焦点请求挂在 BasicTextField 上。
 */
@Composable
private fun SettingsSearchField(query: String, onQueryChange: (String) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    val shape = SmoothRoundedCornerShape(22.dp)
    val borderColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        animationSpec = MotionFade.color(MotionFade.enter),
        label = "settingsSearchBorder",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, borderColor, shape)
            // 点框内空白也聚焦：BasicTextField 只覆盖文字宽度，左侧图标与右侧留白会点不动
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusRequester.requestFocus() })
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            kind = AppIconKind.SearchHint,
            modifier = Modifier.size(18.dp),
            tint = if (focused) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                interactionSource = interaction,
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            stringResource(R.string.settings_search_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                },
            )
        }
        if (query.isNotEmpty()) {
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
    }
}

@Composable
private fun SettingsEntryIndexCard(
    versionName: String,
    onOpenEntry: (SettingsEntry) -> Unit,
) {
    SettingsOverviewCard {
        SettingsEntry.entries.forEachIndexed { index, entry ->
            SettingsOverviewRow(
                title = settingsEntryTitle(entry),
                subtitle = settingsEntryItemLine(entry),
                onClick = { onOpenEntry(entry) },
                trailing = { SettingsEntryValue(entry, versionName) },
            )
            if (index != SettingsEntry.entries.lastIndex) {
                SettingsOverviewDivider()
            }
        }
    }
}

/** 一条命中：入口名或某个设置项名，副行据此决定写「包含项」还是「所属入口」。 */
private data class SettingsSearchHit(
    val entry: SettingsEntry,
    val title: String,
    val subtitle: String,
    val score: Int,
)

/**
 * 搜索结果。
 *
 * 逐项命中而不是只报「哪一组里有」：Android 官方要求深层设置提供搜索，AOSP 要求结果
 * 带标题 + 辅助文本 + 所在层级。这里用「项标题 + 所属入口」实现面包屑，
 * 不做「直接跳到该项」—— 二级页是整组渲染的，跳项要额外做锚点测量，收益不抵复杂度。
 *
 * **模糊匹配**（用户 2026-11-14：「没有模糊搜索功能」）：
 * 纯 `contains` 只能命中连续子串，中文用户少打一个字（「主色」找「主题色」）就全空。
 * 评分见 [settingsMatchScore]：完全相等 > 前缀 > 包含 > 逐字子序列（允许跳字）。
 * 结果按分数降序，同分保持「入口顺序 → 项顺序」，避免每次输入顺序抖动。
 */
@Composable
private fun SettingsSearchResults(query: String, onOpenEntry: (SettingsEntry) -> Unit) {
    val hits = mutableListOf<SettingsSearchHit>()

    SettingsEntry.entries.forEach { entry ->
        val entryTitle = settingsEntryTitle(entry)
        val entryScore = settingsMatchScore(query, entryTitle)
        if (entryScore > 0) {
            hits += SettingsSearchHit(
                entry = entry,
                title = entryTitle,
                subtitle = settingsEntryItemLine(entry),
                score = entryScore + ENTRY_TITLE_BONUS,
            )
        }
        entry.groups.forEach { group ->
            val groupTitle = settingsGroupTitle(group)
            val groupScore = settingsMatchScore(query, groupTitle)
            // 分区名与入口名常常一字不差（「外观」），此时上面那条已经报过，
            // 再报一条会得到两行一模一样的「外观」，删掉低分的这条
            if (groupScore > 0 && groupTitle != entryTitle) {
                hits += SettingsSearchHit(
                    entry = entry,
                    title = groupTitle,
                    subtitle = entryTitle,
                    score = groupScore,
                )
            }
            group.itemTitleRes.forEach { itemRes ->
                val itemTitle = stringResource(itemRes)
                val itemScore = settingsMatchScore(query, itemTitle)
                if (itemScore > 0) {
                    hits += SettingsSearchHit(
                        entry = entry,
                        title = itemTitle,
                        subtitle = groupTitle,
                        score = itemScore,
                    )
                }
            }
        }
        // 合并入口（如「内容与数据」）的名字里可能含「数据」这种分区名之外的关键词，
        // 上面已经按 entryTitle 算过一次；这里不再重复扫 groups 的标题。
    }

    if (hits.isEmpty()) {
        Text(
            stringResource(R.string.settings_search_empty),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            textAlign = TextAlign.Center,
        )
        return
    }

    // sortedByDescending 是稳定排序：同分的两行仍按「入口顺序 + 项顺序」排，
    // 连打几个字不会看到结果在原地换位
    val ordered = hits.sortedByDescending { it.score }

    SettingsOverviewCard {
        ordered.forEachIndexed { index, hit ->
            SettingsOverviewRow(
                title = hit.title,
                subtitle = hit.subtitle,
                onClick = { onOpenEntry(hit.entry) },
                trailing = {},
                highlightSubtitle = true,
            )
            if (index != ordered.lastIndex) {
                SettingsOverviewDivider()
            }
        }
    }
}

/**
 * 模糊匹配打分。0 = 不匹配，分数越高越相关。
 *
 * 只做「逐字子序列」而不引入拼音库：中文输入法下用户敲的是汉字，
 * 「主色 → 主题色」这类跳字需求用子序列就能覆盖；上拼音首字母检索要背一整张拼音表，
 * 体积与维护成本都不值（真要做也该走系统 SearchManager，而不是在设置页塞一张表）。
 *
 * 多词查询用空格分隔，每个词都必须命中（AND 语义），取分数之和。
 */
internal fun settingsMatchScore(query: String, text: String): Int {
    val tokens = query.trim().lowercase().split(' ', '\u3000', '\t')
        .filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return 1
    val haystack = normalizeForSearch(text)
    if (haystack.isEmpty()) return 0
    var total = 0
    for (token in tokens) {
        val needle = normalizeForSearch(token)
        if (needle.isEmpty()) continue
        total += when {
            haystack == needle -> SCORE_EXACT
            haystack.startsWith(needle) -> SCORE_PREFIX
            haystack.contains(needle) -> SCORE_CONTAINS
            isSubsequence(needle, haystack) -> SCORE_SUBSEQUENCE
            else -> return 0
        }
    }
    return total
}

private fun normalizeForSearch(text: String): String =
    buildString(text.length) {
        for (ch in text) {
            if (ch.isWhitespace() || ch == '·' || ch == '・') continue
            append(ch.lowercaseChar())
        }
    }

/** `needle` 的每个字按顺序出现在 `haystack` 里（允许中间跳字）。 */
private fun isSubsequence(needle: String, haystack: String): Boolean {
    if (needle.isEmpty()) return true
    var i = 0
    for (ch in haystack) {
        if (ch == needle[i]) {
            i++
            if (i == needle.length) return true
        }
    }
    return false
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
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
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

// 副行里最多列几个标题：3 个刚好占满一行，再多就只剩省略号没有信息
private const val PREVIEW_ITEM_COUNT = 3

// 入口名命中的权重加成：搜「AI」时应当先看到「AI」这个入口，
// 而不是散落在它下面的七个子项
private const val ENTRY_TITLE_BONUS = 6

private const val SCORE_EXACT = 100
private const val SCORE_PREFIX = 80
private const val SCORE_CONTAINS = 60
private const val SCORE_SUBSEQUENCE = 30
