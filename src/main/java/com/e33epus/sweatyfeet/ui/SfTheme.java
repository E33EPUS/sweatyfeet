package com.e33epus.sweatyfeet.ui;

/**
 * 配置界面主题。照搬 e33chat ChatBubbleTheme 的 DARK 配色，只保留 DARK
 * （sweatyfeet 无主题切换需求）。只留配置界面实际用到的字段，其余
 * e33chat 气泡/横幅色在移植时未用到，已删。
 */
public enum SfTheme {
    DARK;

    public record Colors(
        int configTitle, int configLabel
    ) {}

    public Colors colors() {
        return switch (this) {
            case DARK -> new Colors(
                0xFFFFFFFF, 0xFFFFFFFF
            );
        };
    }
}
