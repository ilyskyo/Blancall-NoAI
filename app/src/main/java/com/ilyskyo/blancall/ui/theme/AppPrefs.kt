package com.ilyskyo.blancall.ui.theme

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.ilyskyo.blancall.algorithm.FsrsEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 应用偏好设置（SharedPreferences 持久化）
 */
object AppPrefs {
    private lateinit var prefs: SharedPreferences

    /**
     * 已知图标 key 集合（[com.ilyskyo.blancall.ui.common.AppIconKind] 的小写名）。
     * SP key 沿用旧 "emoji_icon" 以兼容历史值；init() 中若存储值不在本集合（如旧 emoji），
     * 迁移重置为默认 "logo"。
     */
    private val KNOWN_ICON_KEYS = setOf(
        "logo", "celebrate", "edit", "inbox", "arrowforward", "openinfull", "check"
    )

    private val _predictiveBackFlow = MutableStateFlow(true)
    /** 响应式状态流，Compose 中通过 collectAsState() 订阅 */
    val predictiveBackFlow: StateFlow<Boolean> = _predictiveBackFlow.asStateFlow()

    private val _accentColorFlow = MutableStateFlow(0)
    /** 主题色索引（0=靛蓝 1=海蓝 2=翠绿 3=暖橙 4=玫红 5=石墨） */
    val accentColorFlow: StateFlow<Int> = _accentColorFlow.asStateFlow()

    private val _homeIconKeyFlow = MutableStateFlow("logo")
    /** 首页 Logo 图标 key（AppIconKind 的小写名，如 "logo" / "celebrate" …） */
    val homeIconKeyFlow: StateFlow<String> = _homeIconKeyFlow.asStateFlow()

    private val _subtitleFlow = MutableStateFlow("Fill the blank, recall the knowledge.")
    /** 首页副标题（默认品牌标语，可自定义） */
    val subtitleFlow: StateFlow<String> = _subtitleFlow.asStateFlow()

    private val _showHomeEmojiFlow = MutableStateFlow(true)
    /** 首页左上角表情图标显示开关 */
    val showHomeEmojiFlow: StateFlow<Boolean> = _showHomeEmojiFlow.asStateFlow()

    private val _lightBeigeBackgroundFlow = MutableStateFlow(false)
    /** 浅色模式米黄底色开关：开启使用暖米黄底色，关闭使用纯白底色（深色模式不受影响） */
    val lightBeigeBackgroundFlow: StateFlow<Boolean> = _lightBeigeBackgroundFlow.asStateFlow()

    private val _reviewTemplateFlow = MutableStateFlow("standard")
    /** 复习模板 ID（sprint / standard / deep） */
    val reviewTemplateFlow: StateFlow<String> = _reviewTemplateFlow.asStateFlow()

    private val _hiddenArticleIdsFlow = MutableStateFlow<Set<Long>>(emptySet())
    /** 首页隐藏的文章 ID 集合（仅从首页“最近文章”移除，不从文章列表删除） */
    val hiddenArticleIdsFlow: StateFlow<Set<Long>> = _hiddenArticleIdsFlow.asStateFlow()
    
    private val _firstLaunchDoneFlow = MutableStateFlow(false)
    /** 首次使用引导是否已完成（开屏页 + 欢迎帮助页只出现在第一次使用） */
    val firstLaunchDoneFlow: StateFlow<Boolean> = _firstLaunchDoneFlow.asStateFlow()
    
    private val _useSimilarityRatingFlow = MutableStateFlow(true)
    /** 练习评级方式：true=默写相似度→四档（FSRS-6 默认）；false=旧正确率→四档（回退） */
    val useSimilarityRatingFlow: StateFlow<Boolean> = _useSimilarityRatingFlow.asStateFlow()

    private val _bottomNavEnabledFlow = MutableStateFlow(true)
    /** 底部导航栏开关：开启后底部显示 首页/我的文章/数据 三个入口，首页左下角入口按钮隐藏 */
    val bottomNavEnabledFlow: StateFlow<Boolean> = _bottomNavEnabledFlow.asStateFlow()

