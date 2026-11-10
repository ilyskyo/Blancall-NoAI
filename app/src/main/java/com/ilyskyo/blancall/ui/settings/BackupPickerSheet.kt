// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ilyskyo.blancall.R
import com.ilyskyo.blancall.data.backup.BackupManager
import com.ilyskyo.blancall.ui.common.AppIcon
import com.ilyskyo.blancall.ui.common.AppIconKind
import com.ilyskyo.blancall.ui.common.GlassModalBottomSheet
import com.ilyskyo.blancall.ui.common.PressTier
import com.ilyskyo.blancall.ui.common.pressClick
import java.util.Locale

/**
 * 备份导出前的「选择要备份的数据」面板。
 *
 * 为什么需要它：早期一键备份是「整库全打包」，用户没法只带走文章、也没法跳过体积最大的
 * 手写墨迹与 AI 历史。更要紧的是导入侧——恢复时按 manifest 里登记的类目做**按类替换**，
 * 未包含的类目在磁盘上一个字节都不动；否则「只备份了文章」的包一恢复，
 * 记忆进度和划线会被当成残留删掉。
 *
 * 面板本身只负责选择，不读盘：[stats] 由调用方在 IO 线程算好传入（见 [BackupManager.stats]）。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun BackupPickerSheet(
    stats: List<BackupManager.GroupStat>,
    selected: Set<BackupManager.Group>,
    onToggle: (BackupManager.Group) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onExport: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    // 没有任何文件的类目直接不展示：列表里挂一行「无数据」只会让面板更长
    val rows = remember(stats) { stats.filter { it.files > 0 } }
    val chosenBytes = stats.filter { it.group in selected }.sumOf { it.bytes }
    val canExport = rows.any { it.group in selected }

    GlassModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 18.dp),
        ) {
            Text(
                stringResource(R.string.backup_pick_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.backup_pick_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onSelectAll, enabled = rows.isNotEmpty()) {
                    Text(stringResource(R.string.backup_pick_all), style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = onClearAll, enabled = rows.isNotEmpty()) {
                    Text(stringResource(R.string.backup_pick_none), style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(2.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                rows.forEach { stat ->
                    BackupGroupRow(
                        stat = stat,
                        checked = stat.group in selected,
                        onClick = { onToggle(stat.group) },
                    )
                }
            }

            if (rows.isEmpty()) {
                Text(
                    stringResource(R.string.backup_pick_nothing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 18.dp),
                )
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onExport,
                enabled = canExport,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.backup_pick_export) + " · " + formatBytes(chosenBytes),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun BackupGroupRow(
    stat: BackupManager.GroupStat,
    checked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .pressClick(tier = PressTier.Container, onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PickerCheck(checked)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                groupLabel(stat.group),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                pluralStringResource(
                    R.plurals.backup_group_meta,
                    stat.files,
                    formatBytes(stat.bytes),
                    stat.files,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 自绘勾选圈（项目规定图标一律自绘，不用现成排版符号） */
@Composable
private fun PickerCheck(checked: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (checked) accent else androidx.compose.ui.graphics.Color.Transparent)
            .border(1.5.dp, if (checked) accent else outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            AppIcon(
                kind = AppIconKind.Check,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

@Composable
private fun groupLabel(group: BackupManager.Group): String = stringResource(
    when (group) {
        BackupManager.Group.ARTICLES -> R.string.backup_group_articles
        BackupManager.Group.PROGRESS -> R.string.backup_group_progress
        BackupManager.Group.MARKS -> R.string.backup_group_marks
        BackupManager.Group.ORGANIZE -> R.string.backup_group_organize
        BackupManager.Group.INK -> R.string.backup_group_ink
        BackupManager.Group.PREFS -> R.string.backup_group_prefs
        BackupManager.Group.SESSION -> R.string.backup_group_session
        BackupManager.Group.AI -> R.string.backup_group_ai
    }
)

/** 人类可读体积；B/KB/MB/GB 三种单位足够覆盖学习数据量级。 */
internal fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 ->
        String.format(Locale.ROOT, "%.1f GB", bytes.toDouble() / (1024 * 1024 * 1024))
    bytes >= 1024L * 1024 ->
        String.format(Locale.ROOT, "%.1f MB", bytes.toDouble() / (1024 * 1024))
    bytes >= 1024L -> String.format(Locale.ROOT, "%.0f KB", bytes.toDouble() / 1024)
    else -> "$bytes B"
}
