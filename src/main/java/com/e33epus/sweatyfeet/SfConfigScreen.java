package com.e33epus.sweatyfeet;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Sweaty Feet 自绘配置界面（Mods 列表 → Config 按钮）。
 * 照搬 e33chat ChatBubbleConfigScreen 的手写 Screen 思路，简化版：
 * 顶部分类 tab + 每项输入框/开关 + 底部 保存/恢复默认/返回。
 * 配置存 config/sweatyfeet-server.toml（ModConfigSpec），改完 set() 即时生效。
 *
 * @OnlyIn(CLIENT)：纯客户端类——服务端/单测的 RuntimeDistCleaner 会将其 stub 化，
 * 避免 "Attempted to load class .../Screen for invalid dist DEDICATED_SERVER"。
 */
@OnlyIn(Dist.CLIENT)
public class SfConfigScreen extends Screen {
    private static final int TAB_TOP = 30;
    private static final int LIST_TOP = 56;
    private static final int ROW_H = 22;
    private static final int BOTTOM_H = 36;

    private final Screen lastScreen;
    private int category;
    private int scrollOffset;
    private final List<AbstractWidget> categoryTabs = new ArrayList<>();
    private final List<Entry> entries = new ArrayList<>();
    private final List<AbstractWidget> activeWidgets = new ArrayList<>();

    private interface Entry {
        void build(int x, int y, int w);
        void save();
        void revert();
        void resetToDefault();
    }

    private static final class IntEntry implements Entry {
        private final ModConfigSpec.IntValue value;
        private final Component label;
        private EditBox box;

        IntEntry(ModConfigSpec.IntValue value, String langKey) {
            this.value = value;
            this.label = Component.translatable(langKey);
        }

        @Override
        public void build(int x, int y, int w) {
            box = new EditBox(net.minecraft.client.Minecraft.getInstance().font, x + w / 2, y, w / 2 - 4, 18, label);
            box.setValue(String.valueOf(value.get()));
            box.setFilter(s -> s.matches("\\d*"));
            box.setMaxLength(6);
        }

        @Override
        public void save() {
            try {
                int v = Integer.parseInt(box.getValue());
                value.set(v);
            } catch (NumberFormatException ignored) {
                value.set(value.getDefault());
            }
        }

        @Override
        public void revert() {
            box.setValue(String.valueOf(value.get()));
        }

        @Override
        public void resetToDefault() {
            box.setValue(String.valueOf(value.getDefault()));
        }

        Component label() {
            return label;
        }

        AbstractWidget widget() {
            return box;
        }
    }

    private static final class BoolEntry implements Entry {
        private final ModConfigSpec.BooleanValue value;
        private final Component label;
        private Button button;

        BoolEntry(ModConfigSpec.BooleanValue value, String langKey) {
            this.value = value;
            this.label = Component.translatable(langKey);
        }

        @Override
        public void build(int x, int y, int w) {
            button = Button.builder(Component.literal(value.get() ? "✔" : "✘"),
                b -> {
                    value.set(!value.get());
                    b.setMessage(Component.literal(value.get() ? "✔" : "✘"));
                }).bounds(x + w / 2, y, w / 2 - 4, 18).build();
        }

        @Override
        public void save() {
        }

        @Override
        public void revert() {
            button.setMessage(Component.literal(value.get() ? "✔" : "✘"));
        }

        @Override
        public void resetToDefault() {
            button.setMessage(Component.literal(value.getDefault() ? "✔" : "✘"));
            value.set(value.getDefault());
        }

        Component label() {
            return label;
        }

        AbstractWidget widget() {
            return button;
        }
    }

    public SfConfigScreen(Screen lastScreen) {
        super(Component.translatable("sweatyfeet.config.title"));
        this.lastScreen = lastScreen;
        this.category = 0;
    }

