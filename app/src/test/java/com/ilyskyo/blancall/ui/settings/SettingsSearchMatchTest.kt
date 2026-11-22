// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 设置搜索的打分（[settingsMatchScore] 与 [settingsDocScore]）。
 *
 * 为什么值得单测：这是纯字符串逻辑，界面上「搜不到」与「搜到了但排在后面」看起来都像小毛病，
 * 但用户 2026-11-14 明确点出「没有模糊搜索功能」，2026-11-21 又补了一句
 * 「外貌/外观、颜色、学习提醒 都要能搜出对应的功能」。
 *
 * 三层各钉一条：
 * ① 文本关系（相等 > 前缀 > 包含 > 跳字）；
 * ② 别名（用户嘴里的词）能命中，且排序上让位于界面原词；
 * ③ 噪音词（「软件颜色」里的「软件」）剥掉后仍按实词命中，但整句只有噪音时不算命中。
 */
class SettingsSearchMatchTest {

    private val noNoise = emptySet<String>()
    private val zhNoise = setOf("app", "软件", "应用", "我的")

    private fun doc(title: String, vararg aliases: String) =
        SettingsSearchDoc(title, aliases.toList())

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

    // ── 别名 ──

    @Test
    fun 别名能搜到界面原词() {
        // 「外貌」是用户嘴里的词，「外观」才是界面上的字（2026-11-21 用户原话）
        val score = settingsDocScore("外貌", doc("外观", "外貌", "样子"), noNoise)
        assertTrue("别名应当命中", score > 0)
        // 「颜色」要能落到「主题色」上
        assertTrue(settingsDocScore("颜色", doc("主题色", "颜色", "配色"), noNoise) > 0)
    }

    @Test
    fun 标题命中排在别名命中前面() {
        val byTitle = settingsDocScore("主题色", doc("主题色", "颜色"), noNoise)
        val byAlias = settingsDocScore("颜色", doc("主题色", "颜色"), noNoise)
        assertTrue("界面原词应当比同义词更相关：$byTitle vs $byAlias", byTitle > byAlias)
    }

    @Test
    fun 入口行的别名再低一档() {
        val group = settingsDocScore("颜色", doc("主题色", "颜色"), noNoise)
        val entry = settingsDocScore("颜色", doc("外观", "颜色"), noNoise, entryLevel = true)
        assertTrue("入口是目录，能落到分区就别停在入口：$group vs $entry", group > entry)
    }

    // ── 噪音词 ──

    @Test
    fun 修饰词贴在实词前面也能命中() {
        // 「软件颜色」是一个词：先整词比，比不中再剥掉「软件」
        assertTrue(settingsDocScore("软件颜色", doc("主题色", "颜色"), zhNoise) > 0)
        assertTrue(settingsDocScore("app颜色", doc("主题色", "颜色"), zhNoise) > 0)
        // 空格分隔的英文写法同理
        assertTrue(settingsDocScore("app 颜色", doc("主题色", "颜色"), zhNoise) > 0)
    }

    @Test
    fun 整句只有噪音词时不算命中() {
        // 否则搜「软件」会把全部设置项列出来
        assertEquals(0, settingsDocScore("软件", doc("主题色", "颜色"), zhNoise))
        assertEquals(0, settingsDocScore("app", doc("主题色", "颜色"), zhNoise))
    }

    @Test
    fun 噪音词本身能命中时按命中算() {
        // 「设置」若真的出现在标题里，就不能因为它在噪音表里被跳过
        assertTrue(settingsDocScore("软件", doc("软件更新"), zhNoise) > 0)
    }

    // ── 剥词 ──

    @Test
    fun 剥噪音词只剥首尾且不剥空() {
        assertEquals("颜色", stripSearchNoise("软件颜色", zhNoise))
        assertEquals("颜色", stripSearchNoise("颜色我的", zhNoise))
        assertEquals("颜色", stripSearchNoise("app颜色", zhNoise))
        // 剥完是空串 ⇒ 交给调用方当成「没剥」，否则空查询会被当成全部命中
        assertEquals(null, stripSearchNoise("软件", zhNoise))
        assertEquals(null, stripSearchNoise("颜色", zhNoise))
    }

    @Test
    fun 别名表按逗号与顿号切分() {
        assertEquals(listOf("颜色", "配色", "色板"), parseSearchAliases("颜色,配色、色板"))
        assertEquals(listOf("pdf", "速度"), parseSearchAliases(" pdf , 速度 "))
        assertEquals(emptyList<String>(), parseSearchAliases(" , , "))
    }

