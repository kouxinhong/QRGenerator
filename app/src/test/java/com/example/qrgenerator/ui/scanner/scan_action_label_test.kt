package com.example.qrgenerator.ui.scanner

import com.example.qrgenerator.data.model.QrScene
import com.example.qrgenerator.domain.QrPayloadParser
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanActionLabelTest {
    @Test
    fun `类型匹配时明确填入当前制作场景`(): Unit {
        assertEquals("填入当前 Wi-Fi 卡", getScanActionLabel(QrPayloadParser.parse("WIFI:T:WPA;S:Cafe;P:password;;"), QrScene.WIFI))
        assertEquals("填入当前链接卡", getScanActionLabel(QrPayloadParser.parse("https://example.com/menu"), QrScene.MENU))
        assertEquals("填入当前联系卡", getScanActionLabel(QrPayloadParser.parse("MECARD:N:张三;TEL:13800138000;;"), QrScene.CONTACT))
    }

    @Test
    fun `类型不匹配时说明将制作的新用途而不是覆盖当前场景`(): Unit {
        assertEquals("制作 Wi-Fi 小卡", getScanActionLabel(QrPayloadParser.parse("WIFI:T:WPA;S:Cafe;P:password;;"), QrScene.MENU))
        assertEquals("制作链接小卡", getScanActionLabel(QrPayloadParser.parse("https://example.com"), null))
    }

    @Test
    fun `特殊信息明确原样制作而不承诺可编辑填表`(): Unit {
        val payload: String = "WIFI:T:WPA2-EAP;S:Cafe;P:password;I:user;E:PEAP;;"
        assertEquals("保留原内容制作", getScanActionLabel(QrPayloadParser.parse(payload), QrScene.WIFI))
    }
}
