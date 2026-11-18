// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.handwriting

import com.ilyskyo.blancall.data.handwriting.HandwritingScript
import com.ilyskyo.blancall.ui.settings.composeSymbols
import com.ilyskyo.blancall.ui.theme.AppPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 手写符号栏偏好（设置 → 特性 → 符号栏）的三层解析：存什么、拆成什么、怎么合成。
 *
 * 这里守的核心是一条不变量：**默认值必须等于旧行为**。旧实现的符号集是写死的
 * `PUNCT_CJK`（7 个）/ `PUNCT_LATIN`（8 个），改成一个可配置的字符串之后，
 * 老用户升级上来看到的键必须一个不多、一个不少 —— 否则「加了个设置项」就变成了
 * 「悄悄改了手感」。最后那条用例是这条不变量的守门人。
 */
class HandwritingSymbolSettingsTest {

    // ── normalizeSymbols：写盘前的清洗 ──

    @Test
    fun normalize_dropsHalfWidthAndFullWidthSpaces() {
        assertEquals("，。", AppPrefs.normalizeSymbols(" ， 　。 "))
    }

    @Test
    fun normalize_dedupesKeepingFirstOccurrence() {
        // 两个键写同一个字等于给用户两个长得一样的按钮，点哪个都一样，纯属误点来源
        assertEquals("，。、", AppPrefs.normalizeSymbols("，。、，。"))
    }

    @Test
    fun normalize_capsLengthAtSymbolsMax() {
        val raw = "，。、？！：；…《》（）·—,."
        assertEquals(AppPrefs.SYMBOLS_MAX, AppPrefs.normalizeSymbols(raw).length)
    }

    @Test
    fun normalize_dropsSurrogates() {
        // 半个代理对存下去，画出来是豆腐块、字数统计还会翻倍
        val emoji = "，\uD83D\uDE00。"   // ，😀。
        assertEquals("，。", AppPrefs.normalizeSymbols(emoji))
    }

    // ── symbolsOf：存盘串 → 键列表 ──

    @Test
    fun symbolsOf_keepsBmpCharsInOrder() {
        assertEquals(listOf('，', '。', '？'), symbolsOf("，。？"))
    }

    @Test
    fun symbolsOf_dropsLoneSurrogates() {
        assertEquals(listOf('，', '。'), symbolsOf("，\uD83D\uDE00。"))
    }

    @Test
    fun symbolKeys_picksSetByScript() {
        assertEquals(listOf('，', '。'), symbolKeys(HandwritingScript.Chinese, "，。", ",."))
        assertEquals(listOf(',', '.'), symbolKeys(HandwritingScript.Latin, "，。", ",."))
    }

    // ── composeSymbols：勾选的预设 + 自输补充 ──

    @Test
    fun compose_listsSelectedPresetsInPresetOrder() {
        val presets = listOf(',', '.', '!')
        // 用户先点「!」再点「,」，上栏顺序仍按预设顺序 —— 位置稳定才有肌肉记忆
        assertEquals(",!", composeSymbols(presets, setOf('!', ','), ""))
    }

    @Test
    fun compose_appendsCustomAfterPresets() {
        val presets = listOf(',', '.')
        // 只勾了逗号：句点不该因为「在预设池里」就自动上栏，自输的 ↑ 接在后面
        assertEquals(",↑", composeSymbols(presets, setOf(','), "↑"))
    }

    @Test
    fun compose_dedupesCustomAgainstPresets() {
        val presets = listOf(',', '.')
        assertEquals(",", composeSymbols(presets, setOf(','), ","))
    }

    @Test
    fun compose_dropsWhitespaceFromCustom() {
        assertEquals(",", composeSymbols(listOf(','), setOf(','), "  　"))
    }

    // ── 不变量：默认值 == 旧行为 ──

    @Test
    fun defaults_matchLegacyHardcodedSets() {
        // 旧的 PUNCT_CJK / PUNCT_LATIN 逐字抄在这里，作为回归基线。
        // 这两行是**故意**写死的字面量：若有人改了 AppPrefs 的默认值，这条会红。
        assertEquals("，。、？！：；", AppPrefs.DEFAULT_SYMBOLS_CJK)
        assertEquals(",.?!;:'-", AppPrefs.DEFAULT_SYMBOLS_LATIN)
        assertEquals(7, symbolsOf(AppPrefs.DEFAULT_SYMBOLS_CJK).size)
        assertEquals(8, symbolsOf(AppPrefs.DEFAULT_SYMBOLS_LATIN).size)
    }

    @Test
    fun defaultsFitUnderCap() {
        assertTrue(symbolsOf(AppPrefs.DEFAULT_SYMBOLS_CJK).size <= AppPrefs.SYMBOLS_MAX)
        assertTrue(symbolsOf(AppPrefs.DEFAULT_SYMBOLS_LATIN).size <= AppPrefs.SYMBOLS_MAX)
        // 上限本身要够放下旧的两套，否则「默认值」会被 normalize 截断 —— 那正是最难查的回归
        assertFalse(AppPrefs.SYMBOLS_MAX < 8)
    }
}
