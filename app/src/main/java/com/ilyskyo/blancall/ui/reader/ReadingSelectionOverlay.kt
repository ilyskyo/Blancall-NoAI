// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ilyskyo.blancall.data.repository.ReadingMark
import com.ilyskyo.blancall.ui.common.pressClick
import com.ilyskyo.blancall.ui.theme.SmoothRoundedCornerShape
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 选区覆盖层：画高亮 + 两端手柄 + 浮在选区上方的操作条。
 *
 * 为什么单独一层而不是直接画在段落里：手柄得伸到行外（上方/下方），段落的绘制会被自身
 * 边界裁掉；操作条也要压在所有段落之上。这一层挂在正文根 Box 上，用
 * [ParagraphAnchorSource] 把段内坐标换算成根坐标。
 *
 * 交互约定（与系统文本选择一致的手感）：
 * - 拖任一端手柄 → 逐字扩/缩选区；拖过段尾就继续吃进下一段（翻页模式限制在当前节），
 *   落库时再按段拆成多条
 * - 两端手柄命中圈重叠时（选区只剩一个字）取离手指近的那一端，否则末端永远抓不到
 * - 点空白 → 收起选区（不穿透到正文，避免顺手把悬浮控件也切了）
 * - 点操作条 → 复制 / 划线（或取消划线）/ 批注
 */
