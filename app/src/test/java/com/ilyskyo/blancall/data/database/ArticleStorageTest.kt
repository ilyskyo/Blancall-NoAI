// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.data.database

import com.ilyskyo.blancall.data.model.Article
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ArticleStorageTest {

    private fun newFile(): File =
        File(Files.createTempDirectory("blancall-articles").toFile(), "articles.json")

    @Test
    fun `批量插入一次分配连续 id`() = runBlocking {
        val storage = ArticleStorage(newFile().absolutePath)
        storage.awaitLoaded()
        val ids = storage.insertAll(
            listOf(
                Article(title = "甲", content = "甲文"),
                Article(title = "乙", content = "乙文"),
            )
        )
        assertEquals(listOf(1L, 2L), ids)
        assertEquals(2, storage.articles.first().size)
    }

    @Test
    fun `删除最大 id 后重启不复用旧 id`() = runBlocking {
        val file = newFile()
        val first = ArticleStorage(file.absolutePath)
        first.awaitLoaded()
        val ids = first.insertAll(
            listOf(Article(title = "甲", content = "甲文"), Article(title = "乙", content = "乙文"))
        )
        first.deleteAll(setOf(ids.last()))
        // 模拟重启：新实例重新从磁盘加载，旧实现会把 nextId 算回 maxId+1 = 2
        val second = ArticleStorage(file.absolutePath)
        second.awaitLoaded()
        val newId = second.insert(Article(title = "丙", content = "丙文"))
        assertTrue("新文章不能复用刚删除的旧 id", newId > ids.last())
    }
}
