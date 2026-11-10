// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.model.Article
import com.ilyskyo.blancall.data.model.PracticeRecord
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.ZoneId

/**
 * 只读记忆探针：读取一份 filesDir 快照（articles.json + fsrs_state.json），
 * 用真实的 ArticleMemory / ArticleMemoryShadow 输出覆盖率与新旧投影差异。
 *
 * 真实数据：把设备 filesDir 里的两个文件拉到同一目录，设置环境变量
 * `MEMORY_PROBE_DIR` 后运行本测试；报告写到该目录的 `memory-probe.txt`。
 * 没有环境变量时本测试跳过，不影响常规全量测试。
 */
class MemoryProbeTest {

    @Test
    fun `合成数据能算出覆盖率与影子差异`() {
        val dir = Files.createTempDirectory("blancall-memory-probe").toFile()
        val articles = listOf(
            Article(1L, "article-1", "春眠不觉晓处处闻啼鸟。夜来风雨声花落知多少。"),
            Article(2L, "article-2", "床前明月光疑是地上霜。举头望明月低头思故乡。"),
        )
        writeArticles(File(dir, "articles.json"), articles)
        val first = SentenceSelector.candidates(articles[0]).first()
        writeStates(
            File(dir, "fsrs_state.json"),
            articleStates = mapOf(1L to state(stability = 20.0, due = now + 20 * day)),
            sentenceStates = mapOf(first.key to state(stability = 10.0, due = now - day)),
        )
        // article-2 没有 FSRS 状态，但有一条历史记录：探针应走 records 回退，不能报成 NEW。
        writeRecords(File(dir, "records.json"), listOf(record(articleId = 2L, timestamp = now - 5 * day)))

        val report = probe(dir, now)

        assertTrue(report.contains("articles=2"))
        assertTrue(report.contains("article-1"))
        assertTrue(report.contains("learned=1/2"))
        assertTrue(report.contains("article-2 | LEGACY/LEGACY_ARTICLE"))
        assertTrue(report.contains("shadow="))
        assertTrue(File(dir, "memory-probe.txt").isFile)
    }

    @Test
    fun `真实数据目录可用时输出报告`() {
        val path = System.getenv("MEMORY_PROBE_DIR")
        assumeTrue("未设置 MEMORY_PROBE_DIR，跳过真实数据探针", !path.isNullOrBlank())
        val dir = File(requireNotNull(path))
        assumeTrue("MEMORY_PROBE_DIR 不是目录：$path", dir.isDirectory)
        assumeTrue(
            "目录缺少 articles.json / fsrs_state.json：$path",
            File(dir, "articles.json").isFile && File(dir, "fsrs_state.json").isFile,
        )

        val report = probe(dir)

        println(report)
        assertTrue(report.contains("articles="))
    }

    private fun probe(dir: File, at: Long = System.currentTimeMillis()): String {
        val articles = readArticles(File(dir, "articles.json"))
        val parsed = readStates(File(dir, "fsrs_state.json"))
        val records = readRecords(File(dir, "records.json"))
        val summaries = ArticleMemoryReads.views(
            articles = articles,
            legacyStates = parsed.articles,
            sentenceStates = parsed.sentences,
            recordsByArticle = records.groupBy { it.articleId },
            template = ReviewTemplate.STANDARD,
            pausedArticleIds = parsed.paused,
            enrolledAt = parsed.enrolled,
            now = at,
            zone = ZoneId.systemDefault(),
            model = ArticleMemoryReads.Model.SENTENCE_PROJECTION,
        )
        val diffs = ArticleMemoryShadow.compare(
            articles = articles,
            legacyStates = parsed.articles,
            sentenceStates = parsed.sentences,
            pausedArticleIds = parsed.paused,
            now = at,
        )
        val report = buildString {
            appendLine("memory-probe")
            appendLine("articles=${articles.size}")
            appendLine(
                "eligible=${summaries.sumOf { it.eligible }} " +
                    "learned=${summaries.sumOf { it.learned }} " +
                    "dueArticles=${summaries.count { it.dueCount > 0 }} " +
                    "dueSentences=${summaries.sumOf { it.dueCount }} " +
                    "legacy=${summaries.count { it.source == ArticleMemory.CurveSource.LEGACY_ARTICLE }}"
            )
            for (s in summaries) {
                appendLine(
                    "  ${s.articleId} ${s.title} | ${s.state}/${s.source} | " +
                        "learned=${s.learned}/${s.eligible} new=${s.newCount} due=${s.dueCount} " +
                        "retention=${s.averageRetention ?: "-"} weakest=${s.weakestRetention ?: "-"}"
                )
            }
            appendLine("shadow=${diffs.size}")
            append(ArticleMemoryShadow.report(diffs, at))
        }
        File(dir, "memory-probe.txt").writeText(report)
        return report
    }

    private data class ParsedStates(
        val articles: Map<Long, FsrsEngine.CardState>,
        val sentences: Map<String, FsrsEngine.CardState>,
        val paused: Set<Long>,
        val enrolled: Map<Long, Long>,
    )

    private fun readArticles(file: File): List<Article> {
        val array = JSONArray(file.readText())
        return (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optLong("id", 0L)
            val title = o.optString("title")
            val content = o.optString("content")
            if (id <= 0L || content.isBlank()) null else Article(id = id, title = title, content = content)
        }
    }

