// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.repository.ReviewSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RatingPolicyTest {

    @Test
    fun `强提示按句封顶 HARD`() {
        val cap = RatingPolicy.capFor(ReviewSource.SENTENCE_CLOZE, strongHinted = true)
        assertEquals(FsrsEngine.Rating.HARD, cap)
        assertEquals(
            FsrsEngine.Rating.HARD,
            RatingPolicy.apply(FsrsEngine.Rating.EASY, cap)
        )
        assertEquals(
            FsrsEngine.Rating.AGAIN,
            RatingPolicy.apply(FsrsEngine.Rating.AGAIN, cap)
        )
    }

    @Test
    fun `错题集训默认封顶 HARD 但仍是复习`() {
        val cap = RatingPolicy.capFor(ReviewSource.MISTAKE_DRILL, strongHinted = false)
        assertEquals(FsrsEngine.Rating.HARD, cap)
        assertEquals(
            FsrsEngine.Rating.HARD,
            RatingPolicy.apply(FsrsEngine.Rating.GOOD, cap)
        )
    }

    @Test
    fun `普通挖空和遮挡转练不额外封顶`() {
        assertNull(RatingPolicy.capFor(ReviewSource.SENTENCE_CLOZE, strongHinted = false))
        assertNull(RatingPolicy.capFor(ReviewSource.CUSTOM_CLOZE, strongHinted = false))
        assertNull(RatingPolicy.capFor(ReviewSource.MASK_TO_CLOZE, strongHinted = false))
        assertNull(RatingPolicy.capFor(ReviewSource.SENTENCE_CARD, strongHinted = false))
    }

    @Test
    fun `除集训外所有来源都不额外封顶`() {
        ReviewSource.entries
            .filter { it != ReviewSource.MISTAKE_DRILL }
            .forEach { source ->
                assertNull("$source 不应额外封顶", RatingPolicy.capFor(source, strongHinted = false))
            }
    }

    @Test
    fun `无封顶时评级原样返回`() {
        assertEquals(
            FsrsEngine.Rating.EASY,
            RatingPolicy.apply(FsrsEngine.Rating.EASY, cap = null)
        )
    }
}