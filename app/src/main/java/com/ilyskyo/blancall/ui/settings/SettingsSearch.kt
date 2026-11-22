// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ilyskyo.blancall.R

/**
 * 设置搜索的**打分核心**：纯字符串逻辑，不碰 Compose 状态、不碰资源 id。
 *
 * ## 为什么要别名（用户 2026-11-21）
 * 原实现只比标题文本，于是「外貌」搜不到「外观」、「颜色」搜不到「主题色」——
 * 用户用的词和界面上的词不是同一套。标题是**我们起的名字**，别名是**用户嘴里的名字**，
 * 两者必须分开存：把「颜色」硬塞进标题会改变界面文案，塞进搜索词表则只影响检索。
 *
 * ## 四档文本关系 + 三档折扣
 * 标题命中：完全相等 100 > 前缀 80 > 包含 60 > 逐字子序列 30（允许跳字：「主色」→「主题色」）。
 * 别名命中同档但打折（-30）：用户说的是自己的说法，排序上让位于界面原词。
 * 入口行再打一档（-15）：入口是目录，能落到分区就别停在目录上。
 *
 * ## 单字查询不「跳字」
 * 一个字的情况下「跳字」与「包含」是同一件事（就一个字符，跳无可跳），
 * 所以 `needle.length >= 2` 才进子序列分支 —— 省一次无意义的扫描，也让意图写在代码里：
 * 两个字以上才谈得上模糊。
 */
internal data class SettingsSearchDoc(
    /** 界面上显示的名字 */
    val title: String,
    /** 用户可能用来说它的别的词（同义词表在 strings.xml，见 `settings_search_alias_*`） */
    val aliases: List<String>,
)

internal const val SCORE_EXACT = 100
internal const val SCORE_PREFIX = 80
internal const val SCORE_CONTAINS = 60
internal const val SCORE_SUBSEQUENCE = 30

/** 别名命中的折扣：同档文本关系下，标题命中永远排在别名命中前面 */
internal const val ALIAS_DISCOUNT = 30

/** 入口行在别名上再折一档（分区更具体，能落到分区就别停在入口） */
internal const val ENTRY_ALIAS_DISCOUNT = 15

/** 入口名命中的权重加成：搜「AI」时应当先看到「AI」这个入口，而不是它下面的七个子项 */
internal const val ENTRY_TITLE_BONUS = 6

/** 「软件颜色」里最多剥几层修饰词 */
private const val MAX_NOISE_STRIPS = 3

/**
 * 别名表的书写格式：逗号（中英文）、顿号、竖线、分号、空格都算分隔符。
 *
 * 空格也算，是为了让人随手写都不出错；代价是英文别名必须写成一个词
 * （`spacing` 而不是 `line spacing`）—— 短语被拆成两个词后仍能命中，
 * 只是每个词都成了独立的别名。
 */
private val ALIAS_SEPARATORS = charArrayOf(',', '，', '、', '|', ';', '；', ' ', '\u3000')

internal fun parseSearchAliases(raw: String): List<String> =
    raw.split(*ALIAS_SEPARATORS).map { it.trim() }.filter { it.isNotEmpty() }

/** 噪音词表：用户会在实词前面加的修饰（「软件颜色」「app 颜色」）。剥掉后仍按实词检索。 */
@Composable
internal fun searchNoiseWords(): Set<String> =
    parseSearchAliases(stringResource(R.string.settings_search_noise))
        .map { it.lowercase() }
        .toSet()

/** 分区这一行：名字 + 它自己的同义词。 */
@Composable
internal fun settingsGroupDoc(group: SettingsGroup): SettingsSearchDoc =
    SettingsSearchDoc(settingsGroupTitle(group), parseSearchAliases(stringResource(group.aliasRes)))

/**
 * 入口这一行：名字 + **它下面所有分区的同义词并集**。
 *
 * 并集而不是另写一份：入口就是这些分区的目录，用户搜的东西若在某个分区里，
 * 停在入口也找得到；另写一份等于把同一批同义词维护两遍，两边迟早不一致。
 */
@Composable
internal fun settingsEntryDoc(entry: SettingsEntry): SettingsSearchDoc =
    SettingsSearchDoc(
        settingsEntryTitle(entry),
        entry.groups.flatMap { parseSearchAliases(stringResource(it.aliasRes)) },
    )

/** 单个设置项：没有别名（标题就是它自己，同义词在分区那层已经覆盖） */
@Composable
internal fun settingsItemDoc(titleRes: Int): SettingsSearchDoc =
    SettingsSearchDoc(stringResource(titleRes), emptyList())

/**
 * 一条查询对一个可检索对象的打分。0 = 不命中。
 *
 * 空格分隔 = AND（每个词都要命中），分数取和 —— 多个词都命中说明意图更明确。
 * 唯一的例外是噪音词：它自己一个都没命中时不参与判定，因为「软件颜色」里的「软件」
 * 本来就不是要检索的东西。但若整句全是噪音词（用户只打了「软件」），仍然按不命中处理，
 * 否则会把全部设置项都列出来。
 */
