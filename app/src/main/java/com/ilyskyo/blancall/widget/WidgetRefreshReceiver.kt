// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 日期/时区/时间变化时刷新桌面小组件。
 *
 * `updatePeriodMillis=0` 关掉了系统轮询；若只靠练习提交刷新，
 * 到期数会长期停在旧值，跨午夜「明天到期」也不会变成「今天到期」。
 * 系统时间变化是低频受保护广播，收到后主动 updateAll 一次即可。
 */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in SUPPORTED_ACTIONS) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                BlancallWidget().updateAll(context.applicationContext)
            } catch (_: Exception) {
                // 小组件刷新失败不能影响系统广播；下次日期/练习提交还会再试
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
