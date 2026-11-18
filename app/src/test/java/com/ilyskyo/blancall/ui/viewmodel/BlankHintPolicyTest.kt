// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 弱提示的「能不能淡显」判定（[fadesInFirstChar]）与单字空的强填时刻。
 *
 * 背景：字词挖空的空大多是 1 个字。旧行为对它照样先淡显首字 —— 而那个字就是答案本身，
 * 「弱提示」于是退化成给答案。现在单字空跳过淡显，到点直接强填。
 */
class BlankHintPolicyTest {

    @Test
    fun `单字空不淡显`() {
        assertFalse(fadesInFirstChar(1))
    }

    @Test
    fun `多字空照常淡显`() {
        assertTrue(fadesInFirstChar(2))
        assertTrue(fadesInFirstChar(7))
    }

    @Test
    fun `没有答案时不淡显`() {
        // 正常路径到不了这里（expected 为空时 maybeStartBlankHint 已返回），判据仍要对
        assertFalse(fadesInFirstChar(0))
    }

    @Test
    fun `单字空强填与多字空淡显完成同一时刻`() {
        // 帮助到达的时间点对齐：多字空此刻刚把提示字淡完，单字空则直接填上
        assertEquals(HINT_FIRST_WAIT_MS + HINT_FADE_MS, HINT_SINGLE_CHAR_FILL_MS)
        // 界面承诺的「15 秒补全」
        assertEquals(15_000L, HINT_SINGLE_CHAR_FILL_MS)
        // 且必须早于多字空的强填（再 +5s 观察），否则「不淡显」只是让人白等
        assertTrue(
            HINT_SINGLE_CHAR_FILL_MS < HINT_FIRST_WAIT_MS + HINT_FADE_MS + HINT_AFTER_WAIT_MS
        )
    }
}
