// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.data.backup

import com.ilyskyo.blancall.util.AtomicFiles
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * 学习数据一键备份：把 filesDir 里的全部学习数据打包成单个 ZIP，
 * 可在新安装的 Blancall 上一键导入还原。
 *
 * 背景：`backup_rules.xml` / `data_extraction_rules.xml` 刻意把 file 域排除在
 * 系统自动备份之外（防止旧进度覆盖新数据），因此 App 内必须自带备份通路。
 * 只覆盖学习数据（文章、练习记录、FSRS 状态、挖空/遮挡、标签、首页布局、
 * 阅读偏好、每日一句、墨迹），不含偏好设置与 AI 配置。
 *
 * 磁盘即真相：导出直接读磁盘文件而非内存快照，绕开各 Store 的异步加载时序。
 * 写回统一走 [AtomicFiles]，但各 Store 的内存镜像只在进程启动时加载一次，
 * 因此导入完成后必须重启进程才能生效（调用方负责引导退出）。
 */
object BackupManager {

    /**
     * 备份/恢复失败的类型：只带错误码与参数，展示文案由 UI 层取资源（键 `backup_err_*`）。
     *
     * 早先这里直接抛带中文消息的 IllegalArgumentException —— 界面切到英/日/韩/俄时，
     * 「导入失败：不是有效的 Blancall 备份文件」整句仍是简体中文（多语言守卫对数据层的
     * 字符串字面量全瞎），而测试只能对着中文措辞断言，改个用词就得改测试。
     */
    sealed class BackupError(message: String, cause: Throwable? = null) : Exception(message, cause) {
        /** 不是有效的 Blancall 备份（缺 manifest） */
        class NotABackup : BackupError("not a Blancall backup")
        /** 备份的清单（manifest.json）存在但解析不了 */
        class CorruptManifest(cause: Throwable?) : BackupError("corrupt manifest", cause)
        /** 备份格式版本不受支持（[version] 为文件声明的版本） */
        class UnsupportedVersion(val version: Int) : BackupError("unsupported backup version: $version")
        /** manifest 登记了但归档中缺失的条目（[entryName] 为相对路径） */
        class Incomplete(val entryName: String) : BackupError("missing entry: $entryName")
        /** 条目或整包超出大小上限（[entryName] 为空表示整包超限） */
        class TooLarge(val entryName: String?) : BackupError("too large: ${entryName ?: "total"}")
        /** 条目名非法（绝对路径 / `..` 穿越 / 非法字符） */
        class UnsafeEntry(val entryName: String) : BackupError("unsafe entry: $entryName")
        /** 备份里没有可恢复的数据条目 */
        class EmptyData : BackupError("no data to restore")
        /** 没有可备份的学习数据（导出时） */
        class NothingToExport : BackupError("no data to export")
        /** 打开目标输出流失败（导出时无法写入所选文件） */
        class CannotWrite : BackupError("cannot open output")
        /** 打开来源输入流失败（导入时无法读取所选文件） */
        class CannotRead : BackupError("cannot open input")
    }

    /** 备份格式版本，未来不兼容改动时递增并在导入侧分支处理。 */
    const val FORMAT_VERSION = 1

    private const val MANIFEST_ENTRY = "manifest.json"
    private const val FILES_PREFIX = "files/"

    /**
     * 固定清单：filesDir 根目录下的学习数据主文件（各 Store 硬编码同名）。
     * internal 是给 [com.ilyskyo.blancall.data.backup.LearningDataFileCensusTest] 做 census 比对用的 ——
     * 这份清单就是「一键备份覆盖哪些数据」对用户的承诺，漏登记不会编译失败、只会静默丢数据。
     */
    internal val ROOT_DATA_FILES = listOf(
        "articles.json",
        "records.json",
        "fsrs_state.json",
        "custom_cloze.json",
        "mask_config.json",
        "home_layout.json",
        "sentence_card.json",
        "reader_prefs.json",
        // 划线 / 批注（ReadingMarkStore）：用户逐篇写下的东西，丢了没法重建
        "reading_marks.json",
        "tags.json",
        // 成就解锁时刻（AchievementTimelineStore）：徽章本身是派生的，但「何时拿到」只有这一份，
        // 备份里没有它，恢复之后统计页就答不出「这周新得了几枚」
        "achievements.json",
        // 考试倒排目标（ReminderPrefs 镜像文件）：只存 pref 会在一键备份里静默丢失
        "exam_goal.json",
        // 主题 / 强调色 / 米黄底 / 玻璃染色 / 界面字体字号（AppearanceStore 镜像文件）：
        // 用户调了半天的外观，换机恢复不该回到出厂默认
        "appearance.json",
    )

