// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.Charset

class FileTextExtractorCharsetTest {

    @Test
    fun `Big5 繁体文本不会被 GB18030 抢先解码`() {
        val text = "繁體中文測試，這是一段用來驗證編碼偵測的文字。今天天氣很好，我們一起讀書。"
        val bytes = text.toByteArray(Charset.forName("Big5"))
        assertEquals(Charset.forName("Big5"), FileTextExtractor.detectCharset(bytes))
    }

    @Test
    fun `GBK 简体文本仍识别为中文双字节`() {
        val text = "简体中文测试，这是一段用来验证编码探测的文字。今天天气很好，我们一起读书。"
        val bytes = text.toByteArray(Charset.forName("GBK"))
        val detected = FileTextExtractor.detectCharset(bytes)
        assertEquals("简体中文测试", String(bytes, detected).take(6))
    }
}
