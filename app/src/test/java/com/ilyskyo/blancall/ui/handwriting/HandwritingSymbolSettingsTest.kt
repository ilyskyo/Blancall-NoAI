// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.handwriting

import com.ilyskyo.blancall.data.handwriting.HandwritingScript
import com.ilyskyo.blancall.ui.settings.composeSymbols
import com.ilyskyo.blancall.ui.settings.sanitizeSymbolInput
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
    fun normalize_keepsEverySymbolNoCap() {
        // 7.6.4：条数上限取消（面板那行改成横滑）。旧上限 12 曾把第 13 个起悄悄截掉，
        // 界面上「加进去了」却看不到 —— 现在整串原样保留。
        val raw = "，。、？！：；…《》（）·—,.?↑↓⇌Δ°×÷√±≈≤≥"
        assertEquals(raw, AppPrefs.normalizeSymbols(raw))
        assertTrue(raw.length > 12)
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
    fun defaultsSurviveNormalizeUntouched() {
        // 默认值必须原样穿过清洗：一旦被截断，「升级不改手感」这条不变量就破了
        assertEquals(
            AppPrefs.DEFAULT_SYMBOLS_CJK,
            AppPrefs.normalizeSymbols(AppPrefs.DEFAULT_SYMBOLS_CJK)
        )
        assertEquals(
            AppPrefs.DEFAULT_SYMBOLS_LATIN,
            AppPrefs.normalizeSymbols(AppPrefs.DEFAULT_SYMBOLS_LATIN)
        )
    }

    // ── 补充字符只收「一个符号」 ──

    @Test
    fun symbolChar_rejectsEverythingThatIsAWord() {
        // 汉字、拉丁字母、全角字母、日文假名、数字 —— 都不是符号
        listOf('计', '划', 'V', '缸', 'Ａ', 'ｚ', 'あ', '1', ' ').forEach { ch ->
            assertFalse("$ch 不该被当作符号", AppPrefs.isSymbolChar(ch))
        }
    }

    @Test
    fun symbolChar_acceptsPunctuationAndMath() {
        listOf('，', '。', '≤', '⇌', '°', '×', '√', ',', '.', '—').forEach { ch ->
            assertTrue("$ch 应该被当作符号", AppPrefs.isSymbolChar(ch))
        }
    }

    @Test
    fun symbolChar_acceptsGreekBecausePresetHasDelta() {
        // Δ 是预设池里的一项（理科方程式常用）。它属于字母类，必须单独特批 ——
        // 否则用户勾了 Δ 会被 normalizeSymbols 悄悄丢掉，界面上看着像点不动。
        listOf('Δ', 'Σ', 'Ω', 'π').forEach { ch ->
            assertTrue("$ch 应该被当作符号", AppPrefs.isSymbolChar(ch))
        }
    }

    @Test
    fun sanitize_takesFirstSymbolAndSkipsWords() {
        // 输入法一次上屏一串时，取第一个符号 —— 不是「整段必须合法」
        assertEquals('≤', sanitizeSymbolInput("计划V缸好≤"))
        assertEquals('，', sanitizeSymbolInput("abc，def"))
        // 一个符号都没有：返回 null，输入框保持空（旧版会把整段汉字存进符号栏）
        assertEquals(null, sanitizeSymbolInput("计划V缸好"))
        assertEquals(null, sanitizeSymbolInput("hello world"))
        assertEquals(null, sanitizeSymbolInput("   "))
    }
}
