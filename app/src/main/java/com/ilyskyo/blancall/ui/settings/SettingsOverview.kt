// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
 *    更不会用我们起的名字（「外貌」「颜色」要能搜到「外观」「主题色」）——
 *    匹配走「标题 + 同义词别名」，见 `SettingsSearch.kt`。
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
    /**
     * 检索别名（同义词，逗号分隔）。**不是界面文案**：它只喂搜索，
     * 让「外貌」「颜色」「夜间」这些用户嘴里的词也能落到对应的分区上。
     * 放在资源里而不是 Kotlin 字面量，是因为它必须跟着界面语言走 ——
     * 俄语用户在俄语界面里打的是俄语词。
     */
    val aliasRes: Int,
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
        R.string.settings_search_alias_appearance,
    ),
    THEME_COLOR(
        R.string.theme_color,
        listOf(R.string.theme_color_custom, R.string.material_you),
        R.string.settings_search_alias_theme_color,
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
        R.string.settings_search_alias_features,
    ),
    // 阅读外观曾经只能进「阅读模式」才改得到（2026-11-21 用户：设置页里也要能设置）。
    // 这里放的是**全局默认值**：单篇文章改过之后以那篇的存档为准，
    // 所以卡片末尾有一行「让所有文章改用这里的设置」。
    READING(
        R.string.reading_settings_title,
        listOf(
            R.string.settings_reading_font_size,
            R.string.settings_reading_line_height,
            R.string.reading_font,
            R.string.reading_font_weight,
            R.string.layout_section,
            R.string.background_section,
            R.string.recitation_mask,
            R.string.mask_color_label,
            R.string.settings_pdf_view,
            R.string.reader_speed_title,
            R.string.settings_reading_apply_all,
        ),
        R.string.settings_search_alias_reading,
    ),
    // 练习侧原先只有练习页里的 ⋮ 菜单与手写键盘上的切换，设置页完全看不到。
    PRACTICE(
        R.string.settings_practice_title,
        listOf(
            R.string.hint_length_toggle,
            R.string.hint_content_toggle,
            R.string.hint_punctuation_toggle,
            R.string.sentence_cloze_toggle,
            R.string.sentence_cloze_new_toggle,
            R.string.settings_handwriting_default,
            R.string.settings_practice_back_warn,
        ),
        R.string.settings_search_alias_practice,
    ),
    REVIEW(
        R.string.memory_plan,
        listOf(
            R.string.plan_name_sprint,
            R.string.plan_name_standard,
            R.string.plan_name_deep,
            R.string.daily_goal,
            R.string.settings_exam_date,
            R.string.plan_settings_show_on_card,
        ),
        R.string.settings_search_alias_plan,
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
        R.string.settings_search_alias_reminder,
    ),
    HELP(
        R.string.help_section,
        listOf(R.string.guided_tour, R.string.coach_reset, R.string.help_about),
        R.string.settings_search_alias_help,
    ),
    CONTENT(
        R.string.content_management,
        listOf(R.string.article_tags),
        R.string.settings_search_alias_content,
    ),
    DATA(
        R.string.data_section,
        listOf(
            R.string.data_records_size_title,
            R.string.export_backup,
            R.string.import_backup,
            R.string.data_archive_action,
        ),
        R.string.settings_search_alias_data,
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
        R.string.settings_search_alias_extensions,
    ),
    // 关于页承载品牌信息与版本号（标准版另有隐私政策入口）；收起态没有可点的子项，
    // 所以 Pro 侧不带副行 —— 空列表时行高自然收窄，不会留一条空白辅助文本。
    ABOUT(
        R.string.about_section,
        listOf(R.string.privacy_policy),
        R.string.settings_search_alias_about,
    ),
    DONATION(R.string.donation_heading, emptyList(), R.string.settings_search_alias_donation),
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
        listOf(SettingsGroup.FEATURES, SettingsGroup.READING, SettingsGroup.PRACTICE),
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
            val cloze by AppPrefs.sentenceClozeEnabledFlow.collectAsState()
            val pen by AppPrefs.handwritingInputEnabledFlow.collectAsState()
            val on = (if (indent) 1 else 0) + (if (ink) 1 else 0) + (if (tapExit) 1 else 0) +
                (if (cloze) 1 else 0) + (if (pen) 1 else 0)
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
    /** 每个入口行的登场规格；null = 不参与「依次展开」（不是布局切换的那一次）。 */
    revealDelay: (SettingsEntry) -> SettingsReveal? = { null },
) {
    val versionName = rememberVersionName()

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsLayoutHint(visible = hintVisible, onDismiss = onDismissHint)
        SettingsSearchField(query = query, onQueryChange = onQueryChange)
        if (query.isBlank()) {
            SettingsEntryIndexCard(
                versionName = versionName,
                onOpenEntry = onOpenEntry,
                revealDelay = revealDelay,
            )
        } else {
            SettingsSearchResults(query = query.trim(), onOpenEntry = onOpenEntry)
        }
    }
}