    // ── 真资源回归：用户报过的说法必须落到对应的分区 ──

    /**
     * 拿 `values/strings.xml` 里的**真值**跑一遍，而不是在测试里另写一份假数据。
     *
     * 为什么值得这样写：别名表是手写的，最可能出的错不是算法不对，而是
     * 某个分区漏了某个说法。用真资源跑，就能在改文案/加分区时当场发现「这句话又搜不到了」。
     * 读文件而不是读 R 资源，是因为单测跑在 JVM 上（没有 Android 资源表）；
     * 同目录的 LocaleParityGuardTest 也是这么读的。
     */
    private fun strings(): Map<String, String> {
        val file = java.io.File("src/main/res/values/strings.xml")
        assertTrue("找不到 ${file.absolutePath}", file.isFile)
        return Regex("""<string name="([^"]+)">([^<]*)</string>""")
            .findAll(file.readText())
            .associate { it.groupValues[1] to it.groupValues[2] }
    }

    private fun docOf(strings: Map<String, String>, titleKey: String, aliasKey: String): SettingsSearchDoc {
        val title = strings[titleKey]
        val alias = strings[aliasKey]
        assertTrue("$titleKey 这条文案不存在", title != null)
        assertTrue("$aliasKey 这条别名不存在", alias != null)
        return doc(title!!, *parseSearchAliases(alias!!).toTypedArray())
    }

    @Test
    fun 用户报过的说法都能落到对应分区() {
        val s = strings()
        val appearance = docOf(s, "appearance", "settings_search_alias_appearance")
        val themeColor = docOf(s, "theme_color", "settings_search_alias_theme_color")
        val reminder = docOf(s, "study_reminder", "settings_search_alias_reminder")
        val reading = docOf(s, "reading_settings_title", "settings_search_alias_reading")

        // ①「外貌」→ 外观
        assertTrue("外貌 应命中 外观", settingsDocScore("外貌", appearance, zhNoise) > 0)
        // ②「颜色」→ 主题色（同一个词在真机上还有「挡片颜色」这一项，但分区要排在它前面）
        assertTrue("颜色 应命中 主题色", settingsDocScore("颜色", themeColor, zhNoise) > 0)
        val maskItem = doc(s["mask_color_label"]!!)
        assertTrue(
            "「颜色」应优先落到主题色而不是阅读的挡片",
            settingsDocScore("颜色", themeColor, zhNoise) > settingsDocScore("颜色", maskItem, zhNoise)
        )
        // ③「APP 颜色」「软件颜色」→ 主题色（修饰词不参与判定）
        assertTrue(settingsDocScore("APP 颜色", themeColor, zhNoise) > 0)
        assertTrue(settingsDocScore("软件颜色", themeColor, zhNoise) > 0)
        // ④「学习提醒」→ 学习提醒（分区名）
        assertTrue("学习提醒 应命中 学习提醒分区", settingsDocScore("学习提醒", reminder, zhNoise) > 0)
        // ⑤ 口语说法：通知 / 闹钟 / 夜间 / 字号
        assertTrue(settingsDocScore("通知", reminder, zhNoise) > 0)
        assertTrue(settingsDocScore("闹钟", reminder, zhNoise) > 0)
        assertTrue(settingsDocScore("夜间", appearance, zhNoise) > 0)
        assertTrue(settingsDocScore("字号", reading, zhNoise) > 0)
    }

    @Test
    fun 入口名与分区名的包含关系能被模糊命中() {
        // 「学习提醒」是「学习与提醒」少了一个「与」：跳字要能命中，
        // 但分数必须低于分区名本身（分区是更精确的落点）
        val s = strings()
        val entry = doc(s["settings_entry_study"]!!)
        val group = docOf(s, "study_reminder", "settings_search_alias_reminder")
        val entryScore = settingsDocScore("学习提醒", entry, zhNoise)
        assertTrue("学习与提醒 应能被「学习提醒」跳字命中", entryScore > 0)
        assertTrue(
            "分区名命中应高于入口名跳字：$entryScore vs ${settingsDocScore("学习提醒", group, zhNoise)}",
            settingsDocScore("学习提醒", group, zhNoise) > entryScore
        )
    }
}
