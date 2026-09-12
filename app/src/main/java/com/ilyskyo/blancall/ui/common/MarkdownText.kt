// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * 轻量 Markdown 渲染（零依赖）：把 PRIVACY.md 等文档按行渲染为 Compose 排版。
 *
 * 支持语法：
 * - `#` / `##` / `###` / `####` 标题（逐级缩小）
 * - `**粗体**`、`` `代码` ``、`[文字](链接)`、`~~删除线~~` 行内格式
 * - `- ` / `* ` / `+ ` 无序列表（支持 2 空格缩进分层）
 * - `1. ` / `1) ` 有序列表（保留原编号，支持缩进）
 * - `> ` 引用块（左侧竖线）
 * - `---` / `***` / `___` 分隔线
 * - 空行分段
 *
 * 判定逻辑抽成纯函数 [classifyBlock] / [indentLevel]，便于 JVM 单测锁定行为
 * （渲染本身是 Composable，无法在 JVM 单测里断言，但"某行会走哪个分支"可以）。
 *
 * 本版为**标准版（NoAI）**使用：渲染内容为隐私政策等文档，故不引入 commonmark 依赖；
 * Pro 版另有基于 commonmark 的全量渲染器用于 AI 产出文本，两者互不影响。
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    bodyStyle: TextStyle = MaterialTheme.typography.bodySmall
) {
    val lines = text.lines()
    // 链接颜色在 Composable 上下文获取后传入纯函数解析器
    val linkColor = MaterialTheme.colorScheme.primary
    val quoteBarColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)

    Column(modifier = modifier) {
        lines.forEach { raw ->
            val line = raw.trimEnd()
            val indent = indentLevel(line)
            val startPad = (indent * 14).dp
            when (classifyBlock(line)) {
                MdBlock.BLANK -> Spacer(Modifier.height(6.dp))

                MdBlock.HEADING1 -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        parseInline(blockBody(line), linkColor),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                    )
                }

                MdBlock.HEADING2 -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        parseInline(blockBody(line), linkColor),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                MdBlock.HEADING3 -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        parseInline(blockBody(line), linkColor),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }

                // 四级标题：用正文样式 + SemiBold 区分（设备楷体无 Bold 面，Bold 会回退黑体）
                MdBlock.HEADING4 -> {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        parseInline(blockBody(line), linkColor),
                        style = bodyStyle.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 6.dp, bottom = 1.dp)
                    )
                }

                MdBlock.THEMATIC_BREAK -> HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // 引用块：左侧竖线 + 次要色（竖线用 IntrinsicSize.Min 撑满该行高度）
                MdBlock.QUOTE -> Row(
                    modifier = Modifier
                        .padding(start = startPad, top = 2.dp, bottom = 2.dp)
                        .height(IntrinsicSize.Min)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(quoteBarColor)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        parseInline(blockBody(line), linkColor),
                        style = bodyStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                MdBlock.BULLET -> Text(
                    parseInline("•  ${blockBody(line)}", linkColor),
                    style = bodyStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = startPad, top = 1.dp, bottom = 1.dp)
                )

                // 有序列表：保留原编号（用户文档里的编号可能与正文语义相关，不重新编号）
                MdBlock.ORDERED -> Text(
                    parseInline(orderedBody(line), linkColor),
                    style = bodyStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = startPad, top = 1.dp, bottom = 1.dp)
                )

                MdBlock.PARAGRAPH -> Text(
                    parseInline(line.trim(), linkColor),
                    style = bodyStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ───────────────────────── 块级判定（纯函数，可单测） ─────────────────────────

/** 块级语法类型 */
internal enum class MdBlock {
    BLANK, HEADING1, HEADING2, HEADING3, HEADING4, THEMATIC_BREAK, QUOTE, BULLET, ORDERED, PARAGRAPH
}

/** 分隔线：≥3 个相同符号（- * _），可含空格间隔（CommonMark 允许 `- - -`） */
private val THEMATIC_BREAK = Regex("""^(?:\s*[-*_]\s*){3,}$""")

/** 无序列表项：`- ` / `* ` / `+ ` 开头（后随空格） */
private val UNORDERED = Regex("""^[-*+]\s+.*$""")

/** 有序列表项：1~2 位数字 + `.` 或 `)` + 空格。
 *  限定 1~2 位是刻意的：避免把 "2026. 我们…" 这类正文误判成列表。 */
private val ORDERED = Regex("""^\d{1,2}[.)]\s+.*$""")

/** 判定一行属于哪种块级语法（先 trim，缩进只影响排版不影响类型） */
internal fun classifyBlock(line: String): MdBlock {
    val body = line.trim()
    return when {
        body.isEmpty() -> MdBlock.BLANK
        body.startsWith("#### ") -> MdBlock.HEADING4
        body.startsWith("### ") -> MdBlock.HEADING3
        body.startsWith("## ") -> MdBlock.HEADING2
        body.startsWith("# ") -> MdBlock.HEADING1
        THEMATIC_BREAK.matches(body) -> MdBlock.THEMATIC_BREAK
        body == ">" || body.startsWith("> ") -> MdBlock.QUOTE
        UNORDERED.matches(body) -> MdBlock.BULLET
        ORDERED.matches(body) -> MdBlock.ORDERED
        else -> MdBlock.PARAGRAPH
    }
}

/** 前导空格换算的缩进层级：2 空格 = 1 级，最多 3 级 */
internal fun indentLevel(line: String): Int =
    (line.takeWhile { it == ' ' }.length / 2).coerceIn(0, 3)

/** 去掉标记后的正文（标题 / 引用 / 无序列表共用） */
private fun blockBody(line: String): String {
    val body = line.trim()
    return when {
        body.startsWith("#### ") -> body.removePrefix("#### ").trim()
        body.startsWith("### ") -> body.removePrefix("### ").trim()
        body.startsWith("## ") -> body.removePrefix("## ").trim()
        body.startsWith("# ") -> body.removePrefix("# ").trim()
        body.startsWith("> ") -> body.removePrefix("> ").trim()
        body.startsWith(">") -> body.removePrefix(">").trim()
        UNORDERED.matches(body) -> body.drop(1).trim()
        else -> body
    }
}

/** 有序列表：保留原编号（`1. 内容` / `2) 内容`） */
private fun orderedBody(line: String): String {
    val body = line.trim()
    val m = Regex("""^(\d{1,2})([.)])\s+(.*)$""").find(body) ?: return body
    return "${m.groupValues[1]}${m.groupValues[2]}  ${m.groupValues[3]}"
}

/**
 * 行内格式解析：`**粗体**`、`` `代码` ``、`[文字](链接)`、`~~删除线~~`。
 * 按出现位置取最早者逐段处理，避免嵌套冲突（本应用文档不含嵌套）。
 */
private fun parseInline(text: String, linkColor: Color): AnnotatedString {
    val tokens = mutableListOf<Pair<String, SpanStyle?>>()
    var remaining = text
    while (remaining.isNotEmpty()) {
        val boldStart = remaining.indexOf("**")
        val codeStart = remaining.indexOf('`')
        val linkStart = remaining.indexOf('[')
        val strikeStart = remaining.indexOf("~~")
        val starts = listOf(
            boldStart to 0, codeStart to 1, linkStart to 2, strikeStart to 3
        ).filter { it.first >= 0 }.minByOrNull { it.first }

        if (starts == null) {
            tokens.add(remaining to null)
            break
        }
        val (idx, kind) = starts
        if (idx > 0) tokens.add(remaining.substring(0, idx) to null)

        when (kind) {
            0 -> { // 粗体
                val end = remaining.indexOf("**", idx + 2)
                if (end < 0) {
                    tokens.add(remaining.substring(idx) to null)
                    break
                }
                tokens.add(remaining.substring(idx + 2, end) to SpanStyle(fontWeight = FontWeight.Bold))
                remaining = remaining.substring(end + 2)
            }
            1 -> { // 代码
                val end = remaining.indexOf('`', idx + 1)
                if (end < 0) {
                    tokens.add(remaining.substring(idx) to null)
                    break
                }
                tokens.add(remaining.substring(idx + 1, end) to SpanStyle(fontFamily = FontFamily.Monospace))
                remaining = remaining.substring(end + 1)
            }
            3 -> { // 删除线
                val end = remaining.indexOf("~~", idx + 2)
                if (end < 0) {
                    tokens.add(remaining.substring(idx) to null)
                    break
                }
                tokens.add(
                    remaining.substring(idx + 2, end) to
                        SpanStyle(textDecoration = TextDecoration.LineThrough)
                )
                remaining = remaining.substring(end + 2)
            }
            else -> { // 链接 [text](url)
                val close = remaining.indexOf(']', idx + 1)
                val paren = if (close >= 0) remaining.indexOf('(', close + 1) else -1
                val parenEnd = if (paren >= 0) remaining.indexOf(')', paren + 1) else -1
                if (close < 0 || paren < 0 || parenEnd < 0) {
                    tokens.add(remaining.substring(idx) to null)
                    break
                }
                tokens.add(
                    remaining.substring(idx + 1, close) to
                        SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
                )
                remaining = remaining.substring(parenEnd + 1)
            }
        }
    }

    return buildAnnotatedString {
        tokens.forEach { (part, style) ->
            if (style != null) withStyle(style) { append(part) } else append(part)
        }
    }
}
