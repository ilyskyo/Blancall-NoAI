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

    private val _homeBrandExpandedFlow = MutableStateFlow(false)
    /** 首页品牌栏(Blancall 栏)展开状态：拉下后跨页面(如前往设置再返回)保持，直到用户再次下拉/上滑手动收起 */
    val homeBrandExpandedFlow: StateFlow<Boolean> = _homeBrandExpandedFlow.asStateFlow()

    private val _autoIndentEnabledFlow = MutableStateFlow(true)
    /** 段落首行自动缩进开关（导入时给未缩进段落补两格；关闭后不再新增缩进） */
    val autoIndentEnabledFlow: StateFlow<Boolean> = _autoIndentEnabledFlow.asStateFlow()

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

    private val _builtInLibraryKeysFlow = MutableStateFlow<Set<String>>(emptySet())
    /** 已启用的内置素材库 key 集合（如 "western"=西方思想）。非空时底部导航栏显示「素材库」入口；可扩展多个库 */
    val builtInLibraryKeysFlow: StateFlow<Set<String>> = _builtInLibraryKeysFlow.asStateFlow()

    private val _onboardingSeenFlow = MutableStateFlow(false)
    /** 首次使用引导页是否已看过（首启展示一次，之后可在设置里重看） */
    val onboardingSeenFlow: StateFlow<Boolean> = _onboardingSeenFlow.asStateFlow()

    private val _libraryDisclaimerSeenFlow = MutableStateFlow<Set<String>>(emptySet())
    /** 已看过「非官方内容免责提示」的素材库 id 集合（如 "gaokao"），每个库首次打开提示一次 */
    val libraryDisclaimerSeenFlow: StateFlow<Set<String>> = _libraryDisclaimerSeenFlow.asStateFlow()

    private val _pdfViewModeFlow = MutableStateFlow("text")
    /** PDF 预览视图模式：text=纯文本排版，image=原 PDF 图片渲染；跨篇目持久记忆 */
    val pdfViewModeFlow: StateFlow<String> = _pdfViewModeFlow.asStateFlow()
    private val _readingFontFlow = MutableStateFlow(17f)
    /** 阅读字号(px)，14~24 可调 */
    val readingFontFlow: StateFlow<Float> = _readingFontFlow.asStateFlow()

    private val _readingLineHeightFlow = MutableStateFlow(2.0f)
    /** 阅读行距（行高倍数，1.4~2.4） */
    val readingLineHeightFlow: StateFlow<Float> = _readingLineHeightFlow.asStateFlow()

    private val _readingBgModeFlow = MutableStateFlow(0)
    /** 阅读背景模式：0=跟随主题 1=米白纸张 2=纯白（深色模式永远纯黑） */
    val readingBgModeFlow: StateFlow<Int> = _readingBgModeFlow.asStateFlow()

    private val _readingLayoutModeFlow = MutableStateFlow(0)
    /** 阅读布局模式：0=整篇滚动 1=章节翻页 */
    val readingLayoutModeFlow: StateFlow<Int> = _readingLayoutModeFlow.asStateFlow()

    private val _readingFontFamilyFlow = MutableStateFlow(0)
    /** 阅读字体：0=系统默认 1=宋体 2=黑体 3=等宽 */
    val readingFontFamilyFlow: StateFlow<Int> = _readingFontFamilyFlow.asStateFlow()

    private val _readingFontIdFlow = MutableStateFlow("0")
    /** 阅读字体 id（预设/系统字体路径/导入字体），字体选择唯一权威来源 */
    val readingFontIdFlow: StateFlow<String> = _readingFontIdFlow.asStateFlow()

    private val _readingOcclusionEnabledFlow = MutableStateFlow(false)
    /** 是否启用阅读背诵遮挡 */
    val readingOcclusionEnabledFlow: StateFlow<Boolean> = _readingOcclusionEnabledFlow.asStateFlow()

    private val _readingOcclusionModeFlow = MutableStateFlow("local")
    /** 阅读遮挡算法：local=本地算法 / ai=AI 遮挡 */
    val readingOcclusionModeFlow: StateFlow<String> = _readingOcclusionModeFlow.asStateFlow()

    @SuppressLint("ApplySharedPref")
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        _predictiveBackFlow.value = prefs.getBoolean("predictive_back", true)
        _homeBrandExpandedFlow.value = prefs.getBoolean("home_brand_expanded", false)
        _autoIndentEnabledFlow.value = prefs.getBoolean("auto_indent_enabled", true)
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
        _builtInLibraryKeysFlow.value = prefs.getStringSet("built_in_library_keys", emptySet())?.toSet() ?: emptySet()
        _onboardingSeenFlow.value = prefs.getBoolean("onboarding_seen", false)
        _libraryDisclaimerSeenFlow.value = prefs.getStringSet("library_disclaimer_seen", emptySet())?.toSet() ?: emptySet()
        _pdfViewModeFlow.value = prefs.getString("pdf_view_mode", "text") ?: "text"
        _readingFontFlow.value = prefs.getFloat("reading_font", 17f).coerceIn(14f, 24f)
        _readingLineHeightFlow.value = prefs.getFloat("reading_line_height", 2.0f).coerceIn(1.4f, 2.4f)
        _readingBgModeFlow.value = prefs.getInt("reading_bg_mode", 0)
        _readingLayoutModeFlow.value = prefs.getInt("reading_layout_mode", 0).coerceIn(0, 1)
        _readingFontFamilyFlow.value = prefs.getInt("reading_font_family", 0).coerceIn(0, 3)
        // 阅读字体 id（"0".."3"=预设；绝对路径=系统字体；"imp:x"=导入字体）。
        // 首次升级迁移：旧版仅有 reading_font_family，将其转换为等价 fontId 保留用户选择。
        if (!prefs.contains("reading_font_id")) {
            _readingFontIdFlow.value = prefs.getInt("reading_font_family", 0).coerceIn(0, 3).toString()
        } else {
            _readingFontIdFlow.value = prefs.getString("reading_font_id", "0") ?: "0"
        }
        _readingOcclusionEnabledFlow.value = prefs.getBoolean("reading_occlusion_enabled", false)
        _readingOcclusionModeFlow.value =
            prefs.getString("reading_occlusion_mode", "local")?.takeIf { it == "ai" || it == "local" } ?: "local"
    }

    var predictiveBackEnabled: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean("predictive_back", true) else true
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putBoolean("predictive_back", value) }
                _predictiveBackFlow.value = value
            }
        }

    /** 段落首行自动缩进开关 */
    var autoIndentEnabled: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean("auto_indent_enabled", true) else true
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putBoolean("auto_indent_enabled", value) }
                _autoIndentEnabledFlow.value = value
            }
        }

    /** 首页品牌栏(Blancall 栏)展开状态：展开后跨页面/跨启动保持，直到用户手动收起 */
    var homeBrandExpanded: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean("home_brand_expanded", false) else false
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putBoolean("home_brand_expanded", value) }
                _homeBrandExpandedFlow.value = value
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

    /** 判断某个内置素材库是否已启用（如 "western"=西方思想） */
    fun isLibraryEnabled(key: String): Boolean =
        if (::prefs.isInitialized) prefs.getStringSet("built_in_library_keys", emptySet())?.contains(key) == true else false

    /** 启用/禁用某个内置素材库；空集合表示未启用任何库，底部「素材库」入口随之隐藏 */
    fun setLibraryEnabled(key: String, enabled: Boolean) {
        if (!::prefs.isInitialized) return
        val set = (prefs.getStringSet("built_in_library_keys", emptySet()) ?: emptySet()).toMutableSet()
        if (enabled) set.add(key) else set.remove(key)
        prefs.edit { putStringSet("built_in_library_keys", set) }
        _builtInLibraryKeysFlow.value = set.toSet()
    }

    /** 首次使用引导页是否已看过；首启展示一次后置为 true，之后可从设置里重看 */
    var onboardingSeen: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean("onboarding_seen", false) else false
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putBoolean("onboarding_seen", value) }
                _onboardingSeenFlow.value = value
            }
        }

    /** 某个素材库是否已看过「非官方内容免责提示」（如 "gaokao"） */
    fun isDisclaimerSeen(libId: String): Boolean =
        if (::prefs.isInitialized) prefs.getStringSet("library_disclaimer_seen", emptySet())?.contains(libId) == true else false

    /** 标记某个素材库已看过免责提示（之后不再弹） */
    fun markDisclaimerSeen(libId: String) {
        if (!::prefs.isInitialized) return
        val set = (prefs.getStringSet("library_disclaimer_seen", emptySet()) ?: emptySet()).toMutableSet()
        set.add(libId)
        prefs.edit { putStringSet("library_disclaimer_seen", set) }
        _libraryDisclaimerSeenFlow.value = set.toSet()
    }

    /** 设置 PDF 预览视图模式（text=纯文本排版 / image=原 PDF 图片渲染），跨篇目持久记忆 */
    fun setPdfViewMode(mode: String) {
        if (!::prefs.isInitialized) return
        prefs.edit { putString("pdf_view_mode", mode) }
        _pdfViewModeFlow.value = mode
    }

    /** 当前 PDF 预览视图模式（text=纯文本排版 / image=原 PDF 图片渲染） */
    fun pdfViewMode(): String = _pdfViewModeFlow.value
    // ── 沉浸阅读模式设置 ──

    /** 阅读字号(px) */
    var readingFont: Float
        get() = if (::prefs.isInitialized) _readingFontFlow.value else 17f
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putFloat("reading_font", value.coerceIn(14f, 24f)) }
                _readingFontFlow.value = value.coerceIn(14f, 24f)
            }
        }

    /** 阅读行距（行高倍数） */
    var readingLineHeight: Float
        get() = if (::prefs.isInitialized) _readingLineHeightFlow.value else 2.0f
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putFloat("reading_line_height", value.coerceIn(1.4f, 2.4f)) }
                _readingLineHeightFlow.value = value.coerceIn(1.4f, 2.4f)
            }
        }

    /** 阅读背景模式：0=跟随主题 1=米白 2=纯白（深色模式永远纯黑） */
    var readingBgMode: Int
        get() = if (::prefs.isInitialized) _readingBgModeFlow.value else 0
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putInt("reading_bg_mode", value.coerceIn(0, 2)) }
                _readingBgModeFlow.value = value.coerceIn(0, 2)
            }
        }

    /** 阅读布局模式：0=整篇滚动 1=章节翻页 */
    var readingLayoutMode: Int
        get() = if (::prefs.isInitialized) _readingLayoutModeFlow.value else 0
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putInt("reading_layout_mode", value.coerceIn(0, 1)) }
                _readingLayoutModeFlow.value = value.coerceIn(0, 1)
            }
        }

    /** 阅读字体：0=系统默认 1=宋体(Serif) 2=黑体(SansSerif) 3=等宽(Monospace) */
    var readingFontFamily: Int
        get() = if (::prefs.isInitialized) _readingFontFamilyFlow.value else 0
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putInt("reading_font_family", value.coerceIn(0, 3)) }
                _readingFontFamilyFlow.value = value.coerceIn(0, 3)
            }
        }

    /** 阅读字体 id：预设("0".."3") / 系统字体绝对路径 / 导入字体("imp:文件名")，作为字体选择的唯一权威来源 */
    var readingFontId: String
        get() = if (::prefs.isInitialized) _readingFontIdFlow.value else "0"
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putString("reading_font_id", value) }
                _readingFontIdFlow.value = value
            }
        }

    /** 是否启用阅读背诵遮挡 */
    var readingOcclusionEnabled: Boolean
        get() = if (::prefs.isInitialized) _readingOcclusionEnabledFlow.value else false
        set(value) {
            if (::prefs.isInitialized) {
                prefs.edit { putBoolean("reading_occlusion_enabled", value) }
                _readingOcclusionEnabledFlow.value = value
            }
        }

    /** 阅读遮挡算法：local=本地算法 / ai=AI 遮挡 */
    var readingOcclusionMode: String
        get() = if (::prefs.isInitialized) _readingOcclusionModeFlow.value else "local"
        set(value) {
            val v = if (value == "ai" || value == "local") value else "local"
            if (::prefs.isInitialized) {
                prefs.edit { putString("reading_occlusion_mode", v) }
                _readingOcclusionModeFlow.value = v
            }
        }

    /** 文章阅读位置（0~1 全文进度），按字符比例存储，字号变化后仍可定位 */
    fun getReadingPos(articleId: Long): Float =
        if (::prefs.isInitialized) prefs.getFloat("reading_pos_${articleId}", 0f) else 0f

    /** 保存文章阅读位置（0~1 全文进度） */
    fun setReadingPos(articleId: Long, fraction: Float) {
        if (::prefs.isInitialized) {
            prefs.edit { putFloat("reading_pos_${articleId}", fraction.coerceIn(0f, 1f)) }
        }
    }

    /** 文章累计阅读秒数（跨次进入累计） */
    fun getReadingSeconds(articleId: Long): Long =
        if (::prefs.isInitialized) prefs.getLong("reading_sec_${articleId}", 0L) else 0L

    /** 追加文章阅读秒数 */
    fun addReadingSeconds(articleId: Long, seconds: Long) {
        if (::prefs.isInitialized && seconds > 0) {
            prefs.edit { putLong("reading_sec_${articleId}", getReadingSeconds(articleId) + seconds) }
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
