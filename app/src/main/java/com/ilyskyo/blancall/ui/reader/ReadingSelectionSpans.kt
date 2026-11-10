// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.reader

/**
 * 当前选区：从 [startP] 段 [startA] 到 [endP] 段 [endE)。
 *
 * 段号与字符偏移都按 `splitParagraphs` 口径。选区可以跨段（拖手柄越过段尾），
 * 但存储与渲染仍以「单段一条」为粒度 —— 跨段选区由
 * [readingSelectionSpans] 拆成每段一段。
 */
internal data class ReadingSelectionRange(
    val startP: Int,
    val startA: Int,
    val endP: Int,
    val endE: Int
) {
    val isEmpty: Boolean
        get() = endP < startP || (endP == startP && endE <= startA)

    companion object {
        fun inParagraph(p: Int, a: Int, e: Int) = ReadingSelectionRange(p, a, p, e)
    }
}

/** 选区在某一段内的连续区间 `[a, e)`。 */
internal data class ReadingSelectionSpan(val p: Int, val a: Int, val e: Int)

/**
 * 把跨段选区拆成「每段一段」的区间；中间段整段选中，空段与越界段跳过。
 *
 * [paragraphLength] 返回该段文本长度（取不到给 0）。拆分是纯函数：渲染、
 * 复制、落库都走同一份口径，避免三处各自算一套边界。
 */
internal fun readingSelectionSpans(
    range: ReadingSelectionRange,
    paragraphLength: (Int) -> Int
): List<ReadingSelectionSpan> {
    if (range.isEmpty) return emptyList()
    val out = ArrayList<ReadingSelectionSpan>(range.endP - range.startP + 1)
    for (p in range.startP..range.endP) {
        val len = paragraphLength(p)
        if (len <= 0) continue
        val a = if (p == range.startP) range.startA.coerceIn(0, len) else 0
        val e = if (p == range.endP) range.endE.coerceIn(0, len) else len
        if (e > a) out += ReadingSelectionSpan(p, a, e)
    }
    return out
}

/** 选区全文：段与段之间用换行连接（复制/批注摘录用）。 */
internal fun readingSelectionText(
    range: ReadingSelectionRange,
    paragraphText: (Int) -> String
): String = readingSelectionSpans(range) { paragraphText(it).length }
    .mapNotNull { span ->
        val text = paragraphText(span.p)
        if (span.e <= text.length) text.substring(span.a, span.e) else null
    }
    .joinToString("\n")
