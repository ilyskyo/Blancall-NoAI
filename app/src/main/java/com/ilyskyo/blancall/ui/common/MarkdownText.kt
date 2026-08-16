// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
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
 * - `#` / `##` / `###` 标题（逐级缩小）
 * - `**粗体**` 行内加粗
 * - `` `代码` `` 行内代码（等宽提示，去掉反引号）
 * - `[文字](链接)` 行内链接（保留文字，去掉链接语法）
 * - `- ` 列表项（前缀圆点）
 * - 空行分段
 *
 * 用于隐私政策启动页与设置页查看弹窗，保证正文排版可读。
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    bodyStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodySmall
) {
    val lines = text.lines()
    // 链接颜色在 Composable 上下文获取后传入纯函数解析器
    val linkColor = MaterialTheme.colorScheme.primary
    Column(modifier = modifier) {
        lines.forEach { raw ->
            val line = raw.trimEnd()
            when {
                line.isBlank() -> Spacer(Modifier.height(6.dp))

                line.startsWith("### ") -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        parseInline(line.removePrefix("### ").trim(), linkColor),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }

                line.startsWith("## ") -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        parseInline(line.removePrefix("## ").trim(), linkColor),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                line.startsWith("# ") -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        parseInline(line.removePrefix("# ").trim(), linkColor),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                    )
                }

                line.startsWith("- ") -> {
                    Text(
                        parseInline("•  ${line.removePrefix("- ").trim()}", linkColor),
                        style = bodyStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }

                else -> {
                    Text(
                        parseInline(line, linkColor),
                        style = bodyStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 行内格式解析：`**粗体**`、`` `代码` ``、`[文字](链接)`。
 * 按 ** → ` → []( 的顺序逐段处理，避免嵌套冲突（本应用文档不含嵌套）。
 */
private fun parseInline(text: String, linkColor: androidx.compose.ui.graphics.Color): AnnotatedString {
    val tokens = mutableListOf<Pair<String, SpanStyle?>>()
    var remaining = text
    while (remaining.isNotEmpty()) {
        // 1) **粗体**
        val boldStart = remaining.indexOf("**")
        // 2) `代码`
        val codeStart = remaining.indexOf('`')
        // 3) [文字](链接)
        val linkStart = remaining.indexOf('[')
        val starts = listOf(
            boldStart to 0, codeStart to 1, linkStart to 2
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
