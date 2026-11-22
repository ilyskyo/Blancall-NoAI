// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.settings

import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ilyskyo.blancall.R
import com.ilyskyo.blancall.algorithm.ReadingSpeed
import com.ilyskyo.blancall.data.repository.ReaderPrefs
import com.ilyskyo.blancall.data.repository.ReaderPrefsStore
import com.ilyskyo.blancall.ui.common.AnimatedOverlayPresence
import com.ilyskyo.blancall.ui.common.AppIcon
import com.ilyskyo.blancall.ui.common.AppIconKind
import com.ilyskyo.blancall.ui.common.BlancallAlertDialog
import com.ilyskyo.blancall.ui.common.GlassCard
import com.ilyskyo.blancall.ui.common.GlassDropdownMenu
import com.ilyskyo.blancall.ui.common.GlassMenuItem
import com.ilyskyo.blancall.ui.common.GlassSwitch
import com.ilyskyo.blancall.ui.common.Motion
import com.ilyskyo.blancall.ui.common.MotionFade
import com.ilyskyo.blancall.ui.common.SingleDateCalendar
import com.ilyskyo.blancall.ui.common.pressClick
import com.ilyskyo.blancall.ui.reader.ReaderFont
import com.ilyskyo.blancall.ui.reader.ReaderFonts
import com.ilyskyo.blancall.ui.reader.weightLabel
import com.ilyskyo.blancall.ui.theme.AppPrefs
import com.ilyskyo.blancall.ui.theme.Macaron
import com.ilyskyo.blancall.ui.theme.ReminderPrefs
import com.ilyskyo.blancall.ui.theme.SmoothRoundedCornerShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 设置页里「阅读」与「练习」两块的卡片。
 *
 * ## 为什么要有这两块（2026-11-21 用户）
 * 「排查一下这个软件里，还有哪些设置是只能在其他地方进行的 —— 在阅读模式里能设置的功能，
 * 设置页里也要能设置。」排查结果见 [ReadingDefaultsCard] / [PracticeSettingsCard]：
 * 阅读外观（字号 / 行距 / 字体 / 字重 / 布局 / 背景 / 遮挡 / 挡片色 / 阅读速度）此前**只**
 * 存在于阅读模式的设置面板，练习提示三项只存在于练习页的 ⋮ 菜单，手写与返回提醒只存在于
 * 作答现场。设置页作为「所有开关的总目录」必须能改到它们。
 *
 * ## 与阅读模式的关系：这里是默认值，不是副本
 * 阅读设置自 v7 起按文章存（[ReaderPrefsStore]），一篇文章改过就以那篇为准，
 * 没改过的用 [AppPrefs] 里的全局值当起点。所以本卡片写的是**全局默认值**，
 * 单篇存档不会被静默改写；要改已有存档，走卡片末尾那行「让所有文章改用这里的设置」
 * （显式、二次确认，且保留每篇自己的自定义遮挡配置）。
 */
