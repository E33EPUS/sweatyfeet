package com.e33epus.sweatyfeet;

import net.minecraft.client.texture.NativeImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 坐下脱裤改图的纯逻辑：真实字节序（ABGR）断言 + hex 解析 + 腿部区域判定 + 肤色候选过滤。
 * NativeImage 内部字节序 = ABGR（R 最低字节，反编译 Format.RGBA redOffset=0 实锤），
 * 期望值一律先按"想放的 RGB"经 abgr() 转成像素 int——能区分 ABGR 与旧 RRGGBBAA 实现，
 * 不再用 set/get 往返自洽（自洽测不出布局错误）。跑法：./gradlew test -PrunTests
 */
class SoakSkinClientTest {

    /** "想放的 RGB" 0xRRGGBB → NativeImage 像素 int（ABGR 布局：A 最高字节，然后 B,G,R） */
    private static int abgr(int rgb) {
        return (0xFF << 24) | ((rgb & 0xFF) << 16) | ((rgb >>> 8 & 0xFF) << 8) | (rgb >>> 16 & 0xFF);
    }

    /** 全填一个颜色的 64x64 图 */
    private static NativeImage filled(int pixel) {
        NativeImage img = new NativeImage(64, 64, false);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                img.setColor(x, y, pixel);
            }
        }
        return img;
    }

    @Test
    void buildUndressedWritesSkinColorInAbgrLayout() {
        // 深蓝裤 0x334455 → ABGR 像素 0xFF554433。期望值按 ABGR 算：
        // 旧 RRGGBBAA 实现（写 0xC68863FF）在这里必然挂
        NativeImage base = filled(abgr(0x334455));
        NativeImage out = SoakSkinClient.buildUndressed(base, 0xC68863);
        // 右腿主 (5,20) → 肤色 0xC68863 的 ABGR 布局像素
        assertEquals(abgr(0xC68863), out.getColor(5, 20));
        // 左腿主 (20,50) → 同样肤色
        assertEquals(abgr(0xC68863), out.getColor(20, 50));
        // 右裤 overlay (5,40) → alpha 清 0（第二层消失）
        assertEquals(0, out.getColor(5, 40));
        // 身体区 (20,40) 保持原色不被碰
        assertEquals(abgr(0x334455), out.getColor(20, 40));
        // 头部 (8,8) 不被碰
        assertEquals(abgr(0x334455), out.getColor(8, 8));
        out.close();
        base.close();
    }

    @Test
    void buildUndressedDoesNotTreatBlueAsTransparent() {
        // 纯蓝裤 ABGR 像素 0xFF0000FF（R=0）。旧透明判定 (orig&0xFF)==0 在 ABGR 下
        // 查的是 R 通道 → 蓝裤腿部全被当透明跳过不涂（P1 遗漏 bug，已修）。这里锁定：
        // R=0 的腿部像素也必须被涂成肤色
        NativeImage base = filled(0xFF0000FF);
        NativeImage out = SoakSkinClient.buildUndressed(base, 0xC68863);
        assertEquals(abgr(0xC68863), out.getColor(5, 20));
        out.close();
        base.close();
    }

    @Test
    void buildUndressedSkipsTransparentLegPixels() {
        // 真透明（alpha=0）腿部像素保持透明不动（老 64x32 皮肤下半区可能透明）
        NativeImage base = filled(0x00000000);
        NativeImage out = SoakSkinClient.buildUndressed(base, 0xC68863);
        assertEquals(0, out.getColor(5, 20));
        out.close();
        base.close();
    }

    @Test
    void blueTintWritesPureBluePixels() {
        // 用户提蓝色 0x4A90D9 → ABGR 0xFFD9904A。蓝变紫红 = 通道错位（旧实现会挂）
        NativeImage base = filled(0xFFFFFFFF);
        NativeImage out = SoakSkinClient.buildUndressed(base, 0x4A90D9);
        assertEquals(abgr(0x4A90D9), out.getColor(5, 20));
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
    void autoSamplePrefersSpecifiedPixels() {
        // 指定像素 (4,8)(5,8) 是肤色 → 直接采用（ABGR 像素 0xFF6388C6 = RGB 0xC68863）
        NativeImage base = filled(0x00000000);
        base.setColor(4, 8, abgr(0xC68863));
        assertEquals(0xC68863, SoakSkinClient.sampleSpecifiedPixels(base));
        // 指定像素全非肤色 → 回退分区采样（不崩，结果 0xRRGGBB 格式）
        NativeImage empty = new NativeImage(64, 64, false);
        int fallback = SoakSkinClient.sampleSpecifiedPixels(empty);
        assertEquals(SoakSkinClient.FALLBACK_SKIN_TINT, fallback);
        base.close();
        empty.close();
    }

    @Test
    void sampleSpecifiedPixelsSkipsNonSkinFirstPixel() {
        // (4,8) 是蓝（拒绝）→ 看 (5,8) 肤色 → 采用 (5,8)
        NativeImage base = filled(0x00000000);
        base.setColor(4, 8, abgr(0x4A90D9)); // 提蓝：非暖色拒绝
        base.setColor(5, 8, abgr(0xC68863));
        assertEquals(0xC68863, SoakSkinClient.sampleSpecifiedPixels(base));
        base.close();
    }

    @Test
    void skinTintCandidateFilters() {
        // 肤色暖色通过（ABGR 像素：R 最低字节，然后 G,B,A）
        assertTrue(SoakSkinClient.isSkinTintCandidate(abgr(0xD2A079)));
        // 高饱和纯红拒绝
        assertFalse(SoakSkinClient.isSkinTintCandidate(abgr(0xFF0000)));
        // 非暖色（蓝）拒绝
        assertFalse(SoakSkinClient.isSkinTintCandidate(abgr(0x4A90D9)));
        // 过暗（阴影/头发）拒绝
        assertFalse(SoakSkinClient.isSkinTintCandidate(abgr(0x101010)));
        // 透明拒绝
        assertFalse(SoakSkinClient.isSkinTintCandidate(0x006388C6)); // alpha=0 的肤色 ABGR
    }
}
