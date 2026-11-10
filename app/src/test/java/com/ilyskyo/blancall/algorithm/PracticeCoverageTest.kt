// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import org.junit.Assert.assertEquals
import org.junit.Test

class PracticeCoverageTest {

    @Test
    fun `剩余到期句等于总数减已覆盖`() {
        assertEquals(2, PracticeCoverage(dueTotal = 5, assessed = 3).remaining)
    }

    @Test
    fun `已覆盖超出总数时剩余归零`() {
        assertEquals(0, PracticeCoverage(dueTotal = 3, assessed = 5).remaining)
    }
}