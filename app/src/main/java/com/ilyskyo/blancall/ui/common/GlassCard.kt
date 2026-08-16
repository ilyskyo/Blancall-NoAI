// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 毛玻璃质感卡片：半透明背景 + 顶部高光 + 底部内阴影 + 细描边 + 大圆角。
 *
 * 与 Card 的差异：
 * - 半透明 surface（浅色 0.66 / 深色 0.75），下方氛围背景可透出，形成玻璃感
 * - 顶部线性高光与底部弱内阴影增强立体层次
 * - 自带 1dp 细描边，调用方无需再传 border（避免双边框）
 * - onClick 非空时整卡可点击（无涟漪，内容区交互不受影响）；onClick + onLongClick 时支持长按
 *
 * 实现为纯 Modifier 组合（clip + background + border + 分层高光），
 * 不依赖 Outline 内部结构，跨 Compose 版本稳定。
 *
 * 用于首页与统计页的信息卡片容器；内容结构保持与 Card 一致（ColumnScope）。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val highlightColor = if (isDark) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.18f)
    }
    // 默认玻璃不透明度（保证文字易读，同时保留玻璃透光质感）；
    // 传入 containerColor（语义容器色）时同样高不透明度化
    val bgAlpha = if (isDark) 0.92f else 0.88f
    val bgColor = containerColor?.copy(alpha = if (isDark) 0.85f else 0.80f)
        ?: surfaceColor.copy(alpha = bgAlpha)

    // 点击修饰符：仅 onClick → clickable；onClick + onLongClick → combinedClickable（如文章卡片）
    val clickModifier = when {
        onClick != null && onLongClick != null -> Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick
        )
        onClick != null -> Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
        else -> Modifier
    }

    Box(
        modifier = modifier
            .then(clickModifier)
            // 大圆角裁切 → 半透明玻璃底 → 细描边（绘制于内容之下）
            .clip(shape)
            .background(bgColor)
            .border(1.dp, outlineColor.copy(alpha = 0.5f), shape)
    ) {
        // 顶部高光：玻璃上缘反光（内容之下）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .background(
                    Brush.verticalGradient(
                        0f to highlightColor,
                        1f to Color.Transparent
                    )
                )
        )
        // 底部内阴影：玻璃下缘立体感（内容之下）
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.18f)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(alpha = if (isDark) 0.10f else 0.05f)
                    )
                )
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}
