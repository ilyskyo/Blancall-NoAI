// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ClozeTokens 行为锁定：四级粒度切分、标点不可选、英文整体、区间对齐校验、选空收敛 */
class ClozeTokensTest {

    @Test
    fun `level0 is whole sentence`() {
        val t = ClozeTokens.tokensFor("你好世界。", 0)
        assertEquals(1, t.size)
        assertEquals("你好世界。", t[0].text)
        assertEquals(0, t[0].range.first)
        assertEquals(4, t[0].range.last)
        // 挖空只到实义部分：句末句号留在正文里
        assertEquals(0, t[0].blankRange.first)
        assertEquals(3, t[0].blankRange.last)
    }

    @Test
    fun `level1 splits by punctuation and attaches punct to previous token`() {
        val t = ClozeTokens.tokensFor("你好，世界。再见！", 1)
        assertEquals(listOf("你好，", "世界。", "再见！"), t.map { it.text })
        // 区间连续覆盖全句
        assertEquals(0, t.first().range.first)
        assertEquals(8, t.last().range.last)
    }

    @Test
    fun `level1 blank range stops before the trailing punctuation`() {
        val t = ClozeTokens.tokensFor("你好，世界。", 1)
        // text 带标点（正文里看得见），blankRange 去吃标点之后
        assertEquals("你好，", t[0].text)
        assertEquals(0..1, t[0].blankRange)
        assertEquals("世界。", t[1].text)
        assertEquals(3..4, t[1].blankRange)
        assertTrue(t.all { it.selectable })
    }

    @Test
    fun `level1 single clause falls back to whole sentence`() {
        val t = ClozeTokens.tokensFor("你好世界", 1)
        assertEquals(1, t.size)
        assertEquals("你好世界", t[0].text)
    }

    @Test
    fun `level2 chunks chinese into pairs`() {
        val t = ClozeTokens.tokensFor("你好世界呀", 2)
        assertEquals(listOf("你好", "世界", "呀"), t.map { it.text })
    }

    @Test
    fun `level3 splits chinese into chars`() {
        val t = ClozeTokens.tokensFor("你好世界", 3)
        assertEquals(listOf("你", "好", "世", "界"), t.map { it.text })
    }

    @Test
    fun `english word stays whole at char and word levels`() {
        listOf(2, 3).forEach { level ->
            val t = ClozeTokens.tokensFor("hi你好", level)
            assertTrue(t.first().text == "hi")
            assertEquals(0, t.first().range.first)
            assertEquals(1, t.first().range.last)
        }
    }

    @Test
    fun `level1 without punctuation keeps whole sentence`() {
        val t = ClozeTokens.tokensFor("hi你好", 1)
        assertEquals(1, t.size)
        assertEquals("hi你好", t[0].text)
    }

    @Test
    fun `punctuation never joins a character token`() {
        val t = ClozeTokens.tokensFor("你好，世界", 3)
        assertEquals(listOf("你", "好", "，", "世", "界"), t.map { it.text })
        assertTrue(t.filter { it.text == "，" }.none { it.selectable })
    }

    @Test
    fun `book title mark is its own unselectable token`() {
        val t = ClozeTokens.tokensFor("《红楼梦》", 3)
        assertEquals(listOf("《", "红", "楼", "梦", "》"), t.map { it.text })
        // 只有三个字可选：书名号永远不会被带进空里
        assertEquals(listOf("红", "楼", "梦"), t.filter { it.selectable }.map { it.text })
        assertEquals(3..3, t.first { it.text == "梦" }.blankRange)
    }

    @Test
    fun `quotes brackets and dashes stay out of blanks`() {
        val sentence = "他说：「（试行）甲——乙…丙」"
        val tokens = ClozeTokens.tokensFor(sentence, 3)
        val selected = tokens.filter { it.selectable }.map { it.text }
        assertEquals(listOf("他", "说", "试", "行", "甲", "乙", "丙"), selected)
    }

    @Test
    fun `digits are content not punctuation`() {
        // 旧实现把数字当标点并入前一个字：单字级切出「第1949」这种拆不开的块
        val t = ClozeTokens.tokensFor("第1949年", 3)
        assertEquals(listOf("第", "1949", "年"), t.map { it.text })
        assertTrue(t.all { it.selectable })
        assertEquals(listOf("第", "1949", "年"), ClozeTokens.tokensFor("第1949年", 2).map { it.text })
    }

    @Test
    fun `whitespace stays out of blanks`() {
        val t = ClozeTokens.tokensFor("你好 世界", 3)
        assertEquals(listOf("你", "好", " ", "世", "界"), t.map { it.text })
        assertFalse(t.first { it.text == " " }.selectable)
    }

