// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.settings

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.ilyskyo.blancall.ui.theme.SmoothRoundedCornerShape
import com.ilyskyo.blancall.ui.theme.isBlancallDark
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.ilyskyo.blancall.MainActivity
import com.ilyskyo.blancall.R
import com.ilyskyo.blancall.algorithm.ReviewTemplate
import com.ilyskyo.blancall.data.backup.BackupManager
import com.ilyskyo.blancall.data.repository.InkStore
import com.ilyskyo.blancall.data.repository.RecordRepository
import com.ilyskyo.blancall.data.model.PracticeRecord
import com.ilyskyo.blancall.data.backup.LearningDataReloader
import com.ilyskyo.blancall.notification.NotificationHelper
import com.ilyskyo.blancall.notification.ReminderNotifications
import com.ilyskyo.blancall.notification.ReminderPlan
import com.ilyskyo.blancall.notification.ReminderSchedule
import com.ilyskyo.blancall.notification.ReminderWorker
import com.ilyskyo.blancall.ui.common.AdaptiveChipRow
import com.ilyskyo.blancall.ui.common.AppIcon
import com.ilyskyo.blancall.ui.common.AppLocale

import com.ilyskyo.blancall.ui.common.AnimatedOverlayPresence
import com.ilyskyo.blancall.ui.common.AppIconKind
import com.ilyskyo.blancall.ui.common.BackButton
import com.ilyskyo.blancall.ui.common.BlancallAlertDialog
import com.ilyskyo.blancall.ui.common.DeleteConfirmDialog
import com.ilyskyo.blancall.ui.common.GlassDropdownMenu
import com.ilyskyo.blancall.ui.common.GlassMenuItem
import com.ilyskyo.blancall.ui.common.HapticTier
import com.ilyskyo.blancall.ui.common.rememberHaptic
import com.ilyskyo.blancall.ui.common.GlassButton
import com.ilyskyo.blancall.ui.common.GlassCard
import com.ilyskyo.blancall.ui.common.GlassSwitch
import com.ilyskyo.blancall.ui.common.Motion
import com.ilyskyo.blancall.ui.common.MotionFade
import com.ilyskyo.blancall.ui.common.SymbolKey
import com.ilyskyo.blancall.ui.common.LocalIsLargeScreen
import com.ilyskyo.blancall.ui.common.MarkdownText
import com.ilyskyo.blancall.ui.common.PressTier
import com.ilyskyo.blancall.ui.common.pressClick
import com.ilyskyo.blancall.ui.theme.AccentPresets
import com.ilyskyo.blancall.ui.theme.AppPrefs
import com.ilyskyo.blancall.ui.theme.ReminderFrequency
import com.ilyskyo.blancall.ui.theme.ReminderPrefs
import com.ilyskyo.blancall.ui.theme.ThemeManager
import com.ilyskyo.blancall.ui.theme.ThemeMode
import com.ilyskyo.blancall.ui.theme.AppearanceSnapshot
import com.ilyskyo.blancall.ui.theme.AppearanceStore
import com.ilyskyo.blancall.ui.common.GlassModalBottomSheet
import com.ilyskyo.blancall.ui.tag.TagColorPicker
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.window.DialogProperties
import com.ilyskyo.blancall.ui.common.rememberConfirmHaptic
import kotlinx.coroutines.delay

/**
 * 语言名用固定字形渲染。
 *
 * 为什么不能只靠 localeList：部分 OEM 的字体栈不消费 Paint.textLocales（HyperOS 实测），
 * 汉字字形由应用配置语言选中的字体族决定 —— 日语界面下「繁體中文」会变成日文字形，
 * 每次切换界面语言都会跟着变。这里给每个语言名指定固定的内置子集字体，
 * 不受界面语言与系统字体影响。「跟随系统」不是语言名、而是随界面本地化的文案，
 * 保持继承界面语言。
 */
private fun localeNameStyle(choice: AppLocale, base: TextStyle): TextStyle =
    if (choice == AppLocale.FollowSystem) base
    else base.copy(fontFamily = FontFamily(Font(languageLabelFontRes(choice))))

/**
 * 设置页当前该呈现哪一种形态 —— 下面那个 AnimatedContent 的 targetState。
 *
 * 三态合一：紧凑顶层 / 完整布局 / 某个入口的二级页。带 entryId 而不是枚举，
 * 与 rememberSaveable 那份状态同源，转屏重建后两处不会各说各话。
 *
 * 注意存的是**入口**（[SettingsEntry]）而不是分区（[SettingsGroup]）：
 * 一级目录行是入口，一个入口可能展开两三个分区（见 SettingsEntry.groups）。
 */
/**
 * 备份导入闸门（进程级）。
 *
 * 导入是整库替换 + 全量重载，要跑几秒到十几秒。原先确认后弹窗立刻消失、界面毫无变化，
 * 用户会以为没点上，再点一次 —— 两条 NonCancellable 协程并发写同一个 filesDir，
 * 这是真会把学习数据写坏的路径。
 *
 * 状态放文件级而不是 remember：离开设置页再回来，闸门必须还锁着；
 * 同时它必须是 Compose State —— 旧页面销毁后，新页面的进度框要能跟着协程结束一起消失。
 */
private object BackupImportGate {
    var running by mutableStateOf(false)
}

private data class SettingsScreenKey(val compact: Boolean, val entryId: String?)

/**
 * 切换回执要退回的那一态。
 *
 * 只记「哪一形态、哪一层」：滚动位置不用记 —— 两份 ScrollState 提到页面这一级，
 * 从没被销毁过，切回去就是原位。
 */
private data class SettingsSwapReceipt(val compact: Boolean, val entryId: String?)

/** 形态切换时新内容的垂直行程：18dp 够读出方向，又不至于读成「翻页」。 */
private val SETTINGS_SWAP_EDGE = 18.dp

/**
 * 新内容起手的纵向缩放。
 *
 * 2026-11-18 由 0.985 收到 0.99：容器是 `weight(1f)` 撑满的固定高度，缩放会让整块向内收 ——
 * 600dp 宽下 0.985 是四边各让 4.5dp、700dp 高时底部收 5dp，慢放能看见最后一屏上移再回位。
 * 0.99 的收幅减半，「正在长出来」的重量几乎不变，边缘抖动减半。
 */
private const val SETTINGS_SWAP_SCALE = 0.99f

/** 超过这个系统字号就禁用缩放：字形被纵向压 1%，在 28sp 标题上就是能看见的「被挤了一下」。 */
private const val SETTINGS_SWAP_SCALE_MAX_FONT = 1.15f

/** 转场锁：这段时间内再长按不改目标态（三棵树叠着 = 糊）。 */
private const val SETTINGS_SWAP_LOCK_MS = 260L

/** 回执停留 3.2s。它只是「刚才那一下是我干的、能退」，不是要用户读的通知。 */
private const val SETTINGS_RECEIPT_MS = 3200L

/** 逐块展开的步长与封顶档数：12 组全按 30ms 递进要 360ms，比转场本身还长。 */
private const val SETTINGS_REVEAL_STEP_MS = 30L
private const val SETTINGS_REVEAL_MAX_STEP = 4

/** 逐块展开最晚结束的时刻：锁 260 + 最后一档 120 + 单块约 230，留一点余量后清标志。 */
private const val SETTINGS_REVEAL_TOTAL_MS = 600L

/** 分区块之间的间距（与 Column 的 spacedBy 同值；块内要保持同一节奏）。 */
private val SETTINGS_BLOCK_GAP = 12.dp

/** 长按热区在标题文字左右各留的余量。 */
private val SETTINGS_HOT_PAD = 8.dp

/** 标题换字时那 2dp 上移。 */
private val SETTINGS_TITLE_RISE = 2.dp

/** 预热进度走到这里就浮出目标态名（约 175ms，比 500ms 的阈值早得多 —— 它是预告）。 */
private const val SETTINGS_AIM_THRESHOLD = 0.35f

/** 预热细线的粗细。 */
private val SETTINGS_ARM_LINE = 2.dp

/** 锚定时往上留的余量：分区标题不要正好贴着顶边。 */
private val SETTINGS_ANCHOR_PAD = 12.dp

/**
 * 设置页两种形态之间的转场。
 *
 * 四条纪律，缺一条就会读成「页换了」而不是「东西展开了」：
 * 1. **进场比退场慢一档**（200/230 对 120）：旧内容先撤，新内容再铺开，中间没有两屏文字互糊。
 * 2. **位移与显影同拍**：[Motion.swapSlide] 与 [Motion.reveal] 是同一条弹簧（约 230ms），
 *    淡入 200 收在同一个尾巴上。旧版位移 120、淡入 220，慢放是「先落位、后显影」。
 * 3. **方向随目标翻转**：旧内容朝「新内容来的反方向」撤走 —— 展开时向上、收起时向下。
 *    旧版恒为向上，收起时两棵树同向，读不出「收起来」。
 * 4. **顶栏不动**：返回键与标题是整段转场里唯一的静止参照物。
 */
private fun AnimatedContentTransitionScope<SettingsScreenKey>.settingsSwapTransition(
    edgePx: Int,
    fontScale: Float,
): ContentTransform {
    val toExpanded = !targetState.compact
    // 展开：新内容从上方落下来；收回：新内容从下方升上来
    val enterOffset = if (toExpanded) -edgePx else edgePx
    val enter = fadeIn(MotionFade.alpha(MotionFade.swapEnter)) +
        slideInVertically(Motion.swapSlide()) { enterOffset } +
        scaleIn(
            initialScale = if (fontScale <= SETTINGS_SWAP_SCALE_MAX_FONT) SETTINGS_SWAP_SCALE else 1f,
            animationSpec = Motion.reveal(),
            // 展开时原点在顶边（内容自顶向下长出来），收回时在底边（自底向上收回去）
            transformOrigin = if (toExpanded) TransformOrigin(0.5f, 0f) else TransformOrigin(0.5f, 1f),
        )
    val exit = fadeOut(MotionFade.alpha(MotionFade.exit)) +
        slideOutVertically(Motion.itemSlide()) {
            // 旧内容朝「新内容来的反方向」撤：展开时向上让位，收起时向下沉走
            if (toExpanded) -edgePx / 2 else edgePx / 2
        }
    return enter togetherWith exit
}

/**
 * 顶栏标题换字（设置 ↔ 某个入口名）。
 *
 * 旧版是硬切：内容 220ms 慢慢显影，标题却在一帧里换了字，两处节奏打架。
 * 这里 120ms 淡入 + 2dp 上移，与内容的首拍对齐，但不参与整块转场 —— 标题是参照物。
 */
private fun AnimatedContentTransitionScope<String>.settingsTitleTransition(
    risePx: Int,
): ContentTransform {
    val enter = fadeIn(MotionFade.alpha(MotionFade.exit)) +
        slideInVertically(Motion.itemSlide()) { risePx }
    val exit = fadeOut(MotionFade.alpha(MotionFade.exit)) +
        slideOutVertically(Motion.itemSlide()) { -risePx }
    // 两段标题宽度不同，裁剪会在换字瞬间切掉长标题的字头
    return (enter togetherWith exit).using(SizeTransform(clip = false))
}

/**
 * 一个设置分区块（分区标题 + 卡片 + 尾距）作为整体进出。
 *
 * 为什么要包一层而不是继续让它们当 Column 的直接子项：
 * 只有成为一个节点，才能让「这一块」自己从 0 高度长出来 —— 那正是
 * 用户 2026-11-18 要的「每个设置项都丝滑地收起、展开」。
 *
 * `revealDelayMs == null` 时完全不参与展开（锚点之上的块、以及不是布局切换的场合）：
 * 它们若也从 0 高度长出来，锚点那一块会被推着往下漂。
 *
 * 顺带承担锚点登记：块的 y = 分区标题的 y，`positionInParent()` 就是滚动内容坐标
 * （滚动位移作用在父 Column 的 placement 上，不影响子项相对父的坐标）。
 */
@Composable
private fun SettingsGroupBlock(
    group: SettingsGroup,
    showSection: (SettingsGroup) -> Boolean,
    revealDelay: (SettingsGroup) -> SettingsReveal?,
    sectionTops: MutableMap<SettingsGroup, Int>?,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!showSection(group)) return
    val positioner = if (sectionTops == null) Modifier else Modifier.onGloballyPositioned { coords ->
        sectionTops[group] = coords.positionInParent().y.roundToInt()
    }
    val reveal = revealDelay(group)
    if (reveal == null) {
        Column(
            modifier = Modifier.fillMaxWidth().then(positioner),
            verticalArrangement = Arrangement.spacedBy(SETTINGS_BLOCK_GAP),
            content = content,
        )
        return
    }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(reveal) {
        if (reveal.delayMs > 0) delay(reveal.delayMs)
        shown = true
    }
    AnimatedVisibility(
        visible = shown,
        modifier = positioner,
        // 高度走 Motion.contentSize()（NoBouncy 弹簧，减动效下坍缩成 snap）：
        // 子项按最终尺寸测量一次，只有外层报出来的高度在长 —— 每帧不会重排十几行文字。
        // 不能长高的场合（新树的滚动位置非 0）只做错峰淡入，见 growThisTree。
        enter = (if (reveal.grow) {
            expandVertically(Motion.contentSize(), expandFrom = Alignment.Top)
        } else {
            EnterTransition.None
        }) + fadeIn(MotionFade.alpha(MotionFade.swapEnter)),
        exit = (if (reveal.grow) {
            shrinkVertically(Motion.contentSize(), shrinkTowards = Alignment.Top)
        } else {
            ExitTransition.None
        }) + fadeOut(MotionFade.alpha(MotionFade.exit)),
        label = "settingsGroup",
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SETTINGS_BLOCK_GAP),
            content = content,
        )
    }
}

