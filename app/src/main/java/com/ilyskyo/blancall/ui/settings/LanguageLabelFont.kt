// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.settings

import androidx.annotation.FontRes
import com.ilyskyo.blancall.R
import com.ilyskyo.blancall.ui.common.AppLocale

/**
 * 语言名固定字形对应的内置字体。
 *
 * 字体来自 Noto Sans CJK（SIL OFL 1.1），取 NotoSansCJK-Regular.ttc 的对应字面，
 * 按语言名用到的字形子集化并改名为 Blancall Lang *：简体 / 繁体 / 日 / 韩四套字形
 * 加一份拉丁与西里尔，五个文件合计约 30 KB。
 *
 * 子集里刻意删掉了 GSUB/GPOS/GDEF：Noto CJK 用 GSUB 的 locl 做「同一字体内的
 * 语言字形替换」，只要系统的排版语言是日语，简体、繁体、日文字面都会被换成日文字形 ——
 * 这正是 per-text locale 被 OEM 忽略后，字体固定字形仍然跟着界面语言变的原因。
 * 去掉布局表后只剩各字面自己的基础字形，界面语言不再参与选择。
 */
@FontRes
internal fun languageLabelFontRes(choice: AppLocale): Int = when (choice) {
    AppLocale.FollowSystem -> 0
    AppLocale.ChineseSimplified -> R.font.lang_zh_hans
    AppLocale.ChineseTraditional -> R.font.lang_zh_hant
    AppLocale.English, AppLocale.Russian -> R.font.lang_latin
    AppLocale.Japanese -> R.font.lang_ja
    AppLocale.Korean -> R.font.lang_ko
}
