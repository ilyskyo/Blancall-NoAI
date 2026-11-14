// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.common

import androidx.compose.animation.core.animateFloatAsState
import com.ilyskyo.blancall.ui.common.MotionFade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyskyo.blancall.algorithm.DateRange
import com.ilyskyo.blancall.algorithm.dayOfWeekAtRow
import com.ilyskyo.blancall.algorithm.HEATMAP_YEAR_MATRIX_MIN_DAYS
import com.ilyskyo.blancall.algorithm.heatmapCellMetrics
import com.ilyskyo.blancall.algorithm.heatmapGridForRange
import com.ilyskyo.blancall.algorithm.heatmapYearCellMetrics
import com.ilyskyo.blancall.algorithm.heatmapYearMatrix
import com.ilyskyo.blancall.ui.theme.SmoothRoundedCornerShape
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * 环形进度仪表盘：中心显示百分比，环形显示进度。
 * 用于统计页核心正确率的可视化。
 *
 * @param progress 0f..1f
 * @param label    中心副标题（如"总体正确率"）
 */
@Composable
fun GaugeProgress(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    // rememberSaveable 而不是 remember：这张表住在统计页 LazyColumn 的 item 里，滚出视口后子树会被
    // 回收 —— 用 remember 的话「已播」标记跟着丢，滚回来时又从 0 长出来，用户读成「数据刷新了」。
    var played by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { played = true }
    val animated by animateFloatAsState(
        targetValue = if (played) progress.coerceIn(0f, 1f) else 0f,
        animationSpec = MotionFade.number(MotionFade.chartReveal, 80),
        label = "gaugeProgress"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    // 整个 U 型弧的描边色：让未达到部分也能看清轮廓
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val progressColor = when {
        progress >= 0.8f -> MaterialTheme.colorScheme.primary
        progress >= 0.6f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Box(
        modifier = modifier
            .size(120.dp)
            .semantics { contentDescription = "$label ${(progress * 100).toInt()}%" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            // 整个 U 型弧的描边：先画外圈轮廓（含未达到部分）
            val outlineW = 1.5.dp.toPx()
            val innerStroke = stroke - outlineW * 2
            drawArc(
                color = outlineColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // 背景轨道：淡色填充（比描边窄一圈，露出描边轮廓）
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = innerStroke, cap = StrokeCap.Round)
            )
            // 进度（已移除 sweepGradient，纯色绘制）
            if (animated > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = 135f,
                    sweepAngle = 270f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = innerStroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = progressColor
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 每日练习次数柱状图。
 *
 * @param dailyCounts 最近 N 天的每日练习次数列表（下标 0 = 最早，末位 = 今天）
 */
@Composable
fun DailyTrendChart(
    dailyCounts: List<Int>,
    modifier: Modifier = Modifier
) {
    if (dailyCounts.isEmpty()) return
    val maxCount = (dailyCounts.maxOrNull() ?: 0).coerceAtLeast(1)
    // 柱色：同一蓝紫色相，按数值深浅渐变（今天最深，其余按占比），与学习日历图例同色系连续
    val barColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    // 读屏拿得到的替代文本：这张图是纯 Box/Canvas，没有它，屏幕阅读器在这里是一片空白。
    // 数量句一律走 plurals（英文 "1 day"/"2 days"、俄语 one/few/many 由系统选档）
    val trendDescription = pluralStringResource(
        com.ilyskyo.blancall.R.plurals.a11y_daily_trend,
        dailyCounts.size, dailyCounts.size, dailyCounts.sum(), maxCount
    )

    Column(modifier = modifier.semantics { contentDescription = trendDescription }) {
        // 柱状图区域
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            dailyCounts.forEachIndexed { index, count ->
                val fraction = count.toFloat() / maxCount
                val isToday = index == dailyCounts.lastIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                        .clip(SmoothRoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            barColor.copy(alpha = if (isToday) 0.85f else (0.30f + fraction * 0.5f))
                        )
                        .then(
                            if (isToday) Modifier.border(
                                1.dp, barColor.copy(alpha = 0.85f),
                                SmoothRoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            ) else Modifier
                        )
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        // 日期标签（首/中/尾）
        val total = dailyCounts.size
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val fmt = remember { DateTimeFormatter.ofPattern("M/d", Locale.getDefault()) }
            val today = LocalDate.now()
            Text(today.minusDays((total - 1).toLong()).format(fmt),
                style = MaterialTheme.typography.labelSmall, color = labelColor, fontSize = 9.sp)
            Text(stringResource(com.ilyskyo.blancall.R.string.flow_group_today), style = MaterialTheme.typography.labelSmall,
                color = if (dailyCounts.lastOrNull()?.let { it > 0 } == true) barColor else labelColor,
                fontSize = 9.sp)
        }
    }
}

/**
 * 日历热力图：展示 [range] 区间内每日学习强度。
 *
 * 两种形态，按区间长度自动切换 —— 一年档塞不进周列布局，硬塞就是一片噪点：
 * - 中短区间（≤ [HEATMAP_YEAR_MATRIX_MIN_DAYS] 天）：列 = 一周、行 = 星期几，
 *   由 [heatmapGridForRange] 按星期对齐铺出来；
 * - 一年档：行 = 月份、列 = 该月第几天（[heatmapYearMatrix]），每格能到 8dp，
 *   还能在行首标出月份。53 列的周列版在手机卡片里只有 4~5dp/格、行标签叠成一坨，
 *   2026-11-11 用户报的「近一年很丑」就是这个。
 *
 * @param range   要展示的闭区间
 * @param countOn 某天的练习次数；区间内没练过的日子由它返回 0
 */
@Composable
fun CalendarHeatmap(
    range: DateRange,
    countOn: (LocalDate) -> Int,
    modifier: Modifier = Modifier,
    /** 点按某一天：用于从日历下钻到当日练习记录；null = 纯展示 */
    onDayClick: ((LocalDate) -> Unit)? = null,
) {
    // 一周从哪天开始由**界面语言**决定（中/日/韩/俄从周一、英文从周日）。
    // 必须读 LocalConfiguration 而不是 Locale.getDefault()：低版本的 app 语言是按
    // createConfigurationContext 注入的，默认 Locale 根本不会跟着变。
    val locale = LocalConfiguration.current.locales.get(0)
    val primary = MaterialTheme.colorScheme.primary
    // 0 次的日子用中性灰，而不是主色的极浅版：用主色铺满整张图，
    // 「一年只练过 1 次」看上去像天天都练过（截图上就是一片淡紫），
    // 365 个有色块还糊成一团脏色 —— 这是「丑」的另一半原因。
    val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    // 色阶：同一主色相的连续渐变（浅→深），「少→多」平滑、无跳变
    val levels = listOf(
        primary.copy(alpha = 0.18f),
        primary.copy(alpha = 0.38f),
        primary.copy(alpha = 0.60f),
        primary.copy(alpha = 0.85f)
    )

    // 统计直接走一遍区间，而不是对某套布局的格子求和：两套布局补边的方式不同
    // （周列版按整周补、年矩阵按自然月补），但「区间内练了多少次」必须是一个数。
    var total = 0
    var maxCount = 1
    var cursor = range.start
    while (!cursor.isAfter(range.end)) {
        val c = countOn(cursor)
        total += c
        if (c > maxCount) maxCount = c
        cursor = cursor.plusDays(1)
    }
    // 用 lambda 而不是局部 fun：Compose 编译器插件对 composable 内的局部函数
    // 处理路径更绕，这里没必要冒险
    val colorFor: (Int) -> Color = { count ->
        if (count <= 0) {
            emptyColor
        } else {
            val idx = ((count.toFloat() / maxCount) * (levels.size - 1)).toInt()
                .coerceIn(0, levels.lastIndex)
            levels[idx]
        }
    }

    val isYearMatrix = range.days > HEATMAP_YEAR_MATRIX_MIN_DAYS
    // 同上：Canvas 里的格子对读屏完全不可见，结构（行 = 星期几 / 行 = 月份、列 = 一周 / 日）
    // 也要说清楚，否则用户只听到一句「少 多」
    val calendarDescription = if (isYearMatrix) {
        pluralStringResource(
            com.ilyskyo.blancall.R.plurals.a11y_calendar_year_heatmap,
            range.days, range.days, total
        )
    } else {
        pluralStringResource(
            com.ilyskyo.blancall.R.plurals.a11y_calendar_heatmap,
            range.days, range.days, total
        )
    }
    // 跨年区间必须带年份：只写「11月12 – 11月11」时，两端看起来是同一个月
    val crossYear = range.start.year != range.end.year
    val rangeFmt = remember(locale, crossYear) {
        DateTimeFormatter.ofPattern(if (crossYear) "yyyy/M/d" else "M/d", locale)
    }

    Column(modifier = modifier.semantics { contentDescription = calendarDescription }) {
        HeatmapLegend(emptyColor = emptyColor, levels = levels)
        Spacer(Modifier.height(6.dp))
        if (isYearMatrix) {
            YearMatrixHeatmap(
                range = range,
                countOn = countOn,
                locale = locale,
                colorFor = colorFor,
                onDayClick = onDayClick,
            )
        } else {
            WeekColumnHeatmap(
                range = range,
                countOn = countOn,
                locale = locale,
                colorFor = colorFor,
                onDayClick = onDayClick,
            )
        }
        Spacer(Modifier.height(4.dp))
        // 日期区间是第二层锚点：只写「近 N 周」的话，用户无从把某一格对到具体某一天
        Text(
            "${range.start.format(rangeFmt)} – ${range.end.format(rangeFmt)} · " +
                pluralStringResource(
                    com.ilyskyo.blancall.R.plurals.calendar_range_total, total, total
                ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
    }
}

/**
 * 图例：空 + 4 档强度。
 *
 * 空档必须一起画出来 —— 它现在是中性灰，与强度色不是同一条色带，
 * 不列进图例就没人知道「灰 = 没练」而不是「练得最少」。
 */
@Composable
private fun HeatmapLegend(emptyColor: Color, levels: List<Color>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(com.ilyskyo.blancall.R.string.heatmap_less), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        Spacer(Modifier.width(3.dp))
        (listOf(emptyColor) + levels).forEach { c ->
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(SmoothRoundedCornerShape(2.dp))
                    .background(c)
            )
            Spacer(Modifier.width(2.dp))
        }
        Text(stringResource(com.ilyskyo.blancall.R.string.more), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
    }
}

/**
 * 周列形态：列 = 一周、行 = 星期几（GitHub 布局）。
 * 格子尺寸由 [heatmapCellMetrics] 统一给出，短区间也不会被撑成巨型方块。
 */
@Composable
private fun WeekColumnHeatmap(
    range: DateRange,
    countOn: (LocalDate) -> Int,
    locale: Locale,
    colorFor: (Int) -> Color,
    onDayClick: ((LocalDate) -> Unit)?,
) {
    val firstDay = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    // 摆格逻辑在 algorithm/HeatmapGrid.kt（纯函数，配 JVM 断言）：行 = 星期几、列 = 一周，
    // 列数由区间长度决定。旧实现按 idx = 列*7 + 行 铺满且右端钉在今天，整张图每天错开一行。
    val grid = heatmapGridForRange(range, firstDay, countOn)
    val weeks = grid.size
    val rowLabels = remember(locale, firstDay) {
        (0 until 7).map { dayOfWeekAtRow(it, firstDay).getDisplayName(TextStyle.NARROW, locale) }
    }
    // 网格：单个 Canvas 一次绘制 weeks×7 个单元格，避免 84 个 Composable 节点。
    // 左边一列星期标签是这张图唯一的「哪一行是周几」锚点 —— 原先什么都没有，
    // 于是错位一周这件事在界面上完全看不出来。
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val labelW = 14.dp
        // 上下限与分档在纯函数里（JVM 单测钉住）；这里只负责把 dp 数值换成 Dp
        val metrics = remember(weeks, maxWidth) {
            heatmapCellMetrics(weeks, (maxWidth - labelW - 2.dp).value)
        }
        val cell = metrics.cell.dp
        val spacing = metrics.gap.dp
        val gridW = metrics.gridWidth(weeks).dp
        val gridH = metrics.gridHeight().dp
        // 圆角不得超过格子边长本身的三成：小格配 3dp 圆角会变成一串圆点
        val cornerRadius = minOf(3.dp, cell * 0.3f)
        // 行高装不下 8sp 的字时（格子被压到 7dp 以下）隔行标一次，
        // 否则七个标签会叠成一坨竖线 —— 原来的 8sp 字硬塞进 5dp 行高就是这样
        val labelStep = if (cell + spacing >= 11.dp) 1 else 2
        val stepPx = with(LocalDensity.current) { (cell + spacing).toPx() }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.width(labelW).height(gridH)) {
                rowLabels.forEachIndexed { row, label ->
                    if (row % labelStep != 0) return@forEachIndexed
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(y = (cell + spacing) * row)
                            .height(cell),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 8.sp,
                            // lineHeight 必须跟着字号一起降：labelSmall 自带 16sp 行高，
                            // 8sp 的字撑出 21dp 的盒子，塞进 8~9dp 的行里就是七行标签叠成一坨
                            lineHeight = 9.sp
                        )
                    }
                }
            }
            Spacer(Modifier.width(2.dp))
            Canvas(
                modifier = Modifier
                    .width(gridW)
                    .height(gridH)
                    .then(
                        if (onDayClick == null) Modifier
                        else Modifier.pointerInput(grid, stepPx, onDayClick) {
                            detectTapGestures { offset ->
                                val col = (offset.x / stepPx).toInt()
                                val row = (offset.y / stepPx).toInt()
                                grid.getOrNull(col)?.getOrNull(row)?.let { onDayClick(it.date) }
                            }
                        }
                    )
            ) {
                val sp = spacing.toPx()
                val side = cell.toPx()
                val cr = cornerRadius.toPx()
                for (w in 0 until weeks) {
                    for (d in 0 until 7) {
                        val count = grid[w][d]?.count
                        val color = count?.let { colorFor(it) } ?: Color.Transparent
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(w * (side + sp), d * (side + sp)),
                            size = Size(side, side),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cr, cr)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 一年档：行 = 月份（行首标月份）、列 = 该月第几天。
 *
 * 与周列版相比牺牲了「星期对齐」，但一年档真正要看的是**哪段时间密集**，
 * 而这件事用 31 × 13 的矩阵一眼就能读出来；53 列的周热力图在手机上反而是噪点。
 */
@Composable
private fun YearMatrixHeatmap(
    range: DateRange,
    countOn: (LocalDate) -> Int,
    locale: Locale,
    colorFor: (Int) -> Color,
    onDayClick: ((LocalDate) -> Unit)?,
) {
    val rows = heatmapYearMatrix(range, countOn)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // 月份标签列：中文「11月」、英文「Nov」、俄文「нояб.」都要塞得下，30dp 是 8sp 下的稳妥值
        val labelW = 30.dp
        val metrics = remember(rows.size, maxWidth) {
            heatmapYearCellMetrics((maxWidth - labelW - 2.dp).value)
        }
        val cell = metrics.cell.dp
        val spacing = metrics.gap.dp
        val gridW = metrics.gridWidth(31).dp
        val gridH = (cell + spacing) * rows.size - spacing
        val cornerRadius = minOf(2.dp, cell * 0.22f)
        val stepPx = with(LocalDensity.current) { (cell + spacing).toPx() }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.width(labelW).height(gridH)) {
                rows.forEachIndexed { index, row ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(y = (cell + spacing) * index)
                            .height(cell)
                            .fillMaxWidth()
                            .padding(end = 4.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            row.yearMonth.month.getDisplayName(TextStyle.SHORT, locale),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 8.sp,
                            lineHeight = 9.sp,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
            Spacer(Modifier.width(2.dp))
            Canvas(
                modifier = Modifier
                    .width(gridW)
                    .height(gridH)
                    .then(
                        if (onDayClick == null) Modifier
                        else Modifier.pointerInput(rows, stepPx, onDayClick) {
                            detectTapGestures { offset ->
                                val col = (offset.x / stepPx).toInt()
                                val row = (offset.y / stepPx).toInt()
                                rows.getOrNull(row)?.cells?.getOrNull(col)?.let { onDayClick(it.date) }
                            }
                        }
                    )
            ) {
                val sp = spacing.toPx()
                val side = cell.toPx()
                val cr = cornerRadius.toPx()
                rows.forEachIndexed { rowIndex, row ->
                    row.cells.forEachIndexed { colIndex, cellData ->
                        if (cellData != null) {
                            drawRoundRect(
                                color = colorFor(cellData.count),
                                topLeft = Offset(colIndex * (side + sp), rowIndex * (side + sp)),
                                size = Size(side, side),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cr, cr)
                            )
                        }
                    }
                }
            }
        }
    }
}
