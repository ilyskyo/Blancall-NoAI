// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「复习频率」这个设置的方向守卫。
 *
 * FSRS 的间隔是 `S / factor * (R^(1/decay) - 1)`，decay 为负 ⇒ **目标留存率越低、间隔越长**。
 * v7.6 及以前 `sprint = 0.85 / deep = 0.95`，注释却写「冲刺 85% 间隔短」——
 * 结果选「冲刺备考」（最需要高频复习的场景）拿到的是最长间隔。
 * 默认 standard 把这个问题完全盖住了，用户不可能自己发现。
 */
class TemplateRetentionDirectionTest {

    private val now = 1_700_000_000_000L

    @After
    fun restoreDefault() {
        FsrsEngine.configure(FsrsEngine.DEFAULT_PARAMS, 0.90)
    }

    @Test
    fun `冲刺的目标留存率必须高于标准 深度必须低于标准`() {
        val standard = FsrsEngine.retentionForTemplate("standard")
        assertTrue(
            "冲刺要练得更勤 ⇒ 目标留存率更高",
            FsrsEngine.retentionForTemplate("sprint") > standard,
        )
        assertTrue(
            "深度长期可以容忍更低的到期留存 ⇒ 目标留存率更低",
            FsrsEngine.retentionForTemplate("deep") < standard,
        )
    }

    @Test
    fun `同一张卡在冲刺下的间隔必须短于深度`() {
        val state = FsrsEngine.CardState(
            difficulty = 5.0,
            stability = 30.0,
            due = now,
            lastReview = now - 10L * 24 * 60 * 60 * 1000,
            reviewCount = 3,
            lapses = 0,
        )

        FsrsEngine.configure(FsrsEngine.DEFAULT_PARAMS, FsrsEngine.retentionForTemplate("sprint"))
        val sprint = FsrsEngine.previewIntervalDays(state, FsrsEngine.Rating.GOOD, now)

        FsrsEngine.configure(FsrsEngine.DEFAULT_PARAMS, FsrsEngine.retentionForTemplate("deep"))
        val deep = FsrsEngine.previewIntervalDays(state, FsrsEngine.Rating.GOOD, now)

        assertTrue(
            "冲刺($sprint 天) 必须比深度($deep 天) 更频繁",
            sprint < deep,
        )
    }
}