    /** 条目相对路径的最大层数（根文件 1 层、`ink/x` 2 层、`content_packs/<id>/<file>` 3 层）。 */
    private const val MAX_ENTRY_DEPTH = 3

    /** 单条目解压上限，防异常压缩包拖垮内存（正常学习数据远小于此值）。 */
    private const val MAX_ENTRY_BYTES = 32L * 1024 * 1024

    /**
     * 整包解压总量上限：单条目上限挡不住「many 个小条目」把整包字节全驻内存，
     * restoreZip 是先在内存里凑齐 data 才开始落盘的。
     *
     * 128 MB：常见手机的 Java 堆在 256–512 MB，旧值 512 MB 等于「合法备份也能 OOM」。
     * 真实学习库（几千篇 / 几万条记录）压缩后远小于这个数。
     */
    internal const val MAX_TOTAL_BYTES = 128L * 1024 * 1024

    /** 两段式写回的暂存后缀（不在 collectBackupFiles 的任何收集模式内，天然不进备份）。 */
    private const val IMPORT_SUFFIX = ".import"

    /**
     * 导入结果：restored=写回的数据文件数，removed=被清掉的备份外残留数据文件数，
     * groups=本次备份包含的类目数（0 表示旧格式备份，视为全选）。
     */
    data class RestoreReport(val restored: Int, val removed: Int, val groups: Int = 0)

    /**
     * 备份类目：界面上给用户勾选的粒度。
     *
     * 拆成"类目"而不是"文件"的原因：
     * - 用户不理解 `fsrs_state.json`，但理解"记忆进度"；
     * - 有些数据必须成对带走才有意义（`records.json` 与 `fsrs_state.json`：
     *   只有记录没有记忆状态，恢复后每篇都变成"没练过"）；
     * - 导入侧要按类目做"只替换备份里有的"，不能整库覆盖。
     *
     * [key] 写进 manifest，导入时按它决定动哪些文件。**改名等于格式不兼容**，
     * 因此 key 一律用 ASCII 短词，且不随界面文案变化。
     */
    enum class Group(
        val key: String,
        /** filesDir 根目录下的固定文件 */
        val fileNames: List<String> = emptyList(),
        /** filesDir 下的子目录（条目名形如 `ink/xxx.json`） */
        val dirNames: List<String> = emptyList(),
        /** filesDir 根目录下的文件名前缀（条目名就是文件名本身） */
        val filePrefixes: List<String> = emptyList(),
        /** 仅 Pro 版存在的数据；标准版收集时自然为空 */
        val proOnly: Boolean = false,
    ) {
        /** 文章正文。内容包（用户自己导入的 zip 解出来的 txt）跟文章一起走。 */
        ARTICLES(
            "articles",
            fileNames = listOf("articles.json"),
            dirNames = listOf("content_packs"),
        ),

        /**
         * 练习记录 + 记忆进度。
         *
         * 必须同一个类目：只有 records 没有 fsrs_state，恢复后每篇都显示"没练过"，
         * 复习计划全乱；只有 fsrs_state 没有 records，统计页又是空的。
         */
        PROGRESS(
            "progress",
            fileNames = listOf("records.json", "fsrs_state.json"),
        ),

        /** 划线与批注：用户逐篇手写的内容，丢了重建不了 */
        MARKS("marks", fileNames = listOf("reading_marks.json")),

        /** 标签 / 自定义挖空 / 遮挡配置：组织方式与自建题面 */
        ORGANIZE(
            "organize",
            fileNames = listOf("tags.json", "custom_cloze.json", "mask_config.json"),
        ),

        /** 手写墨迹（错题本里的笔迹图） */
        INK("ink", dirNames = listOf("ink")),

        /** 偏好与外观：阅读排版、首页布局、每日一句、主题、考试目标、成就时刻 */
        PREFS(
            "prefs",
            fileNames = listOf(
                "reader_prefs.json",
                "home_layout.json",
                "sentence_card.json",
                "appearance.json",
                "exam_goal.json",
                "achievements.json",
            ),
        ),

        /** 未完成的练习现场（练习到一半退出时的存档） */
        SESSION("session", filePrefixes = listOf("practice_state_")),

        /** AI 对话历史（Pro 专属；含全文对话，体积大且属隐私，默认不勾） */
        AI("ai", dirNames = listOf("ai_history"), proOnly = true),
        ;

        companion object {
            /** 默认全选，但 AI 历史默认不带（体积与隐私） */
            fun defaultSelection(): Set<Group> = entries.filter { it != AI }.toSet()

            fun fromKeys(keys: Collection<String>?): Set<Group> {
                if (keys == null) return entries.toSet()
                val set = keys.toSet()
                return entries.filter { it.key in set }.toSet()
            }
        }
    }