@Composable
internal fun ReadingSelectionOverlay(
    selection: ReadingSelectionRange,
    /** 允许拖到的段范围：翻页模式 = 当前节，滚动模式 = 全文 */
    paragraphRange: IntRange,
    /** 段锚点（段号 → 活引用）：跨段拖选要靠它取相邻段的 layout 与坐标 */
    anchors: Map<Int, ParagraphAnchorSource>,
    accent: Color,
    isDark: Boolean,
    /** 与选区有交叠的已有记录（用于「编辑批注」的入口与提示） */
    coveringMark: ReadingMark?,
    /** 每一段都已被现有记录覆盖（用于把「划线」切换成「取消划线」） */
    fullyCovered: Boolean,
    onRangeChange: (ReadingSelectionRange) -> Unit,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onHighlight: () -> Unit,
    onUnhighlight: () -> Unit,
    onNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val handleRadius = with(density) { 9.dp.toPx() }
    val hitRadius = with(density) { 30.dp.toPx() }
    val barGap = with(density) { 10.dp.toPx() }
    val margin = with(density) { 12.dp.toPx() }
    // 手柄画在字形包围盒下方约 0.55r 处；判定时把这一截补回去。
    // 不补的话手柄中心正好落在下一行的行框里：横向拖手柄会被算成下行偏移
    //（实测：拖 "quick" 的末端直接跳到下一行的 "dog."）。
    val touchYBias = handleRadius * 0.55f + with(density) { 6.dp.toPx() }

    // 拖动过程中 selection 每帧变化：必须用 rememberUpdatedState 让 pointerInput(Unit)
    // 读到最新值 —— 否则 pointerInput 的 key 变化会重启手势协程，拖到一半就断。
    val selState by rememberUpdatedState(selection)
    val onRangeChangeState by rememberUpdatedState(onRangeChange)
    val onDismissState by rememberUpdatedState(onDismiss)
    val anchorsState by rememberUpdatedState(anchors)
    val rangeState by rememberUpdatedState(paragraphRange)

    val spans = readingSelectionSpans(selection) { p ->
        anchors[p]?.text?.length ?: 0
    }
    val rects = spans.flatMap { span ->
        val a = anchors[span.p]?.anchor() ?: return@flatMap emptyList()
        paragraphRects(a.layout, span.a, span.e).map {
            it.translate(a.topLeft.x, a.topLeft.y)
        }
    }
    if (rects.isEmpty()) return

    val first = rects.first()
    val last = rects.last()
    val startHandle = Offset(first.left, first.bottom + handleRadius * 0.55f)
    val endHandle = Offset(last.right, last.bottom + handleRadius * 0.55f)

    // 手柄与选区矩形也必须走 rememberUpdatedState：pointerInput(Unit) 的协程在首次事件
    // 之后不会随重组重启，闭包会一直拿着首帧坐标 —— 拖过一次后新手柄按旧位置判命中，
    // 结果是拖不动、点一下还被当成点空白把选区收掉（真机复现）。
    val startHandleState = rememberUpdatedState(startHandle)
    val endHandleState = rememberUpdatedState(endHandle)
    val rectsState = rememberUpdatedState(rects)

    // 操作条实测尺寸（首帧为 0 → 先用估算值，量到后再校正一格）
    var barSize by remember { mutableStateOf(IntSize.Zero) }
    val estBarW = with(density) { 210.dp.toPx() }
    val estBarH = with(density) { 42.dp.toPx() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val parentW = constraints.maxWidth.toFloat()
        val parentH = constraints.maxHeight.toFloat()
        val barW = if (barSize.width > 0) barSize.width.toFloat() else estBarW
        val barH = if (barSize.height > 0) barSize.height.toFloat() else estBarH

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        // 手柄优先：两个命中圈重叠时（选区只剩一个字）取更近的一端，
                        // 否则永远判成起点手柄，末端怎么拖都没反应。
                        // 命中点按绘制时的夹取规则算 —— 绘制会把贴边的手柄夹回屏内，
                        // 这里若用未夹取的原始坐标，屏边那颗看得到却抓不到。
                        val startRaw = startHandleState.value
                        val endRaw = endHandleState.value
                        val startHit = Offset(
                            x = startRaw.x.coerceIn(handleRadius, size.width - handleRadius),
                            y = startRaw.y.coerceIn(handleRadius + 2f, size.height - handleRadius - 2f)
                        )
                        val endHit = Offset(
                            x = endRaw.x.coerceIn(handleRadius, size.width - handleRadius),
                            y = endRaw.y.coerceIn(handleRadius + 2f, size.height - handleRadius - 2f)
                        )
                        val dStart = (down.position - startHit).getDistance()
                        val dEnd = (down.position - endHit).getDistance()
                        val which = when {
                            dStart > hitRadius && dEnd > hitRadius -> -1
                            dStart <= dEnd -> 0
                            else -> 1
                        }
                        if (which >= 0) {
                            down.consume()
                            // 各段锚点在一段手势里固定（选择期间正文不滚），按下时取一次
                            val candidates = rangeState.mapNotNull { p ->
                                anchorsState[p]?.takeIf { it.layout != null }
                            }
                            while (true) {
                                val ev = awaitPointerEvent()
                                val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                                ch.consume()
                                if (!ch.pressed) break
                                val point = Offset(ch.position.x, ch.position.y - touchYBias)
                                val src = nearestParagraph(candidates, point) ?: continue
                                val off = textOffsetAt(src, point)
                                val cur = selState
                                if (which == 0) {
                                    // 起点必须早于终点：拖过头就停住，不反转选区
                                    val beforeEnd = src.p < cur.endP ||
                                        (src.p == cur.endP && off < cur.endE)
                                    if (beforeEnd && (src.p != cur.startP || off != cur.startA)) {
                                        onRangeChangeState(
                                            ReadingSelectionRange(src.p, off, cur.endP, cur.endE)
                                        )
                                    }
                                } else {
                                    val newEnd = (off + 1).coerceAtMost(src.text.length)
                                    val afterStart = src.p > cur.startP ||
                                        (src.p == cur.startP && newEnd > cur.startA)
                                    if (afterStart && (src.p != cur.endP || newEnd != cur.endE)) {
                                        onRangeChangeState(
                                            ReadingSelectionRange(cur.startP, cur.startA, src.p, newEnd)
                                        )
                                    }
                                }
                            }
                        } else {
                            // 点空白 = 收起；点选区内部不动（留给操作条）
                            var moved = false
                            val start = down.position
                            while (true) {
                                val ev = awaitPointerEvent()
                                val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                                if ((ch.position - start).getDistance() > viewConfiguration.touchSlop) {
                                    moved = true
                                    // 选着的时候不许滚动正文：选区坐标是长按那一刻的快照，
                                    // 内容一滚，手柄和高亮就会停在原来的位置上（画在字外）。
                                    ch.consume()
                                }
                                if (!ch.pressed) {
                                    if (!moved && rectsState.value.none { it.contains(down.position) }) {
                                        // 整段吞掉这一下：收起选区时不该顺带切换悬浮控件
                                        down.consume()
                                        ch.consume()
                                        onDismissState()
                                    }
                                    break
                                }
                            }
                        }
                    }
                }
        ) {
            // ── 选区底色 ──
            rects.forEach { r ->
                drawRoundRect(
                    color = accent.copy(alpha = if (isDark) 0.34f else 0.22f),
                    topLeft = Offset(r.left, r.top),
                    size = Size(r.width, r.height),
                    cornerRadius = CornerRadius(r.height * 0.18f)
                )
            }
        }

        // ── 操作条：默认贴选区上方；上方放不下就翻到下方 ──
        // 放下方时再额外让开一个手柄的高度：操作条压在选区下缘会把末端手柄整个盖住，
        // 而手柄恰恰是"扩选"的唯一入口（实测过：盖住以后只能重新长按）。
        val barX = (first.left + last.right) / 2f - barW / 2f
        val aboveY = first.top - barH - barGap
        val belowY = last.bottom + barGap + handleRadius * 2.2f
        // 上方放不下才翻到下方；下方也放不下（选区贴着屏幕底部）就夹回可见区域，
        // 宁可稍微压住一点正文，也不能把三个操作按钮推到屏幕外
        val barY = (if (aboveY >= margin) aboveY else belowY)
            .coerceIn(margin, max(margin, parentH - barH - margin))
        SelectionActionBar(
            accent = accent,
            isDark = isDark,
            fullyCovered = fullyCovered,
            hasNote = coveringMark?.hasNote == true,
            onCopy = onCopy,
            onHighlight = onHighlight,
            onUnhighlight = onUnhighlight,
            onNote = onNote,
            modifier = Modifier
                .onSizeChanged { barSize = it }
                .offset {
                    val maxX = max(margin, parentW - barW - margin)
                    IntOffset(barX.coerceIn(margin, maxX).roundToInt(), barY.roundToInt())
                }
        )

        // ── 两端手柄：画在操作条之上（自绘：细杆 + 圆点）──
        // 单独一层且不带 pointerInput：既不会被操作条盖住，也不额外拦截触摸
        // （命中判定仍在下面那层 Canvas 的手势里，用的还是同一组坐标）。
        Canvas(Modifier.fillMaxSize()) {
            listOf(startHandle, endHandle).forEach { h ->
                val hy = h.y.coerceIn(handleRadius + 2f, size.height - handleRadius - 2f)
                val hx = h.x.coerceIn(handleRadius, size.width - handleRadius)
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(hx - 1.2f, hy - handleRadius * 1.9f),
                    size = Size(2.4f, handleRadius * 1.6f),
                    cornerRadius = CornerRadius(1.2f)
                )
                drawCircle(color = accent, radius = handleRadius, center = Offset(hx, hy))
                drawCircle(
                    color = Color.White.copy(alpha = 0.92f),
                    radius = handleRadius * 0.42f,
                    center = Offset(hx, hy)
                )
            }
        }
    }
}

