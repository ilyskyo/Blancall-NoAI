// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 一年档「月 × 日」矩阵的回归锁定。
 *
 * 被钉住的是用户 2026-11-11 报的那条：近一年的热力图在手机上只剩一片 4~5dp 的噪点，
 * 行标签还叠成一坨。改成 31 列 × 13 行之后，「哪天落在哪一行哪一列」「2 月 30 日不能画」
 * 「区间外不能算成练了 0 次」都是可以断言的逻辑 —— 一律在这里守，而不是靠肉眼看截图。
 */
class HeatmapYearMatrixTest {

    /** 用户报 bug 时的真实区间：近一年跨了 13 个自然月（2025-11 → 2026-11） */
    private val range = DateRange(LocalDate.of(2025, 11, 12), LocalDate.of(2026, 11, 11))

    private fun counts(counter: (LocalDate) -> Int = { 1 }) = counter

    @Test
    fun `近一年铺成 13 行，首尾是区间首尾所在的月`() {
        val rows = heatmapYearMatrix(range, counts())
        assertEquals(13, rows.size)
        assertEquals(range.start.year, rows.first().yearMonth.year)
        assertEquals(range.start.monthValue, rows.first().yearMonth.monthValue)
        assertEquals(range.end.year, rows.last().yearMonth.year)
        assertEquals(range.end.monthValue, rows.last().yearMonth.monthValue)
    }

    @Test
    fun `每行固定 31 格，超出该月天数的格子为 null`() {
        val rows = heatmapYearMatrix(range, counts())
        rows.forEach { row ->
            assertEquals(31, row.cells.size)
            val len = row.yearMonth.lengthOfMonth()
            row.cells.forEachIndexed { index, cell ->
                val day = index + 1
                if (day > len) {
                    assertNull("${row.yearMonth} 没有 $day 号，却画了一格", cell)
                } else {
                    // 该月这一天存在，是否在区间内由下一条测试守
                    assertTrue(cell == null || cell.date.dayOfMonth == day)
                }
            }
        }
        // 2026 不是闰年：2 月只到 28 日
        val feb = rows.first { it.yearMonth.monthValue == 2 && it.yearMonth.year == 2026 }
        assertTrue(feb.cells[27] != null)
        assertNull(feb.cells[28])
        assertNull(feb.cells[30])
    }

    @Test
    fun `区间之外的日子是 null，而不是 0 次`() {
        val rows = heatmapYearMatrix(range, counts { 0 })
        // 起点 11/12：同月 1~11 号不在区间里
        val firstRow = rows.first()
        (0 until 11).forEach { index ->
            assertNull("${firstRow.yearMonth} 第 ${index + 1} 天不在区间内", firstRow.cells[index])
        }
        assertTrue(firstRow.cells[11] != null)
        // 终点 11/11：同月 12~30 号不在区间里
        val lastRow = rows.last()
        (11 until 30).forEach { index ->
            assertNull("${lastRow.yearMonth} 第 ${index + 1} 天不在区间内", lastRow.cells[index])
        }
        assertTrue(lastRow.cells[10] != null)
    }

    @Test
    fun `区间内每一天恰好出现在一格，且带的是当天的次数`() {
        val countsByDate = mapOf(
            LocalDate.of(2025, 11, 12) to 3,
            LocalDate.of(2026, 6, 1) to 7,
            LocalDate.of(2026, 11, 11) to 2,
        )
        val rows = heatmapYearMatrix(range) { countsByDate[it] ?: 0 }
        val seen = mutableSetOf<LocalDate>()
        rows.forEach { row ->
            row.cells.filterNotNull().forEach { cell ->
                assertTrue("${cell.date} 重复出现", seen.add(cell.date))
                assertTrue("${cell.date} 落在区间外却画了出来", cell.date in range)
                assertEquals(countsByDate[cell.date] ?: 0, cell.count)
            }
        }
        // 365 天，一天不多一天不少
        assertEquals(range.days, seen.size)
    }

    @Test
    fun `跨年区间不会把两年的同一个月合并成一行`() {
        val rows = heatmapYearMatrix(range, counts())
        val novembers = rows.filter { it.yearMonth.monthValue == 11 }
        assertEquals(2, novembers.size)
        assertEquals(setOf(2025, 2026), novembers.map { it.yearMonth.year }.toSet())
    }

    @Test
    fun `单月区间只铺一行，且天数之外全是 null`() {
        val month = DateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))
        val rows = heatmapYearMatrix(month, counts())
        assertEquals(1, rows.size)
        assertEquals(28, rows.single().cells.count { it != null })
    }

    // ── 格子尺寸 ──

    @Test
    fun `手机卡片宽度下每格约 8dp、31 列不溢出`() {
        val m = heatmapYearCellMetrics(availableWidth = 281f)
        assertEquals(1.5f, m.gap, 0.001f)
        assertTrue("cell=${m.cell}", m.cell > 7f)
        assertTrue("cell=${m.cell}", m.cell <= 10f + 0.001f)
        assertTrue(m.gridWidth(31) <= 281f + 0.01f)
    }

    @Test
    fun `极宽容器下格子封顶 10dp，靠居中而不是撑满`() {
        val m = heatmapYearCellMetrics(availableWidth = 600f)
        assertEquals(10f, m.cell, 0.001f)
        assertTrue(m.gridWidth(31) < 600f)
    }

    @Test
    fun `极窄容器下格子有 3dp 下界`() {
        val m = heatmapYearCellMetrics(availableWidth = 40f)
        assertEquals(3f, m.cell, 0.001f)
    }

    @Test
    fun `年矩阵分界取 200 天`() {
        // 90 天档仍走周列布局，近一年走月矩阵：切换点必须落在两者之间
        assertTrue(presetRange(RangePreset.LAST_90, LocalDate.of(2026, 11, 11)).days < HEATMAP_YEAR_MATRIX_MIN_DAYS)
        assertTrue(presetRange(RangePreset.LAST_365, LocalDate.of(2026, 11, 11)).days > HEATMAP_YEAR_MATRIX_MIN_DAYS)
    }
}
