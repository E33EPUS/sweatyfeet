package com.e33epus.sweatyfeet.ui;

/** 滚动条几何（照搬 e33chat ChatScrollbar 的静态方法，配置界面滚动条用）。 */
public final class SfScrollbar {

    public static final int WIDTH = 6;
    private static final int MIN_THUMB_H = 8;

    private SfScrollbar() {}

    public static int thumbHeight(int trackH, int totalH) {
        if (totalH <= 0) return trackH;
        int h = Math.max(MIN_THUMB_H, (int) ((long) trackH * trackH / totalH));
        return Math.min(h, trackH);
    }

    public static int thumbY(int trackTop, int trackH, int thumbH, int scrollOffset, int maxScroll) {
        int travelRange = trackH - thumbH;
        if (travelRange <= 0) return trackTop;
        return trackTop + (int) ((long) scrollOffset * travelRange / maxScroll);
    }

    public static boolean isHoveringThumb(double mouseX, double mouseY,
                                          int trackX, int thumbY, int thumbH) {
        return mouseX >= trackX && mouseX < trackX + WIDTH
            && mouseY >= thumbY && mouseY < thumbY + thumbH;
    }
}