@Composable
internal fun ReadingDefaultsCard() {
    val fontPx by AppPrefs.readingFontFlow.collectAsState()
    val lineHeight by AppPrefs.readingLineHeightFlow.collectAsState()
    val fontId by AppPrefs.readingFontIdFlow.collectAsState()
    val fontWeight by AppPrefs.readingFontWeightFlow.collectAsState()
    val layoutMode by AppPrefs.readingLayoutModeFlow.collectAsState()
    val bgMode by AppPrefs.readingBgModeFlow.collectAsState()
    val occlusionOn by AppPrefs.readingOcclusionEnabledFlow.collectAsState()
    val occlusionMode by AppPrefs.readingOcclusionModeFlow.collectAsState()
    val occlusionColor by AppPrefs.readingOcclusionColorFlow.collectAsState()
    val pdfMode by AppPrefs.pdfViewModeFlow.collectAsState()
    val customMaskId by AppPrefs.readingOcclusionCustomConfigIdFlow.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showFontDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showLayoutMenu by remember { mutableStateOf(false) }
    var showBgMenu by remember { mutableStateOf(false) }
    var showMaskMenu by remember { mutableStateOf(false) }
    var showPdfMenu by remember { mutableStateOf(false) }
    var confirmApplyAll by remember { mutableStateOf(false) }
    var speedVersion by remember { mutableIntStateOf(0) }

    val fontName = rememberReaderFontName(fontId)
    val layoutLabel = stringResource(
        if (layoutMode == 1) R.string.layout_chapter else R.string.layout_scroll
    )
    val bgLabel = stringResource(
        when (bgMode) {
            1 -> R.string.bg_cream
            2 -> R.string.bg_pure_white
            else -> R.string.bg_follow_theme
        }
    )
    // 只有真的配过自定义遮挡（configId 有效）才把「自定义」摆进选项：
    // 否则选了它也没有配置可用，等于给用户一个按了没反应的字。
    val maskOptions = buildList {
        add("short" to stringResource(R.string.mask_short))
        add("long" to stringResource(R.string.mask_long))
        add("mixed" to stringResource(R.string.mask_mixed))
        if (customMaskId >= 0L || occlusionMode == "custom") {
            add("custom" to stringResource(R.string.mask_custom_fallback))
        }
    }
    val maskLabel = maskOptions.firstOrNull { it.first == occlusionMode }?.second
        ?: stringResource(R.string.mask_long)
    val pdfLabel = stringResource(
        if (pdfMode == "image") R.string.pdf_view_image_mode else R.string.pdf_view_text_mode
    )

    val speedCpm = remember(speedVersion) {
        AppPrefs.getReadingSpeedCpm().takeIf { it > 0f } ?: ReadingSpeed.DefaultCpm
    }
    val speedSampleSeconds = remember(speedVersion) { AppPrefs.getReadingSpeedSampleSeconds() }

    GlassCard(
        backdrop = false,
        modifier = Modifier.fillMaxWidth(),
        shape = SmoothRoundedCornerShape(14.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            // ── 字号 / 行距：拖动只改本地草稿，松手才落盘 ──
            // 每帧写一次 SharedPreferences 会在拖动时把主线程塞满 apply 任务，
            // 真机上表现为滑杆发黏（与句子卡动画速度那条同源，见 AppPrefs 的 preview/flush）。
            SliderSettingRow(
                label = stringResource(R.string.font_size_value, fontPx.roundToInt()),
                value = fontPx,
                valueRange = 14f..36f,
                steps = 21,
                snap = { it.roundToInt().toFloat() },
                onSettled = { AppPrefs.readingFont = it },
            )
            SliderSettingRow(
                label = stringResource(
                    R.string.line_height_value,
                    ((lineHeight * 10).roundToInt() / 10f).toString(),
                ),
                value = lineHeight,
                valueRange = 1.4f..2.4f,
                steps = 9,
                snap = { (it * 10).roundToInt() / 10f },
                onSettled = { AppPrefs.readingLineHeight = it },
            )
            Spacer(Modifier.height(6.dp))

            SettingDivider()
            ValueSettingRow(
                title = stringResource(R.string.reading_font),
                value = fontName,
                onClick = { showFontDialog = true },
            )
            SettingDivider()
            ValueSettingRow(
                title = stringResource(R.string.reading_font_weight),
                value = weightLabel(fontWeight),
                onClick = { showWeightDialog = true },
            )

            SettingDivider()
            Box {
                ValueSettingRow(
                    title = stringResource(R.string.layout_section),
                    value = layoutLabel,
                    onClick = { showLayoutMenu = true },
                )
                GlassDropdownMenu(
                    expanded = showLayoutMenu,
                    onDismissRequest = { showLayoutMenu = false },
                ) {
                    listOf(
                        0 to stringResource(R.string.layout_scroll),
                        1 to stringResource(R.string.layout_chapter),
                    ).forEach { (mode, label) ->
                        MenuChoice(
                            label = label,
                            selected = layoutMode == mode,
                            onClick = {
                                showLayoutMenu = false
                                AppPrefs.readingLayoutMode = mode
                            },
                        )
                    }
                }
            }

            SettingDivider()
            Box {
                ValueSettingRow(
                    title = stringResource(R.string.background_section),
                    value = bgLabel,
                    onClick = { showBgMenu = true },
                )
                GlassDropdownMenu(
                    expanded = showBgMenu,
                    onDismissRequest = { showBgMenu = false },
                ) {
                    listOf(
                        0 to stringResource(R.string.bg_follow_theme),
                        1 to stringResource(R.string.bg_cream),
                        2 to stringResource(R.string.bg_pure_white),
                    ).forEach { (mode, label) ->
                        MenuChoice(
                            label = label,
                            selected = bgMode == mode,
                            onClick = {
                                showBgMenu = false
                                AppPrefs.readingBgMode = mode
                            },
                        )
                    }
                }
            }

            SettingDivider()
            SwitchSettingRow(
                title = stringResource(R.string.recitation_mask),
                desc = stringResource(R.string.settings_reading_mask_desc),
                checked = occlusionOn,
                onCheckedChange = { AppPrefs.readingOcclusionEnabled = it },
            )

            // ── 遮挡子项：只在开启时出现，与阅读模式面板的层级一致 ──
            AnimatedVisibility(
                visible = occlusionOn,
                enter = fadeIn(MotionFade.alpha(MotionFade.enter)) +
                    expandVertically(Motion.contentSize()),
                exit = fadeOut(MotionFade.alpha(MotionFade.exit)) +
                    shrinkVertically(Motion.contentSize()),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Box {
                        ValueSettingRow(
                            title = stringResource(R.string.settings_reading_mask_granularity),
                            value = maskLabel,
                            onClick = { showMaskMenu = true },
                        )
                        GlassDropdownMenu(
                            expanded = showMaskMenu,
                            onDismissRequest = { showMaskMenu = false },
                        ) {
                            maskOptions.forEach { (mode, label) ->
                                MenuChoice(
                                    label = label,
                                    selected = occlusionMode == mode,
                                    onClick = {
                                        showMaskMenu = false
                                        AppPrefs.readingOcclusionMode = mode
                                    },
                                )
                            }
                        }
                    }
                    MaskColorRow(
                        selected = occlusionColor,
                        onSelect = { AppPrefs.readingOcclusionColor = it },
                    )
                }
            }

            SettingDivider()
            Box {
                ValueSettingRow(
                    title = stringResource(R.string.settings_pdf_view),
                    value = pdfLabel,
                    onClick = { showPdfMenu = true },
                )
                GlassDropdownMenu(
                    expanded = showPdfMenu,
                    onDismissRequest = { showPdfMenu = false },
                ) {
                    listOf(
                        "text" to stringResource(R.string.pdf_view_text_mode),
                        "image" to stringResource(R.string.pdf_view_image_mode),
                    ).forEach { (mode, label) ->
                        MenuChoice(
                            label = label,
                            selected = pdfMode == mode,
                            onClick = {
                                showPdfMenu = false
                                AppPrefs.setPdfViewMode(mode)
                            },
                        )
                    }
                }
            }

            SettingDivider()
            // ── 阅读速度：剩余时间估算的依据 ──
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.reader_speed_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (speedSampleSeconds > 0L) {
                            stringResource(
                                R.string.reader_speed_basis_personal,
                                (speedSampleSeconds / 60).toInt(),
                            )
                        } else {
                            stringResource(R.string.reader_speed_basis_default)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    stringResource(R.string.reader_speed_value, speedCpm.roundToInt()),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                if (speedSampleSeconds > 0L) {
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = {
                            AppPrefs.resetReadingSpeed()
                            speedVersion++
                        },
                    ) {
                        Text(
                            stringResource(R.string.reader_speed_reset),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            SettingDivider()
            ValueSettingRow(
                title = stringResource(R.string.settings_reading_apply_all),
                desc = stringResource(R.string.settings_reading_apply_all_desc),
                value = "",
                onClick = { confirmApplyAll = true },
                showChevron = true,
            )

            Text(
                stringResource(R.string.settings_reading_scope_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp),
            )
        }
    }

    if (showFontDialog) {
        ReaderFontDialog(
            currentId = fontId,
            onPick = {
                AppPrefs.readingFontId = it
                // 换字体时收敛字重：不同字体的可用档位不同（霞鹜文楷 3 档、其余 2 档），
                // 不收敛会留下一个当前字体给不出的档位
                AppPrefs.readingFontWeight = ReaderFonts.snapWeight(
                    fontWeight,
                    ReaderFonts.weightMode(it),
                )
                showFontDialog = false
            },
            onDismiss = { showFontDialog = false },
        )
    }

    if (showWeightDialog) {
        val options = ReaderFonts.weightOptions(ReaderFonts.weightMode(fontId))
        BlancallAlertDialog(
            onDismissRequest = { showWeightDialog = false },
            title = { Text(stringResource(R.string.reading_font_weight)) },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { w ->
                        FilterChip(
                            selected = fontWeight == w,
                            onClick = {
                                AppPrefs.readingFontWeight = w
                                showWeightDialog = false
                            },
                            label = { Text(weightLabel(w)) },
                            shape = SmoothRoundedCornerShape(8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWeightDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // 应用到全部文章：默认值改完不落到已有存档上，用户在阅读界面里看不到变化 ——
    // 所以给一条显式出口，但必须二次确认（会覆盖每篇单独调过的字号/行距/背景）。
    AnimatedOverlayPresence(visible = confirmApplyAll) {
        BlancallAlertDialog(
            onDismissRequest = { confirmApplyAll = false },
            title = { Text(stringResource(R.string.settings_reading_apply_all_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reading_apply_all_confirm_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmApplyAll = false
                    val defaults = ReaderPrefs(
                        bgMode = bgMode,
                        fontId = fontId,
                        fontWeight = fontWeight,
                        fontPx = fontPx,
                        lineHeight = lineHeight,
                        layoutMode = layoutMode,
                        occlusionEnabled = occlusionOn,
                        occlusionMode = occlusionMode,
                        occlusionColor = occlusionColor,
                        // 自定义遮挡配置是「某一篇配了哪套」，不算外观默认值；
                        // 写进默认值会把所有文章的配置指向同一份（见 ReaderPrefsStore.applyToAll）
                        occlusionCustomConfigId = -1L,
                    )
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            ReaderPrefsStore.getInstance(context).applyToAll(defaults)
                        }
                        Toast.makeText(
                            context,
                            context.getString(R.string.settings_reading_applied),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }) { Text(stringResource(R.string.settings_reading_apply_all)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmApplyAll = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

/**
 * 练习块。
 *
 * 提示三项（字数 / 内容 / 标点）是全局偏好，此前只在练习页的 ⋮ 菜单里；
 * 遮挡回忆是句子卡的记忆方式，此前只在句子卡 ⋮ 菜单里；
 * 默认手写与「离开练习时提醒」此前只在作答现场出现一次 —— 三处都属于「别处才能改」。
 */
@Composable
internal fun PracticeSettingsCard() {
    val hintLength by AppPrefs.hintLengthEnabledFlow.collectAsState()
    val hintContent by AppPrefs.hintContentEnabledFlow.collectAsState()
    val hintPunctuation by AppPrefs.hintPunctuationEnabledFlow.collectAsState()
    val clozeOn by AppPrefs.sentenceClozeEnabledFlow.collectAsState()
    val clozeNew by AppPrefs.sentenceClozeNewEnabledFlow.collectAsState()
    val penDefault by AppPrefs.handwritingInputEnabledFlow.collectAsState()
    val backWarningDisabled by AppPrefs.practiceBackWarningDisabledFlow.collectAsState()

    GlassCard(
        backdrop = false,
        modifier = Modifier.fillMaxWidth(),
        shape = SmoothRoundedCornerShape(14.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            SwitchSettingRow(
                title = stringResource(R.string.hint_length_toggle),
                desc = stringResource(R.string.hint_length_desc),
                checked = hintLength,
                onCheckedChange = { AppPrefs.hintLengthEnabled = it },
            )
            SettingDivider()
            SwitchSettingRow(
                title = stringResource(R.string.hint_content_toggle),
                desc = stringResource(R.string.hint_content_desc),
                checked = hintContent,
                onCheckedChange = { AppPrefs.hintContentEnabled = it },
            )
            SettingDivider()
            SwitchSettingRow(
                title = stringResource(R.string.hint_punctuation_toggle),
                desc = stringResource(R.string.hint_punctuation_desc),
                checked = hintPunctuation,
                onCheckedChange = { AppPrefs.hintPunctuationEnabled = it },
            )

            SettingDivider()
            SwitchSettingRow(
                title = stringResource(R.string.sentence_cloze_toggle),
                desc = stringResource(
                    if (clozeOn) R.string.sentence_cloze_on_desc
                    else R.string.sentence_cloze_off_desc
                ),
                checked = clozeOn,
                onCheckedChange = { AppPrefs.sentenceClozeEnabled = it },
            )
            // 子项：总开关关着时禁用而不是隐藏 —— 藏起来会让「我开了怎么没变化」再次发生
            SwitchSettingRow(
                title = stringResource(R.string.sentence_cloze_new_toggle),
                desc = stringResource(R.string.sentence_cloze_new_desc),
                checked = clozeNew,
                enabled = clozeOn,
                onCheckedChange = { AppPrefs.sentenceClozeNewEnabled = it },
            )

            SettingDivider()
            SwitchSettingRow(
                title = stringResource(R.string.settings_handwriting_default),
                desc = stringResource(R.string.settings_handwriting_default_desc),
                checked = penDefault,
                onCheckedChange = { AppPrefs.handwritingInputEnabled = it },
            )

            SettingDivider()
            // 存的是「别再提醒我」，开关语义是「提醒我」，所以取反 —— 极性写在控件处，
            // 存储字段不改名（改了会让老用户的既有选择失效）
            SwitchSettingRow(
                title = stringResource(R.string.settings_practice_back_warn),
                desc = stringResource(R.string.settings_practice_back_warn_desc),
                checked = !backWarningDisabled,
                onCheckedChange = { AppPrefs.practiceBackWarningDisabled = !it },
            )
        }
    }
}

/**
 * 记忆计划里的「考试日期」与「今日计划卡显示目标」。
 *
 * 这两项原先只在首页的今日计划面板（[com.ilyskyo.blancall.ui.home.TodayPlanSettingsSheet]）里，
 * 而它们既不随时间变、也不跟当天任务走，属于「设置」而不是「今天的操作」。
 * 放在 [SettingsGroup.REVIEW] 卡片末尾：同一张卡里已经是「计划强度 + 每天学多久」。
 */
@Composable
internal fun PlanDateSettings() {
    val examEpochDay by ReminderPrefs.examEpochDayFlow.collectAsState()
    val showGoalOnCard by ReminderPrefs.showDailyGoalOnCardFlow.collectAsState()
    val today = remember { LocalDate.now() }
    var pickerOpen by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(today.plusMonths(1)) }

    SettingDivider()
    Surface(
        onClick = {
            pending = if (examEpochDay > 0L) LocalDate.ofEpochDay(examEpochDay) else today.plusMonths(1)
            pickerOpen = !pickerOpen
        },
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_exam_date),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.plan_settings_exam_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                if (examEpochDay > 0L) LocalDate.ofEpochDay(examEpochDay).toString()
                else stringResource(R.string.settings_exam_unset),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (examEpochDay > 0L) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            if (examEpochDay > 0L) {
                TextButton(onClick = { ReminderPrefs.examEpochDay = 0L }) {
                    Text(
                        stringResource(R.string.today_plan_clear_exam),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = pickerOpen,
        enter = fadeIn(MotionFade.alpha(MotionFade.enter)) +
            expandVertically(Motion.contentSize()),
        exit = fadeOut(MotionFade.alpha(MotionFade.exit)) +
            shrinkVertically(Motion.contentSize()),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            SingleDateCalendar(
                selected = pending,
                today = today,
                futureEnabled = true,
                onSelect = { pending = it },
            )
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { pickerOpen = false }) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    ReminderPrefs.examEpochDay = pending.toEpochDay()
                    pickerOpen = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            }
        }
    }

    SettingDivider()
    SwitchSettingRow(
        title = stringResource(R.string.plan_settings_show_on_card),
        desc = stringResource(R.string.plan_settings_show_on_card_hint),
        checked = showGoalOnCard,
        onCheckedChange = { ReminderPrefs.showDailyGoalOnCard = it },
    )
}

// ── 下列零件是设置页正文区的公共形状：标题 + 副行 + 行尾值/开关 ──
// 之所以不复用 SettingsScreen 里的 OptionRow：那个是「带选中标记的选项」，
// 这几种是「值 / 开关 / 滑杆」，混用会把选中态读成已选中。

@Composable
private fun SettingDivider() {
    HorizontalDivider(
        Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    )
}

@Composable
private fun SwitchSettingRow(
    title: String,
    desc: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )
            if (desc != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                    modifier = Modifier.padding(end = 16.dp),
                )
            }
        }
        GlassSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun ValueSettingRow(
    title: String,
    value: String,
    desc: String? = null,
    enabled: Boolean = true,
    showChevron: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f),
                )
                if (desc != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        desc,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (value.isNotEmpty()) {
                Text(
                    value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.38f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showChevron) {
                Spacer(Modifier.width(6.dp))
                AppIcon(
                    kind = AppIconKind.ChevronRight,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SliderSettingRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    snap: (Float) -> Float,
    onSettled: (Float) -> Unit,
) {
    var draft by remember { mutableFloatStateOf(snap(value)) }
    var dragging by remember { mutableStateOf(false) }
    // 外部变化（恢复备份 / 另一个入口改了）同步进来；拖动期间不抢用户的手指
    LaunchedEffect(value) {
        if (!dragging && abs(snap(value) - draft) > 0.0005f) draft = snap(value)
    }
    Column(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )
        Slider(
            value = draft,
            onValueChange = {
                dragging = true
                draft = it
            },
            onValueChangeFinished = {
                dragging = false
                val settled = snap(draft)
                draft = settled
                onSettled(settled)
            },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun MenuChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    GlassMenuItem(
        onClick = onClick,
        label = {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        trailing = {
            if (selected) {
                AppIcon(
                    kind = AppIconKind.Check,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
    )
}

/** 挡片颜色：与阅读模式面板同一套六色（[Macaron]），只画色球，不写颜色名。 */
@Composable
private fun MaskColorRow(selected: Int, onSelect: (Int) -> Unit) {
    // 标签独占一行：六个色球 + 标签挤同一行时，320dp 窄屏会被裁掉最后一颗
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
    ) {
        Text(
            stringResource(R.string.mask_color_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val colors = listOf(
                Macaron.review().fill, Macaron.continueP().fill, Macaron.info().fill,
                Macaron.warn().fill, Macaron.lavender().fill, Macaron.neutral().fill,
            )
            colors.forEachIndexed { index, color ->
                val chosen = selected == index
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (chosen) 2.dp else 1.dp,
                            color = if (chosen) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape,
                        )
                        .pressClick { onSelect(index) },
                )
            }
        }
    }
}

/**
 * 字体选择弹窗（预置 / 系统 / 导入三组）。
 *
 * 阅读模式里的同一件事用的是面板内嵌列表；这里空间只有一屏，所以搬进对话框。
 * **导入入口必须一起搬**：只给预置字体的话，用户在阅读模式里导入的字体在这里选不到，
 * 两处看到的「可选字体」就不是同一份。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReaderFontDialog(
    currentId: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sysFonts by produceState(initialValue = emptyList<ReaderFont>(), context) {
        value = withContext(Dispatchers.IO) { ReaderFonts.scanSystemFonts() }
    }
    var imported by remember { mutableStateOf(ReaderFonts.listImportedFonts(context)) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val displayName = runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            }
        }.getOrNull() ?: uri.lastPathSegment ?: "font.ttf"
        val importedFont = runCatching {
            context.contentResolver.openInputStream(uri)
                ?.use { ReaderFonts.importFont(context, displayName, it) }
        }.getOrNull()
        if (importedFont != null) {
            imported = ReaderFonts.listImportedFonts(context)
            onPick(importedFont.id)
        }
    }

    BlancallAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reading_font)) },
        text = {
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ReaderFonts.presets.forEach { font ->
                        FilterChip(
                            selected = currentId == font.id,
                            onClick = { onPick(font.id) },
                            label = { Text(readerFontName(font)) },
                            shape = SmoothRoundedCornerShape(8.dp),
                        )
                    }
                }
                if (imported.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    FontSectionLabel(
                        stringResource(R.string.imported_fonts_count, imported.size)
                    )
                    imported.forEach { font ->
                        FontChoiceRow(
                            name = font.name,
                            selected = currentId == font.id,
                            onClick = { onPick(font.id) },
                        )
                    }
                }
                if (sysFonts.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    FontSectionLabel(
                        stringResource(R.string.system_fonts_count, sysFonts.size)
                    )
                    sysFonts.forEach { font ->
                        FontChoiceRow(
                            name = font.name,
                            selected = currentId == font.id,
                            onClick = { onPick(font.id) },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(SmoothRoundedCornerShape(10.dp))
                        .pressClick {
                            importLauncher.launch(
                                arrayOf(
                                    "font/ttf",
                                    "font/otf",
                                    "application/x-font-ttf",
                                    "application/octet-stream",
                                )
                            )
                        }
                        .padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.import_font_action),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun FontSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FontChoiceRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(SmoothRoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                else Color.Transparent
            )
            .pressClick(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            AppIcon(
                kind = AppIconKind.Check,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** 当前字体名：预置取本地化名，系统/导入取文件名（与阅读模式面板口径一致）。 */
@Composable
private fun rememberReaderFontName(fontId: String): String {
    val preset = ReaderFonts.presets.firstOrNull { it.id == fontId }
    return if (preset != null) readerFontName(preset) else fontId.toFontFileName()
}

@Composable
private fun readerFontName(font: ReaderFont): String =
    if (font.nameRes != 0) stringResource(font.nameRes) else font.name

private fun String.toFontFileName(): String =
    substringAfterLast("imp:").substringAfterLast('/').substringBeforeLast('.')
