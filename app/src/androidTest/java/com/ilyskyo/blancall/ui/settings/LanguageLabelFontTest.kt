// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ilyskyo.blancall.ui.common.AppLocale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 语言名固定字形的资源守卫（设备测试）。
 *
 * 为什么必须落到设备上：字体是否真的带上对应字面、子集有没有漏字，只有让
 * 系统字体引擎加载并渲染才能量到。JVM 侧只检查得到资源名。
 *
 * 用 TextView 而不是 Paint.drawText：Paint 直接画自定义 Typeface 时缺字不会走
 * 字体回退，量出来的「简繁相同」是渲染路径的假象，不是字体本身。
 */
@RunWith(AndroidJUnit4::class)
class LanguageLabelFontTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun 每个语言名都有可加载的固定字形字体() {
        for (choice in AppLocale.entries) {
            if (choice == AppLocale.FollowSystem) continue
            val res = languageLabelFontRes(choice)
            assertNotEquals("${choice.name} 没有对应字体资源", 0, res)
            assertNotNull("${choice.name} 的字体资源加载失败", context.resources.getFont(res))
        }
    }

    @Test
    fun 简繁日三套字形互不相同() {
        val sc = render(AppLocale.ChineseSimplified, "骨")
        val tc = render(AppLocale.ChineseTraditional, "骨")
        val jp = render(AppLocale.Japanese, "骨")
        assertFalse("简繁字形相同，子集没有取到各自字面", sc.sameAs(tc))
        assertFalse("简日字形相同，子集没有取到各自字面", sc.sameAs(jp))
        assertFalse("繁日字形相同，子集没有取到各自字面", tc.sameAs(jp))
    }

    private fun render(choice: AppLocale, text: String): Bitmap {
        val typeface = context.resources.getFont(languageLabelFontRes(choice))
        val view = TextView(context).apply {
            this.typeface = typeface
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 72f)
            this.text = text
        }
        val width = View.MeasureSpec.makeMeasureSpec(160, View.MeasureSpec.EXACTLY)
        val height = View.MeasureSpec.makeMeasureSpec(110, View.MeasureSpec.EXACTLY)
        view.measure(width, height)
        view.layout(0, 0, 160, 110)
        val bitmap = Bitmap.createBitmap(160, 110, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }
}
