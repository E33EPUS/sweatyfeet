package com.e33epus.sweatyfeet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 坐下脱裤改图的纯逻辑：亮度着色 + hex 解析 + 腿部区域判定 + 肤色候选过滤（防回归） */
class SoakSkinClientTest {
    @Test
    void buildUndressedWritesPureColor() {
        // 主层像素 → 纯色块（无亮度明暗，用户要求）；字节序 RGBA：skin<<8|FF
        com.mojang.blaze3d.platform.NativeImage base = new com.mojang.blaze3d.platform.NativeImage(64, 64, false);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                base.setPixelRGBA(x, y, 0x334455FF); // 深蓝裤子
            }
        }
        com.mojang.blaze3d.platform.NativeImage out = SoakSkinClient.buildUndressed(base, 0xC68863);
        // 右腿主 (5,20) → 纯肤色，无蓝色残留
        assertEquals(0xC68863FF, out.getPixelRGBA(5, 20));
        // 右裤 overlay (5,40) → alpha 清 0（第二层消失）
        assertEquals(0, out.getPixelRGBA(5, 40));
        // 身体区 (20,40) 保持原色不被碰
        assertEquals(0x334455FF, out.getPixelRGBA(20, 40));
        // 头部 (8,8) 不被碰
        assertEquals(0x334455FF, out.getPixelRGBA(8, 8));
        out.close();
        base.close();
    }

    @Test
    void parseTintHandlesFormats() {
        assertEquals(0xC68863, SoakSkinClient.parseTint("#C68863"));
        assertEquals(0xC68863, SoakSkinClient.parseTint("c68863"));
        assertEquals(0, SoakSkinClient.parseTint(""));
        assertEquals(0, SoakSkinClient.parseTint("zzz"));
        assertEquals(0, SoakSkinClient.parseTint(null));
    }

    @Test
    void legRegionsCoverLowerBodyAllLayers() {
        // 右腿主区 [0,16)x[16,32)
        assertTrue(SoakSkinClient.isLegPixel(0, 16, 64));
        assertTrue(SoakSkinClient.isLegPixel(15, 31, 64));
        // 右裤 overlay [0,16)x[32,48)
        assertTrue(SoakSkinClient.isLegPixel(8, 40, 64));
        // 左腿主区 [16,32)x[48,64)
        assertTrue(SoakSkinClient.isLegPixel(20, 50, 64));
        // 左裤 overlay [0,16)x[48,64)
        assertTrue(SoakSkinClient.isLegPixel(5, 50, 64));
        // 头/身/臂不碰
        assertFalse(SoakSkinClient.isLegPixel(8, 8, 64));
        assertFalse(SoakSkinClient.isLegPixel(20, 30, 64));
        assertFalse(SoakSkinClient.isLegPixel(40, 20, 64));
        // 身体上衣 overlay 区 (16,32)-(32,48) 绝不涂（之前误当左腿 → 躯干被染色）
        assertFalse(SoakSkinClient.isLegPixel(20, 40, 64));
        // 老式 64x32 皮肤：左腿区不存在跳过，右腿区还在
        assertFalse(SoakSkinClient.isLegPixel(20, 50, 32));
        assertTrue(SoakSkinClient.isLegPixel(0, 16, 32));
    }

    @Test
    void blueTintWritesPureBluePixels() {
        // 用户提蓝色 0x4A90D9，腿部必须是纯蓝（字节序回归：蓝变紫红=通道错位）
        com.mojang.blaze3d.platform.NativeImage base = new com.mojang.blaze3d.platform.NativeImage(64, 64, false);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                base.setPixelRGBA(x, y, 0xFFFFFFFF); // 全白皮
            }
        }
        com.mojang.blaze3d.platform.NativeImage out = SoakSkinClient.buildUndressed(base, 0x4A90D9);
        int p = out.getPixelRGBA(5, 20); // 右腿主区
        assertEquals(0x4A90D9FF, p); // 纯蓝，R=0x4A G=0x90 B=0xD9 A=0xFF
        out.close();
        base.close();
    }

    @Test
    void pickColorComposesRgbFromRgbaBytes() {
        // pickRawPixel 的通道提取逻辑：0xRRGGBBAA → 0xRRGGBB
        int rgba = 0x4A90D9FF;
        int rgb = ((rgba >>> 24) & 0xFF) << 16 | ((rgba >>> 16) & 0xFF) << 8 | ((rgba >>> 8) & 0xFF);
        assertEquals(0x4A90D9, rgb);
        // 反向：0xRRGGBB → 0xRRGGBBFF（buildUndressed 的写法）
        assertEquals(0x4A90D9FF, (0x4A90D9 << 8) | 0xFF);
    }

    @Test
    void autoSamplePrefersSpecifiedPixels() {
        // 指定像素 (4,8)(5,8) 是肤色 → 直接采用
        com.mojang.blaze3d.platform.NativeImage base = new com.mojang.blaze3d.platform.NativeImage(64, 64, false);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                base.setPixelRGBA(x, y, 0x00000000); // 全透明
            }
        }
        base.setPixelRGBA(4, 8, 0xC68863FF); // 第 8 行第 4 列 = 肤色
        // (4,8) 通过候选过滤 → 采样结果 = 肤色（不写配置，纯逻辑验证）
        assertEquals(0xC68863, SoakSkinClient.sampleSpecifiedPixels(base));
        // 指定像素全非肤色 → 回退分区采样（不崩）
        com.mojang.blaze3d.platform.NativeImage empty = new com.mojang.blaze3d.platform.NativeImage(64, 64, false);
        int fallback = SoakSkinClient.sampleSpecifiedPixels(empty);
        assertTrue(fallback == SoakSkinClient.FALLBACK_SKIN_TINT);
        base.close();
        empty.close();
    }

    @Test
    void skinTintCandidateFilters() {
        // 肤色暖色通过（RGBA 字节序：R 在最高字节）
        assertTrue(SoakSkinClient.isSkinTintCandidate(0xD2A079FF));
        // 高饱和纯红（饱和 1.0）拒绝
        assertFalse(SoakSkinClient.isSkinTintCandidate(0xFF0000FF));
        // 非暖色（蓝）拒绝
        assertFalse(SoakSkinClient.isSkinTintCandidate(0x4A90D9FF));
        // 过暗（阴影/头发）拒绝
        assertFalse(SoakSkinClient.isSkinTintCandidate(0x101010FF));
        // 透明拒绝
        assertFalse(SoakSkinClient.isSkinTintCandidate(0xD2A07900));
    }
}
