// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 导入前的注释清理：**每条正则都必须成对收尾**。
 *
 * 旧实现里方括号那条写成 `［[^］］*`（少了闭合的 `］`），
 * 于是「［注］」被删成孤立的「］」，而正文里出现单个未配对的 `［` 时，
 * 正则会一路吃到文末，把中间整段正文删掉 —— 删除发生在落库之前，不可恢复。
 */
class AnnotationCleanupTest {

    private val poetry = GaokaoTextCleaner.GaokaoTextType.POETRY
    private val cleaner = GaokaoTextCleaner()

    @Test
    fun `方括号注释整段删除且不吞后文`() {
        val text = "床前明月光［注：疑是地上霜］\n疑是地上霜"
        val result = cleaner.cleanText(text, poetry)

        assertFalse("注释内容必须被删掉", result.cleanedText.contains("注："))
        assertFalse("不能残留孤立的右括号", result.cleanedText.contains("］"))
        assertTrue("注释之后的正文必须保留", result.cleanedText.contains("疑是地上霜"))
    }

    @Test
    fun `未配对的方括号不会吞掉整篇正文`() {
        // 真实场景：古文里偶有单个「［」当作分隔符，没有对应的「］」
        val text = "学而时习之［\n不亦说乎\n有朋自远方来"
        val result = cleaner.cleanText(text, poetry)

        assertTrue("后面的正文不能被整段吃掉（旧实现会）", result.cleanedText.contains("不亦说乎"))
        assertTrue(result.cleanedText.contains("有朋自远方来"))
    }

    @Test
    fun `圆括号与方头括号注释同样处理`() {
        val text = "落霞与孤鹜齐飞（注：此句写景）\n秋水共长天一色【注：对仗】"
        val result = cleaner.cleanText(text, poetry)

        assertFalse(result.cleanedText.contains("此句写景"))
        assertFalse(result.cleanedText.contains("对仗"))
        assertTrue(result.cleanedText.contains("落霞与孤鹜齐飞"))
        assertTrue(result.cleanedText.contains("秋水共长天一色"))
    }

    @Test
    fun `被删掉的注释要如实记进 removedAnnotations`() {
        val text = "床前明月光［注：一作明月光］"
        val result = cleaner.cleanText(text, poetry)
        assertTrue(
            "删了什么要留痕，便于用户核对",
            result.removedAnnotations.any { it.contains("注：") },
        )
    }

    @Test
    fun `不含注释的正文一字不改`() {
        val text = "床前明月光\n疑是地上霜"
        val result = cleaner.cleanText(text, poetry)
        assertEquals(text.replace("\r\n", "\n"), result.cleanedText.replace("\r\n", "\n"))
    }
}
