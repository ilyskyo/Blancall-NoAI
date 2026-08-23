// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.common

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyskyo.blancall.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 开屏页：欢迎介绍 + 支持开发者 + 进入应用。
 *
 * - 完全离线说明：本地运行、不联网、不收集个人信息
 * - 「点击此处支持开发者」：弹出赞赏码
 * - 「继续」：进入欢迎帮助页（首次引导第二步）
 * - 底部《隐私政策》链接：可随时查看全文
 */
@Composable
fun WelcomeScreen(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    var showDonateDialog by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf(false) }
    var privacyText by remember { mutableStateOf("") }
    // 从 assets 读取隐私政策文档（后台线程，避免主线程 IO）
    LaunchedEffect(Unit) {
        privacyText = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("PRIVACY.md").bufferedReader().use { it.readText() }
            }.getOrDefault("")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            // ── 品牌标题 ──
            Text(
                "Blancall",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "不是清空，是召回。",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(68.dp))

            // ── 完全离线介绍 ──
            Text(
                "Blancall 完全运行在本地，不联网。不收集、传输或共享你的任何个人信息。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "软件由 ilyskyo 开发，向你免费提供。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "如果帮助到了你，欢迎",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            // 「点击此处支持开发者」蓝色可点
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
                        append("点击此处支持开发者")
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { showDonateDialog = true }
                    .padding(vertical = 2.dp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            // ── 继续（始终可点击，进入欢迎帮助页） ──
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("继续", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(14.dp))

            // ── 隐私政策链接 + 说明（无需确认操作） ──
            Text(
                "《隐私政策》",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable {
                        showLegalDialog = true
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "供你查阅。根据《中华人民共和国个人信息保护法》，隐私政策适用于“处理自然人个人信息”的活动。本应用不收集、不传输、不共享你的任何个人信息，因此无需你对《隐私政策》进行任何确认操作。",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    // ── 赞赏码弹窗 ──
    if (showDonateDialog) {
        AlertDialog(
            onDismissRequest = { showDonateDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text("赞赏区·本软件完全免费",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("求求了，如果可以的话，请扫码支持一下我 🥹",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Image(
                        painter = painterResource(id = R.drawable.appreciation_qr),
                        contentDescription = "赞赏二维码",
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .aspectRatio(1f),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDonateDialog = false }) {
                    Text("关闭", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // ── 隐私政策弹窗 ──
    if (showLegalDialog) {
        AlertDialog(
            onDismissRequest = { showLegalDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Column(Modifier.padding(top = 4.dp)) {
                    Text("隐私政策",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(2.dp))
                    Text("Blancall · 数据安全与隐私保护",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
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
                    Text("关闭", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}