    // ────────────────────────────────────────────────
    //  收集
    // ────────────────────────────────────────────────

    /**
     * 某一类目当前在磁盘上实际占用的字节数与文件数（UI 用来显示"约 1.2 MB · 38 项"）。
     * 只看存在的东西，不估。
     */
    data class GroupStat(val group: Group, val bytes: Long, val files: Int)

    fun statOf(filesDir: File, group: Group): GroupStat {
        var bytes = 0L
        var count = 0
        for ((_, f) in collectGroupFiles(filesDir, group)) {
            bytes += f.length()
            count++
        }
        return GroupStat(group, bytes, count)
    }

    /** 全部类目的统计（含空类目，UI 据此置灰）。 */
    fun stats(filesDir: File): List<GroupStat> = Group.entries.map { statOf(filesDir, it) }

    private fun collectGroupFiles(filesDir: File, group: Group): List<Pair<String, File>> {
        val out = mutableListOf<Pair<String, File>>()
        for (name in group.fileNames) {
            val f = File(filesDir, name)
            if (f.isFile) out.add(FILES_PREFIX + name to f)
        }
        for (dir in group.dirNames) {
            val d = File(filesDir, dir)
            if (!d.isDirectory) continue
            // 递归收集（content_packs 是 <id>/<file>，ink 与 ai_history 是一层）。
            // 目录分支必须显式过滤伴生文件：主文件每写一次就有一份 .bak，
            // AI 历史/墨迹会把旧对话、旧答案一起打包（体积翻倍且属隐私残留）。
            d.walkTopDown()
                .filter { it.isFile && !isBackupCompanion(it.name) }
                .sortedBy { it.relativeTo(filesDir).path }
                .forEach { out.add(FILES_PREFIX + it.relativeTo(filesDir).path.replace('\\', '/') to it) }
        }
        for (prefix in group.filePrefixes) {
            filesDir.listFiles { f -> f.isFile && !isBackupCompanion(f.name) && f.name.startsWith(prefix) }
                ?.sortedBy { it.name }
                ?.forEach { out.add(FILES_PREFIX + it.name to it) }
        }
        return out
    }

    /** AtomicFiles 的伴生文件：备份/暂存/损坏现场，永远不属于用户数据本身。 */
    private fun isBackupCompanion(name: String): Boolean =
        name.endsWith(".bak") || name.endsWith(".tmp") || name.contains(".corrupt-")

    /**
     * 收集应备份的文件，返回「ZIP 条目名 → 磁盘文件」列表（缺失的跳过）。
     *
     * [groups] 为空表示"全选"（旧调用点与旧测试的兼容路径）。
     * `.tmp` / `.bak` / `.corrupt-*` 不在任何类目的收集模式内，天然被排除。
     */
    fun collectBackupFiles(
        filesDir: File,
        groups: Set<Group> = Group.entries.toSet(),
    ): List<Pair<String, File>> {
        val effective = groups.ifEmpty { Group.entries.toSet() }
        val out = mutableListOf<Pair<String, File>>()
        // 按 enum 声明顺序输出，保证同一份数据每次打包的条目顺序稳定（便于 diff 与测试）
        for (g in Group.entries) {
            if (g !in effective) continue
            out += collectGroupFiles(filesDir, g)
        }
        return out
    }

    /**
     * 把 [filesDir] 里选中的类目写入 ZIP（manifest + files/ 条目）。
     * 没有任何可备份文件时抛 [BackupError.NothingToExport]，避免产出空备份。
     * IO 异常原样上抛，由调用方提示。
     *
     * 逐条目**流式**写入，不再 `file.readBytes()`：大库（几万条记录 / 整本内容包）
     * 一次性读进内存会在低端机上 OOM，而导出的本意就是给大库做快照。
     */
    fun exportZip(
        filesDir: File,
        output: OutputStream,
        groups: Set<Group> = Group.entries.toSet(),
    ) {
        val effective = groups.ifEmpty { Group.entries.toSet() }
        val entries = collectBackupFiles(filesDir, effective)
        if (entries.isEmpty()) throw BackupError.NothingToExport()
        ZipOutputStream(java.io.BufferedOutputStream(output, 32 * 1024)).use { zip ->
            writeEntry(zip, MANIFEST_ENTRY, buildManifest(entries.map { it.first }, effective))
            for ((entryName, file) in entries) {
                writeFileEntry(zip, entryName, file)
            }
        }
    }

