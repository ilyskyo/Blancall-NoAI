// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ilyskyo.blancall.ui.theme.SmoothRoundedCornerShape

/**
 * 符号栏 / 符号选择面板里的**一枚符号键**。
 *
 * 存在的理由只有一个：手写面板底部那行键和设置页那面网格，此前是两套各自长出来的实现
 * （`Surface` + `Text` 的内边距撑宽、`AssistChip` 的 32dp 高 + 内容宽度），于是同一排键
 * 有的宽有的窄、有的方有的扁 —— 用户的话是「每个符号所占的按钮大小不一致」。
 * 尺寸与形状收在这一个地方，两处调用点只决定「多大一颗」。
 *
 * 三条定死的做法：
 *
 * 1. **固定方寸**：宽高都吃同一个 [size]，字居中。宽度不再由字形决定 ——
 *    `…` 与 `≤` 占同样的格子，扫一行是均匀的节拍。
 * 2. **超椭圆**：[SmoothRoundedCornerShape]（n=3），不是普通圆角矩形。方形面上
 *    圆角-直线交接处的曲率变化更缓，密排时不会读成一排「圆角贴片」。
 * 3. **边框用两层实心面挤出来，不用 `Modifier.border`**：
 *    `border` 的 stroke 以轮廓线居中 —— 一半落在轮廓外被父级裁掉，直线处切得整齐，
 *    超椭圆的曲线处却会切出半像素的灰度边，于是「圆角处比直线处更粗」（用户原话）。
 *    这里改成「外层铺主色、内层缩 1dp 再铺底色」，那条 1dp 色带由两块同形状的面
 *    相减得到，直线段与圆角段的厚度都是 1dp，抗锯齿也只发生在同一条边界上。
 *
 * [selectable] 为 true 时按复选框报告语义（设置页的「哪些符号上栏」是一组开关），
 * 否则按普通按钮（手写面板点一下即插入）。
 */
@Composable
fun SymbolKey(
    char: Char,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectable: Boolean = false,
    enabled: Boolean = true,
    size: Dp = 40.dp,
    corner: Dp = 12.dp,
    style: TextStyle = MaterialTheme.typography.labelLarge,
    contentColor: Color = LocalContentColor.current,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    accentContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    val shape = remember(corner) { SmoothRoundedCornerShape(corner) }
    // 内层缩 1dp 时圆角也跟着减 1dp：超椭圆的等距内缩曲线，半径必须同减，
    // 否则那条边会在圆角处被"拉宽"——正是用户嫌它"圆角处比直线处粗"的同一类毛病。
    val innerShape = remember(corner) { SmoothRoundedCornerShape((corner - 1.dp).coerceAtLeast(0.dp)) }
    val src = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    // semantics 里也有个 `selected`（属性名），起个别名免得读起来分不清是谁
    val isOn = selected
    val container = when {
        !enabled -> colors.surfaceVariant.copy(alpha = 0.4f)
        isOn -> colors.primaryContainer
        else -> colors.surfaceVariant
    }
    val label = when {
        !enabled -> colors.onSurfaceVariant.copy(alpha = 0.38f)
        isOn -> accentContentColor
        else -> contentColor
    }

    Box(
        modifier = modifier
            .size(size)
            // 按压反馈接在 clickable 之前，缩放与压暗才包得住整块键面
            .pressFeedback(src, PressTier.Container, enabled = enabled)
            .clickable(
                interactionSource = src,
                indication = null,
                enabled = enabled,
                role = if (selectable) Role.Checkbox else Role.Button,
            ) { onClick() }
            .semantics(mergeDescendants = true) {
                contentDescription = char.toString()
                if (selectable) {
                    this.selected = isOn
                    role = Role.Checkbox
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // 选中的那圈边是「外主色面 − 内底色面」挤出来的：
        // 两块同形状的面相减，直线段与圆角段的厚度都是 1dp。
        if (isOn) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(accentColor)
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(1.dp)
                    .clip(innerShape)
                    .background(container)
            )
        } else {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(container)
            )
        }
        Text(text = char.toString(), style = style, color = label)
    }
}
