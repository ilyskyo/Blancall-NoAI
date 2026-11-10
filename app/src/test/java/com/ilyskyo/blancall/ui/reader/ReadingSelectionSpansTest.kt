// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [readingSelectionSpans]：跨段选区拆成每段一段的区间。
 *
 * 语义：中间段整段选中；首段从 startA 到段尾，末段从段首到 endE；
 * 起止落在段界上（startA=段长 / endE=0）时该段不产生空区间。
 * 渲染、复制、落库共用这一份口径。
 */
class ReadingSelectionSpansTest {

    private val paragraphs = listOf("0123456789", "abcdefghij", "ABCDEFGHIJ")

    private fun lengths(p: Int): Int = paragraphs.getOrNull(p)?.length ?: 0

    @Test
    fun `同一段内保持原区间`() {
        val spans = readingSelectionSpans(ReadingSelectionRange(0, 2, 0, 5), ::lengths)
        assertEquals(listOf(ReadingSelectionSpan(0, 2, 5)), spans)
    }

    @Test
    fun `跨两段拆成段尾加段首`() {
        val spans = readingSelectionSpans(ReadingSelectionRange(0, 7, 1, 3), ::lengths)
        assertEquals(
            listOf(ReadingSelectionSpan(0, 7, 10), ReadingSelectionSpan(1, 0, 3)),
            spans
        )
    }

    @Test
    fun `跨三段时中间段整段选中`() {
        val spans = readingSelectionSpans(ReadingSelectionRange(0, 8, 2, 4), ::lengths)
        assertEquals(
            listOf(
                ReadingSelectionSpan(0, 8, 10),
                ReadingSelectionSpan(1, 0, 10),
                ReadingSelectionSpan(2, 0, 4)
            ),
            spans
        )
    }

    @Test
    fun `起点落在段尾时不产生首段空区间`() {
        val spans = readingSelectionSpans(ReadingSelectionRange(0, 10, 1, 2), ::lengths)
        assertEquals(listOf(ReadingSelectionSpan(1, 0, 2)), spans)
    }

    @Test
    fun `终点落在段首时不产生末段空区间`() {
        val spans = readingSelectionSpans(ReadingSelectionRange(0, 4, 1, 0), ::lengths)
        assertEquals(listOf(ReadingSelectionSpan(0, 4, 10)), spans)
    }

    @Test
    fun `同段空区间返回空`() {
        assertTrue(readingSelectionSpans(ReadingSelectionRange(0, 5, 0, 5), ::lengths).isEmpty())
    }

    @Test
    fun `反向区间返回空`() {
        assertTrue(readingSelectionSpans(ReadingSelectionRange(2, 3, 0, 4), ::lengths).isEmpty())
    }

    @Test
    fun `缺失段落被跳过`() {
        val spans = readingSelectionSpans(ReadingSelectionRange(0, 8, 3, 2)) { p ->
            if (p == 3) 0 else lengths(p)
        }
        assertEquals(
            listOf(
                ReadingSelectionSpan(0, 8, 10),
                ReadingSelectionSpan(1, 0, 10),
                ReadingSelectionSpan(2, 0, 10)
            ),
            spans
        )
    }

    @Test
    fun `越界的偏移被夹回段长`() {
        val spans = readingSelectionSpans(ReadingSelectionRange(0, 8, 1, 99), ::lengths)
        assertEquals(
            listOf(ReadingSelectionSpan(0, 8, 10), ReadingSelectionSpan(1, 0, 10)),
            spans
        )
    }

    @Test
    fun `复制文本按段用换行连接`() {
        val text = readingSelectionText(ReadingSelectionRange(0, 7, 2, 4)) { p ->
            paragraphs.getOrNull(p).orEmpty()
        }
        assertEquals("789\nabcdefghij\nABCD", text)
    }
}
