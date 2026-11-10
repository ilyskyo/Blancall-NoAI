// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.model.Article

/**
 * 篇级记忆投影：句子级 [FsrsEngine.CardState] 是唯一调度事实，篇级只做聚合展示。
 *
 * 为什么不再用篇级 FSRS 卡直接调度：一次文章练习默认只挖约 1/3 分句，
 * 用这一场的平均相似度推进整篇，会把「没测过」当成「记住了」。
 * 这里只回答「这篇里已学的句子现在怎么样」，不产生新的记忆状态。
 *
 * 旧数据：没有句级状态的文章仍可用旧 `a:` 状态回退（[ArticleState.LEGACY]）；
 * `newCount` 在旧口径下置 0 —— 旧记录没有逐句覆盖信息，不能假装知道哪些句是新句。
 */
object ArticleMemory {

    enum class ArticleState { NEW, LEARNING, DUE, CAUGHT_UP, PAUSED, LEGACY }

    enum class CurveSource { SENTENCE_AGGREGATE, LEGACY_ARTICLE, NONE }

    data class Summary(
        val articleId: Long,
        val title: String,
        val eligible: Int,
        val learned: Int,
        val newCount: Int,
        val dueCount: Int,
        val earliestDue: Long,
        val averageRetention: Float?,
        val weakestRetention: Float?,
        val lastReview: Long,
        val state: ArticleState,
        val source: CurveSource,
        /** 旧口径回退时，旧 `a:` 卡当前是否到期；句级投影恒为 false。 */
        val legacyDue: Boolean = false,
        /** 加入计划时刻（>0 且没有任何记忆状态 = 计划中、今天该开始第一遍）。 */
        val enrolledAt: Long = 0L,
        /** 句级复习次数取最大；旧口径用旧卡的 reviewCount。 */
        val practiceCount: Int = 0,
        /** 句级稳定度按内容字数加权；旧口径用旧卡的 stability。 */
        val averageStability: Float? = null,
    )

    /**
     * 聚合曲线：第 0..days-1 天。
     * [mean] 按内容字数加权，[weakest] 取最低句；旧口径只有一条线。
     */
    data class Curve(
        val mean: List<Float>,
        val weakest: List<Float>,
        val p25: List<Float>?,
        val p75: List<Float>?,
        val target: Float,
        val source: CurveSource,
    )

    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

    /** 0..30 天共 31 个点，与现有预测卡/单篇曲线一致。 */
    const val DEFAULT_CURVE_DAYS = 31

    /** 少于 4 句时不画分位带：样本太少，p25/p75 只是噪声。 */
    private const val MIN_QUANTILE_SAMPLE = 4

    /** 列表批量投影；输出已按展示顺序排序。 */
    fun summaries(
        articles: List<Article>,
        sentenceStates: Map<String, FsrsEngine.CardState>,
        legacyStates: Map<Long, FsrsEngine.CardState> = emptyMap(),
        pausedArticleIds: Set<Long> = emptySet(),
        now: Long = System.currentTimeMillis(),
    ): List<Summary> = articles
        .map { article ->
            summary(
                article = article,
                sentenceStates = sentenceStates,
                legacyState = legacyStates[article.id],
                paused = article.id in pausedArticleIds,
                now = now,
            )
        }
        .sortedWith(displayOrder)

    fun summary(
        article: Article,
        sentenceStates: Map<String, FsrsEngine.CardState>,
        legacyState: FsrsEngine.CardState? = null,
        paused: Boolean = false,
        enrolledAt: Long = 0L,
        now: Long = System.currentTimeMillis(),
        /** 仅当没有任何句级状态时才求值：records 反推很贵，投影文章不该白算一遍。 */
        legacyFallback: (() -> FsrsEngine.CardState?)? = null,
    ): Summary {
        val candidates = SentenceSelector.candidates(article)
        var learned = 0
        var dueCount = 0
        var earliestDue = Long.MAX_VALUE
        var lastReview = 0L
        var weightedRetention = 0.0
        var weightedStability = 0.0
        var weightSum = 0.0
        var weakest = Float.MAX_VALUE
        var maxReviews = 0

        for (pick in candidates) {
            val state = sentenceStates[pick.key] ?: continue
            learned++
            val retention = FsrsEngine.retentionRate(state, now).toFloat()
            val weight = contentWeight(pick.text)
            weightedRetention += retention * weight
            weightedStability += state.stability * weight
            weightSum += weight
            if (retention < weakest) weakest = retention
            if (state.reviewCount > maxReviews) maxReviews = state.reviewCount
            // due=0 表示「没有下次到期」（模板跑完/无排期），不是 1970 年到期。
            if (state.due > 0L && FsrsEngine.isDue(state, now)) dueCount++
            if (state.due > 0L && state.due < earliestDue) earliestDue = state.due
            if (state.lastReview > lastReview) lastReview = state.lastReview
        }

        val legacy = if (learned == 0) legacyState ?: legacyFallback?.invoke() else null
        val legacyRetention = legacy?.let {
            FsrsEngine.retentionRate(it, now).toFloat()
        }
        val source = when {
            learned > 0 -> CurveSource.SENTENCE_AGGREGATE
            legacy != null -> CurveSource.LEGACY_ARTICLE
            else -> CurveSource.NONE
        }
        val state = when {
            paused -> ArticleState.PAUSED
            source == CurveSource.LEGACY_ARTICLE -> ArticleState.LEGACY
            learned == 0 -> ArticleState.NEW
            dueCount > 0 -> ArticleState.DUE
            learned < candidates.size -> ArticleState.LEARNING
            else -> ArticleState.CAUGHT_UP
        }
        val legacyDue = legacy != null && legacy.due > 0L && FsrsEngine.isDue(legacy, now)

        return Summary(
            articleId = article.id,
            title = article.title,
            // 旧口径没有逐句覆盖信息：newCount 保持 0，避免把「不知道」显示成「20 句新」。
            eligible = candidates.size,
            learned = learned,
            newCount = if (source == CurveSource.LEGACY_ARTICLE) 0 else candidates.size - learned,
            dueCount = dueCount,
            earliestDue = if (learned > 0) {
                if (earliestDue == Long.MAX_VALUE) 0L else earliestDue
            } else {
                legacy?.due ?: enrolledAt.coerceAtLeast(0L)
            },
            averageRetention = if (learned > 0 && weightSum > 0.0) {
                (weightedRetention / weightSum).toFloat()
            } else {
                legacyRetention
            },
            weakestRetention = if (learned > 0) weakest else legacyRetention,
            lastReview = if (learned > 0) lastReview else legacy?.lastReview ?: 0L,
            state = state,
            source = source,
            legacyDue = legacyDue,
            enrolledAt = if (learned == 0 && legacy == null) enrolledAt.coerceAtLeast(0L) else 0L,
            practiceCount = if (learned > 0) maxReviews else legacy?.reviewCount ?: 0,
            averageStability = if (learned > 0 && weightSum > 0.0) {
                (weightedStability / weightSum).toFloat()
            } else {
                legacy?.stability?.toFloat()
            },
        )
    }

