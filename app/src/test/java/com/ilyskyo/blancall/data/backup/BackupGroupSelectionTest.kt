// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * 「选择要备份的数据类别」的语义守卫。
 *
 * 两件事必须成立：
 * 1. 导出只包含勾选的类目（未勾选的**一个字节都不进包**）；
 * 2. 导入**只替换备份里包含的类目**，磁盘上其它类目的数据原样保留。
 *
 * 第 2 条是这次改动里最要紧的一条：旧实现是整库替换 + 按全量清单清孤儿，
 * 于是「只备份了文章」的包一恢复，记忆进度、划线、标签全被当成残留删掉。
 */
class BackupGroupSelectionTest {

    private fun tempDir(name: String): File =
        Files.createTempDirectory("blancall-group-$name").toFile()

    private fun seed(dir: File) {
        File(dir, "articles.json").writeText("""[{"id":1}]""")
        File(dir, "records.json").writeText("""{"a":1}""")
        File(dir, "reading_marks.json").writeText("""{"version":1,"articles":{}}""")
        File(dir, "ink").mkdirs()
        File(dir, "ink/r1.json").writeText("""{"v":1}""")
    }

    @Test
    fun `只导出勾选的类目`() {
        val dir = tempDir("export")
        seed(dir)
        val names = BackupManager.collectBackupFiles(
            dir, setOf(BackupManager.Group.ARTICLES),
        ).map { it.first }
        assertEquals(listOf("files/articles.json"), names)
    }

    @Test
    fun `勾选多个类目时按固定顺序输出`() {
        val dir = tempDir("multi")
        seed(dir)
        val names = BackupManager.collectBackupFiles(
            dir,
            setOf(BackupManager.Group.INK, BackupManager.Group.ARTICLES),
        ).map { it.first }
        // 顺序由 enum 声明决定，不随 Set 的迭代顺序变化
        assertEquals(listOf("files/articles.json", "files/ink/r1.json"), names)
    }

    @Test
    fun `内容包与 AI 历史可以进备份`() {
        val dir = tempDir("packs")
        File(dir, "content_packs/p1").mkdirs()
        File(dir, "content_packs/p1/pack.json").writeText("""{"id":"p1"}""")
        File(dir, "content_packs/p1/e0.txt").writeText("正文")
        File(dir, "ai_history").mkdirs()
        File(dir, "ai_history/1.json").writeText("""{"id":1}""")

        val packNames = BackupManager.collectBackupFiles(
            dir, setOf(BackupManager.Group.ARTICLES),
        ).map { it.first }
        assertTrue("内容包属于「文章与内容包」类目", packNames.any { it.contains("content_packs/p1/pack.json") })

        val aiNames = BackupManager.collectBackupFiles(
            dir, setOf(BackupManager.Group.AI),
        ).map { it.first }
        assertTrue(aiNames.contains("files/ai_history/1.json"))
    }

    @Test
    fun `导入只替换备份里的类目 其它类目原样保留`() {
        val src = tempDir("src")
        File(src, "articles.json").writeText("""[{"id":2}]""")
        val zip = File(tempDir("zip"), "b.zip")
        BackupManager.exportZip(
            src, zip.outputStream(), setOf(BackupManager.Group.ARTICLES),
        )

        val dst = tempDir("dst")
        File(dst, "articles.json").writeText("""[{"id":1}]""")
        File(dst, "records.json").writeText("""{"keep":"me"}""")
        File(dst, "reading_marks.json").writeText("""{"version":1,"articles":{}}""")

        val report = BackupManager.restoreZip(zip, dst)

        assertEquals("""[{"id":2}]""", File(dst, "articles.json").readText())
        // 未包含在备份里的类目，一个字节都不能动
        assertEquals("""{"keep":"me"}""", File(dst, "records.json").readText())
        assertEquals(
            """{"version":1,"articles":{}}""",
            File(dst, "reading_marks.json").readText(),
        )
        assertEquals(1, report.groups)
    }

    @Test
    fun `同属一个类目的旧数据仍按备份内容替换`() {
        val src = tempDir("src2")
        File(src, "articles.json").writeText("""[{"id":9}]""")
        val zip = File(tempDir("zip2"), "b2.zip")
        BackupManager.exportZip(
            src, zip.outputStream(), setOf(BackupManager.Group.ARTICLES),
        )

        val dst = tempDir("dst2")
        File(dst, "articles.json").writeText("""[{"id":1}]""")
        // 内容包里多出来的旧条目：属于同一类目，应当被清掉（整类替换语义）
        File(dst, "content_packs/old").mkdirs()
        File(dst, "content_packs/old/pack.json").writeText("""{"id":"old"}""")

        val report = BackupManager.restoreZip(zip, dst)

        assertEquals("""[{"id":9}]""", File(dst, "articles.json").readText())
        assertFalse(
            "同类目内的旧残留要被替换掉",
            File(dst, "content_packs/old/pack.json").exists(),
        )
        assertTrue("清掉的条目要计数", report.removed >= 1)
    }
}