    /**
     * 从 ZIP 恢复学习数据到 [filesDir]，整库替换语义：
     * 备份内文件逐一原子写回；备份外的既有学习数据文件删除，
     * 使恢复后的数据域与备份严格一致，避免残留孤儿进度。
     *
     * 校验（manifest 存在、版本匹配、条目名无路径穿越、至少一个数据条目、
     * manifest 登记的每个条目都能读出来）全部通过后才会落盘；
     * 校验失败抛 [IllegalArgumentException] 且不碰任何文件 —— 包括不清孤儿。
     * 写回分两段（先全部暂存再统一提换），中途一次写失败不会留下新旧混杂的数据。
     * 写回后内存单例仍是旧数据，调用方须引导用户重启应用。
     */
    fun restoreZip(zipFile: File, filesDir: File): RestoreReport {
        var data: List<Pair<String, ByteArray>>
        var groups: Set<Group>
        ZipFile(zipFile).use { zip ->
            val parsed = readDataEntries(zip)
            data = parsed.first
            groups = parsed.second
            data.forEach { (rel, _) ->
                if (!isSafeRelativeName(rel)) throw BackupError.UnsafeEntry(rel)
            }
            if (data.isEmpty()) throw BackupError.EmptyData()
        }
        // 两段式：先把每个条目完整写到 .import 上（磁盘满、写中断都在这里暴露，此时一个
        // 正式数据文件都还没被动），再统一提换。旧实现边读 ZIP 边写主文件，中途失败即
        // 「前半新、后半旧」混杂，而没写成那些条目的旧数据已经被 .bak 轮换走了。
        val staged = data.map { (rel, bytes) ->
            val dst = File(filesDir, rel)
            File(dst.parentFile, dst.name + IMPORT_SUFFIX).also { AtomicFiles.writeStaged(it, bytes) } to dst
        }
        // 提换阶段也必须有回滚：第 N 个失败时前 N-1 个已经是新内容，若不管，
        // 用户会得到一个「文章是新的、FSRS 是旧的」的错配库，比整体失败更难恢复。
        val promoted = mutableListOf<File>()
        try {
            staged.forEach { (stage, dst) ->
                AtomicFiles.promoteStaged(stage, dst)
                promoted += dst
            }
        } catch (e: Exception) {
            for (dst in promoted.asReversed()) {
                val bak = File(dst.parentFile, dst.name + ".bak")
                if (bak.isFile) {
                    // promoteStaged 已把「导入前的内容」链接到 .bak，复制回去即可还原
                    runCatching {
                        bak.copyTo(dst, overwrite = true)
                        bak.delete()
                    }
                } else {
                    // 导入前这个文件不存在（新增），回滚就是删掉它
                    runCatching { dst.delete() }
                }
            }
            throw e
        } finally {
            // 提换成功时 staged 已被 rename 走；这里只清失败中断留下的孤儿暂存文件
            staged.forEach { (stage, _) -> if (stage.exists()) stage.delete() }
        }

        // keep 必须与 collectGroupFiles 返回的键同形（都带 `files/` 前缀）。
        // 这里写成裸相对名过一次错：刚恢复的文件会被自己的孤儿清理当成残留删掉，
        // 测试表现为「restore 报告成功，但目标文件不存在」。
        val keep = data.mapTo(mutableSetOf()) { FILES_PREFIX + it.first }
        var removed = 0
        // 只清理「本次备份覆盖的类目」内的残留：未包含在备份里的类目，磁盘上一个字节都不动。
        // 旧实现按 ROOT_DATA_FILES 全量清理，于是「只备份了文章」的包一恢复，
        // 记忆进度、划线、标签全被当成残留删掉。
        for (g in Group.entries.filter { it in groups }) {
            for ((rel, f) in collectGroupFiles(filesDir, g)) {
                if (rel in keep) {
                    // 本次恢复的文件：删掉导入前那份旧库的 .bak，
                    // 否则主文件将来缺失时会被各 Store 回读，"复活"成导入前的版本
                    val bak = File(f.parentFile, f.name + ".bak")
                    if (bak.isFile) bak.delete()
                } else if (f.delete()) {
                    removed++
                    // 孤儿连同伴生文件一起清，避免 .bak 下次被回读
                    AtomicFiles.deleteWithCompanions(f)
                }
            }
        }
        return RestoreReport(data.size, removed, groups.size)
    }