    /**
     * 单篇聚合曲线。新句不进曲线；没有句级状态时用旧 `a:` 回退，
     * 两者都没有则返回空序列（source = NONE）。
     */
    fun curve(
        article: Article,
        sentenceStates: Map<String, FsrsEngine.CardState>,
        legacyState: FsrsEngine.CardState? = null,
        now: Long = System.currentTimeMillis(),
        days: Int = DEFAULT_CURVE_DAYS,
        target: Float = FsrsEngine.targetRetention().toFloat(),
    ): Curve {
        require(days > 0) { "days must be positive" }

        val learned = SentenceSelector.candidates(article).mapNotNull { pick ->
            sentenceStates[pick.key]?.let { pick to it }
        }

        if (learned.isEmpty()) {
            if (legacyState == null) {
                return Curve(
                    mean = emptyList(),
                    weakest = emptyList(),
                    p25 = null,
                    p75 = null,
                    target = target,
                    source = CurveSource.NONE,
                )
            }
            val series = List(days) { day ->
                FsrsEngine.retentionRate(legacyState, now + day * MILLIS_PER_DAY).toFloat()
            }
            return Curve(
                mean = series,
                weakest = series,
                p25 = null,
                p75 = null,
                target = target,
                source = CurveSource.LEGACY_ARTICLE,
            )
        }

        val weights = learned.map { contentWeight(it.first.text) }
        val mean = ArrayList<Float>(days)
        val weakest = ArrayList<Float>(days)
        val p25 = if (learned.size >= MIN_QUANTILE_SAMPLE) ArrayList<Float>(days) else null
        val p75 = if (learned.size >= MIN_QUANTILE_SAMPLE) ArrayList<Float>(days) else null

        for (day in 0 until days) {
            val at = now + day * MILLIS_PER_DAY
            val values = ArrayList<Pair<Float, Double>>(learned.size)
            var sum = 0.0
            var weightSum = 0.0
            var min = Float.MAX_VALUE
            for (i in learned.indices) {
                val value = FsrsEngine.retentionRate(learned[i].second, at).toFloat()
                val weight = weights[i]
                values += value to weight
                sum += value * weight
                weightSum += weight
                if (value < min) min = value
            }
            mean += (sum / weightSum).toFloat()
            weakest += min
            if (p25 != null && p75 != null) {
                p25 += weightedQuantile(values, 0.25f)
                p75 += weightedQuantile(values, 0.75f)
            }
        }

        return Curve(
            mean = mean,
            weakest = weakest,
            p25 = p25,
            p75 = p75,
            target = target,
            source = CurveSource.SENTENCE_AGGREGATE,
        )
    }

    /**
     * 内容权重：非标点字符数（汉字/字母/数字），至少 1。
     * 不按总长度：标点和空白不承载记忆，不能因为标点多就让短句权重更高。
     */
    private fun contentWeight(text: String): Double =
        text.count { it.isLetterOrDigit() }.coerceAtLeast(1).toDouble()

    /** 加权分位：按值排序后累计权重，取第一个达到 q 总权重的值。 */
    private fun weightedQuantile(values: List<Pair<Float, Double>>, q: Float): Float {
        val sorted = values.sortedBy { it.first }
        val total = sorted.sumOf { it.second }
        if (total <= 0.0) return sorted.first().first
        val threshold = total * q
        var acc = 0.0
        for ((value, weight) in sorted) {
            acc += weight
            if (acc >= threshold) return value
        }
        return sorted.last().first
    }

    private val displayOrder: Comparator<Summary> =
        compareBy<Summary> { if (it.state == ArticleState.DUE || it.legacyDue) 0 else 1 }
            .thenBy { if (it.earliestDue > 0L) it.earliestDue else Long.MAX_VALUE }
            .thenByDescending { it.dueCount }
            .thenBy { it.title }
}