    private void rebuildEntries() {
        entries.clear();
        activeWidgets.clear();
        List<Object> cat = switch (category) {
            case 0 -> List.of(
                new IntEntry(SfConfig.LEVEL_1_SECONDS, "sweatyfeet.config.level1_seconds"),
                new IntEntry(SfConfig.LEVEL_2_SECONDS, "sweatyfeet.config.level2_seconds"),
                new IntEntry(SfConfig.LEVEL_3_SECONDS, "sweatyfeet.config.level3_seconds"));
            case 1 -> List.of(
                new BoolEntry(SfConfig.ENABLE_FUNGUS, "sweatyfeet.config.enable_fungus"),
                new IntEntry(SfConfig.FUNGUS_DELAY_SECONDS, "sweatyfeet.config.fungus_delay_seconds"),
                new BoolEntry(SfConfig.FUNGUS_DAMAGE_ENABLED, "sweatyfeet.config.fungus_damage_enabled"),
                new IntEntry(SfConfig.FUNGUS_DAMAGE_INTERVAL_SECONDS, "sweatyfeet.config.fungus_damage_interval_seconds"),
                new BoolEntry(SfConfig.FUNGUS_INFECTION_ENABLED, "sweatyfeet.config.fungus_infection_enabled"),
                new IntEntry(SfConfig.FUNGUS_INFECTION_RANGE, "sweatyfeet.config.fungus_infection_range"),
                new IntEntry(SfConfig.FUNGUS_INFECTION_INTERVAL_SECONDS, "sweatyfeet.config.fungus_infection_interval_seconds"),
                new BoolEntry(SfConfig.SMELL_ENABLED, "sweatyfeet.config.smell_enabled"),
                new IntEntry(SfConfig.SMELL_RANGE, "sweatyfeet.config.smell_range"));
            case 2 -> List.of(
                new IntEntry(SfConfig.EFFECT_SECONDS, "sweatyfeet.config.effect_seconds"),
                new IntEntry(SfConfig.THROW_DEBUFF_SECONDS, "sweatyfeet.config.throw_debuff_seconds"),
                new IntEntry(SfConfig.DRINK_POISON_SECONDS, "sweatyfeet.config.drink_poison_seconds"),
                new IntEntry(SfConfig.BOTTLE_NAUSEA_SECONDS, "sweatyfeet.config.bottle_nausea_seconds"),
                new IntEntry(SfConfig.DRINK_BUFF_SECONDS, "sweatyfeet.config.drink_buff_seconds"),
                new IntEntry(SfConfig.DEGRADE_SECONDS, "sweatyfeet.config.degrade_seconds"),
                new IntEntry(SfConfig.WASH_SECONDS, "sweatyfeet.config.wash_seconds"));
            case 3 -> List.of(
                new BoolEntry(SfConfig.SLIDE_ENABLED, "sweatyfeet.config.slide_enabled"),
                new IntEntry(SfConfig.SLIDE_RETENTION_PERCENT, "sweatyfeet.config.slide_retention_percent"));
            case 4 -> List.of(
                new BoolEntry(SfConfig.SWEAT_PARTICLES, "sweatyfeet.config.sweat_particles"),
                new IntEntry(SfConfig.SWEAT_PARTICLE_SCALE, "sweatyfeet.config.sweat_particle_scale"),
                new BoolEntry(SfConfig.SNEEZE_PARTICLES, "sweatyfeet.config.sneeze_particles"));
            default -> List.of(
                new BoolEntry(SfConfig.DEBUG_SHOW_TICKS, "sweatyfeet.config.debug_show_ticks"),
                new BoolEntry(SfConfig.DEBUG_FORCE_FUNGUS, "sweatyfeet.config.debug_force_fungus"));
        };
        for (Object o : cat) {
            if (o instanceof IntEntry ie) {
                entries.add(ie);
            } else if (o instanceof BoolEntry be) {
                entries.add(be);
            }
        }
    }

    private void layout() {
        rebuildEntries();
        int x = 20;
        int y = LIST_TOP - scrollOffset;
        int w = this.width - 40;
        for (Entry e : entries) {
            if (y + ROW_H >= LIST_TOP && y < this.height - BOTTOM_H) {
                e.build(x, y, w);
                if (e instanceof IntEntry ie) {
                    activeWidgets.add(ie.widget());
                } else if (e instanceof BoolEntry be) {
                    activeWidgets.add(be.widget());
                }
            }
            y += ROW_H;
        }
    }

    @Override
    protected void init() {
        scrollOffset = 0;
        layout();
        // 底部按钮
        int bw = (this.width - 80) / 3;
        addRenderableWidget(Button.builder(Component.translatable("sweatyfeet.config.save"),
            b -> {
                for (Entry e : entries) {
                    e.save();
                }
                SfConfig.SERVER_SPEC.save();
                this.minecraft.setScreen(lastScreen);
            }).bounds(20, this.height - BOTTOM_H + 8, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("sweatyfeet.config.reset"),
            b -> {
                for (Entry e : entries) {
                    e.resetToDefault();
                }
            }).bounds(40 + bw, this.height - BOTTOM_H + 8, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("sweatyfeet.config.back"),
            b -> this.minecraft.setScreen(lastScreen))
            .bounds(60 + 2 * bw, this.height - BOTTOM_H + 8, bw, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        // 标题
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        // 分类 tab
        String[] cats = {
            "sweatyfeet.config.cat.timing", "sweatyfeet.config.cat.fungus",
            "sweatyfeet.config.cat.durations", "sweatyfeet.config.cat.movement",
            "sweatyfeet.config.cat.visual", "sweatyfeet.config.cat.debug"};
        int tabW = (this.width - 40) / cats.length;
        for (int i = 0; i < cats.length; i++) {
            int x = 20 + i * tabW;
            int color = i == category ? 0x55FF55 : 0xAAAAAA;
            g.drawString(this.font, Component.translatable(cats[i]), x + 4, TAB_TOP + 4, color);
            if (i == category) {
                g.fill(x, TAB_TOP - 2, x + tabW, TAB_TOP + 12, 0x44FFFFFF);
            }
        }
        // 条目 label + 控件
        for (Entry e : entries) {
            if (e instanceof IntEntry ie) {
                g.drawString(this.font, ie.label(), 20, ie.widget().getY() + 4, 0xE0E0E0);
                ie.widget().render(g, mouseX, mouseY, partialTick);
            } else if (e instanceof BoolEntry be) {
                g.drawString(this.font, be.label(), 20, be.widget().getY() + 4, 0xE0E0E0);
                be.widget().render(g, mouseX, mouseY, partialTick);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // tab 切换
        String[] cats = {"timing", "fungus", "durations", "movement", "visual", "debug"};
        int tabW = (this.width - 40) / cats.length;
        for (int i = 0; i < cats.length; i++) {
            int x = 20 + i * tabW;
            if (mouseX >= x && mouseX < x + tabW && mouseY >= TAB_TOP - 2 && mouseY < TAB_TOP + 12) {
                if (i != category) {
                    category = i;
                    scrollOffset = 0;
                    layout();
                }
                return true;
            }
        }
        for (AbstractWidget w : activeWidgets) {
            if (w.isMouseOver(mouseX, mouseY)) {
                w.mouseClicked(mouseX, mouseY, button);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int maxScroll = Math.max(0, entries.size() * ROW_H - (this.height - LIST_TOP - BOTTOM_H));
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (deltaY * 18)));
        layout();
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(lastScreen);
    }
}
