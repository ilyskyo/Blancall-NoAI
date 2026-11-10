// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DictationSentenceStatesTest {

    private val sentences = listOf("春眠不觉晓，处处闻啼鸟。", "夜来风雨声，花落知多少。")

    private fun result(index: Int, similarity: Float, correct: Boolean) =
        AnswerChecker.DictationSentenceResult(
            userText = "x",
            matchedOriginal = sentences.getOrNull(index),
            matchIndex = index,
            similarity = similarity,
            result = if (correct) AnswerChecker.Result.CORRECT else AnswerChecker.Result.INCORRECT,
        )

    private fun check(vararg results: AnswerChecker.DictationSentenceResult) =
        AnswerChecker.DictationCheckResult(
            sentences = results.toList(),
            coverageRate = 1f,
            accuracyRate = 1f,
            orderCorrectRate = 1f,
            overallScore = 1f,
        )

    private fun keyOf(index: Int) = SentenceSelector.sentenceKey(1L, sentences[index])

    private fun mapRating(accuracy: Float) = if (accuracy >= 1f) {
        FsrsEngine.Rating.GOOD
    } else {
        FsrsEngine.Rating.HARD
    }

    @Test
    fun `同一句的多个分句取最差评级`() {
        val perSentence = DictationSentenceStates.build(
            clauses = listOf("春眠不觉晓，", "处处闻啼鸟。"),
            check = check(result(0, 0.9f, correct = true), result(1, 0.4f, correct = false)),
            sentences = sentences,
            clauseToSentence = listOf(0, 0),
            sentenceKeyOf = ::keyOf,
            ratingOf = { _, accuracy -> mapRating(accuracy) },
        )

        assertEquals(1, perSentence.size)
        assertEquals(FsrsEngine.Rating.HARD, perSentence[keyOf(0)])
    }

    @Test
    fun `整句没默写出来记 AGAIN`() {
        val perSentence = DictationSentenceStates.build(
            clauses = listOf("春眠不觉晓，", "处处闻啼鸟。"),
            check = check(result(0, 1f, correct = true)),
            sentences = sentences,
            clauseToSentence = listOf(0, 0),
            sentenceKeyOf = ::keyOf,
            ratingOf = { _, accuracy -> mapRating(accuracy) },
        )

        assertEquals(FsrsEngine.Rating.AGAIN, perSentence[keyOf(0)])
    }

    @Test
    fun `两句各自写回各自的键`() {
        val perSentence = DictationSentenceStates.build(
            clauses = listOf("春眠不觉晓，", "夜来风雨声，"),
            check = check(result(0, 1f, correct = true), result(1, 0.5f, correct = false)),
            sentences = sentences,
            clauseToSentence = listOf(0, 1),
            sentenceKeyOf = ::keyOf,
            ratingOf = { _, accuracy -> mapRating(accuracy) },
        )

        assertEquals(2, perSentence.size)
        assertEquals(FsrsEngine.Rating.GOOD, perSentence[keyOf(0)])
        assertEquals(FsrsEngine.Rating.HARD, perSentence[keyOf(1)])
    }

    @Test
    fun `空分句与解析不出的键都跳过`() {
        val perSentence = DictationSentenceStates.build(
            clauses = listOf("", "春眠不觉晓，"),
            check = check(result(1, 1f, correct = true)),
            sentences = sentences,
            clauseToSentence = listOf(0, 0),
            sentenceKeyOf = { null },
            ratingOf = { _, accuracy -> mapRating(accuracy) },
        )

        assertTrue(perSentence.isEmpty())
    }

    @Test
    fun `越界的句子索引不写状态`() {
        val perSentence = DictationSentenceStates.build(
            clauses = listOf("春眠不觉晓，"),
            check = check(result(0, 1f, correct = true)),
            sentences = sentences,
            clauseToSentence = listOf(9),
            sentenceKeyOf = ::keyOf,
            ratingOf = { _, accuracy -> mapRating(accuracy) },
        )

        assertTrue(perSentence.isEmpty())
    }
}