    /** 解析 manifest 并读出其登记的全部数据条目（相对路径 → 字节）与本次包含的类目。 */
    private fun readDataEntries(zip: ZipFile): Pair<List<Pair<String, ByteArray>>, Set<Group>> {
        val manifestEntry = zip.getEntry(MANIFEST_ENTRY)
            ?: throw BackupError.NotABackup()
        val json = try {
            JSONObject(zip.getInputStream(manifestEntry).reader().readText())
        } catch (e: Exception) {
            throw BackupError.CorruptManifest(e)
        }
        val version = json.optInt("formatVersion")
        if (version != FORMAT_VERSION) throw BackupError.UnsupportedVersion(version)
        // 旧备份（v1 早期）没有 groups 字段 → 视为全选，行为与升级前一致
        val groupKeys = json.optJSONArray("groups")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optString(it)?.takeIf(String::isNotBlank) }
        }
        val groups = Group.fromKeys(groupKeys)
        val names = json.optJSONArray("files") ?: JSONArray()
        val result = mutableListOf<Pair<String, ByteArray>>()
        var totalBytes = 0L
        for (i in 0 until names.length()) {
            val entry = names.optString(i)
            if (!entry.startsWith(FILES_PREFIX)) continue
            // manifest 登记了、归档里却没有 → 必须整包拒绝。旧实现写的是 `?: continue`，
            // 被跳过的条目既进不了 data、也就不在 restoreZip 的 keep 集合里，紧接着那段
            // 孤儿清理会把磁盘上对应的真实数据当残留删掉 —— 截断过或手工改过的备份
            // 「恢复」一次，那一类学习数据就归零。
            val zipEntry = zip.getEntry(entry)
                ?: throw BackupError.Incomplete(entry.removePrefix(FILES_PREFIX))
            val bytes = readEntryBytes(zip, zipEntry)
            totalBytes += bytes.size
            if (totalBytes > MAX_TOTAL_BYTES) throw BackupError.TooLarge(null)
            result.add(entry.removePrefix(FILES_PREFIX) to bytes)
        }
        return result to groups
    }

    /** 读取条目内容，超过上限视为异常包拒绝。 */
    private fun readEntryBytes(zip: ZipFile, entry: ZipEntry): ByteArray {
        zip.getInputStream(entry).use { input: InputStream ->
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_ENTRY_BYTES) throw BackupError.TooLarge(entry.name)
                out.write(buffer, 0, read)
            }
            return out.toByteArray()
        }
    }

    /**
     * 条目相对名的安全校验：最多三级（根文件 / `ink/x.json` / `content_packs/<id>/<file>`），
     * 各段只含常见安全字符，且不含 `..`、不以 `/` 开头。
     *
     * 放宽到三级是为了让内容包与 AI 历史能进备份；路径穿越仍被两处挡住：
     * 段里出现 `..` 直接拒，盘符（`C:`）与反斜杠不匹配 [SAFE_SEGMENT]。
     */
    private fun isSafeRelativeName(rel: String): Boolean {
        if (rel.isBlank() || rel.startsWith("/")) return false
        val parts = rel.split("/")
        if (parts.size > MAX_ENTRY_DEPTH) return false
        return parts.all { it.isNotEmpty() && !it.contains("..") && it.matches(SAFE_SEGMENT) }
    }

    private val SAFE_SEGMENT = Regex("[A-Za-z0-9_.\\-]+")

    private fun buildManifest(entryNames: List<String>, groups: Set<Group>): ByteArray {
        val files = JSONArray()
        entryNames.forEach { files.put(it) }
        // 记录本次备份包含哪些类目：导入侧据此决定"只替换这几类"，
        // 未包含的类目在磁盘上一个字节都不动。旧备份没有这个字段 → 视为全选。
        val gs = JSONArray()
        Group.entries.filter { it in groups }.forEach { gs.put(it.key) }
        return JSONObject()
            .put("formatVersion", FORMAT_VERSION)
            .put("createdAt", System.currentTimeMillis())
            .put("groups", gs)
            .put("files", files)
            .toString()
            .toByteArray(StandardCharsets.UTF_8)
    }

    /** 流式写一个磁盘文件到归档条目（不整块读进内存）。 */
    private fun writeFileEntry(zip: ZipOutputStream, name: String, file: File) {
        zip.putNextEntry(ZipEntry(name))
        file.inputStream().use { input -> input.copyTo(zip, 32 * 1024) }
        zip.closeEntry()
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
    }
}
