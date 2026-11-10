// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * 划线/批注的正文指纹。
 *
 * 划线存的是「段落索引 + 段内偏移」，正文一改就整体漂移：高亮画到别的字上、
 * 批注挂到不相干的句子后面，界面上完全看不出来 —— 比丢失更难被发现。
 * 这里守三件事：
 * 1. 没有标注的文章不写指纹（不为「进过阅读模式」制造写盘）；
 * 2. 首次校验记下基准，之后同一正文不再报警；
 * 3. 正文变化后校验必须返回 true（UI 据此提示用户）。
 */
class ReadingMarkContentHashTest {

    private fun newDir(): File = Files.createTempDirectory("blancall-mark-hash").toFile()

    private fun storeIn(dir: File) = ReadingMarkStore.openForTest(File(dir, "reading_marks.json"))

    @Test
    fun `没有标注的文章不写指纹`() {
        val dir = newDir()
        val store = storeIn(dir)
        assertFalse(store.checkContentHash(42, "hash-a"))
        assertFalse(
            "没有标注就不该因为校验而落盘",
            File(dir, "reading_marks.json").exists(),
        )
    }

    @Test
    fun `首次校验记录基准 重复校验不再报警`() {
        val store = storeIn(newDir())
        store.addMark(7, paragraph = 0, start = 0, end = 3)
        assertFalse(store.checkContentHash(7, "hash-a"))
        assertFalse("同一正文不应反复报警", store.checkContentHash(7, "hash-a"))
    }

    @Test
    fun `正文变化后校验返回 true`() {
        val store = storeIn(newDir())
        store.addMark(7, paragraph = 0, start = 0, end = 3)
        store.checkContentHash(7, "hash-a")
        assertTrue(store.checkContentHash(7, "hash-b"))
    }

    @Test
    fun `指纹能跨实例读回`() {
        val dir = newDir()
        val store = storeIn(dir)
        store.addMark(7, paragraph = 0, start = 0, end = 3)
        store.checkContentHash(7, "hash-a")

        val reopened = storeIn(dir)
        assertFalse("基准应当从磁盘读回", reopened.checkContentHash(7, "hash-a"))
        assertTrue(reopened.checkContentHash(7, "hash-b"))
    }

    @Test
    fun `其它文章的标注与指纹互不影响`() {
        val dir = newDir()
        val store = storeIn(dir)
        store.addMark(1, paragraph = 0, start = 0, end = 2)
        store.addMark(2, paragraph = 0, start = 0, end = 2)
        store.checkContentHash(1, "h1")
        store.checkContentHash(2, "h2")

        assertTrue(store.checkContentHash(1, "changed"))
        assertFalse(store.checkContentHash(2, "h2"))
    }
}
