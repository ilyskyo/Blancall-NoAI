// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

/** 一次练习对「本篇到期句」的覆盖：dueTotal 为生成时选中的到期句数，assessed 为本次实际作答到的。 */
data class PracticeCoverage(
    val dueTotal: Int,
    val assessed: Int,
) {
    init {
        require(dueTotal >= 0 && assessed >= 0)
    }

    val remaining: Int get() = (dueTotal - assessed).coerceAtLeast(0)
}