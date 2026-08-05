package com.e33epus.sweatyfeet.ui;

/**
 * 配置界面主题。照搬 e33chat ChatBubbleTheme，只保留 DARK（sweatyfeet 无主题切换需求）。
 * 配色值全部沿用 e33chat DARK 主题，保证"100% 还原 e33chat 配置界面"。
 */
public enum SfTheme {
    DARK;

    public record Colors(
        int panelBg, int titleBg, int barBg,
        int sidebarBg, int sidebarItemHover, int sidebarItemSelected, int sidebarDivider,
        int divider, int inputBg,
        int textPrimary, int textSecondary, int textMuted,
        int nameColor, int timeColor,
        int popupBg, int popupHover, int popupText,
        int contextBg, int contextHover, int contextText,
        int iconHover,
        int notificationText, int whisperBar,
        int toastBg, int toastText,
        int scrollbar, int scrollbarHover,
        int closeBg, int closeHoverBg, int closeText,
        int systemText, int quoteBar, int duplicateLabel,
        int redDot, int redDotMention,
        int configTitle, int configSection, int configLabel, int configBg,
        int bannerBg, int bannerText, int bannerBar
    ) {}

    public Colors colors() {
        return switch (this) {
            case DARK -> new Colors(
                0xD01E1E1E, 0xFF242424, 0xFF242424,
                0xFF1A1A1A, 0xFF2A2A2A, 0xFF3A3A3A, 0xFF333333,
                0xFF333333, 0xFF2A2A2A,
                0xFFFFFFFF, 0xFFAAAAAA, 0xFF888888,
                0xFFCCCCCC, 0xFF999999,
                0xB31E1E1E, 0xB3444444, 0xFFFFFFFF,
                0xEE2A2A2A, 0xFF4A4A4A, 0xFFFFFFFF,
                0xFF444444,
                0xFFFFFF55, 0xAA7B2D8B,
                0xCC000000, 0xFFFFFFFF,
                0x00FFFFFF, 0x00FFFFFF,
                0xFF333333, 0xFF555555, 0xFFCCCCCC,
                0xFF888888, 0xFFFFFFFF, 0xFFFFAA00,
                0xFFFF0000, 0xFFFF4444,
                0xFFFFFFFF, 0xFFFFAA00, 0xFFFFFFFF, 0xC0101010,
                0xEE333333, 0xFFFFFF55, 0xFFFFFFFF
            );
        };
    }
}
