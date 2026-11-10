// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 归一化的两条硬口径：
 *
 * 1. **排版标点仍然忽略** —— 背课文/古诗文的场景里，多一个逗号、少一个句号不该判错；
 * 2. **语义符号必须参与比对** —— `+ = % < > /` 是内容不是排版，
 *    「H2 + O2」与「H2O2」不是一回事，`50%` 与 `50` 也不是。
 *
 * 旧实现把两者混在一张标点表里一起剥掉，等于对化学式/公式/百分比放行错误答案。
 */
class NormalizationSemanticsTest {

    private fun same(a: String, b: String) =
        AnswerChecker.check(a, b).result == AnswerChecker.Result.CORRECT

    @Test
    fun `排版标点差异仍判对`() {
        assertTrue("中文逗号不该判错", same("学而时习之，不亦说乎", "学而时习之不亦说乎"))
        assertTrue("句号与空格不该判错", same("床前明月光。", "床前明月光"))
        assertTrue("引号不该判错", same("他说“好”", "他说好"))
        assertTrue("全角与半角标点同义", same("甲乙，丙丁", "甲乙,丙丁"))
    }

    @Test
    fun `语义符号参与比对`() {
        assertNotEquals(
            "化学方程式不能因为去掉加号而等价",
            AnswerChecker.Result.CORRECT,
            AnswerChecker.check("H2 + O2 → H2O", "H2O2H2O").result,
        )
        assertNotEquals(
            "百分比与纯数字不是一回事",
            AnswerChecker.Result.CORRECT,
            AnswerChecker.check("50%", "50").result,
        )
        assertNotEquals(
            "小于号是内容",
            AnswerChecker.Result.CORRECT,
            AnswerChecker.check("<3", "3").result,
        )
        // 写对了（含符号）仍然判对
        assertTrue("符号一致时判对", same("H2 + O2 → H2O", "H2+O2→H2O"))
    }

    @Test
    fun `连字符按上下文取舍`() {
        assertTrue(
            "英文复合词里的连字符算排版",
            same("well-known", "wellknown"),
        )
        assertNotEquals(
            "负号/区间里的短横是内容",
            AnswerChecker.Result.CORRECT,
            AnswerChecker.check("-5", "5").result,
        )
    }

    @Test
    fun `小数点在数字之间保留`() {
        assertNotEquals(
            "3.14 与 3.14 之外的形式不等价",
            AnswerChecker.Result.CORRECT,
            AnswerChecker.check("3.14", "314").result,
        )
        assertTrue(same("3.14", "3.14"))
    }

    @Test
    fun `中文数字折叠仍生效`() {
        assertTrue("个位数中文数字与阿拉伯数字等价", same("三人行", "3人行"))
        assertTrue("繁体作答按简体比对", same("學習", "学习"))
    }

    @Test
    fun `生僻字按码点比对不被劈成半个字`() {
        // 𠮷（U+20BB7）是代理对；按 Char 逐位比会算成两处不一致，
        // 短答案会因此掉进 INCORRECT。
        val detail = AnswerChecker.check("𠮷祥", "𠮷祥")
        assertEquals(AnswerChecker.Result.CORRECT, detail.result)

        val typo = AnswerChecker.check("𠮷祥", "吉祥")
        // 一处替换 = 记成他字，而不是「完全不对」
        assertNotEquals(
            "代理对替换不应被判成完全不会",
            AnswerChecker.Result.INCORRECT,
            typo.result,
        )
        val replacement = typo.diff as? AnswerChecker.Diff.Replacement
        assertTrue("替换明细里的字必须是完整字形", replacement?.items?.isNotEmpty() == true)
        assertEquals("𠮷", replacement!!.items.first().expected)
    }
}