    @SuppressLint("ApplySharedPref")
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        _predictiveBackFlow.value = prefs.getBoolean("predictive_back", true)
        _accentColorFlow.value = prefs.getInt("accent_color", 0)
        _homeIconKeyFlow.value = prefs.getString("emoji_icon", "logo")?.takeIf { it in KNOWN_ICON_KEYS } ?: "logo"
        _subtitleFlow.value = prefs.getString("subtitle", "Fill the blank, recall the knowledge.") ?: "Fill the blank, recall the knowledge."
        _showHomeEmojiFlow.value = prefs.getBoolean("show_home_emoji", false)
        _lightBeigeBackgroundFlow.value = prefs.getBoolean("light_beige_background", false)
        _reviewTemplateFlow.value = prefs.getString("review_template", "standard") ?: "standard"
        _hiddenArticleIdsFlow.value = prefs.getStringSet("hidden_articles", emptySet())
            ?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
        _firstLaunchDoneFlow.value = prefs.getBoolean("first_launch_done", false)
        _useSimilarityRatingFlow.value = prefs.getBoolean("use_similarity_rating", true)
        _bottomNavEnabledFlow.value = prefs.getBoolean("bottom_nav_enabled", true)
    }

    var predictiveBackEnabled: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean("predictive_back", true) else true
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putBoolean("predictive_back", value) }
                _predictiveBackFlow.value = value
            }
        }

    var accentColorIndex: Int
        get() = if (::prefs.isInitialized) prefs.getInt("accent_color", 0) else 0
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putInt("accent_color", value) }
                _accentColorFlow.value = value
            }
        }

    var homeIconKey: String
        get() = if (::prefs.isInitialized) {
            (prefs.getString("emoji_icon", "logo") ?: "logo").takeIf { it in KNOWN_ICON_KEYS } ?: "logo"
        } else {
            "logo"
        }
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putString("emoji_icon", value) }
                _homeIconKeyFlow.value = value
            }
        }

    var subtitle: String
        get() = if (::prefs.isInitialized) prefs.getString("subtitle", "Fill the blank, recall the knowledge.") ?: "Fill the blank, recall the knowledge." else "Fill the blank, recall the knowledge."
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putString("subtitle", value) }
                _subtitleFlow.value = value
            }
        }

    var showHomeEmoji: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean("show_home_emoji", true) else true
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putBoolean("show_home_emoji", value) }
                _showHomeEmojiFlow.value = value
            }
        }

    /** 浅色模式米黄底色开关（深色模式始终纯黑） */
    var lightBeigeBackgroundEnabled: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean("light_beige_background", false) else false
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putBoolean("light_beige_background", value) }
                _lightBeigeBackgroundFlow.value = value
            }
        }

    var reviewTemplateId: String
        get() = if (::prefs.isInitialized) prefs.getString("review_template", "standard") ?: "standard" else "standard"
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putString("review_template", value) }
                _reviewTemplateFlow.value = value
                // 同步 FSRS 目标留存率（「复习频率」设置 = FSRS 复习强度）
                FsrsEngine.configure(FsrsEngine.DEFAULT_PARAMS, FsrsEngine.retentionForTemplate(value))
            }
        }

    /** 将文章加入首页隐藏列表（仅从首页"最近文章"移除，不从文章列表删除） */
    fun hideArticleFromHome(id: Long) {
        if (::prefs.isInitialized) {
            val current = _hiddenArticleIdsFlow.value
            if (id !in current) {
                val updated = current + id
                prefs.edit { putStringSet("hidden_articles", updated.map { it.toString() }.toSet()) }
                _hiddenArticleIdsFlow.value = updated
            }
        }
    }

    /** 练习评级方式开关：相似度→四档（默认）/ 正确率→四档（回退旧行为） */
    var useSimilarityRating: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean("use_similarity_rating", true) else true
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putBoolean("use_similarity_rating", value) }
                _useSimilarityRatingFlow.value = value
            }
        }

    /** 底部导航栏开关（开启后底部出现 首页/我的文章/数据 导航） */
    var bottomNavEnabled: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean("bottom_nav_enabled", true) else true
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putBoolean("bottom_nav_enabled", value) }
                _bottomNavEnabledFlow.value = value
            }
        }

    /** 首次使用引导是否已完成（开屏页与欢迎帮助页只在第一次出现） */
    var firstLaunchDone: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean("first_launch_done", false) else false
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putBoolean("first_launch_done", value) }
                _firstLaunchDoneFlow.value = value
            }
        }
}