internal fun settingsDocScore(
    query: String,
    doc: SettingsSearchDoc,
    noise: Set<String>,
    entryLevel: Boolean = false,
): Int {
    val tokens = query.trim().lowercase().split(' ', '\u3000', '\t').filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return 1
    val title = normalizeForSearch(doc.title)
    if (title.isEmpty()) return 0
    val aliases = doc.aliases.map { normalizeForSearch(it) }.filter { it.isNotEmpty() }
    val discount = if (entryLevel) ALIAS_DISCOUNT + ENTRY_ALIAS_DISCOUNT else ALIAS_DISCOUNT

    var total = 0
    var scored = false
    for (token in tokens) {
        val needle = normalizeForSearch(token)
        if (needle.isEmpty()) continue
        var score = docTokenScore(needle, title, aliases, discount)
        if (score == 0) {
            // 「软件颜色」是一个词时先剥掉「软件」；剥完还不行就还原成整词判定
            val stripped = stripSearchNoise(needle, noise)
            if (stripped != null) score = docTokenScore(stripped, title, aliases, discount)
        }
        if (score > 0) {
            total += score
            scored = true
            continue
        }
        if (needle in noise) continue
        return 0
    }
    if (!scored) return 0
    return if (entryLevel) total + ENTRY_TITLE_BONUS else total
}

/** 一个词对一个对象的得分：标题取最高，别名打一档折扣后再比。 */
private fun docTokenScore(
    needle: String,
    title: String,
    aliases: List<String>,
    discount: Int,
): Int {
    var best = textScore(needle, title)
    for (alias in aliases) {
        val score = textScore(needle, alias)
        if (score == 0) continue
        val discounted = score - discount
        if (discounted > best) best = discounted
    }
    return best.coerceAtLeast(0)
}

/**
 * 剥掉词首/词尾的噪音词（「软件颜色」→「颜色」、「颜色app」→「颜色」）。
 *
 * 返回 null 表示没剥（或剥空了）—— 调用方要能区分「没剥」与「剥成空串」，
 * 后者若当成命中，会把空查询算成全部命中。
 */
internal fun stripSearchNoise(token: String, noise: Set<String>): String? {
    if (noise.isEmpty()) return null
    var current = token
    var changed = false
    var guard = 0
    while (guard++ < MAX_NOISE_STRIPS) {
        val hit = noise.firstOrNull { word ->
            word.isNotEmpty() && current.length > word.length &&
                (current.startsWith(word) || current.endsWith(word))
        } ?: break
        current = if (current.startsWith(hit)) current.removePrefix(hit) else current.removeSuffix(hit)
        changed = true
    }
    return if (changed && current.isNotEmpty()) current else null
}

/**
 * 兼容旧签名的纯文本打分（[SettingsSearchMatchTest] 与「只给一段文字」的场合用）。
 *
 * 与 [settingsDocScore] 的区别：没有别名、没有噪音词剥除 —— 它就是「这段话里有没有这个词」。
 */
internal fun settingsMatchScore(query: String, text: String): Int {
    val tokens = query.trim().lowercase().split(' ', '\u3000', '\t').filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return 1
    val haystack = normalizeForSearch(text)
    if (haystack.isEmpty()) return 0
    var total = 0
    for (token in tokens) {
        val needle = normalizeForSearch(token)
        if (needle.isEmpty()) continue
        val score = textScore(needle, haystack)
        if (score == 0) return 0
        total += score
    }
    return total
}

/** 四档文本关系。单字不做子序列（见文件头）。 */
internal fun textScore(needle: String, haystack: String): Int = when {
    haystack == needle -> SCORE_EXACT
    haystack.startsWith(needle) -> SCORE_PREFIX
    haystack.contains(needle) -> SCORE_CONTAINS
    needle.length >= 2 && isSubsequence(needle, haystack) -> SCORE_SUBSEQUENCE
    else -> 0
}

/** 大小写折叠 + 去掉空白与间隔点：`Theme · color` 与 `theme color` 必须同分。 */
internal fun normalizeForSearch(text: String): String =
    buildString(text.length) {
        for (ch in text) {
            if (ch.isWhitespace() || ch == '·' || ch == '・') continue
            append(ch.lowercaseChar())
        }
    }

/** `needle` 的每个字按顺序出现在 `haystack` 里（允许中间跳字）。 */
internal fun isSubsequence(needle: String, haystack: String): Boolean {
    if (needle.isEmpty()) return true
    var i = 0
    for (ch in haystack) {
        if (ch == needle[i]) {
            i++
            if (i == needle.length) return true
        }
    }
    return false
}