@Composable
fun SettingsScreen(navController: NavController) {
    val themeMode by ThemeManager.themeMode.collectAsState()
    // 首页副标题编辑弹窗（首页品牌栏收起时也可从这里修改）
    var showSubtitleDialog by remember { mutableStateOf(false) }
    val homeSubtitle by AppPrefs.subtitleFlow.collectAsState()

    // ── 学习数据一键备份/恢复 ──
    val backupContext = LocalContext.current
    val backupResources = LocalResources.current
    val backupScope = rememberCoroutineScope()
    // 待确认的导入文件（选定后先弹覆盖确认，确认才落盘）
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    // 导入成功后的重启提示；非空即弹窗
    var backupRestartMessage by remember { mutableStateOf<String?>(null) }
    // ── 练习记录体积可见性（审计 2.2-15）──
    // records.json 是无界追加的学习档案：用户看不到它有多大，也就无从判断该不该归档。
    // 这里只做「可见性 + 超预算提示」，不做任何静默淘汰 —— 删用户的学习记录必须由用户自己发起。
    val recordsFile = remember { File(backupContext.filesDir, "records.json") }
    // null = 还没有记录文件（新装/刚清过）：这时不该显示「当前 0 B」，
    // 那会被读成「有记录但很小」，而事实是「还没有记录」。
    var recordsSizeBytes by remember { mutableStateOf<Long?>(null) }
    // 刷新触发器必须是**数据版本**，不能借某条 UI 文案当 key：早先挂在 backupRestartMessage 上，
    // 语义是错位的（改文案会触发重算、数据变了不一定触发），而且练完一轮回到设置页时体积根本不更新。
    // 导入备份会整体替换文件，所以导入成功时 +1 就够覆盖真正的变化。
    var recordsVersion by remember { mutableIntStateOf(0) }
    LaunchedEffect(recordsVersion) {
        recordsSizeBytes = withContext(Dispatchers.IO) {
            if (recordsFile.exists()) recordsFile.length() else null
        }
    }

    // ── 赞赏区（会话级隐藏 + 保存赞赏码）──
    val donationContext = LocalContext.current
    var showDonationMenu by remember { mutableStateOf(false) }
    // 保存完成后的结果目录；非空即弹「保存成功」弹窗
    var donationSavedDir by remember { mutableStateOf<String?>(null) }
    // 保存动作的触发计数：权限回调与直接保存都汇到这一条路径，避免两处逻辑分叉
    var donationSaveTrigger by remember { mutableIntStateOf(0) }
    val donationSavePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) donationSaveTrigger++
        else Toast.makeText(
            donationContext, R.string.donation_save_failed, Toast.LENGTH_SHORT
        ).show()
    }
    LaunchedEffect(donationSaveTrigger) {
        if (donationSaveTrigger == 0) return@LaunchedEffect
        val dir = withContext(Dispatchers.IO) { saveDonationQrToGallery(donationContext) }
        if (dir != null) donationSavedDir = dir
        else Toast.makeText(
            donationContext, R.string.donation_save_failed, Toast.LENGTH_SHORT
        ).show()
    }
    // 发起保存。Android 9 及以下写公共相册需要存储权限，先申请；Android 10+ 走 MediaStore 无需权限。
    val startSaveDonationQr: () -> Unit = {
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                donationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            donationSavePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            donationSaveTrigger++
        }
    }

    // ── 备份导出：先勾选要带走的数据类别，再让用户选文件 ──
    // 页面一打开就统计体积会白读一遍全库，所以只在面板打开时（IO 线程）算，算完再展示。
    var backupPickerVisible by remember { mutableStateOf(false) }
    var backupSelection by remember { mutableStateOf<Set<BackupManager.Group>>(emptySet()) }
    var pendingBackupGroups by remember { mutableStateOf<Set<BackupManager.Group>>(emptySet()) }
    var backupSelectionInitialized by remember { mutableStateOf(false) }
    val backupStats by produceState(
        initialValue = emptyList<BackupManager.GroupStat>(),
        key1 = backupPickerVisible,
    ) {
        if (!backupPickerVisible) return@produceState
        value = withContext(Dispatchers.IO) { BackupManager.stats(backupContext.filesDir) }
    }
    LaunchedEffect(backupPickerVisible, backupStats) {
        if (backupPickerVisible && !backupSelectionInitialized && backupStats.isNotEmpty()) {
            // 默认勾选「有数据且非隐私类」的类目：AI 历史体积大且含全文对话，默认不带
            backupSelection = backupStats
                .filter { it.files > 0 && it.group in BackupManager.Group.defaultSelection() }
                .mapTo(mutableSetOf()) { it.group }
            backupSelectionInitialized = true
        }
    }
    LaunchedEffect(backupPickerVisible) {
        // 面板每次关闭都复位，下次打开重新按默认勾选（而不是沿用上次的临时选择）
        if (!backupPickerVisible) backupSelectionInitialized = false
    }

    val backupExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            backupScope.launch {
                val failure = withContext(Dispatchers.IO) {
                    runCatching {
                        backupContext.contentResolver.openOutputStream(uri)?.use { out ->
                            BackupManager.exportZip(
                                backupContext.filesDir, out, pendingBackupGroups,
                            )
                        } ?: throw BackupManager.BackupError.CannotWrite()
                    }.exceptionOrNull()
                }
                Toast.makeText(
                    backupContext,
                    if (failure == null) backupResources.getString(R.string.backup_export_done)
                    else backupResources.getString(
                        R.string.backup_export_failed, backupErrorMessage(backupContext, failure)
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    if (backupPickerVisible) {
        BackupPickerSheet(
            stats = backupStats,
            selected = backupSelection,
            onToggle = { g ->
                backupSelection = if (g in backupSelection) backupSelection - g else backupSelection + g
            },
            onSelectAll = {
                backupSelection = backupStats.filter { it.files > 0 }
                    .mapTo(mutableSetOf()) { it.group }
            },
            onClearAll = { backupSelection = emptySet() },
            onExport = {
                pendingBackupGroups = backupSelection
                backupPickerVisible = false
                backupScope.launch {
                    // 等 sheet 退场一帧再拉起系统文件选择器：同帧创建另一个窗口时，
                    // 部分 ROM 会把 chooser 顶掉（项目里已有同类踩坑记录）
                    kotlinx.coroutines.delay(90)
                    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                    backupExportLauncher.launch("blancall_backup_$stamp.zip")
                }
            },
            onDismissRequest = { backupPickerVisible = false },
        )
    }

    // ── 「导出并归档旧记录」（审计 2.2-15）：确认 → 选文件 → 导出成功后才裁剪 ──
    // 两步之间任何一步失败都不删数据：宁可归档不成，不可静默淘汰用户履历。
    val archiveRecordRepo = remember { RecordRepository.getInstance(recordsFile.absolutePath) }
    var archiveDialogVisible by remember { mutableStateOf(false) }
    var archiveCandidateCount by remember { mutableIntStateOf(0) }
    var pendingArchive by remember { mutableStateOf<List<PracticeRecord>?>(null) }
    val recordsArchiveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val toArchive = pendingArchive
        pendingArchive = null
        if (uri == null || toArchive == null) return@rememberLauncherForActivityResult
        backupScope.launch {
            val failure = withContext(Dispatchers.IO) {
                runCatching {
                    backupContext.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(archiveRecordRepo.archiveJson(toArchive).toByteArray(Charsets.UTF_8))
                    } ?: throw BackupManager.BackupError.CannotWrite()
                    // 导出成功后才裁剪；上面任何一步失败都会走 catch，一条都不删
                    archiveRecordRepo.removeByIds(toArchive.map { it.id }.toSet())
                }.exceptionOrNull()
            }
            if (failure == null) {
                recordsVersion++
                Toast.makeText(
                    backupContext,
                    backupResources.getString(com.ilyskyo.blancall.R.string.data_archive_done, toArchive.size),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    backupContext,
                    backupResources.getString(com.ilyskyo.blancall.R.string.data_archive_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val backupImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
        }
    }

    // ── 布局双态（渐进式披露）──
    // 紧凑 = 1 级概览 + 通用 2 级详情页；完整 = 现有分区原样平铺（逐字保留，老用户的安全区）。
    // 双态的意义：展开态承担「原位保护」（NN/g Spatial Memory 要求常用区在原位留副本），
    // 收起态承担「两级结构 + 搜索」（Android 官方对 15+ 项设置的要求），两者不冲突。
    val compactLayout by AppPrefs.settingsCompactFlow.collectAsState()
    val layoutHintSeen by AppPrefs.settingsLayoutHintSeenFlow.collectAsState()
    // 当前打开的是哪一个入口；null = 1 级（概览 / 完整布局）。完整布局下恒为 null。
    // 存 id 而不是枚举本身：rememberSaveable 只认可序列化类型，转屏 / 分屏重建后
    // 才还能停在原来那一页（平板上分屏拖动分隔线会反复重建 Activity）。
    var activeEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    val activeEntry = activeEntryId?.let { id ->
        SettingsEntry.entries.firstOrNull { it.name == id }
    }
    // 搜索词提到页面这一级：进出二级页时概览整体离开组合，留在概览内部会丢 ——
    // 返回后搜索框空着、列表却还是筛选后的样子，用户得重输一遍（Baymard 点过的失效模式）。
    // 两种布局共用同一个词：切换布局时用户的查询意图不该被清掉，
    // 只是同一句话在紧凑布局筛入口、在完整布局筛分区（见 matchingGroups）。
    var settingsQuery by rememberSaveable { mutableStateOf("") }
    // 两种形态各记各的滚动位置：切过去还能回到自己上次停的地方（空间记忆），
    // 也不会因为另一种形态更短而被夹到顶部去。
    // 走 rememberSaveable：转屏 / 分屏拖分隔线会反复重建 Activity，普通 remember 每次都归零。
    val overviewScroll = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    // 完整态里每个分区标题相对滚动内容顶部的 y（px）。只放内存、**不做 State**：
    // 每次布局都会写一遍，做成 State 等于让整页跟着布局重组。
    val sectionTops = remember { HashMap<SettingsGroup, Int>() }
    // 切到完整态时把锚点当**初值**交给新的 ScrollState —— 初值在首帧之前就生效，
    // 所以不会出现「先落到顶部、下一帧再跳下去」。generation 换了才重建。
    var expandedAnchor by remember { mutableIntStateOf(0) }
    var expandedGeneration by remember { mutableIntStateOf(0) }
    val expandedScroll = rememberSaveable(expandedGeneration, saver = ScrollState.Saver) {
        ScrollState(expandedAnchor)
    }
    // 每个入口都从顶部进入：remember(activeEntry) 换入口即新建状态，天然回到 0 且不闪一帧
    val detailScroll = remember(activeEntry) { ScrollState(0) }

    // 转场锁：260ms 内再长按不改目标态。否则 AnimatedContent 会把中间态当成起点，
    // 三棵树同时在场 —— 不是崩，是糊。
    var swapLocked by remember { mutableStateOf(false) }
    LaunchedEffect(swapLocked) {
        if (swapLocked) {
            delay(SETTINGS_SWAP_LOCK_MS)
            swapLocked = false
        }
    }
    // 回执：只有「刚切过」才存在。撤销窗口与它同寿，到点自己走。
    var receipt by remember { mutableStateOf<SettingsSwapReceipt?>(null) }
    LaunchedEffect(receipt, swapLocked) {
        if (receipt == null || swapLocked) return@LaunchedEffect
        delay(SETTINGS_RECEIPT_MS)
        receipt = null
    }
    // 「这一次切换」要不要让每个设置块依次展开。只在布局切换的那一次为真：
    // 首次进设置页、转屏重建都不该把展开放一遍。
    var revealPending by remember { mutableStateOf(false) }
    // 切换那一刻概览是不是停在顶部（1 = 停在顶部）。存快照，而不是让组合直接读
    // ScrollState.value：那是个 State，在组合里读等于让整页跟着每一帧滚动重组。
    var overviewWasAtTop by remember { mutableIntStateOf(1) }
    // 这次切到完整态时锚在哪一组：锚点之上的块不参与高度展开，否则它们会把锚点推下去
    var anchorOrdinalAtSwitch by remember { mutableIntStateOf(0) }
    // 切到完整态时注入的初始滚动位置。非 0 时**不能让块从 0 高度长出来**：
    // ScrollState 会在内容变矮时把 value 夹到 maxValue，锚点会被夹到 0 并永远丢失。
    var anchorScrollAtSwitch by remember { mutableIntStateOf(0) }
    // 两处 dp → px 的换算：锚点余量与标题换字的上移都要在 switchLayout / 标题区之前算好
    val anchorPadPx = with(LocalDensity.current) { SETTINGS_ANCHOR_PAD.roundToPx() }
    val titleRisePx = with(LocalDensity.current) { SETTINGS_TITLE_RISE.roundToPx() }

    // 切换只改状态：转场交给下面那个 AnimatedContent（见 settingsSwapTransition）。
    // anchor 是调用方指定的落点入口（二级页长按 = 当前入口）；null 时按概览视口顶部那一行算。
    val switchLayout: (Boolean, SettingsEntry?) -> Unit = { toCompact, anchorEntry ->
        if (toCompact != compactLayout && !swapLocked) {
            // 概览此刻停在哪儿：紧凑行要不要从 0 高度长出来，取决于新树会不会恢复到一个
            // 非零滚动位置 —— 会的话，上方那几行一长高，视口里的内容就会被推着漂。
            val atTop = overviewScroll.value == 0
            overviewWasAtTop = if (atTop) 1 else 0
            if (!toCompact) {
                // 锚定：二级页长按 → 当前入口的第一组；概览里长按 → 视口顶端那一行（默认「外观」）。
                // 概览整屏只有六个入口行，所以这里恒取第一行 —— 滚过的少数情况宁可回到顶部，
                // 也不能让锚点随着展开动画一路往下漂。
                val anchorGroup = anchorEntry?.groups?.firstOrNull()
                    ?: SettingsEntry.entries.firstOrNull()?.groups?.firstOrNull()
                anchorOrdinalAtSwitch = anchorGroup?.ordinal ?: 0
                val targetY = anchorGroup?.let { sectionTops[it] }
                if (targetY != null) {
                    expandedAnchor = (targetY - anchorPadPx).coerceAtLeast(0)
                    expandedGeneration++
                }
                anchorScrollAtSwitch = expandedAnchor
            }
            // 回执里的「撤销」要退回的那一态：记的是**切换前**的形态与层级
            receipt = SettingsSwapReceipt(compact = compactLayout, entryId = activeEntryId)
            activeEntryId = null
            AppPrefs.settingsCompact = toCompact
            // 用户 2026-11-14：「长按过一次，再长按回来之后，这个提示就应该自动消失」。
            // 「学会了」的判据是**真的切换过**，不是点过 ×；所以标记写在这里，
            // 而不是只挂在 onDismissHint 上 —— 否则每次切回概览都要再看一遍同一句提示。
            AppPrefs.markSettingsLayoutHintSeen()
            revealPending = true
            swapLocked = true
        }
    }
    // 撤销：同一条转场反向播放，不产生第二次回执。滚动位置不用还原 ——
    // 两份 ScrollState 提到页面这一级，从没被销毁过，回来就是原位。
    val undoSettingsSwap: () -> Unit = {
        val last = receipt
        if (last != null) {
            receipt = null
            // 退回时也要重算这两条「新树会不会恢复到非零滚动位置」——
            // 不重算的话，撤销回一个滚过的位置却仍让块从 0 高度长出来，
            // ScrollState 会把偏移夹到 0，用户就「撤销到了顶部」。
            overviewWasAtTop = if (overviewScroll.value == 0) 1 else 0
            anchorScrollAtSwitch = expandedScroll.value
            activeEntryId = last.entryId
            AppPrefs.settingsCompact = last.compact
            revealPending = true
            swapLocked = true
        }
    }
    // 二级页里的系统返回键先回 1 级，不能直接退出设置页（Baymard：子页返回「2 steps back」是头号翻车）
    BackHandler(enabled = activeEntry != null) { activeEntryId = null }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            // 底部系统导航条让位（与导入/阅读等子页面一致）：缺少它时设置页
            // 滚动内容会伸到导航条区域之下，底部露出一段白区且内容被截断。
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)
        ) {
        // 顶部返回按钮 + 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onClick = {
                if (activeEntry != null) activeEntryId = null else navController.popBackStack()
            })
            Spacer(Modifier.width(12.dp))
            // ── 标题区 = 长按热区 ──
            // 只留长按这一个入口是用户 2026-11-13 的决定（撤掉胶囊与双击）；
            // 读屏用户走 semantics 上的 onLongClick 动作，不必真的按住不放。
            //
            // 热区只覆盖「标题文字 + 左右各 8dp」（2026-11-18 修订）：旧版 `weight(1f)` 铺满
            // 整行剩余宽度（约 240dp），而误触几乎都来自拇指停在标题右侧那片空白里 ——
            // 面积降一个量级，视觉上一个像素都没变。
            val titleText = activeEntry?.let { settingsEntryTitle(it) }
                ?: stringResource(com.ilyskyo.blancall.R.string.settings)
            // 目标态名：既是长按的预告（200ms 后浮出），也是读屏的动作标签
            val toggleLabel = stringResource(
                if (compactLayout) com.ilyskyo.blancall.R.string.settings_layout_full
                else com.ilyskyo.blancall.R.string.settings_layout_compact
            )
            val layoutStateDescription = stringResource(
                if (compactLayout) com.ilyskyo.blancall.R.string.settings_layout_state_compact
                else com.ilyskyo.blancall.R.string.settings_layout_state_full,
                if (compactLayout) SettingsEntry.entries.size else SettingsGroup.entries.size,
            )
            // 预热进度：由**动画时钟**推，不由 pointer 事件推 ——
            // 手指按住不动时一个事件都不会来，靠 positionChange 推的进度会僵在原地。
            var pressing by remember { mutableStateOf(false) }
            val arm = remember { Animatable(0f) }
            val longPressMs = LocalViewConfiguration.current.longPressTimeoutMillis
            val confirmHaptic = rememberConfirmHaptic()
            val lockedHaptic = rememberHaptic(HapticTier.Toggle)
            val lockedNow by rememberUpdatedState(swapLocked)
            LaunchedEffect(pressing) {
                if (pressing) arm.animateTo(1f, MotionFade.armSweep(longPressMs.toInt()))
                else arm.animateTo(0f, MotionFade.armRelax())
            }
            // 200ms 起浮出目标态名。derivedStateOf 只在跨过阈值那一下重组一次，
            // 不会让整块标题跟着每一帧进度重组。
            val aimVisible by remember {
                derivedStateOf { pressing && arm.value >= SETTINGS_AIM_THRESHOLD }
            }
            // 细线：底槽 6%、走满的那一段 14%。两个都极淡，是为了不跟标题抢第一眼
            val armColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f)
            val trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    // 热区向外多留 8dp，视觉位置不能跟着挪：先往左挪同样的量再吃 padding，
                    // 标题左缘仍与下方内容对齐（那 8dp 落在 BackButton 与标题之间的 12dp 空档里）
                    .offset(x = -SETTINGS_HOT_PAD)
                    // 先手势后 padding：触摸区要把那 8dp 也算进去
                    .pointerInput(compactLayout, activeEntry) {
                        awaitEachGesture {
                            val down = awaitFirstDown(
                                requireUnconsumed = false, pass = PointerEventPass.Initial,
                            )
                            val slop = viewConfiguration.touchSlop
                            var drift = Offset.Zero
                            pressing = true
                            // 观察而不消费：Main pass 是子→父，内层先消费就分不清
                            // 「超时」与「被取消」（HomeCardCanvas 踩过同一个坑）
                            val interrupted = withTimeoutOrNull(
                                viewConfiguration.longPressTimeoutMillis
                            ) {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    drift += change.positionChange()
                                    // 累计位移超 slop 即取消：正常长按的手抖不该被吃掉
                                    if (drift.getDistance() > slop) break
                                }
                            }
                            pressing = false
                            if (interrupted == null) {
                                // 时间到，且全程按住、没滑出 slop
                                if (lockedNow) {
                                    // 转场锁里的第二次长按：只给一次轻触觉，不改目标态。
                                    // 改了会让 AnimatedContent 以中间态为起点，三棵树叠着糊。
                                    lockedHaptic()
                                } else {
                                    // 唯一的提交确认，必须硬
                                    confirmHaptic()
                                    switchLayout(!compactLayout, activeEntry)
                                }
                                // 达标后吃掉剩余事件直到抬手：否则抬手会被下面的可点元素接走
                                //（旧版「长按进编辑后点完成又跳详情」同源）
                                down.consume()
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    event.changes.forEach { it.consume() }
                                    if (event.changes.all { !it.pressed }) break
                                }
                            }
                            // 未达标（抬手 / 滑出 slop）什么都不用做：pressing=false 已经把线退回
                        }
                    }
                    .semantics(mergeDescendants = true) {
                        onLongClick(label = toggleLabel) {
                            switchLayout(!compactLayout, activeEntry)
                            return@onLongClick true
                        }
                        // 读屏用户切完立刻知道自己在哪一态，不必靠数行数
                        stateDescription = layoutStateDescription
                    }
                    .padding(horizontal = SETTINGS_HOT_PAD),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedContent(
                        targetState = titleText,
                        transitionSpec = { settingsTitleTransition(titleRisePx) },
                        label = "settingsTitle",
                        contentAlignment = Alignment.CenterStart,
                    ) { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            // 预热细线画字形正下方：走长度而不是走亮度，不透明度压在 14%，
                            // 它只是「还要按多久」的读数，不该抢标题。字重字号一律不动 ——
                            // 改字重会触发文字重排，标题会跳一下。
                            modifier = Modifier.drawWithContent {
                                drawContent()
                                if (pressing || arm.value > 0f) {
                                    val lineH = SETTINGS_ARM_LINE.toPx()
                                    val top = size.height - lineH
                                    if (pressing) {
                                        drawRect(
                                            color = trackColor,
                                            topLeft = Offset(0f, top),
                                            size = Size(size.width, lineH),
                                        )
                                    }
                                    drawRect(
                                        color = armColor,
                                        topLeft = Offset(0f, top),
                                        size = Size(size.width * arm.value, lineH),
                                    )
                                }
                            },
                        )
                    }
                    // 预告：目标态名。它是提示，不是按钮 —— 不描边、不填色、不进焦点
                    AnimatedVisibility(
                        visible = aimVisible,
                        enter = fadeIn(MotionFade.alpha(MotionFade.exit)) +
                            slideInHorizontally(Motion.itemSlide()) { it / 4 },
                        exit = fadeOut(MotionFade.alpha(MotionFade.exit)) +
                            slideOutHorizontally(Motion.itemSlide()) { it / 4 },
                    ) {
                        Text(
                            text = toggleLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                            modifier = Modifier.padding(start = 10.dp),
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── 形态切换的转场 ──
        // 用户 2026-11-14：「长按切换两种布局时……要那种非常丝滑的收起和展开的效果」；
        // 2026-11-18 追加：「每个设置项都丝滑地收起、展开」。
        // 旧实现是 StateSwap 的纯交叉淡出（180/120）：两棵树在同一位置叠着溶开，
        // 一屏 6 行目录与一屏 13 张卡片的文字会糊在一起，读起来是「闪了一下」而不是「展开」。
        //
        // 现在的编排（仍然**保留双树同时在场**，绝不退回单树换形态 —— 那会抓到空白帧，
        // 见 StateSwap 的 KDoc）：
        //   整块  = 淡入 200 + 从 18dp 外落下（与 scaleIn 同一条 230ms 弹簧）+ 0.99 倍铺开
        //   块内  = 每个分区块 / 入口行按 index 错峰 30ms 依次「展开」（高度 + 淡入）
        //   退场  = 淡出 120 + 反向撤走 9dp（方向随目标翻转，比进场快一档）
        val swapEdgePx = with(LocalDensity.current) { SETTINGS_SWAP_EDGE.toPx().toInt() }
        val fontScale = LocalDensity.current.fontScale
        AnimatedContent(
            targetState = SettingsScreenKey(compact = compactLayout, entryId = activeEntryId),
            transitionSpec = { settingsSwapTransition(swapEdgePx, fontScale) },
            label = "settingsLayout",
            modifier = Modifier.weight(1f)
        ) { key ->
            val keyEntry = key.entryId?.let { id ->
                SettingsEntry.entries.firstOrNull { it.name == id }
            }
            // 二级页只渲染这个入口含的分区；完整布局照旧全部平铺
            // 完整布局顶层带搜索（2026-11-21）：输入了词就只留命中的分区，
            // 命中判据与紧凑布局那份搜索索引同源（matchingGroups）。
            val filteringFull = !key.compact && key.entryId == null && settingsQuery.isNotBlank()
            val matchedGroups: Set<SettingsGroup>? =
                if (filteringFull) matchingGroups(settingsQuery) else null
            val showSection: (SettingsGroup) -> Boolean = { group ->
                (!key.compact || keyEntry?.groups?.contains(group) == true) &&
                    (matchedGroups == null || group in matchedGroups)
            }
            // 这一棵树要不要走「逐块展开」：**在它第一次组合时定格**。
            // 用 remember(key) 而不是直接读 revealPending —— 后者在 600ms 后会被清掉，
            // 直接读会让已经开始的展开在半途被换成静态树（动画硬切一刀）。
            val animateThisTree = remember(key) { revealPending }
            LaunchedEffect(key) {
                if (animateThisTree) {
                    delay(SETTINGS_REVEAL_TOTAL_MS)
                    revealPending = false
                }
            }
            // 登场顺序 = 分区在枚举里的次序。锚点之上的块不参与「高度展开」：
            // 它们若从 0 高度长出来，锚点那一块会被推着往下漂 —— 那是比不动更糟的一种动。
            // 用切换时记下的锚点序：切换后 activeEntryId 已清空，这里已经拿不到入口了
            val anchorOrder = remember(key) { anchorOrdinalAtSwitch }
            // 这一棵树能不能让块「长高」：只有新树的滚动位置为 0 时才行。
            // 否则 ScrollState 会把恢复的偏移夹到当前内容高度（内容从 0 长起来的头几帧尤其矮），
            // 锚点一旦被夹到 0 就再也回不来 —— 那种场合只做错峰淡入，不动高度。
            val growThisTree = remember(key) {
                if (key.compact) overviewWasAtTop == 1 else anchorScrollAtSwitch == 0
            }
            val groupRevealDelay: (SettingsGroup) -> SettingsReveal? = { group ->
                if (!animateThisTree) null
                else SettingsReveal(
                    delayMs = minOf(group.ordinal - anchorOrder, SETTINGS_REVEAL_MAX_STEP)
                        .coerceAtLeast(0) * SETTINGS_REVEAL_STEP_MS,
                    grow = growThisTree,
                )
            }
            // 只有完整态登记分区 y：二级页里同一个分区会在另一个滚动容器里被量一次，
            // 那份坐标会覆盖掉完整态的锚点，切过去就落到错误位置。
            val topsSink: MutableMap<SettingsGroup, Int>? = if (key.compact) null else sectionTops
            val entryRevealDelay: (SettingsEntry) -> SettingsReveal? = { entry ->
                if (!animateThisTree) null
                else SettingsReveal(
                    delayMs = minOf(entry.ordinal, SETTINGS_REVEAL_MAX_STEP) * SETTINGS_REVEAL_STEP_MS,
                    grow = growThisTree,
                )
            }
            // 正文里要不要再写分区标题：**只有组名与入口名一字不差时才省**（见 sectionTitleVisible）。
            // 旧规则是「二级页一律隐藏分区标题」，合并入口后会导致「内容与数据」页里
            // 三段（内容管理 / 数据 / 扩展）没有小标题，读起来是一整块 —— 必须按组判。

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        when {
                            key.entryId != null -> detailScroll
                            key.compact -> overviewScroll
                            else -> expandedScroll
                        }
                    ),
                verticalArrangement = Arrangement.spacedBy(SETTINGS_BLOCK_GAP)
            ) {
                // 1 级概览（只在紧凑布局的顶层出现；完整布局与二级页都直接摊开各自的分区）
                if (key.compact && key.entryId == null) {
                    SettingsCompactOverview(
                        query = settingsQuery,
                        onQueryChange = { settingsQuery = it },
                        hintVisible = !layoutHintSeen,
                        onDismissHint = { AppPrefs.markSettingsLayoutHintSeen() },
                        onOpenEntry = { entry ->
                            // 转场中的旧树不接点击：否则会打开一个「已经不存在的入口」
                            if (!swapLocked) activeEntryId = entry.name
                        },
                        revealDelay = entryRevealDelay,
                    )
                }

                // 完整布局的顶部搜索栏（2026-11-21 用户：两种布局都要有）。
                // 与紧凑布局同一枚自绘控件、同一个搜索词，只是落点不同：
                // 这里不跳页，直接把没命中的分区藏掉 —— 一屏 13 个分区，长了眼睛也不好找。
                if (!key.compact && key.entryId == null) {
                    SettingsSearchField(
                        query = settingsQuery,
                        onQueryChange = { settingsQuery = it },
                    )
                    if (filteringFull && matchedGroups.isNullOrEmpty()) {
                        Text(
                            stringResource(R.string.settings_search_empty),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                SettingsGroupBlock(SettingsGroup.APPEARANCE, showSection, groupRevealDelay, topsSink) {
                    // ── 外观 ──
                    SettingsSectionTitle(R.string.appearance, visible = sectionTitleVisible(SettingsGroup.APPEARANCE, keyEntry))

                    GlassCard(
                        backdrop = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = SmoothRoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.padding(4.dp)) {
                            // 浅色米黄底色开关（深色模式始终纯黑，不受此开关影响）
                            val lightBeige by AppPrefs.lightBeigeBackgroundFlow.collectAsState()

                            // 主题模式：固定 3 行列表（与米黄底色开关无关）
                            OptionRow(
                                label = stringResource(com.ilyskyo.blancall.R.string.theme_light),
                                selected = themeMode == ThemeMode.LIGHT,
                                onClick = { ThemeManager.setThemeMode(ThemeMode.LIGHT) }
                            )
                            OptionDivider(
                                faded = themeMode == ThemeMode.LIGHT || themeMode == ThemeMode.DARK
                            )
                            OptionRow(
                                label = stringResource(com.ilyskyo.blancall.R.string.theme_dark),
                                selected = themeMode == ThemeMode.DARK,
                                onClick = { ThemeManager.setThemeMode(ThemeMode.DARK) }
                            )
                            OptionDivider(
                                faded = themeMode == ThemeMode.DARK || themeMode == ThemeMode.SYSTEM
                            )
                            OptionRow(
                                label = stringResource(com.ilyskyo.blancall.R.string.theme_system),
                                selected = themeMode == ThemeMode.SYSTEM,
                                onClick = { ThemeManager.setThemeMode(ThemeMode.SYSTEM) }
                            )

                            HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            // ── 界面语言 ──
                            // 语言名用各自语言的写法（见 AppLocale 注释）：切换入口若跟着当前语言走，
                            // 换错一次就找不到回来的那一项。「跟随系统」是模式不是语言，跟着界面走。
                            // 13 以上走框架 LocaleManager，13 以下走本地 tag + attachBaseContext 注入，
                            // 两条通道都能切，所以不按版本设门。
                            // 七个选项不再整列摊在设置页里（那会占掉大半屏）：
                            // 点这一行就地弹出选项卡，选完立刻收起。
                            val localeContext = LocalContext.current
                            val currentLocale = AppLocale.current(localeContext)
                            // 语言字形是标题的「第五个字」：尺寸与间隙都按 sp 换算，
                            // 跟随系统字号一起缩放，否则大字号下会缩成一个小点。
                            val localeDensity = LocalDensity.current
                            val languageGlyphSize = with(localeDensity) { 18.sp.toDp() }
                            val languageGlyphWidth = languageGlyphSize * 28f / 24f
                            val languageGlyphGap = with(localeDensity) { 1.5.sp.toDp() }
                            // 字形视框中「文」的墨迹重心偏高约 0.8sp，与汉字正文视觉居中对齐时微微下沉
                            val languageGlyphDrop = with(localeDensity) { 0.8.sp.toDp() }
                            var showLocaleMenu by remember { mutableStateOf(false) }
                            val localeLabel: @Composable (AppLocale) -> String = { choice: AppLocale ->
                                if (choice == AppLocale.FollowSystem)
                                    stringResource(com.ilyskyo.blancall.R.string.theme_system) else choice.nativeName
                            }
                            Box {
                                Surface(
                                    onClick = { showLocaleMenu = true },
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            // 「文A」字形紧跟标题，当成第五个字：认语言入口靠图形不靠汉字，
                                            // 换成看不懂的语言时也能一眼找到这一行。
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(stringResource(com.ilyskyo.blancall.R.string.ui_language),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface)
                                                Spacer(Modifier.width(languageGlyphGap))
                                                AppIcon(
                                                    kind = AppIconKind.Language,
                                                    modifier = Modifier
                                                        .height(languageGlyphSize)
                                                        .width(languageGlyphWidth)
                                                        .offset(y = languageGlyphDrop),
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Text(stringResource(com.ilyskyo.blancall.R.string.ui_language_desc),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(end = 16.dp))
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(localeLabel(currentLocale),
                                                style = localeNameStyle(currentLocale, MaterialTheme.typography.labelMedium),
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                GlassDropdownMenu(
                                    expanded = showLocaleMenu,
                                    onDismissRequest = { showLocaleMenu = false }
                                ) {
                                    AppLocale.entries.forEach { choice ->
                                        val picked = currentLocale == choice
                                        GlassMenuItem(
                                            onClick = {
                                                showLocaleMenu = false
                                                // 13+ 由框架重建，13 以下由 AppLocale.apply 自己 recreate
                                                AppLocale.apply(localeContext, choice)
                                            },
                                            label = {
                                                Text(localeLabel(choice),
                                                    style = localeNameStyle(choice, MaterialTheme.typography.bodyMedium),
                                                    color = MaterialTheme.colorScheme.onSurface)
                                            },
                                            trailing = {
                                                if (picked) AppIcon(
                                                    kind = AppIconKind.Check,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(com.ilyskyo.blancall.R.string.beige_background), style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(com.ilyskyo.blancall.R.string.beige_background_desc),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 16.dp))
                                }
                                GlassSwitch(
                                    checked = lightBeige,
                                    onCheckedChange = { AppPrefs.lightBeigeBackgroundEnabled = it }
                                )
                            }

                            // 页面灰色底（默认关 = 与卡片同色，即旧版观感）；开启后底色深一档、卡片靠面差立起来。
                            // 存储语义仍是 flatBackground=true 表示「同色」，这里只把开关的极性倒过来：
                            // 用户理解的是「打开=灰底」，不是「关掉同色」。
                            val flatBackground by AppearanceStore.state.collectAsState()
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(com.ilyskyo.blancall.R.string.flat_background),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(com.ilyskyo.blancall.R.string.flat_background_desc),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 16.dp))
                                }
                                GlassSwitch(
                                    checked = !flatBackground.flatBackground,
                                    onCheckedChange = { on ->
                                        AppearanceStore.update { it.copy(flatBackground = !on) }
                                    }
                                )
                            }

                            HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            // ── 界面字体（标题/正文族）──
                            // 内置霞鹜文楷与阅读页共用同一份 ttf，不额外增包体；
                            // 「系统衬线」在某些国产 ROM 上会退化成黑体，选内置楷体可绕开机型差异。
                            val appearance by AppearanceStore.state.collectAsState()
                            var showFontMenu by remember { mutableStateOf(false) }
                            val fontLabelRes = when (appearance.uiFont) {
                                AppearanceSnapshot.FONT_SANS -> com.ilyskyo.blancall.R.string.ui_font_sans
                                AppearanceSnapshot.FONT_XWWK -> com.ilyskyo.blancall.R.string.ui_font_xwwk
                                else -> com.ilyskyo.blancall.R.string.ui_font_serif
                            }
                            Box {
                                Surface(
                                    onClick = { showFontMenu = true },
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(stringResource(com.ilyskyo.blancall.R.string.ui_font),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface)
                                            Text(stringResource(com.ilyskyo.blancall.R.string.ui_font_desc),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(end = 16.dp))
                                        }
                                        Text(stringResource(fontLabelRes),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                GlassDropdownMenu(
                                    expanded = showFontMenu,
                                    onDismissRequest = { showFontMenu = false }
                                ) {
                                    listOf(
                                        AppearanceSnapshot.FONT_SERIF to com.ilyskyo.blancall.R.string.ui_font_serif,
                                        AppearanceSnapshot.FONT_SANS to com.ilyskyo.blancall.R.string.ui_font_sans,
                                        AppearanceSnapshot.FONT_XWWK to com.ilyskyo.blancall.R.string.ui_font_xwwk,
                                    ).forEach { (value, labelRes) ->
                                        val picked = appearance.uiFont == value
                                        GlassMenuItem(
                                            onClick = {
                                                showFontMenu = false
                                                AppearanceStore.update { it.copy(uiFont = value) }
                                            },
                                            label = {
                                                Text(stringResource(labelRes),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface)
                                            },
                                            trailing = {
                                                if (picked) AppIcon(
                                                    kind = AppIconKind.Check,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            // ── 界面字号 ──
                            FontScaleSlider(current = appearance.uiFontScale)

                            HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            // ── 玻璃染色（液态玻璃是否吸取主题色）──
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(com.ilyskyo.blancall.R.string.glass_tint),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(com.ilyskyo.blancall.R.string.glass_tint_desc),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 16.dp))
                                }
                                GlassSwitch(
                                    checked = appearance.glassTint,
                                    onCheckedChange = { v -> AppearanceStore.update { it.copy(glassTint = v) } }
                                )
                            }

                            HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            // ── 导航栏液态玻璃开关（关闭后底部导航栏回退纯色底）──
                            val navGlass by AppPrefs.navLiquidGlassFlow.collectAsState()
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(com.ilyskyo.blancall.R.string.nav_glass), style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(com.ilyskyo.blancall.R.string.nav_glass_desc),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 16.dp))
                                }
                                GlassSwitch(
                                    checked = navGlass,
                                    onCheckedChange = { AppPrefs.navLiquidGlassEnabled = it }
                                )
                            }

                            HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            // ── 触感强度（本 app 自己的四档）──
                            // 不去镜像系统的全局触感开关：直振本来就是为绕开「定制 ROM 把触感
                            // 弱化到无感」而写的（见 ui/common/Haptics.kt），镜像开关等于把那个
                            // 老问题重新请回来。想关，就用这里的「关」。
                            val hapticLevel by AppPrefs.hapticLevelFlow.collectAsState()
                            var showHapticMenu by remember { mutableStateOf(false) }
                            // 存进偏好的是字符串档名；认不出来时按「标准」显示，不给空标签
                            val hapticLabelRes = hapticLevelChoices
                                .firstOrNull { it.first == hapticLevel }?.second
                                ?: hapticLevelChoices[1].second
                            Box {
                                Surface(
                                    onClick = { showHapticMenu = true },
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(stringResource(com.ilyskyo.blancall.R.string.haptic_strength), style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface)
                                            Spacer(Modifier.height(2.dp))
                                            Text(stringResource(com.ilyskyo.blancall.R.string.haptic_strength_desc),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(end = 16.dp))
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(stringResource(hapticLabelRes),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                GlassDropdownMenu(
                                    expanded = showHapticMenu,
                                    onDismissRequest = { showHapticMenu = false }
                                ) {
                                    hapticLevelChoices.forEach { (value, labelId) ->
                                        val picked = hapticLevel == value
                                        GlassMenuItem(
                                            onClick = {
                                                showHapticMenu = false
                                                AppPrefs.setHapticLevel(value)
                                            },
                                            label = {
                                                Text(stringResource(labelId),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface)
                                            },
                                            trailing = {
                                                if (picked) AppIcon(
                                                    kind = AppIconKind.Check,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            // ── 首页副标题（点击编辑；首页品牌栏收起时也能从这里修改）──
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .pressClick { showSubtitleDialog = true }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(com.ilyskyo.blancall.R.string.home_subtitle), style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(com.ilyskyo.blancall.R.string.home_subtitle_desc),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 16.dp))
                                }
                                Text(stringResource(com.ilyskyo.blancall.R.string.edit), style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary)
                            }

                            // 内置素材库已移至下方「拓展功能」分组
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }

                SettingsGroupBlock(SettingsGroup.THEME_COLOR, showSection, groupRevealDelay, topsSink) {
                    // ── 主题色 ──
                    SettingsSectionTitle(R.string.theme_color, visible = sectionTitleVisible(SettingsGroup.THEME_COLOR, keyEntry))

                    GlassCard(
                        backdrop = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = SmoothRoundedCornerShape(14.dp)
                    ) {
                        val appearance by AppearanceStore.state.collectAsState()
                        val isDark = isBlancallDark()
                        var showCustomPicker by remember { mutableStateOf(false) }
                        val accentIndex = appearance.accentIndex
                        // 6 颗 40dp + 5×8dp = 280dp。可用宽度 = 屏宽 − 页面 40 − 卡片 32 = 288dp（360dp 机型），
                        // 留 8dp 余量。旧版 44dp + 12dp = 324dp 在 360dp 屏溢出 36dp，居中排列 ⇒ 首尾各被裁 18dp。
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 根据系统深色模式选择 primaryDark/primaryLight，保证深色下对比度
                            AccentPresets.forEachIndexed { index, preset ->
                                val isSelected = !appearance.useCustomAccent && index == accentIndex
                                val accentColor = if (isDark) preset.primaryDark else preset.primaryLight
                                Surface(
                                    onClick = { AppearanceStore.setAccentIndex(index) },
                                    shape = RoundedCornerShape(50),
                                    color = if (isSelected) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(40.dp),
                                    border = if (isSelected)
                                        BorderStroke(2.5.dp, accentColor)
                                    else
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(22.dp),
                                            shape = RoundedCornerShape(50),
                                            color = accentColor,
                                            content = {}
                                        )
                                        if (isSelected) {
                                            AppIcon(
                                                kind = AppIconKind.Check,
                                                tint = accentColor,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // ── 自定义主题色（色环自由取色）──
                        Surface(
                            onClick = { showCustomPicker = true },
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(com.ilyskyo.blancall.R.string.theme_color_custom),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(com.ilyskyo.blancall.R.string.theme_color_custom_desc),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 16.dp))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val dot = if (appearance.useCustomAccent) Color(appearance.customArgb)
                                    else MaterialTheme.colorScheme.outlineVariant
                                    Box(
                                        Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(dot)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                                    )
                                    if (appearance.useCustomAccent) {
                                        Spacer(Modifier.width(8.dp))
                                        AppIcon(
                                            kind = AppIconKind.Check,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // ── 跟随系统取色（Android 12+ 动态取色）──
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(com.ilyskyo.blancall.R.string.material_you),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(com.ilyskyo.blancall.R.string.material_you_desc),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 16.dp))
                                }
                                GlassSwitch(
                                    checked = appearance.materialYou,
                                    onCheckedChange = { v -> AppearanceStore.update { it.copy(materialYou = v) } }
                                )
                            }
                        }

                        if (showCustomPicker) {
                            // 单独抽一层：GlassModalBottomSheet 的签名里带 SheetState（实验性 M3 API），
                            // 调用点必须 OptIn —— 把 OptIn 关在这个小组件里，别污染整个设置页。
                            CustomAccentSheet(
                                appearance = appearance,
                                onDismiss = { showCustomPicker = false }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }

                SettingsGroupBlock(SettingsGroup.FEATURES, showSection, groupRevealDelay, topsSink) {
                    // ── 特性 ──
                    SettingsSectionTitle(R.string.features, visible = sectionTitleVisible(SettingsGroup.FEATURES, keyEntry))

                    GlassCard(
                        backdrop = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = SmoothRoundedCornerShape(14.dp)
                    ) {
                        // 段落首行缩进开关（导入时给未缩进段落补两格；关闭后不再新增缩进）
                        val autoIndentEnabled by AppPrefs.autoIndentEnabledFlow.collectAsState()
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(com.ilyskyo.blancall.R.string.indent_first_line), style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text(stringResource(com.ilyskyo.blancall.R.string.indent_first_line_desc),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 16.dp))
                            }
                            GlassSwitch(
                                checked = autoIndentEnabled,
                                onCheckedChange = { AppPrefs.autoIndentEnabled = it }
                            )
                        }

                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // ── 手写：符号栏 ──
                        // 开关与内容分两行，是因为它们是两个问题：「有没有这一排」和「这一排是哪几个」。
                        // 分区里刻意不加二级小标题：一个卡片里再塞一行灰字，多的是噪音，不是信息。
                        val symbolBar by AppPrefs.handwritingSymbolBarFlow.collectAsState()
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(com.ilyskyo.blancall.R.string.handwriting_symbol_bar),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text(stringResource(com.ilyskyo.blancall.R.string.handwriting_symbol_bar_desc),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 16.dp))
                            }
                            GlassSwitch(
                                checked = symbolBar,
                                onCheckedChange = { AppPrefs.handwritingSymbolBar = it }
                            )
                        }

                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // 符号栏内容：整行可点，进底部面板编辑（中文 / 英文两套分别编辑）。
                        // 行尾直接给两套的字符个数 —— 概览里最有信息量的就是「我配了几个」。
                        val symbolsCjk by AppPrefs.handwritingSymbolsCjkFlow.collectAsState()
                        val symbolsLatin by AppPrefs.handwritingSymbolsLatinFlow.collectAsState()
                        var showSymbolSheet by remember { mutableStateOf(false) }
                        Surface(
                            onClick = { showSymbolSheet = true },
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(com.ilyskyo.blancall.R.string.handwriting_symbols),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(com.ilyskyo.blancall.R.string.handwriting_symbols_desc),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 16.dp))
                                }
                                Text(
                                    stringResource(
                                        com.ilyskyo.blancall.R.string.handwriting_symbols_count,
                                        symbolsCjk.length,
                                        symbolsLatin.length
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (showSymbolSheet) {
                            HandwritingSymbolSheet(onDismiss = { showSymbolSheet = false })
                        }

                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // ── 手写：低相似度是否直接上屏 ──
                        // 默认关 = 现状（不够确定就停在候选条等点选）。打开后中文单字与连写
                        // 都直接取 top-1；「疑似多字切不开」与「生僻字表外」两道守卫不受影响。
                        val weakCommit by AppPrefs.handwritingAutoCommitWeakFlow.collectAsState()
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(com.ilyskyo.blancall.R.string.handwriting_auto_commit_weak),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text(stringResource(com.ilyskyo.blancall.R.string.handwriting_auto_commit_weak_desc),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 16.dp))
                            }
                            GlassSwitch(
                                checked = weakCommit,
                                onCheckedChange = { AppPrefs.handwritingAutoCommitWeak = it }
                            )
                        }

                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // 错题墨迹保留（错题回顾）：学习统计的练习历史中回看答错空的手写笔迹
                        val keepInk by AppPrefs.keepInkEnabledFlow.collectAsState()
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(com.ilyskyo.blancall.R.string.keep_wrong_ink), style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text(stringResource(com.ilyskyo.blancall.R.string.keep_wrong_ink_desc),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 16.dp))
                            }
                            GlassSwitch(
                                checked = keepInk,
                                onCheckedChange = { AppPrefs.keepInkEnabled = it }
                            )
                        }

                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // 清空已存墨迹（开关只拦新增，已存数据需手动清）
                        val inkContext = LocalContext.current
                        val inkResources = LocalResources.current
                        val inkScope = rememberCoroutineScope()
                        var confirmClearInk by remember { mutableStateOf(false) }
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(com.ilyskyo.blancall.R.string.clear_saved_ink), style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f))
                            TextButton(onClick = { confirmClearInk = true }) {
                                Text(stringResource(com.ilyskyo.blancall.R.string.clear))
                            }
                        }
                        if (confirmClearInk) {
                            // 墨迹是用户手写的内容、删了重建不了，必须二次确认：
                            // 这一行紧挨「保留错题墨迹」开关，误触概率不低，而旧实现单击即删。
                            DeleteConfirmDialog(
                                title = stringResource(com.ilyskyo.blancall.R.string.clear_ink_confirm_title),
                                message = stringResource(com.ilyskyo.blancall.R.string.clear_ink_confirm_msg),
                                onConfirm = {
                                    confirmClearInk = false
                                    inkScope.launch {
                                        withContext(Dispatchers.IO) {
                                            InkStore.getInstance(
                                                inkContext.filesDir.resolve("ink").absolutePath
                                            ).clearAll()
                                        }
                                        Toast.makeText(inkContext, inkResources.getString(com.ilyskyo.blancall.R.string.ink_cleared_toast), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDismiss = { confirmClearInk = false },
                            )
                        }

                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // ── 句子卡片：点按翻页动画 ──
                        // 只影响「点按评级按钮换卡」那一段飞出；手动上下滑是翻卡手势本体，不能关。
                        val tapExitEnabled by AppPrefs.sentenceTapExitEnabledFlow.collectAsState()
                        val tapExitSpeed by AppPrefs.sentenceTapExitSpeedFlow.collectAsState()
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(com.ilyskyo.blancall.R.string.sentence_card_anim_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text(stringResource(com.ilyskyo.blancall.R.string.sentence_card_anim_desc),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 16.dp))
                            }
                            GlassSwitch(
                                checked = tapExitEnabled,
                                onCheckedChange = { AppPrefs.sentenceTapExitEnabled = it }
                            )
                        }
                        AnimatedVisibility(
                            visible = tapExitEnabled,
                            enter = fadeIn(MotionFade.alpha(MotionFade.enter)) +
                                expandVertically(Motion.contentSize()),
                            exit = fadeOut(MotionFade.alpha(MotionFade.exit)) +
                                shrinkVertically(Motion.contentSize())
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Row(
                                    Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(com.ilyskyo.blancall.R.string.sentence_card_anim_speed),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        stringResource(
                                            com.ilyskyo.blancall.R.string.sentence_card_anim_speed_value,
                                            (Motion.cardTapExitSeconds(tapExitSpeed) * 1000).roundToInt()
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = tapExitSpeed,
                                    // 拖动只改内存（每帧落盘会在 AtomicFiles 的 fsync 上卡 UI），松手再落盘
                                    onValueChange = { AppPrefs.previewSentenceTapExitSpeed(it) },
                                    onValueChangeFinished = { AppPrefs.flushSentenceTapExitSpeed() },
                                    valueRange = 0f..1f,
                                    steps = 9,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                // 预览卡：同一份 Motion 谱子 + 同一段位移算式，拖动每跨一格就飞一次
                                SentenceTapExitPreview(
                                    enabled = true,
                                    speed = tapExitSpeed,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp)
                                )
                                Text(
                                    stringResource(com.ilyskyo.blancall.R.string.sentence_card_anim_preview_hint),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 14.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }

                SettingsGroupBlock(SettingsGroup.READING, showSection, groupRevealDelay, topsSink) {
                    // ── 阅读设置 ──
                    // 这些项以前只能进「阅读模式」才改得到（2026-11-21 用户要求搬进来）。
                    // 这里写的是**全局默认值**：单篇改过就以那篇的存档为准，
                    // 卡片末尾那行「让所有文章改用这里的设置」负责把默认值推给已有存档。
                    SettingsSectionTitle(
                        R.string.reading_settings_title,
                        visible = sectionTitleVisible(SettingsGroup.READING, keyEntry),
                    )
                    ReadingDefaultsCard()
                    Spacer(Modifier.height(8.dp))
                }

                SettingsGroupBlock(SettingsGroup.PRACTICE, showSection, groupRevealDelay, topsSink) {
                    // ── 练习 ──
                    // 提示三项原先藏在练习页的 ⋮ 菜单里，手写/返回提醒只在作答现场出现。
                    SettingsSectionTitle(
                        R.string.settings_practice_title,
                        visible = sectionTitleVisible(SettingsGroup.PRACTICE, keyEntry),
                    )
                    PracticeSettingsCard()
                    Spacer(Modifier.height(8.dp))
                }

                SettingsGroupBlock(SettingsGroup.REVIEW, showSection, groupRevealDelay, topsSink) {
                    // ── 复习 ──
                    SettingsSectionTitle(R.string.memory_plan, visible = sectionTitleVisible(SettingsGroup.REVIEW, keyEntry))

                    GlassCard(
                        backdrop = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = SmoothRoundedCornerShape(14.dp)
                    ) {
                        val templateId by AppPrefs.reviewTemplateFlow.collectAsState()
                        val templates = listOf(ReviewTemplate.SPRINT, ReviewTemplate.STANDARD, ReviewTemplate.DEEP)
                        Column(Modifier.padding(4.dp)) {
                            templates.forEachIndexed { idx, template ->
                                val isSelected = template.id == templateId
                                OptionRow(
                                    // B2-1/S3：计划名按 id 取资源（name 字段已从算法层移除）
                                    label = when (template.id) {
                                        "sprint" -> stringResource(com.ilyskyo.blancall.R.string.plan_name_sprint)
                                        "deep" -> stringResource(com.ilyskyo.blancall.R.string.plan_name_deep)
                                        else -> stringResource(com.ilyskyo.blancall.R.string.plan_name_standard)
                                    },
                                    description = when (template.id) {
                                        "sprint" -> stringResource(com.ilyskyo.blancall.R.string.plan_aggressive)
                                        "deep" -> stringResource(com.ilyskyo.blancall.R.string.plan_relaxed)
                                        else -> stringResource(com.ilyskyo.blancall.R.string.plan_balanced)
                                    },
                                    selected = isSelected,
                                    onClick = { AppPrefs.reviewTemplateId = template.id }
                                )
                                if (idx < templates.size - 1) {
                                    OptionDivider(
                                        faded = isSelected || templates[idx + 1].id == templateId
                                    )
                                }
                            }
                            // 「每天学多久」是首页今日计划的输入，不属于提醒；
                            // 放在这里而不是提醒卡片的折叠区，关掉提醒后设置仍然可见。
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                            StudyDurationSettings()
                            // 考试日期与「卡片上显示目标进度」原先只在首页今日计划面板里能改，
                            // 和上面两行同属「计划怎么摆」，放在同一张卡里
                            PlanDateSettings()
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }

                SettingsGroupBlock(SettingsGroup.REMINDER, showSection, groupRevealDelay, topsSink) {
                    // ── 学习提醒 ──
                    SettingsSectionTitle(R.string.study_reminder, visible = sectionTitleVisible(SettingsGroup.REMINDER, keyEntry))

                    ReminderSettingsCard()

                    Spacer(Modifier.height(8.dp))
                }

                SettingsGroupBlock(SettingsGroup.HELP, showSection, groupRevealDelay, topsSink) {
                    // ── 帮助 ──
                    SettingsSectionTitle(R.string.help_section, visible = sectionTitleVisible(SettingsGroup.HELP, keyEntry))

                    GlassCard(
                        backdrop = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = SmoothRoundedCornerShape(14.dp)
                    ) {
                        Surface(
                            onClick = { navController.navigateOnce("guide") },
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(
                                        AppIconKind.School,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(stringResource(com.ilyskyo.blancall.R.string.guided_tour), style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface)
                                }
                                AppIcon(
                                    AppIconKind.ChevronRight,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        Surface(
                            // 一次性引导看过后就再也不会弹了，必须有出口能重来 ——
                            // 否则用户误关一次，等于永久失去「长按可以做什么」这条信息
                            onClick = { AppPrefs.resetCoachMarks() },
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(
                                        kind = AppIconKind.Refresh,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            stringResource(com.ilyskyo.blancall.R.string.coach_reset),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            stringResource(com.ilyskyo.blancall.R.string.coach_reset_desc),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        Surface(
                            onClick = { navController.navigateOnce("help") },
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(
                                        AppIconKind.Library,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(stringResource(com.ilyskyo.blancall.R.string.help_about), style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface)
                                }
                                AppIcon(
                                    AppIconKind.ChevronRight,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }

                SettingsGroupBlock(SettingsGroup.CONTENT, showSection, groupRevealDelay, topsSink) {
                    // ── 内容管理 ──
                    SettingsSectionTitle(R.string.content_management, visible = sectionTitleVisible(SettingsGroup.CONTENT, keyEntry))

                    GlassCard(
                        backdrop = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = SmoothRoundedCornerShape(14.dp)
                    ) {
                        Surface(
                            onClick = { navController.navigateOnce("tag_manager") },
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(
                                        kind = AppIconKind.Tag,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(stringResource(com.ilyskyo.blancall.R.string.article_tags), style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface)
                                        Text(stringResource(com.ilyskyo.blancall.R.string.article_tags_desc),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                AppIcon(
                                    AppIconKind.ChevronRight,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }

                SettingsGroupBlock(SettingsGroup.DATA, showSection, groupRevealDelay, topsSink) {
                    // ── 数据（学习数据整机备份，换机/重装可一键迁移） ──
                    SettingsSectionTitle(R.string.data_section, visible = sectionTitleVisible(SettingsGroup.DATA, keyEntry))

                    GlassCard(
                        backdrop = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = SmoothRoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.padding(4.dp)) {
                            // 体积可见性：只报数 + 超预算时给一句提示，不做任何删除动作。
                            // 没有记录文件时整行连同分隔线一起不出现 —— 显示「当前 0 B」会被读成
                            // 「有记录但很小」，而事实是「还没有记录」。
                            recordsSizeBytes?.let { size ->
                                Row(
                                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(stringResource(com.ilyskyo.blancall.R.string.data_records_size_title),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface)
                                        Text(
                                            stringResource(com.ilyskyo.blancall.R.string.data_records_size_value,
                                                formatFileSize(size)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(end = 16.dp)
                                        )
                                        if (size >= RECORDS_SIZE_BUDGET_BYTES) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(stringResource(com.ilyskyo.blancall.R.string.data_records_size_warn),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.padding(end = 16.dp))
                                        }
                                    }
                                    // 归档出口（审计 2.2-15）：只在记录多到能归档时才做动作；
                                    // 数量不足时按钮仍在（教用户有这个能力），点击给一句「暂无需归档」
                                    TextButton(onClick = {
                                        backupScope.launch {
                                            val total = withContext(Dispatchers.IO) {
                                                archiveRecordRepo.awaitLoaded()
                                                archiveRecordRepo.records.value.size
                                            }
                                            archiveCandidateCount =
                                                (total - RecordRepository.ARCHIVE_KEEP_RECENT).coerceAtLeast(0)
                                            if (archiveCandidateCount <= 0) {
                                                Toast.makeText(
                                                    backupContext,
                                                    backupResources.getString(com.ilyskyo.blancall.R.string.data_archive_none),
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                archiveDialogVisible = true
                                            }
                                        }
                                    }) {
                                        Text(stringResource(com.ilyskyo.blancall.R.string.data_archive_action))
                                    }
                                }

                                HorizontalDivider(
                                    Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }

                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(com.ilyskyo.blancall.R.string.export_backup), style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(com.ilyskyo.blancall.R.string.export_backup_desc),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 16.dp))
                                }
                                TextButton(onClick = {
                                    // 先选类别，再选文件：见页面顶部的 BackupPickerSheet
                                    backupPickerVisible = true
                                }) { Text(stringResource(com.ilyskyo.blancall.R.string.export_action)) }
                            }

                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(com.ilyskyo.blancall.R.string.import_backup), style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(com.ilyskyo.blancall.R.string.import_backup_desc),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 16.dp))
                                }
                                TextButton(onClick = {
                                    // 放开 MIME 过滤：部分文件管理器返回的备份类型不固定，
                                    // 内容合法性由导入时的备份清单校验兜底
                                    backupImportLauncher.launch(arrayOf("*/*"))
                                }) { Text(stringResource(com.ilyskyo.blancall.R.string.import_action)) }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }

                SettingsGroupBlock(SettingsGroup.EXTENSIONS, showSection, groupRevealDelay, topsSink) {
                    // ── 拓展功能 ──
                    SettingsSectionTitle(R.string.extensions, visible = sectionTitleVisible(SettingsGroup.EXTENSIONS, keyEntry))

                    GlassCard(
                        backdrop = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = SmoothRoundedCornerShape(14.dp)
                    ) {
                        // ── 内置素材库：多库各自可启用，未来可继续追加 ──
                        Text(stringResource(com.ilyskyo.blancall.R.string.built_in_library), style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                        val enabledSet by AppPrefs.builtInLibraryKeysFlow.collectAsState()
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(com.ilyskyo.blancall.R.string.western_thoughts), style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface)
                            }
                            GlassSwitch(
                                checked = "western" in enabledSet,
                                onCheckedChange = { AppPrefs.setLibraryEnabled("western", it) }
                            )
                        }
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(com.ilyskyo.blancall.R.string.library_gaokao), style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface)
                            }
                            GlassSwitch(
                                checked = "gaokao" in enabledSet,
                                onCheckedChange = { AppPrefs.setLibraryEnabled("gaokao", it) }
                            )
                        }
                        // 预留更多库：在此 Row 之后追加即可（如化学句子大全）

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                        // 内容包：用户自己的 zip（txt/md + 清单），可浏览、可一键全文导入
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(com.ilyskyo.blancall.R.string.content_pack_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    stringResource(com.ilyskyo.blancall.R.string.content_pack_settings_desc),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            }
                            GlassButton(
                                onClick = { navController.navigateOnce("content_packs") },
                                modifier = Modifier.heightIn(min = 34.dp)
                            ) {
                                Text(
                                    stringResource(com.ilyskyo.blancall.R.string.import_action),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        // 数据互换：人类可读 JSON 导出 + CSV 回灌
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(com.ilyskyo.blancall.R.string.interchange_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    stringResource(com.ilyskyo.blancall.R.string.interchange_settings_desc),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            }
                            GlassButton(
                                onClick = { navController.navigateOnce("data_interchange") },
                                modifier = Modifier.heightIn(min = 34.dp)
                            ) {
                                Text(
                                    stringResource(com.ilyskyo.blancall.R.string.open),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }

                SettingsGroupBlock(SettingsGroup.ABOUT, showSection, groupRevealDelay, topsSink) {
                    // ── 关于 ──
                    SettingsSectionTitle(R.string.about_section, visible = sectionTitleVisible(SettingsGroup.ABOUT, keyEntry))

                    GlassCard(
                        backdrop = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = SmoothRoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Blancall", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(2.dp))
                            Text("Fill the blank, recall the knowledge.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(com.ilyskyo.blancall.R.string.app_tagline),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(com.ilyskyo.blancall.R.string.app_features_line),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }

                SettingsGroupBlock(SettingsGroup.DONATION, showSection, groupRevealDelay, topsSink) {
                    // ── 赞赏区 ──
                    // 整块可被「隐藏该区域」收起。隐藏态是**会话级**的（SettingsSessionState，
                    // 只在内存里，不落盘）：本次使用不再出现，软件重新启动后自动恢复。
                    if (!SettingsSessionState.donationHidden) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            // 二级页里标题栏已经写着「支持 Blancall」，这一行只留右侧动作按钮；
                            // 单子项时 SpaceBetween 会把按钮推到左边，所以按是否显示标题切排列
                            horizontalArrangement =
                                if (sectionTitleVisible(SettingsGroup.DONATION, keyEntry)) Arrangement.SpaceBetween
                                else Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (sectionTitleVisible(SettingsGroup.DONATION, keyEntry)) {
                                Text(stringResource(com.ilyskyo.blancall.R.string.donation_heading), style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            // 圆圈按钮：右边缘与下方卡片的右边框对齐，提示这一行可以点
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                            CircleShape
                                        )
                                        .pressClick(tier = PressTier.Icon) { showDonationMenu = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AppIcon(
                                        kind = AppIconKind.MoreVert,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                        contentDescription = stringResource(com.ilyskyo.blancall.R.string.more)
                                    )
                                }
                                GlassDropdownMenu(
                                    expanded = showDonationMenu,
                                    onDismissRequest = { showDonationMenu = false }
                                ) {
                                    GlassMenuItem(
                                        onClick = {
                                            showDonationMenu = false
                                            SettingsSessionState.donationHidden = true
                                            // 入口页里还有「帮助」「关于」两段，藏掉赞赏不会留下空白页，
                                            // 所以不退出二级页（旧版只有一个赞赏分区时才需要退）
                                        },
                                        label = {
                                            Column {
                                                Text(stringResource(com.ilyskyo.blancall.R.string.donation_hide),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface)
                                                Spacer(Modifier.height(2.dp))
                                                Text(stringResource(com.ilyskyo.blancall.R.string.donation_hide_hint),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    )
                                    GlassMenuItem(
                                        onClick = {
                                            showDonationMenu = false
                                            startSaveDonationQr()
                                        },
                                        label = {
                                            Text(stringResource(com.ilyskyo.blancall.R.string.donation_save_qr),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    )
                                }
                            }
                        }

                        GlassCard(
                            backdrop = false,
                            modifier = Modifier.fillMaxWidth(),
                            shape = SmoothRoundedCornerShape(14.dp)
                        ) {
                            val qrSide = if (LocalIsLargeScreen) 200.dp else 168.dp
                            Column(
                                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(stringResource(com.ilyskyo.blancall.R.string.donation_statement),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center)
                                Spacer(Modifier.height(6.dp))
                                Text(stringResource(com.ilyskyo.blancall.R.string.donation_invite),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    textAlign = TextAlign.Center)
                                Spacer(Modifier.height(16.dp))
                                // 二维码必须「白底黑码」才能被相机识别：这里的 Color.White 是刚性要求，
                                // 不要换成 colorScheme.surface 或半透明白，深色模式下会掉对比度导致扫不出。
                                // clip 必须写在 background 之前，否则圆角不生效（background(shape) 只画底、不裁内容）。
                                Box(
                                    modifier = Modifier
                                        .size(qrSide)
                                        .clip(SmoothRoundedCornerShape(14.dp))
                                        .background(Color.White)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.appreciation_qr_card),
                                        contentDescription = stringResource(com.ilyskyo.blancall.R.string.donation_qr),
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }

                    // ── 赞赏码保存完成弹窗 ──
                    AnimatedOverlayPresence(target = donationSavedDir) { dir ->
                        BlancallAlertDialog(
                            onDismissRequest = { donationSavedDir = null },
                            title = {
                                Text(stringResource(com.ilyskyo.blancall.R.string.donation_saved_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface)
                            },
                            text = {
                                Text(stringResource(com.ilyskyo.blancall.R.string.donation_saved_body, dir),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    donationSavedDir = null
                                    launchWeChat(donationContext)
                                }) { Text(stringResource(com.ilyskyo.blancall.R.string.donation_open_wechat)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { donationSavedDir = null }) {
                                    Text(stringResource(com.ilyskyo.blancall.R.string.done))
                                }
                            }
                        )
                    }
                }

                // 版本号与隐私政策都要用 context（隐私那一段要读 assets/PRIVACY.md），
                // 两段分属两个渲染门、不再共享作用域，所以在门的上一层取一次
                val context = LocalContext.current

                if (showSection(SettingsGroup.ABOUT)) {
                    // ── 版本号 ──
                    val versionName = remember {
                        runCatching {
                            @Suppress("DEPRECATION")
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        }.getOrNull() ?: ""
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Blancall v$versionName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                if (showSection(SettingsGroup.ABOUT)) {
                    // ── 隐私政策入口（可随时再次查看）──
                    var showLegalDialog by remember { mutableStateOf(false) }
                    var privacyText by remember { mutableStateOf("") }
                    LaunchedEffect(Unit) {
                        privacyText = withContext(Dispatchers.IO) {
                            runCatching {
                                context.assets.open("PRIVACY.md").bufferedReader().use { it.readText() }
                            }.getOrDefault("")
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            stringResource(com.ilyskyo.blancall.R.string.privacy_policy),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .pressClick {
                                    showLegalDialog = true
                                }
                                .padding(top = 6.dp, bottom = 8.dp)
                        )
                    }

                    if (showLegalDialog) {
                        BlancallAlertDialog(
                            onDismissRequest = { showLegalDialog = false },
                            shape = SmoothRoundedCornerShape(28.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            title = {
                                Column(Modifier.padding(top = 4.dp)) {
                                    Text(stringResource(com.ilyskyo.blancall.R.string.privacy_policy),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.height(2.dp))
                                    Text(stringResource(com.ilyskyo.blancall.R.string.privacy_dialog_subtitle),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            text = {
                                Column {
                                    Spacer(Modifier.height(8.dp))
                                    // 圆角卡片容器内滚动查看（assets/PRIVACY.md）
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(SmoothRoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                            .heightIn(max = 420.dp)
                                            .verticalScroll(rememberScrollState())
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        MarkdownText(text = privacyText)
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showLegalDialog = false }) {
                                    Text(stringResource(com.ilyskyo.blancall.R.string.close), color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    // ── 切换回执（浮层，不参与布局高度）──
    // 它回答两件用户此刻真正在意的事：「刚才那一下是我干的」「能不能退」。
    // 不遮内容、不抢焦点：贴底浮着，3.2s 自己走。
    //
    // 文案要留住一帧：visible 变 false 的那一帧 receipt 已经置空，
    // 直接读会闪成空串（与 AnimatedOverlayPresence 同一个理由）。
    var receiptShown by remember { mutableStateOf<SettingsSwapReceipt?>(null) }
    if (receipt != null) receiptShown = receipt
    val receiptVisible = receipt != null && !swapLocked
    val receiptText = stringResource(
        if (receiptShown?.compact == true) com.ilyskyo.blancall.R.string.settings_layout_switched_full
        else com.ilyskyo.blancall.R.string.settings_layout_switched_compact
    )
    AnimatedVisibility(
        visible = receiptVisible,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        enter = fadeIn(MotionFade.alpha(MotionFade.enter)) +
            slideInVertically(Motion.itemSlide()) { it / 2 },
        exit = fadeOut(MotionFade.alpha(MotionFade.exit)) +
            slideOutVertically(Motion.itemSlide()) { it / 2 },
        label = "settingsReceipt",
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SmoothRoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.inverseSurface)
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = receiptText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                maxLines = 2,
                modifier = Modifier
                    .weight(1f)
                    // 读屏用户切完也听得到「刚才做了什么」，不必靠摸回执条
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
            Box(
                modifier = Modifier
                    .clip(SmoothRoundedCornerShape(10.dp))
                    .pressClick(tier = PressTier.Plain, onClick = undoSettingsSwap)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = stringResource(com.ilyskyo.blancall.R.string.settings_layout_undo),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.inversePrimary,
                )
            }
        }
    }

    }

    // ── 首页副标题编辑弹窗 ──
    AnimatedOverlayPresence(visible = showSubtitleDialog) {
        var editText by remember(homeSubtitle) { mutableStateOf(homeSubtitle) }
        BlancallAlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            title = { Text(stringResource(com.ilyskyo.blancall.R.string.edit_home_subtitle)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editText,
                        // 默认副标题 38 字 > 旧 30 字限制，编辑会被拦截；取消限制
                        onValueChange = { editText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(com.ilyskyo.blancall.R.string.custom_subtitle)) },
                        shape = SmoothRoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 直接保存（允许清空：清空即移除副标题）
                        AppPrefs.subtitle = editText
                        showSubtitleDialog = false
                    },
                    shape = SmoothRoundedCornerShape(14.dp)
                ) { Text(stringResource(com.ilyskyo.blancall.R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showSubtitleDialog = false }) { Text(stringResource(com.ilyskyo.blancall.R.string.cancel)) }
            }
        )
    }

    // ── 归档确认弹窗：确认后才导出并裁剪 ──
    AnimatedOverlayPresence(visible = archiveDialogVisible) {
        BlancallAlertDialog(
            onDismissRequest = { archiveDialogVisible = false },
            title = { Text(stringResource(com.ilyskyo.blancall.R.string.data_archive_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        com.ilyskyo.blancall.R.string.data_archive_confirm_body,
                        archiveCandidateCount, RecordRepository.ARCHIVE_KEEP_RECENT
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        archiveDialogVisible = false
                        backupScope.launch {
                            val olds = withContext(Dispatchers.IO) {
                                archiveRecordRepo.oldestRecords(archiveCandidateCount)
                            }
                            if (olds.isEmpty()) return@launch
                            pendingArchive = olds
                            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            recordsArchiveLauncher.launch("blancall_records_archive_$stamp.json")
                        }
                    },
                    shape = SmoothRoundedCornerShape(14.dp)
                ) { Text(stringResource(com.ilyskyo.blancall.R.string.data_archive_action)) }
            },
            dismissButton = {
                TextButton(onClick = { archiveDialogVisible = false }) {
                    Text(stringResource(com.ilyskyo.blancall.R.string.cancel))
                }
            }
        )
    }

    // ── 导入备份确认弹窗：确认后才覆盖当前学习数据 ──
    AnimatedOverlayPresence(target = pendingImportUri) { importUri ->
        BlancallAlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text(stringResource(com.ilyskyo.blancall.R.string.import_backup_title)) },
            text = {
                Text(stringResource(com.ilyskyo.blancall.R.string.import_backup_warning))
            },
            confirmButton = {
                Button(
                    onClick = importClick@{
                        if (BackupImportGate.running) return@importClick
                        pendingImportUri = null
                        BackupImportGate.running = true
                        // 导入是破坏性整库替换 + 全量重载：不能被「离开设置页/旋转屏幕」取消，
                        // 否则文件已换、内存未重载，下一次任意保存会把恢复的数据覆盖回旧库。
                        // NonCancellable 让这次 job 不受 rememberCoroutineScope 生命周期影响。
                        val importJob = backupScope.launch(NonCancellable) {
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    val temp = File(backupContext.cacheDir, "backup_import.zip")
                                    try {
                                        backupContext.contentResolver.openInputStream(importUri)?.use { input ->
                                            temp.outputStream().use { output ->
                                                val buffer = ByteArray(8192)
                                                var total = 0L
                                                while (true) {
                                                    val read = input.read(buffer)
                                                    if (read < 0) break
                                                    total += read
                                                    if (total > BackupManager.MAX_TOTAL_BYTES) {
                                                        throw BackupManager.BackupError.TooLarge(null)
                                                    }
                                                    output.write(buffer, 0, read)
                                                }
                                            }
                                        } ?: throw BackupManager.BackupError.CannotRead()
                                        BackupManager.restoreZip(temp, backupContext.filesDir)
                                    } finally {
                                        temp.delete()
                                    }
                                }
                            }
                            result.onSuccess {
                                recordsVersion++   // 文件被整体替换 → 让体积那一行重算
                                // 全量重载各 Store 的内存镜像（B1-3）：漏一个就会在下次写盘时
                                // 用旧内存覆盖刚恢复的数据 —— 清单完整性由 LearningDataReloaderTest 钉死
                                val reloadFailures = withContext(Dispatchers.IO) {
                                    LearningDataReloader.reloadAll(backupContext)
                                }
                                val doneText = when {
                                    it.removed > 0 -> backupResources.getString(
                                        R.string.backup_restore_done_cleaned, it.restored, it.removed
                                    )
                                    // 部分类目备份：说清楚是「几类」而不是「整库」
                                    it.groups > 0 -> backupResources.getQuantityString(
                                        R.plurals.backup_restore_groups,
                                        it.restored,
                                        it.restored,
                                        it.groups,
                                    )
                                    else -> backupResources.getString(R.string.backup_restore_done, it.restored)
                                }
                                backupRestartMessage = doneText +
                                    backupResources.getString(R.string.backup_restore_applied_hint) +
                                    if (reloadFailures > 0) {
                                        // 个别 Store 重载失败：数据已在盘上、重启后必然生效 —— 如实说明
                                        backupResources.getString(
                                            R.string.backup_restore_reload_failed, reloadFailures
                                        )
                                    } else ""
                            }.onFailure {
                                Toast.makeText(
                                    backupContext,
                                    backupResources.getString(
                                        R.string.backup_import_failed,
                                        backupErrorMessage(backupContext, it)
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        // 成功、失败、重载抛异常 —— 任何一条路径结束都要开闸，
                        // 否则用户被永久锁在「正在恢复」里。invokeOnCompletion 覆盖全部路径。
                        importJob.invokeOnCompletion { BackupImportGate.running = false }
                    },
                    shape = SmoothRoundedCornerShape(14.dp)
                ) { Text(stringResource(com.ilyskyo.blancall.R.string.import_action)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text(stringResource(com.ilyskyo.blancall.R.string.cancel)) }
            }
        )
    }

    // ── 导入进行中：不可关闭的进度框 ──
    // 闸门只是兜底；真正要消灭的是「点了没反应」的错觉 —— 整库替换 + 全量重载
    // 期间必须让用户看见正在发生什么，否则他会重开导入流程再点一次。
    AnimatedOverlayPresence(target = if (BackupImportGate.running) Unit else null) {
        BlancallAlertDialog(
            onDismissRequest = { /* 不可取消：NonCancellable 协程已经在写盘 */ },
            title = { Text(stringResource(com.ilyskyo.blancall.R.string.backup_importing_title)) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(com.ilyskyo.blancall.R.string.backup_importing_body))
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )
    }

    // ── 导入成功提示：各 Store 已全量重载（B1-3），无需重启 ──
    AnimatedOverlayPresence(target = backupRestartMessage) { message ->
        BlancallAlertDialog(
            onDismissRequest = { backupRestartMessage = null },
            title = { Text(stringResource(com.ilyskyo.blancall.R.string.import_success)) },
            text = { Text(message) },
            confirmButton = {
                Button(
                    onClick = { backupRestartMessage = null },
                    shape = SmoothRoundedCornerShape(14.dp)
                ) { Text(stringResource(com.ilyskyo.blancall.R.string.done)) }
            }
        )
    }
}

/**
 * 练习记录体积预算：超过就在设置页给一句「建议导出备份」。
 *
 * 为什么是 20 MB：`records.json` 是全量常驻内存的 JSONL，审计估 1 万条约 15–25 MB，
 * 到那个量级时冷启动解析已经开始可感。这里只在用户看得见的地方点出来 ——
 * **不做静默淘汰**：删掉的是用户自己练出来的履历，只能由用户自己决定。
 */
private const val RECORDS_SIZE_BUDGET_BYTES = 20L * 1024 * 1024

/**
 * 文件大小的人类可读写法（B/KB/MB/GB，一位小数）。
 * 用 [Locale.US] 固定小数点，避免在逗号做小数分隔符的语言里出现「1,5 MB」这种和别处不一致的写法。
 */
private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> String.format(Locale.US, "%.1f GB", bytes.toDouble() / (1L shl 30))
    bytes >= 1L shl 20 -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1L shl 20))
    bytes >= 1L shl 10 -> String.format(Locale.US, "%.1f KB", bytes.toDouble() / (1L shl 10))
    else -> "$bytes B"
}

/**
 * 备份错误 → 本地化展示文案。
 *
 * Toast 在协程里弹（`backupScope.launch`），取不到 @Composable 上下文，所以用
 * `Context.getString` 而不是 `stringResource`；数据层只给错误类型
 * （[BackupManager.BackupError]），文案归属这条线不断（键 `backup_err_*`）。
 */
private fun backupErrorMessage(context: Context, e: Throwable): String = when (e) {
    is BackupManager.BackupError.NotABackup -> context.getString(R.string.backup_err_not_backup)
    is BackupManager.BackupError.CorruptManifest -> context.getString(R.string.backup_err_corrupt)
    is BackupManager.BackupError.UnsupportedVersion -> context.getString(R.string.backup_err_version)
    is BackupManager.BackupError.Incomplete -> context.getString(R.string.backup_err_incomplete, e.entryName)
    is BackupManager.BackupError.TooLarge -> context.getString(R.string.backup_err_too_large)
    is BackupManager.BackupError.UnsafeEntry -> context.getString(R.string.backup_err_unsafe, e.entryName)
    is BackupManager.BackupError.EmptyData -> context.getString(R.string.backup_err_empty)
    is BackupManager.BackupError.NothingToExport -> context.getString(R.string.backup_err_nothing)
    is BackupManager.BackupError.CannotWrite -> context.getString(R.string.backup_err_cannot_write)
    is BackupManager.BackupError.CannotRead -> context.getString(R.string.backup_err_cannot_read)
    else -> context.getString(R.string.backup_err_unknown)
}

/**
 * 自定义主题色面板：色环取色（复用标签取色器，颜色数学全在 ColorOps 里）。
 *
 * 取色即生效 —— 拖动色环时整页颜色会跟着「溶」过去（BlancallTheme 的 140ms 过渡），
 * 不需要「应用」按钮，所以底部只有一个「完成」用来收起面板。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomAccentSheet(
    appearance: AppearanceSnapshot,
    onDismiss: () -> Unit,
) {
    // 拖动期间只改内存，离开组合这一下才落盘 —— 滑掉面板、点完成、返回键都走这里
    DisposableEffect(Unit) { onDispose { AppearanceStore.flush() } }
    GlassModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(stringResource(com.ilyskyo.blancall.R.string.theme_color_custom),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            // 色环拖动同样是每帧回调 → 只改内存，面板关闭时统一落盘
            TagColorPicker(
                color = if (appearance.useCustomAccent) appearance.customArgb
                else appearance.accentPreset().primaryLight.toArgb(),
                previewName = "Aa",
                onColorChange = { c ->
                    AppearanceStore.preview {
                        it.copy(customArgb = c or (0xFF shl 24), useCustomAccent = true)
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
            GlassButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(com.ilyskyo.blancall.R.string.done),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/**
 * 符号栏预设池：自定义面板里可勾选的候选字符。
 *
 * 三族混排（中文标点 / 英文标点 / 理科常用），因为用户实际会同时需要它们：
 * 语文默写要「，。？」、英语听写要 `' -`、理科填空要「↑↓⇌Δ°」。这里只是**候选**，
 * 真正上栏的是勾中的那些 —— 条数不设上限：面板下方那行是横滑的（见 HandwritingPanel）。
 *
 * 刻意不放空格：符号栏是「一键一个字符」，空格会让人误以为能在面板里分词。
 */
private val SYMBOL_PRESETS: List<Char> = listOf(
    // 中文标点
    '，', '。', '、', '？', '！', '：', '；', '…', '《', '》', '（', '）', '·', '—',
    // 英文标点（半角：英文空本就该用半角）
    ',', '.', '?', '!', ';', ':', '\'', '-', '/', '(',
    // 理科常用（化学方程式 / 数学关系）
    '↑', '↓', '⇌', 'Δ', '°', '×', '÷', '√', '±', '≈', '≤', '≥',
)

/**
 * 从一段输入里取出**第一个**可用作符号的字符（判据见 `AppPrefs.isSymbolChar`）。
 *
 * 取第一个而不是要求「整段必须合法」：手机输入法一次会上屏一串（联想词、粘贴整句），
 * 严格要求长度为 1 的话用户点候选词就什么也进不去，看着像输入框坏了。
 * 遇到汉字、字母就跳过，遇到第一个符号即停；一个符号都没有则返回 null（框保持空）。
 */
internal fun sanitizeSymbolInput(raw: String): Char? = raw.firstOrNull { AppPrefs.isSymbolChar(it) }

/**
 * 把「勾中的预设」与「自输的补充字符」合成一条符号串。
 *
 * 顺序规则：先按 [presets] 的固定顺序列出勾中项，再接自输字符。
 * 为什么不用点击顺序：勾选顺序没有语义，而固定顺序保证「加了又删、删了又加」之后
 * 符号栏不会悄悄换位 —— 位置稳定才谈得上肌肉记忆。
 *
 * 去重与空白过滤统一由 `AppPrefs.normalizeSymbols` 在写回时执行，这里只负责
 * 自输字符不与预设重复。
 */
internal fun composeSymbols(presets: List<Char>, selected: Set<Char>, custom: String): String {
    val out = StringBuilder()
    for (ch in presets) if (ch in selected) out.append(ch)
    for (ch in custom) {
        if (ch.isWhitespace() || ch.isHighSurrogate() || ch.isLowSurrogate()) continue
        if (out.indexOf(ch.toString()) >= 0) continue
        out.append(ch)
    }
    return out.toString()
}

/**
 * 符号栏内容编辑面板（设置 → 特性 → 符号栏内容）。
 *
 * 中文 / 英文各一套 —— 中文空的符号（，。？！）和英文空的符号（, . ? !）本就不是一回事，
 * 用一套会必然错一半。顶部两个 chip 切换当前编辑哪一套。
 *
 * 改动**立即写入** AppPrefs，没有「保存」：与自定义主题色同一套心智 —— 改完点完成收起，
 * 不想要就恢复默认。手写板在练习页、不在这张面板后面，所以即时生效不会让人看不到结果。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HandwritingSymbolSheet(onDismiss: () -> Unit) {
    val cjk by AppPrefs.handwritingSymbolsCjkFlow.collectAsState()
    val latin by AppPrefs.handwritingSymbolsLatinFlow.collectAsState()
    var editLatin by remember { mutableStateOf(false) }

    val raw = if (editLatin) latin else cjk
    val chosen = raw.toSet()
    // 自输补充 = 当前集合里不在预设池的那些字符
    val custom = raw.filter { it !in SYMBOL_PRESETS }
    val write: (String) -> Unit = { v ->
        if (editLatin) AppPrefs.handwritingSymbolsLatin = v
        else AppPrefs.handwritingSymbolsCjk = v
    }
    // 输入框里那**一个**待添加的字符（还没上栏）。切到另一套就清空：
    // 给中文打的「，」不该跟着跳到英文那套里。
    var pending by remember(editLatin) { mutableStateOf("") }
    val pendingChar = sanitizeSymbolInput(pending)
    // 已在栏上就不给点：点了也是原地不动，不如让按钮明说"没得加"
    val canAdd = pendingChar != null && pendingChar !in chosen
    val addPending: () -> Unit = {
        val ch = sanitizeSymbolInput(pending)
        if (ch != null && ch !in chosen) {
            write(composeSymbols(SYMBOL_PRESETS, chosen, custom + ch))
        }
        pending = ""
    }

    GlassModalBottomSheet(onDismissRequest = onDismiss) {
        // ⚠️ 不要再套 Modifier.verticalScroll：GlassModalBottomSheet 的 contentScrollable
        // 默认 true，已经给内容加了滚动。再叠一层，内层会拿到无限高约束 →
        // 运行期直接崩「Vertically scrollable component was measured with an infinity
        // maximum height constraints」（真机复现：一点「符号栏内容」应用就退出）。
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                stringResource(com.ilyskyo.blancall.R.string.handwriting_symbols_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))

            // ── 编辑哪一套 ──
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    stringResource(com.ilyskyo.blancall.R.string.handwriting_symbols_cjk) to false,
                    stringResource(com.ilyskyo.blancall.R.string.handwriting_symbols_latin) to true,
                ).forEach { (label, isLatin) ->
                    val active = editLatin == isLatin
                    AssistChip(
                        onClick = { editLatin = isLatin },
                        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                        shape = SmoothRoundedCornerShape(8.dp),
                        // 选中态加 1dp 主色描边：浅色主题下 primaryContainer（淡紫）与
                        // surfaceVariant（浅灰）亮度太接近，只靠填色分不出「现在在编哪一套」。
                        // 与底栏选中胶囊同一套语言（填色 + 描边）。
                        border = if (active) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            // ── 预设勾选 ──
            // 一枚键一个固定方寸（42dp 超椭圆），不再用 AssistChip ——
            // chip 的宽由字符撑开、高由 M3 定死，一排里 `…` 和 `≤` 会一大一小。
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SYMBOL_PRESETS.forEach { ch ->
                    val on = ch in chosen
                    SymbolKey(
                        char = ch,
                        onClick = {
                            val next = if (on) raw.filter { it != ch } else raw + ch
                            write(next.toString())
                        },
                        selected = on,
                        selectable = true,
                        size = 42.dp,
                        corner = 13.dp,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // ── 自输补充 ──
            // 标签左、用量右：右上角永远是「这一栏一共几个」，不是「补充了几个」。
            // 没有上限可报了（旧版是「12 / 12」），因此这里只说总数。
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(com.ilyskyo.blancall.R.string.handwriting_symbols_add),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(
                        com.ilyskyo.blancall.R.string.handwriting_symbols_total,
                        raw.length
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))

            // 输入框 + 添加：一次只收一个符号。
            // 旧版把输入框当成「整条补充串」在用 —— 粘一段汉字进去也能存，
            // 存完符号栏里躺着一排汉字。现在输入框只装一个待添加的字符，
            // 由「添加」按钮确认后才上栏（用户原话：输入完没有添加按钮，没法确认）。
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = pending,
                    // 过滤发生在输入的第一时间：汉字/字母/整段文字进不来，框里只会出现一个符号
                    onValueChange = { input ->
                        pending = sanitizeSymbolInput(input)?.toString().orEmpty()
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addPending() }),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) { inner ->
                    // 自绘容器：M3 OutlinedTextField 的 14dp 圆角与整页不一致，
                    // 且它自带 56dp 最小高度，会让面板底部多出一段空白。
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(SmoothRoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (pending.isEmpty()) {
                            Text(
                                stringResource(com.ilyskyo.blancall.R.string.handwriting_symbols_add_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        inner()
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .height(46.dp)
                        // 宽度随文字走但不小于 72dp：中文「添加」两个字够用，
                        // 俄文「Добавить」也不至于被裁成「Добав」
                        .widthIn(min = 72.dp)
                        .clip(SmoothRoundedCornerShape(12.dp))
                        .background(
                            if (canAdd) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .pressClick(tier = PressTier.Container, enabled = canAdd, onClick = addPending)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(com.ilyskyo.blancall.R.string.handwriting_symbols_add_action),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (canAdd) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // 已添加的自输字符：同款格子，点一下即移除 ——
            // 「加了没法撤」是上一版的另一个缺口（只能恢复默认，把预设也一起清了）。
            if (custom.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(com.ilyskyo.blancall.R.string.handwriting_symbols_added),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    custom.forEach { ch ->
                        SymbolKey(
                            char = ch,
                            onClick = { write(raw.filter { it != ch }.toString()) },
                            selected = true,
                            selectable = true,
                            size = 42.dp,
                            corner = 13.dp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            // 限制写在这里而不是弹窗报错：用户读到它的时机正好是"准备打汉字"的那一刻
            Text(
                stringResource(com.ilyskyo.blancall.R.string.handwriting_symbols_only_cjk_latin),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = {
                        write(
                            if (editLatin) AppPrefs.DEFAULT_SYMBOLS_LATIN
                            else AppPrefs.DEFAULT_SYMBOLS_CJK
                        )
                    }
                ) {
                    Text(stringResource(com.ilyskyo.blancall.R.string.handwriting_symbols_reset))
                }
                Spacer(Modifier.weight(1f))
                GlassButton(onClick = onDismiss) {
                    Text(
                        stringResource(com.ilyskyo.blancall.R.string.done),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 触感强度的四个可选项：`value` 存进偏好（与 ui.common.HapticLevel 的字符串一一对应），
 * `label` 上屏。顺序就是屏幕上从左到右 —— 「关」放最后，避免手滑点掉全部反馈。
 */
// 顶层数据表不是组合上下文，所以存 @StringRes，由渲染处解出来（与底栏 tab 同一做法）
private val hapticLevelChoices = listOf(
    "light" to com.ilyskyo.blancall.R.string.haptic_weak,
    "standard" to com.ilyskyo.blancall.R.string.haptic_standard,
    "strong" to com.ilyskyo.blancall.R.string.haptic_strong,
    "off" to com.ilyskyo.blancall.R.string.haptic_off,
)

/**
 * 正文分区标题该不该出现。
 *
 * 判据只有一条：**组名与入口名是不是同一个词**。相同就没必要在页内再写一遍
 * （标题栏已经写着「外观」），不同就必须写 —— 合并入口里
 * 「内容与数据」下面的「内容管理 / 数据 / 扩展」三段全靠这几个小标题分家。
 * 展开态（entry == null）一律显示，与原布局逐字一致。
 */
@Composable
private fun sectionTitleVisible(group: SettingsGroup, entry: SettingsEntry?): Boolean =
    sectionTitleVisible(settingsGroupTitle(group), entry)

@Composable
private fun sectionTitleVisible(groupTitle: String, entry: SettingsEntry?): Boolean =
    entry == null || groupTitle != settingsEntryTitle(entry)

/**
 * 设置分区标题。
 *
 * 只在「1 级」写标题：完整布局与紧凑布局的顶层，标题把分区串成目录；进了 2 级页之后，
 * 标题栏已经写着同一个分区名，正文里再写一遍等于同一句话说两次。
 * 判定由调用点传 [visible]，组件本身不猜 —— 这条规则将来要改也只改一处，不会散在 12 个分区里。
 */
@Composable
private fun SettingsSectionTitle(titleRes: Int, visible: Boolean) {
    SettingsSectionTitle(stringResource(titleRes), visible)
}

@Composable
private fun SettingsSectionTitle(text: String, visible: Boolean) {
    if (!visible) return
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * 单选项行。
 *
 * 选中态不是「整行铺底的矩形」，而是一块**内缩的圆角高亮**：
 * 距卡片左右各 8dp（卡片自身的 4dp 内边距 + 本行的 4dp）、
 * 上下各 6dp（卡片 4dp + 本行 2dp），圆角 10dp。
 * 旧实现直接吃 [Surface] 默认的 RectangleShape，直角块一路顶到卡片描边，
 * 又比分隔线（左右各内缩 16dp）宽出一截，看着像一块贴错位置的色块。
 *
 * 三个容易踩的点：
 * - 行内水平 padding 由 16dp 收到 12dp，正好抵消行级的 4dp 缩进 ——
 *   文字左缘仍与卡片里其它行（界面语言、开关行都是 20dp）严格对齐。
 * - 右侧勾图钉**常驻 20dp 占位**，只用透明度/缩放表达选中：否则勾出现时
 *   会把 weight(1f) 的文字区挤窄，description 的换行点一变、行高就跳。
 * - 未选中底色用 Transparent 而非 surface：GlassCard 的底本来就与 surface 同色，
 *   铺一层不透明色只会在圆角处多出一圈色差。
 * - 文字不随选中换色：内缩色块 + 勾已经说清「这一项是选中的」，
 *   再把标题染成主色会让整行读起来像强调/链接，也会和右侧「当前值用主色」的摘要行撞语义。
 */
@Composable
private fun OptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    description: String? = null
) {
    // 深色底更吃颜色，选中块的浓度要比浅色高一档才看得出边界
    val highlight = MaterialTheme.colorScheme.primary.copy(
        alpha = if (isBlancallDark()) 0.18f else 0.10f
    )
    // 选中块与勾图共用同一条淡入时长：两者是同一个动作（「这一项现在是选中的」），
    // 色块硬切、勾渐显会让它们读起来像两个事件。
    val rowColor by animateColorAsState(
        targetValue = if (selected) highlight else Color.Transparent,
        animationSpec = MotionFade.color(MotionFade.enter),
        label = "optionHighlight"
    )
    val checkPresence by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = MotionFade.alpha(MotionFade.enter),
        label = "optionCheck"
    )
    Surface(
        onClick = onClick,
        color = rowColor,
        shape = SmoothRoundedCornerShape(10.dp),
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface)
                if (description != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                AppIcon(
                    kind = AppIconKind.Check,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .alpha(checkPresence)
                        .scale(0.82f + 0.18f * checkPresence)
                )
            }
        }
    }
}

/**
 * 选项之间的细分隔线。[faded] 为真时淡出但保留 1dp 占位（不参与布局变化）。
 *
 * 选中块自己有完整的圆角边界，线压在上头会显得脏；同时它比线宽 8dp，
 * 两者并存时右端会露出「线在块里断掉」的错觉。所以选中项上下相邻的线一律淡出。
 */
@Composable
private fun OptionDivider(faded: Boolean) {
    val lineAlpha by animateFloatAsState(
        targetValue = if (faded) 0f else 0.3f,
        animationSpec = MotionFade.alpha(MotionFade.enter),
        label = "optionDivider"
    )
    HorizontalDivider(
        Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = lineAlpha)
    )
}

// ========== 学习时长 / 学习提醒设置 ==========

/**
 * 每日学习时长（分钟）。
 *
 * 它只喂首页「今日计划」，与通知里的「今天还差几次」（dailyPracticeGoal）是两个目标。
 * 放在复习计划卡片里、且不随提醒开关消失：关掉提醒的人仍然有每日计划要设置。
 */
@Composable
private fun StudyDurationSettings() {
    val goalMinutes by ReminderPrefs.goalMinutesFlow.collectAsState()
    var showCustomMinutesDialog by remember { mutableStateOf(false) }
    var customMinutesInput by remember { mutableStateOf("") }

    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(stringResource(com.ilyskyo.blancall.R.string.daily_goal), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        // 当前值可能恰是放不下的那个预设：保它可见，否则整行会像“没选中”
        val durationPresets = listOf(5, 10, 20)
        AdaptiveChipRow(
            modifier = Modifier.fillMaxWidth(),
            spacing = 8.dp,
            alwaysTrailing = 1,
            keepIndices = if (goalMinutes in durationPresets) {
                setOf(durationPresets.indexOf(goalMinutes))
            } else {
                emptySet()
            },
        ) {
            durationPresets.forEach { mins ->
                FilterChip(
                    selected = goalMinutes == mins,
                    onClick = { ReminderPrefs.goalMinutes = mins },
                    label = { AdaptiveChipLabel(stringResource(com.ilyskyo.blancall.R.string.minutes_unit, mins)) },
                    shape = SmoothRoundedCornerShape(8.dp)
                )
            }
            FilterChip(
                selected = goalMinutes !in listOf(5, 10, 20),
                onClick = {
                    customMinutesInput = if (goalMinutes in listOf(5, 10, 20)) "" else goalMinutes.toString()
                    showCustomMinutesDialog = true
                },
                label = {
                    AdaptiveChipLabel(
                        if (goalMinutes in listOf(5, 10, 20)) stringResource(com.ilyskyo.blancall.R.string.custom)
                        else stringResource(com.ilyskyo.blancall.R.string.minutes_unit, goalMinutes)
                    )
                },
                shape = SmoothRoundedCornerShape(8.dp)
            )
        }
    }

    AnimatedOverlayPresence(visible = showCustomMinutesDialog) {
        BlancallAlertDialog(
            onDismissRequest = { showCustomMinutesDialog = false },
            title = { Text(stringResource(com.ilyskyo.blancall.R.string.custom_daily_goal_title)) },
            text = {
                OutlinedTextField(
                    value = customMinutesInput,
                    onValueChange = { input ->
                        customMinutesInput = input.filter { it.isDigit() }.take(4)
                    },
                    label = { Text(stringResource(com.ilyskyo.blancall.R.string.minutes_field_label)) },
                    suffix = { Text(stringResource(com.ilyskyo.blancall.R.string.minutes_word)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(
                    enabled = customMinutesInput.toIntOrNull() != null,
                    onClick = {
                        customMinutesInput.toIntOrNull()?.let { ReminderPrefs.goalMinutes = it.coerceIn(1, 1440) }
                        showCustomMinutesDialog = false
                    }
                ) { Text(stringResource(com.ilyskyo.blancall.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomMinutesDialog = false }) {
                    Text(stringResource(com.ilyskyo.blancall.R.string.cancel))
                }
            }
        )
    }
}

private fun openReminderNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    runCatching { context.startActivity(intent) }
}

/** 权限 / 渠道被系统关掉时的提示行：文案 + 一个直接去系统设置的出口。 */
@Composable
private fun ReminderStatusRow(text: String, action: String, onAction: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
        TextButton(onClick = onAction) {
            Text(action, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun QuietHourRow(labelRes: Int, minutes: Int, onClick: () -> Unit) {
    val context = LocalContext.current
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(labelRes), style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface)
            Text(
                NotificationHelper.formatTimeOfDay(context, minutes),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderSettingsCard() {
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val enabled by ReminderPrefs.enabledFlow.collectAsState()
    val hour by ReminderPrefs.hourFlow.collectAsState()
    val minute by ReminderPrefs.minuteFlow.collectAsState()
    val frequency by ReminderPrefs.frequencyFlow.collectAsState()
    val reminderDays by ReminderPrefs.reminderDaysFlow.collectAsState()
    val quietEnabled by ReminderPrefs.quietHoursEnabledFlow.collectAsState()
    val quietStart by ReminderPrefs.quietStartMinutesFlow.collectAsState()
    val quietEnd by ReminderPrefs.quietEndMinutesFlow.collectAsState()
    val dailyGoal by ReminderPrefs.dailyPracticeGoalFlow.collectAsState()

    var showTimePicker by remember { mutableStateOf(false) }
    var showQuietStartPicker by remember { mutableStateOf(false) }
    var showQuietEndPicker by remember { mutableStateOf(false) }
    var showCustomGoalDialog by remember { mutableStateOf(false) }
    var customGoalInput by remember { mutableStateOf("") }

    // 系统通知可能在本页之外被改（权限弹窗 / 系统设置 / 通知渠道被关），
    // 每次回到前台重读一次，否则开关会一直显示过期的「已开启」
    var notificationStatus by remember { mutableStateOf(ReminderNotifications.status(context)) }
    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        val owner = lifecycleOwner ?: return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationStatus = ReminderNotifications.status(context)
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    val nextAt = ReminderSchedule.nextAt(LocalDateTime.now(), hour, minute, reminderDays)
    val nextAtText = NotificationHelper.formatReminderTime(context, nextAt)
    val reminderInQuietHours = quietEnabled &&
        ReminderPlan.isQuietMinute(hour * 60 + minute, quietStart, quietEnd)

    fun reschedule() {
        // WorkManager 未初始化等异常不应崩设置页；下一次回前台还有补调度兜底
        runCatching { ReminderWorker.scheduleNext(context) }
    }

    GlassCard(
        backdrop = false,
        modifier = Modifier.fillMaxWidth(),
        shape = SmoothRoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(4.dp)) {
            // ── 开关行 ──
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(com.ilyskyo.blancall.R.string.reminder_enable), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        when {
                            !enabled -> stringResource(com.ilyskyo.blancall.R.string.reminder_off_state)
                            !notificationStatus.notificationsEnabled ->
                                stringResource(com.ilyskyo.blancall.R.string.reminder_on_blocked)
                            notificationStatus.channelsBlocked ->
                                stringResource(com.ilyskyo.blancall.R.string.reminder_on_channel_blocked)
                            else -> stringResource(com.ilyskyo.blancall.R.string.reminder_on_at, nextAtText)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (enabled && !notificationStatus.allGood) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                GlassSwitch(
                    checked = enabled,
                    onCheckedChange = { newEnabled ->
                        ReminderPrefs.enabled = newEnabled
                        if (newEnabled) {
                            // 先排期，再请求权限。旧实现只在「权限已授予」的分支里补排，
                            // 权限恰好被拒时队列里连 Worker 都没有 —— 界面显示「已开启 · 下次 20:00」，
                            // 当天却一声不响，用户开完就锁屏根本不会回来触发 onResume 兜底。
                            runCatching { ReminderWorker.scheduleNext(context) }
                            (context as? MainActivity)?.requestNotificationPermissionIfNeeded()
                        } else {
                            runCatching { ReminderWorker.cancel(context) }
                        }
                        notificationStatus = ReminderNotifications.status(context)
                    }
                )
            }

            // ── 展开设置（仅开启时显示）──
            AnimatedVisibility(
                visible = enabled,
                enter = fadeIn(MotionFade.alpha(MotionFade.enter)) +
                    expandVertically(Motion.contentSize()),
                exit = fadeOut(MotionFade.alpha(MotionFade.exit)) +
                    shrinkVertically(Motion.contentSize())
            ) {
                Column {
                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // 提醒时间
                    Surface(
                        onClick = { showTimePicker = true },
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(com.ilyskyo.blancall.R.string.reminder_time), style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    NotificationHelper.formatTimeOfDay(context, hour * 60 + minute),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("▸", style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // 提醒时间落在免打扰窗口里时，当天会被 Worker 静默跳过；
                    // 以前没有任何地方告诉用户，现在把因果关系直接写在时间下面
                    if (reminderInQuietHours) {
                        Text(
                            stringResource(com.ilyskyo.blancall.R.string.reminder_time_in_quiet_hours),
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // 提醒频率与提醒日
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(stringResource(com.ilyskyo.blancall.R.string.reminder_frequency), style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ReminderFrequency.entries.forEach { freq ->
                                FilterChip(
                                    selected = frequency == freq,
                                    onClick = {
                                        ReminderPrefs.frequency = freq
                                        reschedule()
                                    },
                                    label = { Text(stringResource(freq.labelRes), style = MaterialTheme.typography.labelSmall) },
                                    shape = SmoothRoundedCornerShape(8.dp)
                                )
                            }
                        }

                        if (frequency == ReminderFrequency.CUSTOM) {
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(com.ilyskyo.blancall.R.string.reminder_days), style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DayOfWeek.values().forEach { day ->
                                    val selected = day in reminderDays
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            val newDays = if (selected) reminderDays - day else reminderDays + day
                                            if (newDays.isEmpty()) {
                                                // 空集合会让提醒彻底静默；宁可拦下这次取消
                                                Toast.makeText(
                                                    context,
                                                    com.ilyskyo.blancall.R.string.reminder_keep_one_day,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                ReminderPrefs.frequency = ReminderFrequency.CUSTOM
                                                ReminderPrefs.reminderDays = newDays
                                                reschedule()
                                            }
                                        },
                                        label = {
                                            Text(
                                                ReminderSchedule.shortDayName(day, locale),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        shape = SmoothRoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        } else if (frequency != ReminderFrequency.DAILY) {
                            // 预设档以前只写「每周 3 天」，用户看不出是哪三天
                            Spacer(Modifier.height(8.dp))
                            Text(
                                ReminderSchedule.formatDays(reminderDays, locale),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // 每日练习次数目标：通知里的「今天还差 N 次」按它计算。
                    // 与上面的「每日学习时长」是两个目标，分开放在各自语义正确的位置。
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(stringResource(com.ilyskyo.blancall.R.string.custom_goal_label), style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(com.ilyskyo.blancall.R.string.reminder_daily_goal_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1, 3, 5, 10).forEach { g ->
                                FilterChip(
                                    selected = dailyGoal == g,
                                    onClick = { ReminderPrefs.dailyPracticeGoal = g },
                                    label = {
                                        AdaptiveChipLabel(
                                            stringResource(com.ilyskyo.blancall.R.string.goal_count_chip, g)
                                        )
                                    },
                                    shape = SmoothRoundedCornerShape(8.dp)
                                )
                            }
                            FilterChip(
                                selected = dailyGoal !in listOf(1, 3, 5, 10),
                                onClick = {
                                    customGoalInput = if (dailyGoal in listOf(1, 3, 5, 10)) "" else dailyGoal.toString()
                                    showCustomGoalDialog = true
                                },
                                label = {
                                    AdaptiveChipLabel(
                                        if (dailyGoal in listOf(1, 3, 5, 10)) {
                                            stringResource(com.ilyskyo.blancall.R.string.custom)
                                        } else {
                                            stringResource(com.ilyskyo.blancall.R.string.goal_count_chip, dailyGoal)
                                        }
                                    )
                                },
                                shape = SmoothRoundedCornerShape(8.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // 免打扰时段
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Row(
                            Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(com.ilyskyo.blancall.R.string.reminder_quiet_hours),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    if (quietEnabled) {
                                        "${NotificationHelper.formatTimeOfDay(context, quietStart)} – " +
                                            NotificationHelper.formatTimeOfDay(context, quietEnd)
                                    } else {
                                        stringResource(com.ilyskyo.blancall.R.string.reminder_off_state)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            GlassSwitch(
                                checked = quietEnabled,
                                onCheckedChange = {
                                    ReminderPrefs.quietHoursEnabled = it
                                    reschedule()
                                }
                            )
                        }
                        AnimatedVisibility(
                            visible = quietEnabled,
                            enter = fadeIn(MotionFade.alpha(MotionFade.enter)) +
                                expandVertically(Motion.contentSize()),
                            exit = fadeOut(MotionFade.alpha(MotionFade.exit)) +
                                shrinkVertically(Motion.contentSize())
                        ) {
                            Column {
                                Spacer(Modifier.height(4.dp))
                                QuietHourRow(com.ilyskyo.blancall.R.string.date_range_start, quietStart) {
                                    showQuietStartPicker = true
                                }
                                QuietHourRow(com.ilyskyo.blancall.R.string.date_range_end, quietEnd) {
                                    showQuietEndPicker = true
                                }
                                if (quietStart == quietEnd) {
                                    Text(
                                        stringResource(com.ilyskyo.blancall.R.string.reminder_quiet_same),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // 系统通知状态：权限被拒 / 渠道被关时给出明确出口，
                    // 不再让开关在「系统已拦截」的情况下继续显示「已开启」
                    if (!notificationStatus.notificationsEnabled) {
                        ReminderStatusRow(
                            text = stringResource(com.ilyskyo.blancall.R.string.reminder_notifications_off),
                            action = stringResource(com.ilyskyo.blancall.R.string.reminder_open_settings),
                            onAction = { openReminderNotificationSettings(context) }
                        )
                    } else if (notificationStatus.channelsBlocked) {
                        ReminderStatusRow(
                            text = stringResource(com.ilyskyo.blancall.R.string.reminder_channels_some_off),
                            action = stringResource(com.ilyskyo.blancall.R.string.reminder_open_settings),
                            onAction = { openReminderNotificationSettings(context) }
                        )
                    }

                    Surface(
                        onClick = { openReminderNotificationSettings(context) },
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(com.ilyskyo.blancall.R.string.reminder_system_notification_settings),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text("▸", style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    Surface(
                        onClick = {
                            val posted = NotificationHelper.showTest(context, nextAtText)
                            val message = when {
                                posted -> com.ilyskyo.blancall.R.string.reminder_test_sent
                                !notificationStatus.notificationsEnabled ->
                                    com.ilyskyo.blancall.R.string.notification_permission_required
                                // 系统通知开着却发不出去，只剩「提醒渠道被关」这一种可能
                                else -> com.ilyskyo.blancall.R.string.reminder_channels_some_off
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        },
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(com.ilyskyo.blancall.R.string.reminder_test_notification),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text("▸", style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Text(
                        stringResource(com.ilyskyo.blancall.R.string.reminder_schedule_note),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // ── 提醒时间 ──
    AnimatedOverlayPresence(visible = showTimePicker) {
        TimePickerDialog(
            title = stringResource(com.ilyskyo.blancall.R.string.set_reminder_time),
            currentHour = hour,
            currentMinute = minute,
            onConfirm = { h, m ->
                ReminderPrefs.hour = h
                ReminderPrefs.minute = m
                showTimePicker = false
                reschedule()
            },
            onDismiss = { showTimePicker = false }
        )
    }

    // ── 免打扰起止：与提醒时间共用同一套分钟级选择器 ──
    AnimatedOverlayPresence(visible = showQuietStartPicker) {
        TimePickerDialog(
            title = stringResource(com.ilyskyo.blancall.R.string.reminder_quiet_hours),
            currentHour = quietStart / 60,
            currentMinute = quietStart % 60,
            onConfirm = { h, m ->
                ReminderPrefs.quietStartMinutes = h * 60 + m
                showQuietStartPicker = false
                reschedule()
            },
            onDismiss = { showQuietStartPicker = false }
        )
    }
    AnimatedOverlayPresence(visible = showQuietEndPicker) {
        TimePickerDialog(
            title = stringResource(com.ilyskyo.blancall.R.string.reminder_quiet_hours),
            currentHour = quietEnd / 60,
            currentMinute = quietEnd % 60,
            onConfirm = { h, m ->
                ReminderPrefs.quietEndMinutes = h * 60 + m
                showQuietEndPicker = false
                reschedule()
            },
            onDismiss = { showQuietEndPicker = false }
        )
    }

    // ── 自定义每日练习次数 ──
    AnimatedOverlayPresence(visible = showCustomGoalDialog) {
        BlancallAlertDialog(
            onDismissRequest = { showCustomGoalDialog = false },
            title = { Text(stringResource(com.ilyskyo.blancall.R.string.custom_goal_title)) },
            text = {
                OutlinedTextField(
                    value = customGoalInput,
                    onValueChange = { input ->
                        customGoalInput = input.filter { it.isDigit() }.take(3)
                    },
                    label = { Text(stringResource(com.ilyskyo.blancall.R.string.custom_goal_label)) },
                    suffix = { Text(stringResource(com.ilyskyo.blancall.R.string.goal_times_unit)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(
                    enabled = customGoalInput.toIntOrNull() != null,
                    onClick = {
                        customGoalInput.toIntOrNull()?.let {
                            ReminderPrefs.dailyPracticeGoal = it.coerceAtLeast(1)
                        }
                        showCustomGoalDialog = false
                    }
                ) { Text(stringResource(com.ilyskyo.blancall.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomGoalDialog = false }) {
                    Text(stringResource(com.ilyskyo.blancall.R.string.cancel))
                }
            }
        )
    }
}

// ========== 简易时间选择弹窗 ==========

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimePickerDialog(
    title: String,
    currentHour: Int,
    currentMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    // 以 currentHour/currentMinute 为 key，避免外部时间变化时输入框仍显示旧值
    var hourText by remember(currentHour) { mutableStateOf(currentHour.toString().padStart(2, '0')) }
    var minuteText by remember(currentMinute) { mutableStateOf(currentMinute.toString().padStart(2, '0')) }

    BlancallAlertDialog(
        onDismissRequest = onDismiss,
        shape = SmoothRoundedCornerShape(20.dp),
        title = { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 手动输入区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(com.ilyskyo.blancall.R.string.hours_unit), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = hourText,
                            onValueChange = { v ->
                                if (v.length <= 2 && v.all { it.isDigit() }) {
                                    hourText = v
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            placeholder = { Text("0-23", style = MaterialTheme.typography.bodySmall) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                            shape = SmoothRoundedCornerShape(10.dp)
                        )
                    }
                    Text(" : ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(com.ilyskyo.blancall.R.string.minutes_short), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = minuteText,
                            onValueChange = { v ->
                                if (v.length <= 2 && v.all { it.isDigit() }) {
                                    minuteText = v
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            placeholder = { Text("0-59", style = MaterialTheme.typography.bodySmall) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                            shape = SmoothRoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 快速预设（FlowRow 自动换行，避免挤压叠压）
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        stringResource(com.ilyskyo.blancall.R.string.preset_8am) to Pair(8, 0),
                        stringResource(com.ilyskyo.blancall.R.string.preset_1230pm) to Pair(12, 30),
                        stringResource(com.ilyskyo.blancall.R.string.preset_6pm) to Pair(18, 0),
                        stringResource(com.ilyskyo.blancall.R.string.preset_8pm) to Pair(20, 0),
                        stringResource(com.ilyskyo.blancall.R.string.preset_9pm) to Pair(21, 0)
                    ).forEach { (label, time) ->
                        val h = hourText.toIntOrNull() ?: -1
                        val m = minuteText.toIntOrNull() ?: -1
                        val isActive = h == time.first && m == time.second
                        AssistChip(
                            onClick = {
                                hourText = time.first.toString().padStart(2, '0')
                                minuteText = time.second.toString().padStart(2, '0')
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            shape = SmoothRoundedCornerShape(8.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isActive)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = (hourText.toIntOrNull() ?: currentHour).coerceIn(0, 23)
                    val m = (minuteText.toIntOrNull() ?: currentMinute).coerceIn(0, 59)
                    onConfirm(h, m)
                },
                shape = SmoothRoundedCornerShape(10.dp)
            ) { Text(stringResource(com.ilyskyo.blancall.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(com.ilyskyo.blancall.R.string.cancel)) }
        }
    )
}

/**
 * 自适应单行文字：放不下时自动缩小字号，保证永远排在同一行（不换行）。
 * 用于选项卡/FilterChip 的 label，避免不同设备/字号下文字被挤成两行。
 */
@Composable
private fun AdaptiveChipLabel(text: String) {
    val style = MaterialTheme.typography.labelSmall
    var fontSize by remember(text) { mutableStateOf(style.fontSize) }
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        style = style.copy(fontSize = fontSize),
        onTextLayout = { result ->
            // 溢出时逐步缩小字号（下限 8sp），直到能单行放下
            if (result.hasVisualOverflow && fontSize.value > 11f) {
                fontSize = (fontSize.value - 0.5f).sp
            }
        }
    )
}

// ========== 赞赏区：会话级隐藏状态 / 赞赏码保存 ==========

/**
 * 设置页的**会话级**界面状态（只存在于内存，不随任何偏好落盘）。
 *
 * 赞赏区「隐藏该区域」按用户要求只对本次运行生效：藏起来之后本次使用不再出现，
 * 软件重新启动就自动恢复。所以它不能写进 AppPrefs（那会变成永久隐藏），也不能用
 * `remember`（离开设置页再回来就会被重新组合出来，与「本次使用」的承诺不符）。
 * 复位时机见 `MainActivity.onCreate`：只有真正重新启动（savedInstanceState == null）
 * 才清；系统因配置变化（主题、深色模式等）重建 Activity 时不清，免得刚藏好又冒出来。
 */
internal object SettingsSessionState {
    /** 赞赏区是否在本次运行中被隐藏 */
    var donationHidden by mutableStateOf(false)
}

/** 微信包名 */
private const val WECHAT_PACKAGE = "com.tencent.mm"

/**
 * 跳转微信。相册里的赞赏码要靠微信「扫一扫 → 从相册选取」才用得上，这里只负责把微信拉起来
 * —— 不硬编码「扫一扫」页的组件名（各版本组件名并不稳定，匹配不上会直接抛 ActivityNotFound，
 * 比停在微信首页更糟）。未安装微信时给一句提示，而不是静默失败。
 */
private fun launchWeChat(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(WECHAT_PACKAGE)
    if (intent == null) {
        Toast.makeText(context, R.string.donation_wechat_missing, Toast.LENGTH_SHORT).show()
        return
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure {
        Toast.makeText(context, R.string.donation_wechat_missing, Toast.LENGTH_SHORT).show()
    }
}

/**
 * 把赞赏码写进系统相册的 `Pictures/Blancall/`，成功返回给用户看的目录名，失败返回 null。
 *
 * Android 10+ 走 MediaStore：`IS_PENDING` 两段写（写完才对外可见，半张图不会进相册），无需任何权限；
 * Android 9 及以下没有 RELATIVE_PATH，只能直接写公共目录，因此需要 WRITE_EXTERNAL_STORAGE
 * （清单里已按 maxSdkVersion=28 声明，界面在调用前申请）。
 */
private fun saveDonationQrToGallery(context: Context): String? {
    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.appreciation_qr) ?: return null
    val fileName = "blancall_donation_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
    val shownDir = "${Environment.DIRECTORY_PICTURES}/Blancall"
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, shownDir)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            val out = resolver.openOutputStream(uri) ?: return null
            out.use {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) return null
            }
            // 写入完成才解除 pending，此时这张图才会出现在相册里
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Blancall"
            )
            if (!dir.exists() && !dir.mkdirs()) return null
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) return null
            }
            // 10 以下写公共目录不会被媒体库自动收录，得主动通知扫描
            @Suppress("DEPRECATION")
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
        }
        shownDir
    } catch (e: Exception) {
        // 日志串保持 ASCII：中文串字面量在 cjk-baseline.tsv 里是只减不增的棘轮，这条不是界面文案
        Log.w(TAG_DONATION, "donation image save to gallery failed", e)
        null
    } finally {
        bitmap.recycle()
    }
}

private const val TAG_DONATION = "DonationQr"

/**
 * 界面字号：无极滑杆 + 5% 刻度吸附。
 *
 * 拖动期间**只动本地值**（滑杆位置与百分比），松手才把字号写进 [AppearanceStore]。
 * 字号挂在整棵主题上，拖动时每帧上报会让全树按帧重组 —— 手感就是滑块发涩、跟不上手指。
 */
@Composable
private fun FontScaleSlider(current: Float) {
    var value by remember { mutableFloatStateOf(current) }
    var dragging by remember { mutableStateOf(false) }
    val stopHaptic = rememberHaptic(HapticTier.Toggle)
    var lastStop by remember { mutableStateOf(false) }

    // 外部变化（恢复备份 / 重新进入设置页）同步到滑杆；拖动期间不抢
    LaunchedEffect(current) {
        if (!dragging && abs(current - value) > 0.0005f) value = current
    }

    Column {
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(com.ilyskyo.blancall.R.string.ui_font_scale),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(com.ilyskyo.blancall.R.string.ui_font_scale_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 16.dp))
            }
            Text("${(value * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = { raw ->
                dragging = true
                // 拖动中保留连续值（只在刻度附近吸附），否则 1% 一档会一跳十几像素
                val dragged = AppearanceStore.dragUiFontScale(raw)
                value = dragged
                val atStop = AppearanceStore.isFontScaleStop(dragged)
                if (atStop && !lastStop) stopHaptic()
                lastStop = atStop
            },
            onValueChangeFinished = {
                dragging = false
                lastStop = false
                // 松手再规整到 1%，存下的是干净值
                val settled = AppearanceStore.snapUiFontScale(value)
                value = settled
                AppearanceStore.preview { it.copy(uiFontScale = settled) }
                AppearanceStore.flush()
            },
            valueRange = AppearanceStore.MIN_SCALE..AppearanceStore.MAX_SCALE,
            // 不设 steps：连续滑动；刻度吸附由 snapUiFontScale 完成
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}


/** 设置页入口统一去重：快速双击不再把同一目的页压两层。 */
private fun androidx.navigation.NavController.navigateOnce(route: String) {
    navigate(route) { launchSingleTop = true }
}
