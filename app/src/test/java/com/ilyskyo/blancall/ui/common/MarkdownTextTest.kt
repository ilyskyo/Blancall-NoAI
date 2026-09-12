// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * [classifyBlock] / [indentLevel] 的回归测试。
 *
 * 为什么只测判定而不测渲染：渲染是 @Composable，JVM 单测跑不起来；但"某一行会走哪个分支"
 * 是纯函数，把它钉死即可覆盖两类风险：
 * ① 新增语法分支误伤既有文档（PRIVACY.md 必须仍只走 标题/列表/正文 这几支）；
 * ② 新增语法的识别本身写错（如把 "2026. 年" 误判成有序列表）。
 */
class MarkdownTextTest {

    // ── ① 既有文档不被新分支误伤（拿真实的 PRIVACY.md 逐行验证）──

    private fun locatePrivacy(): File? {
        val candidates = listOf(
            "src/main/assets/PRIVACY.md",              // 工作目录 = app 模块
            "app/src/main/assets/PRIVACY.md",          // 工作目录 = 项目根
            "../app/src/main/assets/PRIVACY.md",
            "../../app/src/main/assets/PRIVACY.md"
        )
        return candidates.map { File(it) }.firstOrNull { it.isFile }
    }

    @Test
    fun `真实隐私政策只使用受支持的块级语法`() {
        val f = locatePrivacy()
        assumeTrue("未能定位 PRIVACY.md，跳过真实文件校验（其余用例仍会验证判定逻辑）", f != null)

        val allowed = setOf(
            MdBlock.BLANK, MdBlock.HEADING1, MdBlock.HEADING2, MdBlock.HEADING3,
            MdBlock.BULLET, MdBlock.PARAGRAPH
        )
        val unexpected = mutableListOf<String>()
        f!!.readLines().forEachIndexed { i, line ->
            if (classifyBlock(line) !in allowed) unexpected.add("第 ${i + 1} 行: $line")
        }
        assertTrue(
            "隐私政策出现了预期外的块级语法（说明新分支误伤或文档新增了语法，需人工确认）：\n" +
                unexpected.joinToString("\n"),
            unexpected.isEmpty()
        )
    }

    @Test
    fun `隐私政策仍能识别出标题与列表`() {
        val f = locatePrivacy()
        assumeTrue("未能定位 PRIVACY.md", f != null)
        val kinds = f!!.readLines().map { classifyBlock(it) }
        assertTrue("应至少识别出一级标题", kinds.any { it == MdBlock.HEADING1 })
        assertTrue("应至少识别出二级标题", kinds.any { it == MdBlock.HEADING2 })
        assertTrue("应至少识别出列表项", kinds.any { it == MdBlock.BULLET })
    }

    // ── ② 新增语法识别 ──

    @Test
    fun `四级标题识别`() {
        assertEquals(MdBlock.HEADING4, classifyBlock("#### 四级标题"))
        assertEquals(MdBlock.HEADING3, classifyBlock("### 三级"))
        assertEquals(MdBlock.HEADING2, classifyBlock("## 二级"))
        assertEquals(MdBlock.HEADING1, classifyBlock("# 一级"))
    }

    @Test
    fun `分隔线识别三种写法`() {
        assertEquals(MdBlock.THEMATIC_BREAK, classifyBlock("---"))
        assertEquals(MdBlock.THEMATIC_BREAK, classifyBlock("***"))
        assertEquals(MdBlock.THEMATIC_BREAK, classifyBlock("___"))
        assertEquals(MdBlock.THEMATIC_BREAK, classifyBlock("- - -"))
        assertEquals(MdBlock.THEMATIC_BREAK, classifyBlock("--------"))
    }

    @Test
    fun `不足三个符号与有内容的行不算分隔线`() {
        assertEquals(MdBlock.PARAGRAPH, classifyBlock("--"))
        assertEquals(MdBlock.PARAGRAPH, classifyBlock("**"))
        assertEquals(MdBlock.PARAGRAPH, classifyBlock("--- 说明文字"))
        assertEquals(MdBlock.PARAGRAPH, classifyBlock("**粗体开头的正文**"))
    }

    @Test
    fun `引用块识别`() {
        assertEquals(MdBlock.QUOTE, classifyBlock("> 引用内容"))
        assertEquals(MdBlock.QUOTE, classifyBlock(">"))
        assertEquals(MdBlock.QUOTE, classifyBlock("  > 缩进的引用"))
    }

    @Test
    fun `无序列表识别三种标记`() {
        assertEquals(MdBlock.BULLET, classifyBlock("- 项目"))
        assertEquals(MdBlock.BULLET, classifyBlock("* 项目"))
        assertEquals(MdBlock.BULLET, classifyBlock("+ 项目"))
        assertEquals(MdBlock.BULLET, classifyBlock("  - 缩进项目"))
    }

    @Test
    fun `有序列表识别并保留编号`() {
        assertEquals(MdBlock.ORDERED, classifyBlock("1. 第一项"))
        assertEquals(MdBlock.ORDERED, classifyBlock("2) 第二项"))
        assertEquals(MdBlock.ORDERED, classifyBlock("10. 第十项"))
        assertEquals(MdBlock.ORDERED, classifyBlock("  3. 缩进项"))
    }

    @Test
    fun `疑似有序列表的正文不被误判`() {
        // 四位数字（年份）不匹配：只有 1~2 位数字才会被判成列表
        assertEquals(MdBlock.PARAGRAPH, classifyBlock("2026. 年我们做了很多事"))
        // 数字后没有空格不是列表
        assertEquals(MdBlock.PARAGRAPH, classifyBlock("1.第一项"))
        // 要求以数字开头，中间出现不算
        assertEquals(MdBlock.PARAGRAPH, classifyBlock("版本 1. 说明"))
    }

    @Test
    fun `缺少空格的井号不是标题`() {
        assertEquals(MdBlock.PARAGRAPH, classifyBlock("#没有空格"))
        assertEquals(MdBlock.PARAGRAPH, classifyBlock("###没有空格"))
        assertEquals(MdBlock.HEADING1, classifyBlock("# 有空格"))
    }

    @Test
    fun `空行与纯空白判定为分段`() {
        assertEquals(MdBlock.BLANK, classifyBlock(""))
        assertEquals(MdBlock.BLANK, classifyBlock("   "))
        assertEquals(MdBlock.BLANK, classifyBlock("\t"))
    }

    // ── ③ 缩进层级 ──

    @Test
    fun `缩进层级按两空格一级且最多三级`() {
        assertEquals(0, indentLevel("无缩进"))
        assertEquals(1, indentLevel("  两空格"))
        assertEquals(2, indentLevel("    四空格"))
        assertEquals(3, indentLevel("      六空格"))
        assertEquals(3, indentLevel("            十二空格"))
        assertEquals(0, indentLevel("- 顶格列表"))
    }
}
