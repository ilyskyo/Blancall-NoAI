// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 设置搜索的模糊匹配打分（[settingsMatchScore]）。
 *
 * 为什么值得单测：这是纯字符串逻辑，界面上「搜不到」与「搜到了但排在后面」看起来都像小毛病，
 * 但用户 2026-11-14 明确点出「没有模糊搜索功能」。这里把三种层级的命中各钉一条，
 * 一旦有人把子序列分支删掉（比如为了「减少误命中」），测试会红而不是悄悄退化。
 */
class SettingsSearchMatchTest {

    @Test
    fun 连续子串按包含命中() {
        assertTrue(settingsMatchScore("主题", "主题色") >= 60)
        assertTrue(settingsMatchScore("字体", "字体大小") >= 60)
    }

    @Test
    fun 跳字命中_主色可以搜到主题色() {
        // 中文用户常少打中间的字：旧实现用 contains，这里会返回 0（=搜不到）
        assertEquals(30, settingsMatchScore("主色", "主题色"))
        // 「深浅」是「深浅模式」的前缀，走的是更靠前的分支，不该被降级成跳字
        assertEquals(80, settingsMatchScore("深浅", "深浅模式"))
    }

    @Test
    fun 前缀比包含更相关_完全相等最高() {
        val exact = settingsMatchScore("主题色", "主题色")
        val prefix = settingsMatchScore("主题", "主题色")
        val contains = settingsMatchScore("题色", "主题色")
        assertTrue("完全相等应高于前缀", exact > prefix)
        assertTrue("前缀应高于包含", prefix > contains)
        assertTrue("包含应高于跳字", contains > settingsMatchScore("主色", "主题色"))
    }

    @Test
    fun 多个关键词必须同时命中() {
        // 空格分隔 = AND：任一词不命中就整条不命中
        assertTrue(settingsMatchScore("主题 颜色", "主题 颜色设置") > 0)
        assertEquals(0, settingsMatchScore("主题 备份", "主题色"))
    }

    @Test
    fun 无关词不命中() {
        assertEquals(0, settingsMatchScore("zzz", "主题色"))
        assertEquals(0, settingsMatchScore("备份", "主题色"))
    }

    @Test
    fun 空查询视为全部命中() {
        assertEquals(1, settingsMatchScore("", "任何东西"))
        assertEquals(1, settingsMatchScore("   ", "任何东西"))
    }

    @Test
    fun 忽略大小写与分隔点() {
        assertEquals(
            settingsMatchScore("theme", "theme color"),
            settingsMatchScore("THEME", "Theme · color"),
        )
    }
}