    private fun readStates(file: File): ParsedStates {
        val root = JSONObject(file.readText())
        val articles = HashMap<Long, FsrsEngine.CardState>()
        val sentences = HashMap<String, FsrsEngine.CardState>()
        val paused = HashSet<Long>()
        val enrolled = HashMap<Long, Long>()
        root.keys().forEach { key ->
            when {
                key == "version" -> Unit
                key == "paused" -> root.optJSONObject(key)?.keys()?.forEach { id ->
                    id.toLongOrNull()?.let { paused += it }
                }

                key == "enrolled" -> root.optJSONObject(key)?.keys()?.forEach { id ->
                    id.toLongOrNull()?.let { enrolled[it] = root.optJSONObject(key)!!.optLong(id, 0L) }
                }

                key.startsWith(SentenceSelector.SENTENCE_KEY_PREFIX) -> {
                    root.optJSONObject(key)?.let { sentences[key] = stateOf(it) }
                }

                else -> key.toLongOrNull()?.let { id ->
                    root.optJSONObject(key)?.let { articles[id] = stateOf(it) }
                }
            }
        }
        return ParsedStates(articles, sentences, paused, enrolled)
    }

    private fun stateOf(o: JSONObject) = FsrsEngine.CardState(
        difficulty = o.optDouble("difficulty", 0.0),
        stability = o.optDouble("stability", 0.0),
        due = o.optLong("due", 0L),
        lastReview = o.optLong("lastReview", 0L),
        reviewCount = o.optInt("reviewCount", 0),
        lapses = o.optInt("lapses", 0),
        lastRating = o.optString("lastRating", ""),
    )

    private fun writeArticles(file: File, articles: List<Article>) {
        val array = JSONArray()
        for (a in articles) {
            array.put(
                JSONObject()
                    .put("id", a.id)
                    .put("title", a.title)
                    .put("content", a.content)
            )
        }
        file.writeText(array.toString())
    }

    private fun writeStates(
        file: File,
        articleStates: Map<Long, FsrsEngine.CardState>,
        sentenceStates: Map<String, FsrsEngine.CardState>,
        enrolled: Map<Long, Long> = emptyMap(),
    ) {
        val root = JSONObject().put("version", 3)
        articleStates.forEach { (id, s) -> root.put(id.toString(), stateJson(s)) }
        sentenceStates.forEach { (key, s) -> root.put(key, stateJson(s)) }
        if (enrolled.isNotEmpty()) {
            val map = JSONObject()
            enrolled.forEach { (id, at) -> map.put(id.toString(), at) }
            root.put("enrolled", map)
        }
        file.writeText(root.toString())
    }

    private fun writeRecords(file: File, records: List<PracticeRecord>) {
        file.writeText(
            records.joinToString(separator = "\n", postfix = "\n") { r ->
                JSONObject()
                    .put("articleId", r.articleId)
                    .put("mode", r.mode)
                    .put("totalBlanks", r.totalBlanks)
                    .put("correctCount", r.correctCount)
                    .put("timestamp", r.timestamp)
                    .put("rating", r.rating)
                    .toString()
            }
        )
    }

    private fun readRecords(file: File): List<PracticeRecord> {
        if (!file.isFile) return emptyList()
        val text = file.readText().trim()
        if (text.isEmpty()) return emptyList()
        val out = mutableListOf<PracticeRecord>()
        if (text.startsWith("[")) {
            val array = JSONArray(text)
            for (i in 0 until array.length()) {
                array.optJSONObject(i)?.let { recordOf(it)?.let(out::add) }
            }
        } else {
            text.lineSequence().forEach { line ->
                val raw = line.trim()
                if (raw.isNotEmpty()) {
                    runCatching { recordOf(JSONObject(raw))?.let(out::add) }
                }
            }
        }
        return out
    }

    private fun recordOf(o: JSONObject): PracticeRecord? {
        val articleId = o.optLong("articleId", 0L)
        if (articleId <= 0L) return null
        return PracticeRecord(
            articleId = articleId,
            mode = o.optString("mode", "SENTENCE"),
            totalBlanks = o.optInt("totalBlanks", 0),
            correctCount = o.optInt("correctCount", 0),
            timestamp = o.optLong("timestamp", 0L),
            similarity = o.optDouble("similarity", 0.0).toFloat(),
            rating = o.optInt("rating", 0),
        )
    }

    private fun record(articleId: Long, timestamp: Long) = PracticeRecord(
        articleId = articleId,
        mode = "SENTENCE",
        totalBlanks = 10,
        correctCount = 8,
        timestamp = timestamp,
    )

    private fun stateJson(s: FsrsEngine.CardState) = JSONObject()
        .put("difficulty", s.difficulty)
        .put("stability", s.stability)
        .put("due", s.due)
        .put("lastReview", s.lastReview)
        .put("reviewCount", s.reviewCount)
        .put("lapses", s.lapses)

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun state(
        stability: Double,
        due: Long,
        lastReview: Long = now - 5 * day,
        reviewCount: Int = 1,
    ) = FsrsEngine.CardState(
        difficulty = 5.0,
        stability = stability,
        due = due,
        lastReview = lastReview,
        reviewCount = reviewCount,
    )
}