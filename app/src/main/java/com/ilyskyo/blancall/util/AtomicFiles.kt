// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.util

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 原子化文本写盘：全部 Store 的统一落盘入口。
 *
 * ## 为什么不「先把主文件改名成 .bak」
 *
 * v7.6 及以前是：`主文件 → rename 成 .bak` → `tmp → rename 成主文件`。
 * 两步之间主文件在磁盘上**不存在**：断电、内核 panic、进程被 OOM 杀掉时，
 * 目录项可能只持久化了前半步，重启后就是「主文件没有、.bak 完好」。
 * 各 Store 里凡是在 `!file.exists()` 分支直接 return 的（RecordRepository 就是），
 * 会把这种状态读成「首次使用」，随后用空库覆盖 —— 用户的历史一次性归零。
 *
 * 现在改成 POSIX 的原子替换语义：
 * 1. 写唯一命名的 tmp 并 `fd.sync()`（数据先落盘）；
 * 2. 用**硬链接**给现有主文件做一份 `.bak`（主文件全程不动）；
 * 3. `ATOMIC_MOVE` 一次替换主文件 —— rename(2) 本身是原子的，没有中间态；
 * 4. 父目录 `fsync`（尽力而为），让目录项也落盘。
 *
 * 任何时刻磁盘上要么是「旧主文件 + 新 tmp」，要么是「新主文件 + 旧 .bak」，
 * 不存在「主文件缺失」的窗口。
 *
 * ## 并发
 *
 * 同一个文件的写按绝对路径串行（[fileLocks]）。tmp 名带 UUID，
 * 即使将来有人绕过锁直接调 writeStaged，两个写者也不会互相截断。
 *
 * 异常语义：**不吞异常** —— 写失败原样抛给调用方，由各 Store 维持既有处理。
 * 目录 fsync 为纯增强，失败时静默降级。
 */
object AtomicFiles {

    /** 按绝对路径的文件级互斥；条目数等于「被写过的文件数」，量级很小。 */
    private val fileLocks = ConcurrentHashMap<String, Any>()

    private fun lockOf(file: File): Any =
        fileLocks.computeIfAbsent(file.absolutePath) { Any() }

    /** 写 [text]（UTF-8）到 [file]，原子替换。 */
    fun writeTextAtomic(file: File, text: String) =
        writeBytesAtomic(file, text.toByteArray(Charsets.UTF_8))

    /** 写二进制内容到 [file]，原子替换（导入整库时也走这条）。 */
    fun writeBytesAtomic(file: File, bytes: ByteArray) {
        val dir = file.parentFile ?: throw IOException("no parent dir: ${file.absolutePath}")
        dir.mkdirs()
        synchronized(lockOf(file)) {
            // 兼容旧版固定命名 `<name>.tmp`：v7.6 及以前所有 tmp 都叫这个名字，
            // 升级后第一次写就要把它清掉，否则历史残留永远留着（既占空间，
            // 也让「读到半截 tmp」这类人工排查被误导）。
            runCatching { File(dir, file.name + ".tmp").delete() }
            // tmp 名带 UUID：即便有调用方绕过这把锁，也不会出现「两个写者截断同一个 tmp」
            val tmp = File(dir, file.name + "." + UUID.randomUUID().toString().take(8) + ".tmp")
            try {
                FileOutputStream(tmp).use { out ->
                    out.write(bytes)
                    runCatching { out.fd.sync() }
                }
                rotateBackup(file)
                moveAtomic(tmp, file)
                fsyncDirectoryBestEffort(dir)
            } finally {
                if (tmp.exists()) tmp.delete()
            }
        }
    }

    /**
     * 写入暂存文件并同步数据块（[promoteStaged] 的前半段）。
     * 供「要一次替换多个文件」的整库导入使用：先把全部内容写到暂存名上，
     * 磁盘满 / 写中断这些失败就都在碰正式文件之前暴露。
     */
    fun writeStaged(staged: File, bytes: ByteArray) {
        staged.parentFile?.mkdirs()
        FileOutputStream(staged).use { out ->
            out.write(bytes)
            runCatching { out.fd.sync() }
        }
    }

    /**
     * 把已完整落盘的 [staged] 提换为主文件 [file]（.bak 轮换 → 原子替换 → 目录 fsync）。
     * 与 [writeBytesAtomic] 同协议，区别只在内容早已写在盘上。
     */
    fun promoteStaged(staged: File, file: File) {
        val dir = file.parentFile ?: throw IOException("no parent dir: ${file.absolutePath}")
        dir.mkdirs()
        synchronized(lockOf(file)) {
            rotateBackup(file)
            moveAtomic(staged, file)
            fsyncDirectoryBestEffort(dir)
        }
    }

