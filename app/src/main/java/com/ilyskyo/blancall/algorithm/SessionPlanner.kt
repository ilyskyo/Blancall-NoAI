// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.model.Article

/**
 * 句级会话计划器：在时间预算内按「到期句 → 弱句 → 新句 → 错题」选句。
 *
 * 只做选择，不写任何状态。UI 接上后，一次练习只推进被选中的句子；
 * 未覆盖的到期句会留在 [Plan.dueRemaining] 里，结束页如实显示，不静默跳过。
 *
 * `weakKeys` / `mistakeKeys` 由调用方从错题本、records 等处算好传入：
 * 算法层不猜「哪句弱」，只负责优先级与预算。
 */
object SessionPlanner {

    enum class Kind { DUE, WEAK, NEW, MISTAKE }

    data class Candidate(
        val key: String,
        val articleId: Long,
        val articleTitle: String,
        val text: String,
        val kind: Kind,
        val due: Long,
        val retention: Float?,
    )

    data class Coverage(
        val eligible: Int,
        val dueBefore: Int,
        val selectedDue: Int,
        val selectedWeak: Int,
        val selectedNew: Int,
        val selectedMistake: Int,
    )

    data class ArticlePlan(
        val articleId: Long,
        val title: String,
        val candidates: List<Candidate>,
        val minutes: Double,
    )

    data class Plan(
        val articlePlans: List<ArticlePlan>,
        val selectedKeys: Set<String>,
        val minutes: Double,
        val dueTotal: Int,
        val dueRemaining: Int,
        val newTotal: Int,
        val newRemaining: Int,
        val coverageByArticle: Map<Long, Coverage>,
    )

    data class Input(
        val articles: List<Article>,
        val sentenceStates: Map<String, FsrsEngine.CardState>,
        val weakKeys: Set<String> = emptySet(),
        val mistakeKeys: Set<String> = emptySet(),
        val excludedArticleIds: Set<Long> = emptySet(),
        val budgetMinutes: Double = Double.MAX_VALUE,
        val newSentenceLimit: Int = Int.MAX_VALUE,
        val now: Long = System.currentTimeMillis(),
        val minutesPerSentence: (String) -> Double = { 1.0 },
    )

    fun plan(input: Input): Plan {
        require(input.budgetMinutes >= 0.0 && !input.budgetMinutes.isNaN())
        require(input.newSentenceLimit >= 0)

        val due = ArrayList<Candidate>()
        val weak = ArrayList<Candidate>()
        val fresh = ArrayList<Candidate>()
        val pickByKey = HashMap<String, Pair<Article, SentenceSelector.Pick>>()
        val eligibleByArticle = HashMap<Long, Int>()
        val dueByArticle = HashMap<Long, Int>()

        for (article in input.articles) {
            if (article.id in input.excludedArticleIds) continue
            val picks = SentenceSelector.candidates(article)
            eligibleByArticle[article.id] = picks.size
            for (pick in picks) {
                pickByKey[pick.key] = article to pick
                val state = input.sentenceStates[pick.key]
                val candidate = candidate(article, pick, state, Kind.DUE, input.now)
                when {
                    state != null && state.due > 0L && FsrsEngine.isDue(state, input.now) -> {
                        due += candidate
                        dueByArticle[article.id] = (dueByArticle[article.id] ?: 0) + 1
                    }

                    state != null && pick.key in input.weakKeys -> {
                        weak += candidate.copy(kind = Kind.WEAK)
                    }

                    state == null -> {
                        fresh += candidate.copy(kind = Kind.NEW)
                    }
                }
            }
        }

        val dueKeys = due.mapTo(HashSet()) { it.key }
        val weakKeys = weak.mapTo(HashSet()) { it.key }
        val freshKeys = fresh.mapTo(HashSet()) { it.key }
        val mistake = input.mistakeKeys
            .asSequence()
            .filter { it !in dueKeys && it !in weakKeys && it !in freshKeys }
            .mapNotNull { key ->
                pickByKey[key]?.let { (article, pick) ->
                    candidate(
                        article = article,
                        pick = pick,
                        state = input.sentenceStates[key],
                        kind = Kind.MISTAKE,
                        now = input.now,
                    )
                }
            }
            .sortedWith(compareBy({ it.articleId }, { it.text }))
            .toList()

        due.sortWith(compareBy<Candidate> { it.due }.thenBy { it.articleId }.thenBy { it.text })
        weak.sortWith(
            compareBy<Candidate> { it.retention ?: 1f }
                .thenBy { it.articleId }
                .thenBy { it.text }
        )

        val selected = LinkedHashMap<String, Candidate>()
        var minutes = 0.0

        fun take(group: List<Candidate>, limit: Int): Boolean {
            var taken = 0
            var blocked = false
            for (c in group) {
                if (taken >= limit) break
                if (c.key in selected) continue
                val cost = input.minutesPerSentence(c.text).coerceAtLeast(0.0)
                if (minutes + cost <= input.budgetMinutes + 1e-9) {
                    selected[c.key] = c
                    minutes += cost
                    taken++
                } else {
                    blocked = true
                }
            }
            return blocked
        }

        val dueBlocked = take(due, Int.MAX_VALUE)
        if (!dueBlocked) {
            val weakBlocked = take(weak, Int.MAX_VALUE)
            if (!weakBlocked) {
                val newBlocked = take(fresh, input.newSentenceLimit)
                if (!newBlocked) take(mistake, Int.MAX_VALUE)
            }
        }

        val selectedList = selected.values.toList()
        val selectedByArticle = selectedList.groupBy { it.articleId }
        val articlePlans = input.articles.mapNotNull { article ->
            val items = selectedByArticle[article.id] ?: return@mapNotNull null
            ArticlePlan(
                articleId = article.id,
                title = article.title,
                candidates = items,
                minutes = items.sumOf { input.minutesPerSentence(it.text).coerceAtLeast(0.0) },
            )
        }
        val coverage = input.articles.associate { article ->
            val items = selectedByArticle[article.id].orEmpty()
            article.id to Coverage(
                eligible = eligibleByArticle[article.id] ?: 0,
                dueBefore = dueByArticle[article.id] ?: 0,
                selectedDue = items.count { it.kind == Kind.DUE },
                selectedWeak = items.count { it.kind == Kind.WEAK },
                selectedNew = items.count { it.kind == Kind.NEW },
                selectedMistake = items.count { it.kind == Kind.MISTAKE },
            )
        }
        val selectedDue = selectedList.count { it.kind == Kind.DUE }
        val selectedNew = selectedList.count { it.kind == Kind.NEW }
        return Plan(
            articlePlans = articlePlans,
            selectedKeys = selected.keys,
            minutes = minutes,
            dueTotal = due.size,
            dueRemaining = (due.size - selectedDue).coerceAtLeast(0),
            newTotal = fresh.size,
            newRemaining = (fresh.size - selectedNew).coerceAtLeast(0),
            coverageByArticle = coverage,
        )
    }

    private fun candidate(
        article: Article,
        pick: SentenceSelector.Pick,
        state: FsrsEngine.CardState?,
        kind: Kind,
        now: Long,
    ) = Candidate(
        key = pick.key,
        articleId = article.id,
        articleTitle = article.title,
        text = pick.text,
        kind = kind,
        due = state?.due?.coerceAtLeast(0L) ?: 0L,
        retention = state?.let { FsrsEngine.retentionRate(it, now).toFloat() },
    )
}