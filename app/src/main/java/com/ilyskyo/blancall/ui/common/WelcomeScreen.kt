// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.common

import android.widget.Toast
import com.ilyskyo.blancall.ui.common.pressClick
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
import androidx.compose.ui.res.stringResource
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
                stringResource(com.ilyskyo.blancall.R.string.welcome_tagline),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(68.dp))

            // ── 完全离线介绍 ──
            Text(
                stringResource(com.ilyskyo.blancall.R.string.welcome_privacy_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(com.ilyskyo.blancall.R.string.welcome_developer),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(com.ilyskyo.blancall.R.string.welcome_support_prefix),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            // 「点击此处支持开发者」蓝色可点
            val supportLink = stringResource(com.ilyskyo.blancall.R.string.welcome_support_link)
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
                        append(supportLink)
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .pressClick { showDonateDialog = true }
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
                Text(stringResource(com.ilyskyo.blancall.R.string.continue_action), style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(14.dp))

            // ── 隐私政策链接 + 说明（无需确认操作） ──
            Text(
                stringResource(com.ilyskyo.blancall.R.string.welcome_privacy_link),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .pressClick {
                        showLegalDialog = true
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(com.ilyskyo.blancall.R.string.welcome_privacy_note),
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
        BlancallAlertDialog(
            onDismissRequest = { showDonateDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(stringResource(com.ilyskyo.blancall.R.string.donation_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(com.ilyskyo.blancall.R.string.welcome_donation_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Image(
                        painter = painterResource(id = R.drawable.appreciation_qr),
                        contentDescription = stringResource(com.ilyskyo.blancall.R.string.donation_qr),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .aspectRatio(1f),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDonateDialog = false }) {
                    Text(stringResource(com.ilyskyo.blancall.R.string.close), color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // ── 隐私政策弹窗 ──
    if (showLegalDialog) {
        BlancallAlertDialog(
            onDismissRequest = { showLegalDialog = false },
            shape = RoundedCornerShape(28.dp),
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
                    Text(stringResource(com.ilyskyo.blancall.R.string.close), color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}
