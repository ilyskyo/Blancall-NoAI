// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.ilyskyo.blancall.algorithm.FsrsEngine
import com.ilyskyo.blancall.algorithm.MemoryModelConfig
import com.ilyskyo.blancall.data.repository.FsrsStateStore
import com.ilyskyo.blancall.data.repository.ReviewLogStore
import com.ilyskyo.blancall.data.repository.ReviewSource
import com.ilyskyo.blancall.ui.theme.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 提交后的评分辅助（FSRS 状态更新与评级解析）。
 *
 * 从 PracticeViewModel.kt 拆出（纯搬移；行为不变）。
 */

    /**
     * 练习结束后更新 FSRS 记忆状态：评级 → 状态更新 → 持久化。
     * FSRS 按文章维护独立难度与稳定性，实现完全自适应的复习调度。
     * 任何失败都不影响练习主流程。
     */
    internal fun PracticeViewModel.updateFsrsState(
        articleId: Long,
        rating: FsrsEngine.Rating,
        source: ReviewSource,
        recordId: Long,
    ) {
        // 跨文练习的 articleId = -1（混合内容无单篇归属）：负/零 id 不写 FSRS，避免孤儿状态
        if (articleId <= 0L) return
        // 投影口径下篇级只作旧数据回退：新练习只推进句子，不能再写 a:。
        if (!MemoryModelConfig.writesLegacyArticleStates()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val store = FsrsStateStore.getInstance(
                        getApplication<Application>().filesDir.resolve("fsrs_state.json").absolutePath
                    )
                    // 必须先等初始加载完成：冷启动经通知直达练习时，store 可能还在读盘，
                    // 以空 CardState 为基线会把 reviewCount/stability 归零（整段记忆史丢失）。
                    store.awaitLoaded()
                    // 读旧值、算新值、落盘必须在 store 的同一把锁里完成：
                    // 旧实现先 get 再 save，两次提交交错时后写会覆盖前写。
                    val now = System.currentTimeMillis()
                    val updated = store.updateWithPrevious(articleId) { prev ->
                        FsrsEngine.review(prev, rating, now)
                    } ?: return@withContext
                    val (prev, newState) = updated
                    // 练习事件日志（B3-4）：每次提交都写，含第一次；与状态更新同批落盘
                    ReviewLogStore.append(
                        reviewLogFile(), "a:$articleId", now, rating.value,
                        elapsedMs = if (prev.lastReview > 0) now - prev.lastReview else 0L,
                        state = newState,
                        source = source,
                        recordId = recordId
                    )
                } catch (_: Exception) {
                    /* FSRS 状态更新失败不影响主流程 */
                }
            }
        }
    }

    /**
     * 评级解析：默认按默写相似度→四档（FSRS-6 产品语义）；
     * 回退开关开启旧行为时按正确率→四档。
     *
     * 封顶规则：本次有任何一个空是被「强提示自动填入」的，评级最高只给 HARD。
     * 判分仍算它对（用户看到的确实是正确答案），但记忆状态不能被提示喂出来 ——
     * 否则等 20 秒让系统填满就能拿 EASY 与最长间隔。
     */
    internal fun PracticeViewModel.resolveRating(similarity: Float, accuracy: Float): FsrsEngine.Rating {
        val raw = if (AppPrefs.useSimilarityRating) {
            FsrsEngine.gradeFromSimilarity(similarity)
        } else {
            FsrsEngine.ratingFromAccuracy(accuracy)
        }
        // 反向默写的强提示没有「空序号」可记（整段输入），但同样不能把提示喂出来的字
        // 算成 EASY：_strongHintCount 在任何模式只要发生过强填就 > 0，统一封顶。
        return if ((strongHintedBlanks.isNotEmpty() || _strongHintCount.value > 0) &&
            raw.value > FsrsEngine.Rating.HARD.value
        ) {
            FsrsEngine.Rating.HARD
        } else {
            raw
        }
    }

    /**
     * 逐句写回句子级 FSRS 状态（键 = `s:<articleId>:<hash16>`，与每日一句同一命名空间）。
     *
     * 这里刻意不复用 [resolveRating] 的相似度通道：逐句只有「这几个空写对几个」，
     * 没有整段默写相似度这个概念，硬套会让 `useSimilarityRating` 那个开关在句子级悄悄失效。
     * 写入走 IO 线程、任何失败只记日志（与 [updateFsrsState] 同样的「绝不影响练习主流程」）。
     */
    internal fun PracticeViewModel.updateSentenceStates(
        ratings: Map<String, FsrsEngine.Rating>,
        source: ReviewSource,
        recordId: Long,
    ) {
        if (ratings.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val store = FsrsStateStore.getInstance(
                        getApplication<Application>().filesDir.resolve("fsrs_state.json").absolutePath
                    )
                    store.awaitLoaded()
                    val now = System.currentTimeMillis()
                    // 「读旧值 + 计算 + 写回」必须在 store 的同一把锁里完成：
                    // 先逐句 get、再统一 save 会让并发提交互相覆盖，reviewCount 少推进一次。
                    val transitions = store.updateSentences(
                        ratings.mapValues { (_, rating) ->
                            { previous: FsrsEngine.CardState -> FsrsEngine.review(previous, rating, now) }
                        }
                    )
                    if (transitions.isEmpty()) return@withContext
                    val events = transitions.map { transition ->
                        ReviewLogStore.ReviewEvent(
                            key = transition.key,
                            ts = now,
                            rating = ratings.getValue(transition.key).value,
                            elapsedMs = if (transition.previous.lastReview > 0) {
                                now - transition.previous.lastReview
                            } else {
                                0L
                            },
                            state = transition.next,
                            source = source,
                            recordId = recordId
                        )
                    }
                    ReviewLogStore.appendAll(reviewLogFile(), events)
                } catch (_: Exception) {
                    /* 句子级状态写失败不影响本次练习结果 */
                }
            }
        }
    }

/** 练习事件日志文件（B3-4）：filesDir 根，命名与 [ReviewLogStore] 同源 */
private fun PracticeViewModel.reviewLogFile(): File =
    getApplication<Application>().filesDir.resolve(ReviewLogStore.FILE_NAME)
