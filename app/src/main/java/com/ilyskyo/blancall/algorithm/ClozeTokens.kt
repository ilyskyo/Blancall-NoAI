// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

/**
 * 自定义挖空编辑器的 token 切分逻辑。
 *
 * 按级 Level 切分 token：
 * - 0 复句
 * - 1 分句：按句内标点切，标点跟随前一个 token
 * - 2 字词：连续汉字每 2 字一块（末尾余 1 字自成一 tok），英文单词整体、数字串整体
 * - 3 单字：每个汉字一个 token，英文单词整体、数字串整体
 *
 * ## 标点不是挖空单位（2026-11 修）
 * 旧实现把标点「并入前一个 token」，于是「书」在单字级切出的是 `「` 与 `书」` 两块：
 * 用户想挖「书」，挖掉的是「书》」，答案里凭空多一个书名号，而长按只能往粗里拆、
 * 不可能再细分 —— 死路。这类字符不止书名号：引号、括号、破折号、省略号、间隔号，
 * 以及**数字**（`isLetter()` 对数字为假，旧实现把它当标点）、空格，全都会被前一个字吞掉。
 *
 * 现在的口径：标点/空白是**不可选**的 token，永远留在正文里；空只覆盖实义字符
 * （汉字 / 字母 / 数字）。这与自动「字词挖空」的候选口径一致 —— 那条路径从来只挑
 * 汉字和英文单词，本来就不挖标点。于是「字词挖空不需要标点提示」不是省掉一个开关，
 * 而是标点根本不会进空。
 *
 * [EditToken.blankRange] 与 [EditToken.range] 分开是为了分句级：token 尾部带标点
 * （正文里看得见），但真正挖掉的只有实义部分 —— 标点留在句子中。
 */
object ClozeTokens {

    /**
     * token：文本 + 句内字符区间。
     *
     * @param selectable false = 标点/空白，照原样显示，永不成空（点它没有反应）
     * @param blankRange 选中这一块后真正挖掉的区间：两端去标点/空白后的实义区间。
     *        不可选的 token 该字段无意义（等于 [range]）。
     */
    data class EditToken(
        val text: String,
        val range: IntRange,
        val selectable: Boolean = true,
        val blankRange: IntRange = range,
    )

    fun isChinese(c: Char): Boolean = c in '\u4e00'..'\u9fff' || c in '\u3400'..'\u4dbf'

    /**
     * 非十进制数字的「内容数字」：下标 ₂、上标 ²、分数 ½、罗马数字 Ⅳ 等。
     * `isLetterOrDigit` 对它们全为假，按标点处理会把「CO₂排放」拆成两半。
     */
    private fun isOtherNumber(c: Char): Boolean {
        val t = Character.getType(c)
        return t == Character.LETTER_NUMBER.toInt() || t == Character.OTHER_NUMBER.toInt()
    }

    /**
     * 字词挖空允许吃的字符：汉字 / 字母 / 数字（含 ₂ 这类非十进制数字）。
     * 标点、空白、符号都不在其中 —— 它们永远不成为空的一部分。
     */
    fun isContentChar(c: Char): Boolean = isChinese(c) || c.isLetterOrDigit() || isOtherNumber(c)

    /**
     * 句中第 [i] 个位置是否属于实义内容。
     * 代理对（生僻字 𠮷、emoji）的两个 Char 都算内容，避免把罕用名字拆成半个字。
     */
    private fun isContentAt(sentence: String, i: Int): Boolean {
        val c = sentence[i]
        if (Character.isHighSurrogate(c)) {
            return i + 1 < sentence.length && Character.isLowSurrogate(sentence[i + 1])
        }
        if (Character.isLowSurrogate(c)) {
            return i > 0 && Character.isHighSurrogate(sentence[i - 1])
        }
        return isContentChar(c)
    }

    fun tokensFor(sentence: String, level: Int): List<EditToken> {
        if (sentence.isEmpty()) return emptyList()
        return when {
            level <= 0 -> listOf(wholeToken(sentence))
            level == 1 -> clauseTokens(sentence)
            else -> wordTokens(sentence, chunk = if (level == 2) 2 else 1)
        }
    }

    private fun wholeToken(sentence: String): EditToken {
        val range = 0 until sentence.length
        val content = contentTrim(sentence, range)
        return EditToken(
            text = sentence,
            range = range,
            selectable = content != IntRange.EMPTY,
            blankRange = content,
        )
    }

    /** 分句级：按标点/空白切段，标点跟随前一段；挖空只覆盖段内实义部分 */
    private fun clauseTokens(sentence: String): List<EditToken> {
        val out = mutableListOf<EditToken>()
        val n = sentence.length
        var start = 0
        var i = 0
        while (i < n) {
            if (!isContentAt(sentence, i)) {
                i++
                while (i < n && !isContentAt(sentence, i)) i++
                out.add(segmentToken(sentence, start, i))
                start = i
            } else i++
        }
        if (start < n) out.add(segmentToken(sentence, start, n))
        return if (out.size <= 1) listOf(wholeToken(sentence)) else out
    }

    private fun segmentToken(sentence: String, from: Int, toExclusive: Int): EditToken {
        val range = from until toExclusive
        val content = contentTrim(sentence, range)
        return EditToken(
            text = sentence.substring(from, toExclusive),
            range = range,
            selectable = content != IntRange.EMPTY,
            blankRange = content,
        )
    }

