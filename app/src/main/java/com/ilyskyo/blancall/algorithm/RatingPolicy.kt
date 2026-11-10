// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.repository.ReviewSource

/**
 * 写回句级/篇级记忆时的统一封顶规则。
 *
 * 两条独立的上限：
 * - 强提示自动填入：该句有提示空 → 最高 HARD，避免「等 20 秒让系统填满」拿到 EASY；
 * - 错题集训：一次通常只覆盖一个空，不能证明整句掌握 → 默认最高 HARD。
 *   它是真实复习，仍会推进状态，只是推进得慢；重复集训按多次 HARD 累积。
 *
 * 其他入口（普通挖空/默写/自定义/遮挡转练/句子卡自评）不额外封顶。
 */
object RatingPolicy {

    fun capFor(source: ReviewSource, strongHinted: Boolean): FsrsEngine.Rating? = when {
        strongHinted -> FsrsEngine.Rating.HARD
        source == ReviewSource.MISTAKE_DRILL -> FsrsEngine.Rating.HARD
        else -> null
    }

    fun apply(rating: FsrsEngine.Rating, cap: FsrsEngine.Rating?): FsrsEngine.Rating =
        if (cap != null && rating.value > cap.value) cap else rating
}