    @Test
    fun `tokens tile the sentence without gaps`() {
        val s = "化学平衡常数K，只与温度有关 (temperature)！"
        listOf(1, 2, 3).forEach { level ->
            var cursor = 0
            ClozeTokens.tokensFor(s, level).forEach { t ->
                assertEquals(cursor, t.range.first)
                cursor = t.range.last + 1
            }
            assertEquals(s.length, cursor)
        }
    }

    @Test
    fun `contentRanges splits a blank that straddles punctuation`() {
        // 旧配置里存的是「红·楼」这类跨标点的区间
        assertEquals(listOf(0..0, 2..2), ClozeTokens.contentRanges("红·楼", listOf(0..2)))
    }

    @Test
    fun `contentRanges trims punctuation at both ends`() {
        val sentence = "《红楼梦》很好看"
        val ranges = ClozeTokens.contentRanges(sentence, listOf(0..4))
        assertEquals(listOf(1..3), ranges)
        assertEquals("红楼梦", sentence.substring(ranges[0].first, ranges[0].last + 1))
    }

    @Test
    fun `contentRanges drops punctuation-only and out of range input`() {
        assertEquals(emptyList<IntRange>(), ClozeTokens.contentRanges("，，", listOf(0..1)))
        assertEquals(emptyList<IntRange>(), ClozeTokens.contentRanges("你好", emptyList()))
        assertEquals(emptyList<IntRange>(), ClozeTokens.contentRanges("", listOf(0..3)))
        // 越界裁剪后仍能取到内容
        assertEquals(listOf(0..1), ClozeTokens.contentRanges("你好", listOf(0..9)))
    }

    @Test
    fun `contentRanges keeps adjacent content merged but never across punctuation`() {
        // 相邻（无间隙）合并成一个空；被标点隔开的保持两个
        assertEquals(listOf(0..3), ClozeTokens.contentRanges("你好世界", listOf(0..1, 2..3)))
        assertEquals(listOf(0..1, 3..4), ClozeTokens.contentRanges("你好，世界", listOf(0..1, 3..4)))
    }

    @Test
    fun `subscript and superscript numbers count as content`() {
        // 「CO₂排放」这种混排片段不能被拆开：₂ 是 OTHER_NUMBER，不是标点
        assertEquals(listOf(0..4), ClozeTokens.contentRanges("CO₂排放", listOf(0..4)))
        assertEquals(listOf("CO", "₂", "排", "放"), ClozeTokens.tokensFor("CO₂排放", 3).map { it.text })
    }

    @Test
    fun `contentRanges keeps surrogate pairs intact`() {
        val sentence = "「𠮷田」"
        val ranges = ClozeTokens.contentRanges(sentence, listOf(0..4))
        assertEquals(listOf(1..3), ranges)
    }

    @Test
    fun `rangesAligned accepts whole-token unions`() {
        val s = "你好世界"
        // 单字级：连续 3 个字
        assertTrue(ClozeTokens.rangesAligned(s, 3, listOf(0..2)))
        // 字词级：两个整块
        assertTrue(ClozeTokens.rangesAligned(s, 2, listOf(0..1, 2..3)))
    }

    @Test
    fun `rangesAligned ignores punctuation between tokens`() {
        // 单字级选「红」「楼」：中间夹着间隔号不影响判定（标点不参与选空）
        assertTrue(ClozeTokens.rangesAligned("红·楼", 3, listOf(0..0)))
        assertTrue(ClozeTokens.rangesAligned("红·楼", 3, listOf(2..2)))
        // 跨标点的一个区间不再对齐
        assertFalse(ClozeTokens.rangesAligned("红·楼", 3, listOf(0..2)))
    }

    @Test
    fun `rangesAligned rejects mid-token cuts`() {
        val s = "你好世界"
        // 字词级下切在块中间（单字）
        assertFalse(ClozeTokens.rangesAligned(s, 2, listOf(0..0)))
        // 越界区间
        assertFalse(ClozeTokens.rangesAligned(s, 3, listOf(0..9)))
    }

    @Test
    fun `restoreLevel refines until aligned`() {
        val s = "你好世界呀"
        // 字词级下 0..2 切在「世界」块中间 → 细化到单字级
        assertEquals(3, ClozeTokens.restoreLevel(s, 2, listOf(0..2)))
        // 本来就对齐则维持原粒度
        assertEquals(2, ClozeTokens.restoreLevel(s, 2, listOf(0..1)))
        // 空区间集：直接维持原粒度
        assertEquals(1, ClozeTokens.restoreLevel(s, 1, emptyList()))
    }
}