    /** 字词级 / 单字级：只有实义字符成为 token；标点与空白自成一块且不可选 */
    private fun wordTokens(sentence: String, chunk: Int): List<EditToken> {
        val out = mutableListOf<EditToken>()
        val n = sentence.length
        var i = 0
        while (i < n) {
            val c = sentence[i]
            when {
                // 生僻字/emoji：整对算一个 token
                Character.isHighSurrogate(c) && i + 1 < n && Character.isLowSurrogate(sentence[i + 1]) -> {
                    out.add(EditToken(sentence.substring(i, i + 2), i..(i + 1)))
                    i += 2
                }
                isChinese(c) -> {
                    var runEnd = i
                    while (runEnd < n && isChinese(sentence[runEnd])) runEnd++
                    var s = i
                    while (s < runEnd) {
                        val e = minOf(s + chunk, runEnd)
                        out.add(EditToken(sentence.substring(s, e), s until e))
                        s = e
                    }
                    i = runEnd
                }
                c.isLetter() -> {
                    // 英文单词整体（数字与字母混排如 CO2 会在这里被拆成 CO / 2，各自仍可单独挖）
                    var runEnd = i
                    while (runEnd < n && sentence[runEnd].isLetter() && !isChinese(sentence[runEnd])) runEnd++
                    out.add(EditToken(sentence.substring(i, runEnd), i until runEnd))
                    i = runEnd
                }
                c.isDigit() -> {
                    // 数字是内容，不是标点：整串独立成 token，既不被前一个字吞掉，也不被拆散
                    var runEnd = i
                    while (runEnd < n && sentence[runEnd].isDigit()) runEnd++
                    out.add(EditToken(sentence.substring(i, runEnd), i until runEnd))
                    i = runEnd
                }
                isContentChar(c) -> {
                    // 其余内容字符（下标 ₂、罗马数字 Ⅳ…）：各自独立，仍可单独挖
                    out.add(EditToken(sentence.substring(i, i + 1), i..i))
                    i++
                }
                else -> {
                    var runEnd = i
                    while (runEnd < n && !isContentAt(sentence, runEnd)) runEnd++
                    out.add(
                        EditToken(
                            sentence.substring(i, runEnd),
                            i until runEnd,
                            selectable = false,
                        )
                    )
                    i = runEnd
                }
            }
        }
        return out
    }

    /** 把区间两端收进实义字符之内（不碰中间），全为标点时返回空区间 */
    private fun contentTrim(sentence: String, range: IntRange): IntRange {
        if (range.isEmpty()) return IntRange.EMPTY
        var a = range.first.coerceIn(0, sentence.length - 1)
        var b = range.last.coerceIn(0, sentence.length - 1)
        while (a <= b && !isContentAt(sentence, a)) a++
        while (b >= a && !isContentAt(sentence, b)) b--
        return if (a > b) IntRange.EMPTY else a..b
    }

    /**
     * 把「用户选中的区间」收敛成字词挖空真正要吃的空（保存与应用共用这一份口径）：
     * 1. 越界裁剪 + 相邻/重叠合并（[RangeOps.normalizeClampedRanges]）；
     * 2. 按标点/空白切开 —— 一个空绝不跨标点，答案里不会混进书名号；
     * 3. 两端不含标点/空白。
     *
     * 旧的已保存配置里躺着 `书》` 这种区间，加载与应用时都过这一遍，无需迁移脚本。
     */
    fun contentRanges(sentence: String, ranges: List<IntRange>): List<IntRange> {
        if (sentence.isEmpty() || ranges.isEmpty()) return emptyList()
        val clamped = RangeOps.normalizeClampedRanges(sentence.length, ranges)
        val out = mutableListOf<IntRange>()
        for (r in clamped) {
            var start = -1
            for (i in r) {
                if (isContentAt(sentence, i)) {
                    if (start < 0) start = i
                } else if (start >= 0) {
                    out.add(start until i)
                    start = -1
                }
            }
            if (start >= 0) out.add(start..r.last)
        }
        return out
    }

    /**
     * 区间集合在 level 粒度下是否「完整对齐」：每个区间恰好是若干连续完整 token 的并集。
     * 用于恢复已保存配置的粒度时校验（粒度更粗时已存区间可能切在 token 中间）。
     *
     * 只拿可选 token 的 [EditToken.blankRange] 比对：标点不参与选空，不能因为它夹在
     * 两个 token 之间就把一个合法的空判成「对不齐」。
     */
    fun rangesAligned(sentence: String, level: Int, ranges: List<IntRange>): Boolean {
        if (ranges.isEmpty()) return true
        val tokens = tokensFor(sentence, level).filter { it.selectable }.map { it.blankRange }
        return ranges.all { r ->
            if (r.first < 0 || r.last >= sentence.length) return@all false
            val covered = tokens.filter { it.first >= r.first && it.last <= r.last }
            if (covered.isEmpty()) return@all false
            var cursor = r.first
            for (t in covered.sortedBy { it.first }) {
                if (t.first != cursor) return@all false
                cursor = t.last + 1
            }
            cursor == r.last + 1
        }
    }

    /**
     * 恢复配置时的粒度：从偏好粒度 prefLevel 起，逐级细化（最多 3）
     * 直到该句全部已存区间对齐；到 3 仍不对齐（理论罕见）则维持 3。
     */
    fun restoreLevel(sentence: String, prefLevel: Int, ranges: List<IntRange>): Int {
        var level = prefLevel.coerceIn(0, 3)
        while (level < 3 && !rangesAligned(sentence, level, ranges)) level++
        return level
    }
}
