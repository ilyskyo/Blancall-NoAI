// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

/**
 * Phase 4 的全局读口径开关。
 *
 * 默认 `LEGACY`：所有读路径行为与旧版一致；真实数据影子清单确认后，
 * 改为 `SENTENCE_PROJECTION` 即可切换。保留 `LEGACY` 作为回滚。
 */
object MemoryModelConfig {
    @Volatile
    var model: ArticleMemoryReads.Model = ArticleMemoryReads.Model.LEGACY

    /**
     * 是否仍允许写篇级 `a:` 卡。
     *
     * 切到句级投影后，篇级只作旧数据回退，不能再由新练习推进；否则一篇里有几句到期时，
     * 整篇 `a:` 会被同一场练习再推进一次，形成两套互相打架的调度事实。
     */
    fun writesLegacyArticleStates(): Boolean =
        model == ArticleMemoryReads.Model.LEGACY
}