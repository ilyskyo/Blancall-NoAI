// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

/**
 * 反向默写的逐句状态写回：把「分句级判分」映射成「句子键 → 评级」。
 *
 * 同篇默写以前只写篇级，句子卡完全不知道这次练习；跨文默写只写句级但映射是内联的。
 * 抽成纯函数后两条路径共用一套规则：
 * - 分句 → 全文句子索引由调用方给出（同篇/跨文各自的口径）；
 * - 同一句里的多个分句取最差评级；
 * - 整句没默写出来（判分结果里没有该分句）记 AGAIN；
 * - 空分句、越界索引、无法解析的句键直接跳过，不制造孤儿状态。
 */
object DictationSentenceStates {

    fun build(
        clauses: List<String>,
        check: AnswerChecker.DictationCheckResult,
        sentences: List<String>,
        clauseToSentence: List<Int>,
        sentenceKeyOf: (sentenceIndex: Int) -> String?,
        ratingOf: (similarity: Float, accuracy: Float) -> FsrsEngine.Rating,
    ): Map<String, FsrsEngine.Rating> {
        val matched = check.sentences.associateBy { it.matchIndex }
        val out = LinkedHashMap<String, FsrsEngine.Rating>()
        clauses.forEachIndexed { index, clause ->
            if (clause.isBlank()) return@forEachIndexed
            val sentenceIndex = clauseToSentence.getOrNull(index) ?: return@forEachIndexed
            if (sentenceIndex !in sentences.indices) return@forEachIndexed
            val key = sentenceKeyOf(sentenceIndex) ?: return@forEachIndexed
            val result = matched[index]
            val rating = if (result == null) {
                FsrsEngine.Rating.AGAIN
            } else {
                ratingOf(
                    result.similarity,
                    if (result.result == AnswerChecker.Result.CORRECT) 1f else 0f,
                )
            }
            val prev = out[key]
            out[key] = if (prev == null) rating else minOf(prev, rating)
        }
        return out
    }
}