/**
 * 一次「逐块登场」的规格：等多久、要不要连高度一起长。
 *
 * `grow = false` 的场合（新树会恢复到一个非零滚动位置）：只做错峰淡入。
 * 高度一变，ScrollState 就会把恢复的偏移夹到当前内容高度，锚点会被夹到 0。
 */
internal data class SettingsReveal(val delayMs: Long, val grow: Boolean)

/**
 * 一个入口行的进出容器。
 *
 * 用户 2026-11-18：「每个设置项都丝滑地收起、展开」。
 * 行本身按最终尺寸测量一次，只有外层报出来的高度在长（`expandVertically` 裁切 + 报高度，
 * 不会把缩小的约束传给子项）—— 所以六行依次长出来时，每帧都不会重排行内文字。
 *
 * `delayMs == null` 时退回静态 Column：不是布局切换的场合（首次进页、转屏重建）不该放动画。
 */
@Composable
private fun SettingsEntryRowBlock(
    reveal: SettingsReveal?,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (reveal == null) {
        Column(Modifier.fillMaxWidth(), content = content)
        return
    }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(reveal) {
        if (reveal.delayMs > 0) delay(reveal.delayMs)
        shown = true
    }
    AnimatedVisibility(
        visible = shown,
        enter = (if (reveal.grow) {
            expandVertically(Motion.contentSize(), expandFrom = Alignment.Top)
        } else {
            EnterTransition.None
        }) + fadeIn(MotionFade.alpha(MotionFade.swapEnter)),
        exit = (if (reveal.grow) {
            shrinkVertically(Motion.contentSize(), shrinkTowards = Alignment.Top)
        } else {
            ExitTransition.None
        }) + fadeOut(MotionFade.alpha(MotionFade.exit)),
        label = "settingsEntryRow",
    ) {
        Column(Modifier.fillMaxWidth(), content = content)
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
 * 两种布局共用的同一枚（2026-11-21 用户：完整布局也要有顶部搜索栏）：
 * 紧凑布局里它筛入口，完整布局里它筛分区 —— 控件只认 query，不认布局。
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
internal fun SettingsSearchField(query: String, onQueryChange: (String) -> Unit) {
    val clearLabel = stringResource(R.string.clear_search)
    val keyboard = LocalSoftwareKeyboardController.current
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
                // 点了键盘上的「搜索」就收键盘：结果是本地即时筛的，没有第二个「执行」动作，
                // 键盘留着只会挡住半屏结果
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
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
                    // 命中区 44dp（搜索框 52dp 内可容纳的上限），原来 28dp 只有一半；
                    // 同时补名称与角色：AppIcon 未传 contentDescription 时，
                    // 读屏会把这个 × 念成一个没有名字的按钮。
                    .size(44.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = clearLabel
                        role = Role.Button
                    }
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

/**
 * 完整布局的搜索：命中哪些**分区**。
 *
 * 紧凑布局的搜索落点是入口（点一条进二级页），完整布局没有二级页 ——
 * 所有分区本来就摊在同一屏，所以这里的落点是「把没命中的分区藏起来」，
 * 让命中项留在原位（空间记忆不丢）。两处用的是同一套 [settingsDocScore]，
 * 同一句查询在两种布局里给出的命中范围必须一致，只是呈现方式不同。
 *
 * 分区名（含别名）命中即整块保留（用户搜「外貌」也要看到「外观」）；
 * 否则看它下面的设置项标题。只认 [SettingsGroup.itemTitleRes] 这份清单 ——
 * 它同时喂概览副行，不存在第二份索引。
 *
 * 这里**不认入口别名**：完整布局把分区当筛选单位，一句「备份」若把整个
 * 「内容与数据」（内容 / 数据 / 拓展）都留下来，筛完几乎等于没筛。
 */
@Composable
internal fun matchingGroups(query: String): Set<SettingsGroup> {
    if (query.isBlank()) return emptySet()
    val noise = searchNoiseWords()
    val hits = mutableSetOf<SettingsGroup>()
    SettingsGroup.entries.forEach { group ->
        var matched = settingsDocScore(query, settingsGroupDoc(group), noise) > 0
        if (!matched) {
            for (res in group.itemTitleRes) {
                if (settingsDocScore(query, settingsItemDoc(res), noise) > 0) {
                    matched = true
                    break
                }
            }
        }
        if (matched) hits += group
    }
    return hits
}

@Composable
private fun SettingsEntryIndexCard(
    versionName: String,
    onOpenEntry: (SettingsEntry) -> Unit,
    revealDelay: (SettingsEntry) -> SettingsReveal?,
) {
    SettingsOverviewCard {
        SettingsEntry.entries.forEachIndexed { index, entry ->
            SettingsEntryRowBlock(reveal = revealDelay(entry)) {
                SettingsOverviewRow(
                    title = settingsEntryTitle(entry),
                    subtitle = settingsEntryItemLine(entry),
                    onClick = { onOpenEntry(entry) },
                    trailing = { SettingsEntryValue(entry, versionName) },
                )
                // 分隔线跟着行一起长出来：它属于这一行，留在外面会在展开中途露出一条浮线
                if (index != SettingsEntry.entries.lastIndex) {
                    SettingsOverviewDivider()
                }
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
 * **同义词 + 模糊匹配**（用户 2026-11-14「没有模糊搜索功能」、2026-11-21
 * 「外貌/颜色/学习提醒 都要能搜到」）：评分见 [settingsDocScore]，
 * 标题与别名分开算，别名打一档折扣。
 *
 * **每个入口只出一条导航行**：入口名与它下面某个分区名同时命中时（「学习与提醒」vs
 * 「学习提醒」），两行点进去是同一页，并列出来只是噪音 —— 留分高的那个名字当标题。
 * 逐项命中（「提醒时间」「挡片颜色」）另算，它们是真正不同的设置项。
 *
 * 结果按分数降序，同分保持「入口顺序 → 项顺序」，避免每次输入顺序抖动。
 */
@Composable
private fun SettingsSearchResults(query: String, onOpenEntry: (SettingsEntry) -> Unit) {
    val noise = searchNoiseWords()
    val hits = mutableListOf<SettingsSearchHit>()

    SettingsEntry.entries.forEach { entry ->
        val entryTitle = settingsEntryTitle(entry)
        val entryScore = settingsDocScore(query, settingsEntryDoc(entry), noise, entryLevel = true)
        // 分区里分最高的那个：它比入口更具体，用户搜的词若正对着某个分区，就该停在那儿
        var groupBestScore = 0
        var groupBestTitle = entryTitle
        entry.groups.forEach { group ->
            val score = settingsDocScore(query, settingsGroupDoc(group), noise)
            if (score > groupBestScore) {
                groupBestScore = score
                groupBestTitle = settingsGroupTitle(group)
            }
        }
        val navScore = maxOf(entryScore, groupBestScore)
        if (navScore > 0) {
            val navTitle = if (entryScore >= groupBestScore) entryTitle else groupBestTitle
            hits += SettingsSearchHit(
                entry = entry,
                title = navTitle,
                // 标题就是入口名时，副行改成「包含哪几项」；否则写所属入口当面包屑
                subtitle = if (navTitle == entryTitle) {
                    settingsEntryItemLine(entry)
                } else {
                    entryTitle
                },
                score = navScore,
            )
        }
        entry.groups.forEach { group ->
            val groupTitle = settingsGroupTitle(group)
            group.itemTitleRes.forEach { itemRes ->
                val itemScore = settingsDocScore(query, settingsItemDoc(itemRes), noise)
                if (itemScore > 0) {
                    hits += SettingsSearchHit(
                        entry = entry,
                        title = stringResource(itemRes),
                        subtitle = groupTitle,
                        score = itemScore,
                    )
                }
            }
        }
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

