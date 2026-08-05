package com.e33epus.sweatyfeet.ui;

/** 平滑动画（照搬 e33chat Animation）：easeOutCubic 进度插值，配置界面滚动用。 */
public final class SfAnim {

    private SfAnim() {}

    public static float easeOutCubic(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
    }

    /** 从 startMs 起的缓出进度 [0,1]；closing = true 用二次渐隐。 */
    public static float progress(long startMs, int durationMs, boolean closing) {
        long elapsed = net.minecraft.Util.getMillis() - startMs;
        float t = net.minecraft.util.Mth.clamp((float) elapsed / durationMs, 0f, 1f);
        if (closing) return 1.0f - (t * t);
        return easeOutCubic(t);
    }
}