    /**
     * 批量追加多行文本（UTF-8，自动补换行）并只 fsync 一次 —— 供一次练习提交写多条
     * 复习事件使用。逐条调用 [appendTextLine] 会产生 N 次 fsync，在低速存储上明显拖慢
     * 提交；这里把 N 行写进同一个流，持久性语义不变。
     *
     * 每行「正文 + 换行」拼成一个 ByteArray 后一次 write：旧实现是两次 write()，
     * 两个线程并发 append 时可能在「正文之后、换行之前」交错，两条 JSON 粘成一行，
     * 读取端逐行容错会把两条一起跳过。
     *
     * 崩溃语义与 [appendTextLine] 相同：被杀只会留下最后半行，读取端逐行容错跳过。
     */
    fun appendTextLines(file: File, lines: List<String>) {
        if (lines.isEmpty()) return
        file.parentFile?.mkdirs()
        synchronized(lockOf(file)) {
            FileOutputStream(file, true).use { out ->
                for (line in lines) {
                    val body = line.toByteArray(Charsets.UTF_8)
                    val chunk = ByteArray(body.size + 1)
                    body.copyInto(chunk)
                    chunk[body.size] = '\n'.code.toByte()
                    out.write(chunk)
                }
                runCatching { out.fd.sync() }
            }
            fsyncDirectoryBestEffort(file.parentFile)
        }
    }

    /**
     * 追加一行文本（UTF-8，自动补换行）并 fsync —— 供「只增不改」的大文件
     * （练习记录 JSONL）做 O(1) 落盘，避免每次 insert 全量重写。
     *
     * 与 [writeTextAtomic] 的分工：
     * - 追加语义决定不能用 tmp+rename（会变成整体替换）；持久性靠 fd.sync；
     * - 崩溃语义：追加中被杀只会留下**半行**，读取端按「逐行容错解析」跳过即可；
     * - `.bak` 轮换只由全量重写路径（删除/转格式）负责，追加路径不动备份。
     */
    fun appendTextLine(file: File, line: String) {
        appendTextLines(file, listOf(line))
    }

    /**
     * 给现有 [file] 做一份 `.bak`。
     *
     * 用硬链接而不是复制：同一个 inode 多一个名字，代价接近零，也不占用额外空间；
     * 紧接着的 rename 只会把「主文件」这个名字指向新 inode，`.bak` 仍然稳定指向旧内容。
     * 不支持硬链接的文件系统（FAT、部分 ROM、Windows 单测）回退为复制。
     *
     * **注意：这里绝不移动或删除主文件** —— 那是旧实现的空窗来源。
     */
    private fun rotateBackup(file: File) {
        if (!file.isFile) return
        val bak = File(file.parentFile, file.name + ".bak")
        runCatching { if (bak.exists()) bak.delete() }
        try {
            Files.createLink(bak.toPath(), file.toPath())
        } catch (_: Exception) {
            // 跨设备 / 不支持硬链接：退回复制。复制失败只影响「能否回滚」，不阻断写入。
            runCatching { file.copyTo(bak, overwrite = true) }
        }
    }

    /** 原子替换；失败时退化为「先复制内容再删源」，至少不丢数据。 */
    private fun moveAtomic(src: File, dst: File) {
        try {
            Files.move(
                src.toPath(), dst.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
            return
        } catch (_: Exception) {
            // 落到下面的兼容路径
        }
        if (src.renameTo(dst)) return
        // 最后兜底：流式复制（不用 readBytes，避免大文件 OOM），成功后再删源。
        // 注意：rotateBackup 用硬链接把 .bak 指到 dst 的旧 inode，直接 FileOutputStream(dst)
        // 会连同 .bak 一起写成新内容，回滚能力丢失。先断开硬链接、把旧内容复制成独立 .bak。
        val bak = File(dst.parentFile, dst.name + ".bak")
        if (dst.isFile && bak.isFile) {
            runCatching {
                bak.delete()
                dst.copyTo(bak, overwrite = true)
            }
        }
        src.inputStream().use { input ->
            FileOutputStream(dst).use { output -> input.copyTo(output, DEFAULT_BUFFER_SIZE) }
        }
        src.delete()
    }

    /**
     * 目录 fsync：Linux / Android 允许以只读打开目录并 fsync（持久化 rename 的目录项）。
     * Windows（JVM 单测环境）不允许打开目录、个别文件系统也不支持 —— 捕获后静默降级，
     * 目录 fsync 只是额外的持久化保证，不影响主写入路径。
     */
    private fun fsyncDirectoryBestEffort(dir: File?) {
        if (dir == null) return
        try {
            FileChannel.open(dir.toPath(), StandardOpenOption.READ).use { it.force(true) }
        } catch (_: Exception) {
            // 见上：单测（Windows）与不支持的平台走这里，属预期降级
        }
    }

    /**
     * 删除文件及其伴生文件（`.bak` / `.tmp` / `.corrupt-*`）。
     *
     * 供「删除文章/删除墨迹/清理超限」这类真正要让数据消失的路径使用：
     * 只删主文件会留下 `.bak`，下次主文件损坏时被各 Store 回读，删掉的数据「复活」。
     */
    fun deleteWithCompanions(file: File) {
        val dir = file.parentFile ?: return
        val name = file.name
        runCatching { file.delete() }
        dir.listFiles { f ->
            f.isFile && (
                f.name == "$name.bak" ||
                    f.name.startsWith("$name.corrupt-") ||
                    (f.name.startsWith("$name.") && f.name.endsWith(".tmp"))
                )
        }?.forEach { runCatching { it.delete() } }
    }
}
