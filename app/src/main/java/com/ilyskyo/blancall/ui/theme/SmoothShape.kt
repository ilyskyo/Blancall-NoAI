// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * 平滑圆角（squircle）：角部走超椭圆 |x|^n + |y|^n = 1，而不是四分之一圆弧。
 *
 * 普通圆角在「直边 → 圆弧」的切点处曲率由 0 突变到 1/r，放大看会有一个小折点；
 * 超椭圆在切点处的曲率同样是 0，曲率连续变化，就是 iOS 图标 / 小米 Logo 那种
 * 「有生命感」的转角。n = 3 与小米 Logo 一致：n = 2 退化为普通圆角，n 越大越方。
 *
 * 只用于矩形面。胶囊（`RoundedCornerShape(50)`）与正圆（`CircleShape`）保持原样 ——
 * 这两种形状本身就由短边决定半径，套超椭圆反而会把两端压扁。
 */
@Immutable
class SmoothRoundedCornerShape(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
) : CornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        if (topStart == 0f && topEnd == 0f && bottomEnd == 0f && bottomStart == 0f) {
            return Outline.Rectangle(size.toRect())
        }
        val ltr = layoutDirection == LayoutDirection.Ltr
        val topLeft = if (ltr) topStart else topEnd
        val topRight = if (ltr) topEnd else topStart
        val bottomRight = if (ltr) bottomEnd else bottomStart
        val bottomLeft = if (ltr) bottomStart else bottomEnd

        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(topLeft, 0f)
            lineTo(w - topRight, 0f)
            // 右上：从顶边切点走到右边切点（参数方向与左角相反）
            appendSuperellipseCorner(this, w, 0f, topRight, -1f, 1f, reversed = true)
            lineTo(w, h - bottomRight)
            appendSuperellipseCorner(this, w, h, bottomRight, -1f, -1f, reversed = false)
            lineTo(bottomLeft, h)
            appendSuperellipseCorner(this, 0f, h, bottomLeft, 1f, -1f, reversed = true)
            lineTo(0f, topLeft)
            appendSuperellipseCorner(this, 0f, 0f, topLeft, 1f, 1f, reversed = false)
            close()
        }
        return Outline.Generic(path)
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): SmoothRoundedCornerShape = SmoothRoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SmoothRoundedCornerShape) return false
        return topStart == other.topStart &&
            topEnd == other.topEnd &&
            bottomEnd == other.bottomEnd &&
            bottomStart == other.bottomStart
    }

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        return result
    }

    override fun toString(): String =
        "SmoothRoundedCornerShape(topStart = $topStart, topEnd = $topEnd, " +
            "bottomEnd = $bottomEnd, bottomStart = $bottomStart)"
}

private const val CORNER_SEGMENTS = 16

// 超椭圆指数 n = 3（与小米 Logo 相同）
private const val EXPONENT = 3f

private val HALF_PI = (PI / 2).toFloat()

/**
 * 沿角部追加超椭圆采样点。
 *
 * 按极角均匀取点（而不是超椭圆的参数角）：参数角在切点附近变化极慢，
 * 会留下长折线；极角采样 16 段时 40dp 圆角的最大弦高误差约 0.05px。
 */
private fun appendSuperellipseCorner(
    path: Path,
    cornerX: Float,
    cornerY: Float,
    radius: Float,
    horizontalSign: Float,
    verticalSign: Float,
    reversed: Boolean,
) {
    if (radius <= 0f) return
    for (i in 1..CORNER_SEGMENTS) {
        val step = if (reversed) CORNER_SEGMENTS - i else i
        val angle = step * HALF_PI / CORNER_SEGMENTS
        val c = cos(angle)
        val s = sin(angle)
        // 极径 ρ(β) = r / (cos^n β + sin^n β)^(1/n)
        val radial = 1f / (c.pow(EXPONENT) + s.pow(EXPONENT)).pow(1f / EXPONENT)
        val u = 1f - radial * c
        val v = 1f - radial * s
        path.lineTo(cornerX + horizontalSign * u * radius, cornerY + verticalSign * v * radius)
    }
}

fun SmoothRoundedCornerShape(size: Dp): SmoothRoundedCornerShape = SmoothRoundedCornerShape(
    CornerSize(size), CornerSize(size), CornerSize(size), CornerSize(size)
)

/** 像素值版本：与 [androidx.compose.foundation.shape.RoundedCornerShape] 的 Float 重载同义。 */
fun SmoothRoundedCornerShape(size: Float): SmoothRoundedCornerShape = SmoothRoundedCornerShape(
    CornerSize(size), CornerSize(size), CornerSize(size), CornerSize(size)
)

fun SmoothRoundedCornerShape(
    topStart: Dp = 0.dp,
    topEnd: Dp = 0.dp,
    bottomEnd: Dp = 0.dp,
    bottomStart: Dp = 0.dp,
): SmoothRoundedCornerShape = SmoothRoundedCornerShape(
    CornerSize(topStart), CornerSize(topEnd), CornerSize(bottomEnd), CornerSize(bottomStart)
)
