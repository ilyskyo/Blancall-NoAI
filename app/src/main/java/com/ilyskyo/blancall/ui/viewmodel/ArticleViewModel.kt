// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ilyskyo.blancall.algorithm.SentenceSelector
import com.ilyskyo.blancall.data.model.Article
import com.ilyskyo.blancall.data.repository.ArticleRepository
import com.ilyskyo.blancall.data.repository.CustomClozeStore
import com.ilyskyo.blancall.data.repository.FsrsStateStore
import com.ilyskyo.blancall.data.repository.HomeLayoutStore
import com.ilyskyo.blancall.data.repository.InkStore
import com.ilyskyo.blancall.data.repository.MaskConfigStore
import com.ilyskyo.blancall.data.repository.RecordRepository
import com.ilyskyo.blancall.data.repository.ReaderPrefsStore
import com.ilyskyo.blancall.data.repository.ReadingMarkStore
import com.ilyskyo.blancall.data.repository.SentenceCardStore
import com.ilyskyo.blancall.data.repository.TagStore
import com.ilyskyo.blancall.ui.theme.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArticleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ArticleRepository.getInstance(
        application.filesDir.resolve("articles.json").absolutePath
    )

    /**
     * 编辑保存后「已清理多少句失效记忆状态」的一次性提示数（0 = 无提示）。
     *
     * 走 StateFlow 而不是回调：清理发生在 VM 的协程里，而提示要在编辑页上弹 Toast ——
     * 回调会把这个协程的生命周期绑到某个 Composable 上，退出编辑页时回调就丢了，
     * 于是「改了文章但没告诉我状态被清了」这类静默丢失又回来了。UI 消费后调 [consumeOrphanNotice]。
     */
    private val _orphanStatesPruned = MutableStateFlow(0)
    val orphanStatesPruned: StateFlow<Int> = _orphanStatesPruned.asStateFlow()

    /** 提示已展示，计数归零（避免旋转屏幕后重复弹） */
    fun consumeOrphanNotice() {
        _orphanStatesPruned.value = 0
    }

    val articles: StateFlow<List<Article>> = repository.allArticles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 冷启动门闩：文章库异步读盘，首页在就绪前不能把空列表当成真实空态。 */
    suspend fun awaitLoaded() = repository.awaitLoaded()

    fun insertArticle(
        title: String,
        content: String,
        autoIndent: Boolean = true,
        author: String = ""
    ) {
        viewModelScope.launch {
            try {
                repository.insert(
                    Article(title = title, content = content, autoIndent = autoIndent, author = author)
                )
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "insertArticle failed", e)
            }
        }
    }

    suspend fun insertArticleBlocking(
        title: String,
        content: String,
        autoIndent: Boolean = true,
        author: String = ""
    ): Long {
        return repository.insert(
            Article(title = title, content = content, autoIndent = autoIndent, author = author)
        )
    }

    fun deleteArticle(article: Article) = deleteArticles(listOf(article))

    /**
     * 批量删除：文章库与首页卡片各只落盘一次，其余按 id 级联。
     * 旧实现多选删除逐篇起协程，每篇都全量重写 articles/records/fsrs，
     * 且 HomeLayout 的 getCards/saveCards 之间没有同一把锁，交错时会互相覆盖。
     */
    fun deleteArticles(articles: List<Article>) {
        if (articles.isEmpty()) return
        val ids = articles.map { it.id }.toSet()
        viewModelScope.launch {
            try {
                repository.deleteAll(ids)
                val filesDir = getApplication<Application>().filesDir
                val recordRepo = RecordRepository.getInstance(
                    filesDir.resolve("records.json").absolutePath
                )
                val fsrsStore = FsrsStateStore.getInstance(
                    filesDir.resolve("fsrs_state.json").absolutePath
                )
                withContext(Dispatchers.IO) {
                    try {
                        ids.forEach { recordRepo.deleteByArticleId(it) }
                        ids.forEach {
                            fsrsStore.remove(it)
                            fsrsStore.removeSentencesForArticle(it)
                        }
                        val clozeStore = CustomClozeStore.getInstance(filesDir)
                        val maskStore = MaskConfigStore.getInstance(filesDir)
                        val layout = HomeLayoutStore.getInstance(filesDir)
                        // 先根据卡片反查（旧卡无 articleId 时需查配置库）再删配置，顺序不可颠倒
                        val cards = layout.getCards()
                        val keep = cards.filterNot { c ->
                            ids.any { id ->
                                when (c.type) {
                                    HomeLayoutStore.CardType.CUSTOM_CLOZE ->
                                        c.articleId == id ||
                                            (c.articleId <= 0L && runCatching { clozeStore.findArticleIdByConfigId(c.refId) }.getOrNull() == id)
                                    HomeLayoutStore.CardType.CUSTOM_MASK ->
                                        c.articleId == id ||
                                            (c.articleId <= 0L && runCatching { maskStore.findArticleIdByConfigId(c.refId) }.getOrNull() == id)
                                    HomeLayoutStore.CardType.ARTICLE ->
                                        c.refId == id || c.id == HomeLayoutStore.articleCardId(id)
                                    else -> false
                                }
                            }
                        }
                        if (keep.size != cards.size) layout.saveCards(keep)
                        ids.forEach { id ->
                            clozeStore.removeArticle(id)
                            maskStore.removeArticle(id)
                            filesDir.resolve("practice_state_$id.json").delete()
                            SentenceCardStore.getInstance(filesDir).clearIfArticle(id)
                            TagStore.getInstance(filesDir).removeArticle(id)
                            ReaderPrefsStore.getInstance(getApplication()).removeArticle(id)
                            ReadingMarkStore.getInstance(filesDir).removeArticle(id)
                            AppPrefs.clearArticleReadingState(id)
                            InkStore.getInstance(filesDir.resolve("ink").absolutePath)
                                .deleteByArticleId(id)
                        }
                    } catch (e: Exception) {
                        Log.e("ArticleViewModel", "deleteArticles cascade cleanup failed", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "deleteArticles failed", e)
            }
        }
    }

    fun updateArticle(article: Article) {
        viewModelScope.launch {
            try {
                repository.update(article.copy(updatedAt = System.currentTimeMillis()))
                pruneOrphanSentenceStates(article)
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "updateArticle failed", e)
            }
        }
    }

    /**
     * 编辑期句子孤儿清理（2026-10-13 用户拍板：**prune + 提示**，不做「迁移」）。
     *
     * 为什么选 prune：迁移要按相似度把旧句的记忆状态搬到新句，而相似度会误配 ——
     * 把 A 句的记忆挂到 B 句上，界面上完全看不出来（两句都「有状态」），
     * 却让调度器按一句从未被记过的话来安排复习，比没有状态更难发现、更难纠正。
     * prune 的代价只是「改写过的句子要重新学」——这个代价用户看得见，也补得回来。
     *
     * 失败一律静默：清理是存储卫生，不该让「保存文章」这件事失败。
     */
    private suspend fun pruneOrphanSentenceStates(article: Article) {
        try {
            val store = FsrsStateStore.getInstance(
                getApplication<Application>().filesDir.resolve("fsrs_state.json").absolutePath
            )
            // await/allSentenceStates/removeSentenceKeys 里有阻塞等锁与整文件落盘，
            // 全部放到 IO；调用方是 viewModelScope（Main）。
            val removed = withContext(Dispatchers.IO) {
                store.awaitLoaded()
                val orphans = SentenceSelector.orphanSentenceKeys(
                    store.allSentenceStates().keys, article.id, article.content
                )
                store.removeSentenceKeys(orphans)
            }
            if (removed > 0) _orphanStatesPruned.value = removed
        } catch (e: Exception) {
            Log.e("ArticleViewModel", "pruneOrphanSentenceStates failed", e)
        }
    }

    suspend fun getArticleById(id: Long): Article? {
        return repository.getArticleById(id)
    }
}