/**
 * 手指落在哪一段：按纵向距离取最近一段（段间空隙归更近的一侧）。
 *
 * 只在候选范围内找 —— 翻页模式下候选限当前节，拖到页面底也不会跳进相邻页的段落。
 */
private fun nearestParagraph(
    candidates: List<ParagraphAnchorSource>,
    position: Offset
): ParagraphAnchorSource? {
    var best: ParagraphAnchorSource? = null
    var bestDistance = Float.MAX_VALUE
    for (src in candidates) {
        val layout = src.layout ?: continue
        val top = src.topLeft.y
        val bottom = top + layout.size.height
        val distance = when {
            position.y < top -> top - position.y
            position.y > bottom -> position.y - bottom
            else -> 0f
        }
        if (distance < bestDistance) {
            bestDistance = distance
            best = src
        }
    }
    return best
}

/** 指针在段内的字符偏移；落在段外时 getOffsetForPosition 会自动夹到首/尾 */
private fun textOffsetAt(src: ParagraphAnchorSource, position: Offset): Int {
    val layout = src.layout ?: return 0
    return layout.getOffsetForPosition(position - src.topLeft)
        .coerceIn(0, src.text.length)
}

/**
 * 选区操作条：复制 / 划线 / 批注。
 *
 * 纯文字按钮（不配图标）：三个动作用两个字就能说清，加图标只会在这个尺寸下变糊。
 * 底色用不透明的面板色 + 轻投影 —— 它浮在正文之上，半透明会让底下的字穿上来。
 */
@Composable
private fun SelectionActionBar(
    accent: Color,
    isDark: Boolean,
    fullyCovered: Boolean,
    hasNote: Boolean,
    onCopy: () -> Unit,
    onHighlight: () -> Unit,
    onUnhighlight: () -> Unit,
    onNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isDark) Color(0xFF1C1C1E) else Color.White
    val fg = if (isDark) Color(0xFFE8E6E1) else Color(0xFF1F1F1F)
    Row(
        modifier = modifier
            // 条内的空白也不能穿透到下面的"点空白收起选区"：
            // 手指落在两个按钮之间时，用户显然不是想取消选择
            .pointerInput(Unit) { detectTapGestures { } }
            .shadow(14.dp, SmoothRoundedCornerShape(14.dp), ambientColor = Color.Black.copy(alpha = 0.18f))
            .clip(SmoothRoundedCornerShape(14.dp))
            .background(bg)
            .height(42.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BarAction(
            label = stringResource(com.ilyskyo.blancall.R.string.reader_select_copy),
            color = fg,
            onClick = onCopy
        )
        BarDivider(isDark)
        if (fullyCovered && !hasNote) {
            BarAction(
                label = stringResource(com.ilyskyo.blancall.R.string.reader_select_unhighlight),
                color = accent,
                onClick = onUnhighlight
            )
        } else if (!fullyCovered) {
            BarAction(
                label = stringResource(com.ilyskyo.blancall.R.string.reader_select_highlight),
                color = accent,
                onClick = onHighlight
            )
        }
        // fullyCovered && hasNote：不显示「取消划线」。批注记录的划线本身就是批注的锚点，
        // 整条删除会连批注正文一起丢；要让用户先在「编辑批注」里明确删除。
        if (!(fullyCovered && hasNote)) BarDivider(isDark)
        BarAction(
            label = if (hasNote) {
                stringResource(com.ilyskyo.blancall.R.string.reader_select_edit_note)
            } else {
                stringResource(com.ilyskyo.blancall.R.string.reader_select_note)
            },
            color = fg,
            onClick = onNote
        )
    }
}

@Composable
private fun BarAction(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(SmoothRoundedCornerShape(10.dp))
            .pressClick { onClick() }
            .padding(horizontal = 13.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun BarDivider(isDark: Boolean) {
    Box(
        Modifier
            .width(0.6.dp)
            .height(18.dp)
            .background(if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f))
    )
}
