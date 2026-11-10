// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 对话框窗口动画守卫。
 *
 * Compose 的 `Dialog` 是独立 window：它的系统进入动画取自 **对话框主题**
 * （`android:dialogTheme` → `windowAnimationStyle`），而 Activity 主题上那一份
 * `windowAnimationStyle` 管不到它。部分 ROM（华为 / 小米等）在系统对话框主题里
 * 塞了「从右下角推入」，于是卡片自己的居中缩放之上又叠了一层角上飞入。
 *
 * 这条守卫锁死三件事：
 * 1. 日 / 夜两套 `Theme.Blancall` 都把 `android:dialogTheme` 指到自有样式；
 * 2. 自有对话框样式把 `windowAnimationStyle` 置为 `@null`；
 * 3. 对话框样式自我声明 `android:dialogTheme` —— 从浮层里再开出来的下一层弹窗
 *    （例如遮罩配置浮层里的重命名 / 删除）不能换回系统主题。
 */
class DialogWindowThemeGuardTest {

    @Test
    fun 日夜两套主题都把对话框指到无动画样式() {
        listOf(DAY, NIGHT).forEach { path ->
            val styles = parse(File(path))
            assertEquals(
                "$path 的 Theme.Blancall 必须覆写 android:dialogTheme；" +
                    "只写 windowAnimationStyle 的旧做法对 Dialog 窗口无效",
                DialogStyle,
                styles["Theme.Blancall"]?.get("android:dialogTheme")
            )
            val dialog = styles["Theme.Blancall.Dialog"]
            assertTrue("$path 缺少 Theme.Blancall.Dialog", dialog != null)
            assertEquals(
                "$path 的 Theme.Blancall.Dialog 必须把窗口动画置空（@null）；" +
                    "否则 ROM 的右下角推入会重新叠在居中缩放上",
                "@null",
                dialog!!["android:windowAnimationStyle"]
            )
            assertEquals(
                "$path 的 Theme.Blancall.Dialog 必须自我声明 android:dialogTheme，" +
                    "嵌套弹窗才会继续使用这套无动画主题",
                DialogStyle,
                dialog["android:dialogTheme"]
            )
        }
    }

    @Test
    fun `夜间主题也要关掉 Activity 窗口动画`() {
        // 这是修复前真实存在的缺口：values-night 重写 Theme.Blancall 时漏掉了这一项，
        // 系统夜间模式下 Activity 窗口会重新长出转场动画。
        val night = parse(File(NIGHT))["Theme.Blancall"]
        assertEquals(
            "$NIGHT 的 Theme.Blancall 必须显式关掉 Activity 窗口动画（night 是独立定义，不会继承 values 那份）",
            "@style/BlancallWindowAnimationNone",
            night?.get("android:windowAnimationStyle")
        )
    }

    @Test
    fun 解析器不是空转() {
        // 反向对照：缺项 / 写错值的样例必须被同一套读取逻辑判为不合格，
        // 否则上面两条断言可能只是因为解析器什么都没读到而“碰巧”对不上。
        val bad = parseXml(
            """
            <resources xmlns:android="http://schemas.android.com/apk/res/android">
                <style name="Theme.Blancall">
                    <item name="android:windowAnimationStyle">@style/BlancallWindowAnimationNone</item>
                </style>
                <style name="Theme.Blancall.Dialog" parent="@android:style/Theme.Material.Light.Dialog">
                    <item name="android:windowAnimationStyle">@style/Animation.Dialog</item>
                </style>
            </resources>
            """.trimIndent()
        )
        assertNull("缺 android:dialogTheme 的 Theme.Blancall 必须被判为不合格", bad["Theme.Blancall"]?.get("android:dialogTheme"))
        assertEquals(
            "对话框主题若仍指向系统动画样式，必须被判为不合格",
            "@style/Animation.Dialog",
            bad["Theme.Blancall.Dialog"]?.get("android:windowAnimationStyle")
        )
        assertNull(
            "未自我声明 dialogTheme 的样式必须被判为不合格",
            bad["Theme.Blancall.Dialog"]?.get("android:dialogTheme")
        )
    }

    private fun parse(file: File): Map<String, Map<String, String>> {
        assertTrue("找不到 ${file.absolutePath} —— 测试工作目录变了，这个守卫已经失效", file.isFile)
        return parseXml(file.readText())
    }

    private fun parseXml(xml: String): Map<String, Map<String, String>> =
        STYLE.findAll(xml).associate { style ->
            style.groupValues[1] to ITEM.findAll(style.groupValues[2]).associate { item ->
                item.groupValues[1] to item.groupValues[2].trim()
            }
        }

    private companion object {
        const val DAY = "src/main/res/values/themes.xml"
        const val NIGHT = "src/main/res/values-night/themes.xml"
        const val DialogStyle = "@style/Theme.Blancall.Dialog"

        val STYLE = Regex("""<style\s+name="([^"]+)"[^>]*>(.*?)</style>""", RegexOption.DOT_MATCHES_ALL)
        val ITEM = Regex("""<item\s+name="([^"]+)"[^>]*>(.*?)</item>""", RegexOption.DOT_MATCHES_ALL)
    }
}
