package com.e33epus.sweatyfeet;

import com.e33epus.sweatyfeet.ui.SfAnim;
import com.e33epus.sweatyfeet.ui.SfColoredTextureRenderer;
import com.e33epus.sweatyfeet.ui.SfScrollbar;
import com.e33epus.sweatyfeet.ui.SfTheme;
import com.e33epus.sweatyfeet.ui.SfUiElement;
import com.e33epus.sweatyfeet.ui.SfUiTextureManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Sweaty Feet 配置界面——100% 复刻 e33chat ChatBubbleConfigScreen 的交互与视觉：
 * 左侧可折叠标签树（tab + 子分类）、右侧选项（label + .desc tooltip + 控件）、
 * 全屏 DARK 主题背景、常驻滚动条 + easeOutCubic 平滑滚动、编辑模型（打开快照→退出回滚/保存保留）、
 * Esc 弹 ConfirmScreen 询问丢弃、底部 Done/Exit/Save 按改动数切换 + "changed N" 提示。
 *
 * 与 e33chat 的差异：只 DARK 主题（无主题切换）、无气泡预览带、无 hex 色板；
 * 配置用 NeoForge ModConfigSpec；分类 = sweatyfeet 6 个逻辑类别 + 语义子分类。
 *
 * 输入框可输入的关键：EditBox 全部走 addRenderableWidget 注册，依赖 Screen 默认事件链
 * （点击 focus + 键盘转发），绝不重写 mouseClicked 手动转发——那是旧版的 bug。
 */
@OnlyIn(Dist.CLIENT)
public class SfConfigScreen extends Screen {
    private static final int ROW_H = 32;
    private static final int HEADER_H = 32;
    private static final int START_Y = 40;
    private static final int CAT_X = 24;
    private static final int CAT_W = 96;
    private static final int CAT_ROW_H = 22;
    private static final int SUB_ROW_H = 18;
    private static final int INPUT_W = 90;

    private final Screen lastScreen;

    private SfTheme.Colors c() {
        return SfTheme.DARK.colors();
    }

    private int dividerX, optLabelX, inputX;
    private int selectedCat;
    private int selectedSub = -1;
    private int scrollOffset;
    private int treeScroll;
    private final List<AbstractWidget> scrollWidgets = new ArrayList<>();
    // 左侧树折叠状态（随 buildCats 的 tab 数动态分配，写死长度必越界——7 tab 崩过）
    private boolean[] expanded = {};
    // 右侧选项 / 左侧树各一套平滑滚动(easeOutCubic 时间轴)+滚动条拖拽
    private float rAnimFrom, rAnimTo;
    private long rAnimStart;
    private int rAnimDur;
    private boolean rAnimOn;
    private boolean rBarDrag;
    private int rBarDragY, rBarDragOff;
    private float tAnimFrom, tAnimTo;
    private long tAnimStart;
    private int tAnimDur;
    private boolean tAnimOn;
    private boolean tBarDrag;
    private int tBarDragY, tBarDragOff;

    private interface WidgetFactory {
        AbstractWidget create(int y);
    }

    private record Opt(String key, WidgetFactory factory, Supplier<String> tooltip) {
        static Opt header(String key) {
            return new Opt(key, null, null);
        }
        boolean isHeader() {
            return factory == null;
        }
    }

    private record Cat(String key, List<Opt> opts) {
    }

    private List<Cat> cats;

    // 编辑模型：打开时快照所有配置项；退出回滚到快照，保存保留（set 后 save 落盘）
    private interface Tracked {
        boolean changed();
        void revert();
    }

    private <T> Tracked track(ModConfigSpec.ConfigValue<T> v) {
        T snapshot = v.get();
        return new Tracked() {
            @Override
            public boolean changed() {
                return !Objects.equals(v.get(), snapshot);
            }

            @Override
            public void revert() {
                v.set(snapshot);
            }
        };
    }

    private final List<Tracked> tracked = new ArrayList<>();
    private Button doneBtn, exitBtn, saveBtn;

    private void buildCats() {
        if (cats != null) return;
        cats = new ArrayList<>();

        // 汗脚：穿戴计时/降级 + 2级打滑（合并原 sweat + slideSmell 的打滑部分）
        List<Opt> sweat = new ArrayList<>();
        sweat.add(Opt.header("sweatyfeet.config.section.wear"));
        sweat.add(new Opt("sweatyfeet.config.level1_seconds",
            y -> mkIntBox(y, String.valueOf(SfConfig.LEVEL_1_SECONDS.get()), 1, 86400, 6, SfConfig.LEVEL_1_SECONDS::set), null));
        sweat.add(new Opt("sweatyfeet.config.level2_seconds",
            y -> mkIntBox(y, String.valueOf(SfConfig.LEVEL_2_SECONDS.get()), 1, 86400, 6, SfConfig.LEVEL_2_SECONDS::set), null));
        sweat.add(new Opt("sweatyfeet.config.level3_seconds",
            y -> mkIntBox(y, String.valueOf(SfConfig.LEVEL_3_SECONDS.get()), 1, 86400, 6, SfConfig.LEVEL_3_SECONDS::set), null));
        sweat.add(Opt.header("sweatyfeet.config.section.degrade"));
        sweat.add(new Opt("sweatyfeet.config.degrade_seconds",
            y -> mkIntBox(y, String.valueOf(SfConfig.DEGRADE_SECONDS.get()), 1, 86400, 6, SfConfig.DEGRADE_SECONDS::set), null));
        sweat.add(new Opt("sweatyfeet.config.effect_seconds",
            y -> mkIntBox(y, String.valueOf(SfConfig.EFFECT_SECONDS.get()), 1, 86400, 6, SfConfig.EFFECT_SECONDS::set), null));
        sweat.add(Opt.header("sweatyfeet.config.section.slide"));
        sweat.add(new Opt("sweatyfeet.config.slide_enabled", y -> mkBoolButton(y, SfConfig.SLIDE_ENABLED), null));
        sweat.add(new Opt("sweatyfeet.config.slide_retention_percent",
            y -> mkIntBox(y, String.valueOf(SfConfig.SLIDE_RETENTION_PERCENT.get()), 1, 100, 3, SfConfig.SLIDE_RETENTION_PERCENT::set), null));
        cats.add(new Cat("sweatyfeet.config.cat.sweat", sweat));

        // 真菌：触发/扣血/传染 + 臭味（合并原 fungus + slideSmell 的臭味部分）
        List<Opt> fungus = new ArrayList<>();
        fungus.add(Opt.header("sweatyfeet.config.section.fungus_trigger"));
        fungus.add(new Opt("sweatyfeet.config.enable_fungus", y -> mkBoolButton(y, SfConfig.ENABLE_FUNGUS), null));
        fungus.add(new Opt("sweatyfeet.config.fungus_delay_seconds",
            y -> mkIntBox(y, String.valueOf(SfConfig.FUNGUS_DELAY_SECONDS.get()), 1, 86400, 6, SfConfig.FUNGUS_DELAY_SECONDS::set), null));
        fungus.add(Opt.header("sweatyfeet.config.section.fungus_damage"));
        fungus.add(new Opt("sweatyfeet.config.fungus_damage_enabled", y -> mkBoolButton(y, SfConfig.FUNGUS_DAMAGE_ENABLED), null));
        fungus.add(new Opt("sweatyfeet.config.fungus_damage_interval_seconds",
            y -> mkIntBox(y, String.valueOf(SfConfig.FUNGUS_DAMAGE_INTERVAL_SECONDS.get()), 1, 86400, 6, SfConfig.FUNGUS_DAMAGE_INTERVAL_SECONDS::set), null));
        fungus.add(Opt.header("sweatyfeet.config.section.fungus_infection"));
        fungus.add(new Opt("sweatyfeet.config.fungus_infection_enabled", y -> mkBoolButton(y, SfConfig.FUNGUS_INFECTION_ENABLED), null));
        fungus.add(new Opt("sweatyfeet.config.fungus_infection_range",
            y -> mkIntBox(y, String.valueOf(SfConfig.FUNGUS_INFECTION_RANGE.get()), 1, 64, 2, SfConfig.FUNGUS_INFECTION_RANGE::set), null));
        fungus.add(new Opt("sweatyfeet.config.fungus_infection_interval_seconds",
            y -> mkIntBox(y, String.valueOf(SfConfig.FUNGUS_INFECTION_INTERVAL_SECONDS.get()), 1, 86400, 6, SfConfig.FUNGUS_INFECTION_INTERVAL_SECONDS::set), null));
        fungus.add(Opt.header("sweatyfeet.config.section.smell"));
        fungus.add(new Opt("sweatyfeet.config.smell_enabled", y -> mkBoolButton(y, SfConfig.SMELL_ENABLED), null));
        fungus.add(new Opt("sweatyfeet.config.smell_range",
            y -> mkIntBox(y, String.valueOf(SfConfig.SMELL_RANGE.get()), 1, 64, 2, SfConfig.SMELL_RANGE::set), null));
        cats.add(new Cat("sweatyfeet.config.cat.fungus", fungus));

        // 物品与饮品：洗脚/洗靴 + 汗液瓶/饮品（合并原 wash + bottle）
        List<Opt> item = new ArrayList<>();
        item.add(Opt.header("sweatyfeet.config.section.wash"));
        item.add(new Opt("sweatyfeet.config.wash_seconds",
            y -> mkIntBox(y, String.valueOf(SfConfig.WASH_SECONDS.get()), 1, 86400, 6, SfConfig.WASH_SECONDS::set), null));
        item.add(Opt.header("sweatyfeet.config.section.boot_wash"));
        item.add(new Opt("sweatyfeet.config.wash_boots_enabled", y -> mkBoolButton(y, SfConfig.WASH_BOOTS_ENABLED), null));
        item.add(new Opt("sweatyfeet.config.wash_boots_seconds",
            y -> mkIntBox(y, String.valueOf(SfConfig.WASH_BOOTS_SECONDS.get()), 1, 86400, 6, SfConfig.WASH_BOOTS_SECONDS::set), null));
        item.add(Opt.header("sweatyfeet.config.section.bottle"));
        item.add(new Opt("sweatyfeet.config.bottle_nausea_seconds",
            y -> mkIntBox(y, String.valueOf(SfConfig.BOTTLE_NAUSEA_SECONDS.get()), 1, 86400, 6, SfConfig.BOTTLE_NAUSEA_SECONDS::set), null));
        item.add(new Opt("sweatyfeet.config.drink_poison_seconds",
            y -> mkIntBox(y, String.valueOf(SfConfig.DRINK_POISON_SECONDS.get()), 1, 86400, 6, SfConfig.DRINK_POISON_SECONDS::set), null));
        item.add(new Opt("sweatyfeet.config.drink_buff_seconds",
            y -> mkIntBox(y, String.valueOf(SfConfig.DRINK_BUFF_SECONDS.get()), 1, 86400, 6, SfConfig.DRINK_BUFF_SECONDS::set), null));
        cats.add(new Cat("sweatyfeet.config.cat.item", item));

        // 表现与调试：粒子/坐凳脱鞋 + debug（合并原 visual + debug）
        List<Opt> visual = new ArrayList<>();
        visual.add(Opt.header("sweatyfeet.config.section.particles"));
        visual.add(new Opt("sweatyfeet.config.sweat_particles", y -> mkBoolButton(y, SfConfig.SWEAT_PARTICLES), null));
        visual.add(new Opt("sweatyfeet.config.sweat_particle_scale",
            y -> mkIntBox(y, String.valueOf(SfConfig.SWEAT_PARTICLE_SCALE.get()), 1, 10, 2, SfConfig.SWEAT_PARTICLE_SCALE::set), null));
        visual.add(new Opt("sweatyfeet.config.sneeze_particles", y -> mkBoolButton(y, SfConfig.SNEEZE_PARTICLES), null));
        visual.add(Opt.header("sweatyfeet.config.section.undress"));
        visual.add(new Opt("sweatyfeet.config.soak_undress_enabled", y -> mkBoolButton(y, SfConfig.SOAK_UNDRESS_ENABLED), null));
        visual.add(new Opt("sweatyfeet.config.soak_undress_tint",
            y -> mkStrBox(y, SfConfig.SOAK_UNDRESS_TINT.get(), 9, SfConfig.SOAK_UNDRESS_TINT::set), null));
        visual.add(new Opt("sweatyfeet.config.soak_undress_pick",
            y -> Button.builder(Component.translatable("sweatyfeet.picker.open"),
                b -> minecraft.setScreen(new SoakSkinPickerScreen(minecraft.player, this)))
                .bounds(inputX, y, INPUT_W, 20).build(), null));
        visual.add(Opt.header("sweatyfeet.config.section.debug"));
        visual.add(new Opt("sweatyfeet.config.debug_show_ticks", y -> mkBoolButton(y, SfConfig.DEBUG_SHOW_TICKS), null));
        visual.add(new Opt("sweatyfeet.config.debug_state_log", y -> mkBoolButton(y, SfConfig.DEBUG_STATE_LOG), null));
        visual.add(new Opt("sweatyfeet.config.debug_flow_log", y -> mkBoolButton(y, SfConfig.DEBUG_FLOW_LOG), null));
        visual.add(new Opt("sweatyfeet.config.debug_force_sweat", y -> mkBoolButton(y, SfConfig.DEBUG_FORCE_SWEAT), null));
        visual.add(new Opt("sweatyfeet.config.debug_force_level3", y -> mkBoolButton(y, SfConfig.DEBUG_FORCE_LEVEL3), null));
        visual.add(new Opt("sweatyfeet.config.debug_force_fungus", y -> mkBoolButton(y, SfConfig.DEBUG_FORCE_FUNGUS), null));
        visual.add(new Opt("sweatyfeet.config.debug_undress", y -> mkBoolButton(y, SfConfig.DEBUG_UNDRESS), null));
        cats.add(new Cat("sweatyfeet.config.cat.visual", visual));

        expanded = new boolean[cats.size()];
        java.util.Arrays.fill(expanded, true);
    }

    public SfConfigScreen(Screen lastScreen) {
        super(Component.translatable("sweatyfeet.config.title"));
        this.lastScreen = lastScreen;
        snapshotAll();
    }

    // 快照全部配置项
    private void snapshotAll() {
        tracked.add(track(SfConfig.LEVEL_1_SECONDS));
        tracked.add(track(SfConfig.LEVEL_2_SECONDS));
        tracked.add(track(SfConfig.LEVEL_3_SECONDS));
        tracked.add(track(SfConfig.ENABLE_FUNGUS));
        tracked.add(track(SfConfig.FUNGUS_DELAY_SECONDS));
        tracked.add(track(SfConfig.FUNGUS_DAMAGE_ENABLED));
        tracked.add(track(SfConfig.FUNGUS_DAMAGE_INTERVAL_SECONDS));
        tracked.add(track(SfConfig.FUNGUS_INFECTION_ENABLED));
        tracked.add(track(SfConfig.FUNGUS_INFECTION_RANGE));
        tracked.add(track(SfConfig.FUNGUS_INFECTION_INTERVAL_SECONDS));
        tracked.add(track(SfConfig.SMELL_ENABLED));
        tracked.add(track(SfConfig.SMELL_RANGE));
        tracked.add(track(SfConfig.EFFECT_SECONDS));
        tracked.add(track(SfConfig.DRINK_POISON_SECONDS));
        tracked.add(track(SfConfig.BOTTLE_NAUSEA_SECONDS));
        tracked.add(track(SfConfig.DRINK_BUFF_SECONDS));
        tracked.add(track(SfConfig.DEGRADE_SECONDS));
        tracked.add(track(SfConfig.WASH_SECONDS));
        tracked.add(track(SfConfig.WASH_BOOTS_ENABLED));
        tracked.add(track(SfConfig.WASH_BOOTS_SECONDS));
        tracked.add(track(SfConfig.SLIDE_ENABLED));
        tracked.add(track(SfConfig.SLIDE_RETENTION_PERCENT));
        tracked.add(track(SfConfig.SWEAT_PARTICLES));
        tracked.add(track(SfConfig.SWEAT_PARTICLE_SCALE));
        tracked.add(track(SfConfig.SNEEZE_PARTICLES));
        tracked.add(track(SfConfig.SOAK_UNDRESS_ENABLED));
        tracked.add(track(SfConfig.SOAK_UNDRESS_TINT));
        tracked.add(track(SfConfig.DEBUG_UNDRESS));
        tracked.add(track(SfConfig.DEBUG_SHOW_TICKS));
        tracked.add(track(SfConfig.DEBUG_FORCE_FUNGUS));
        tracked.add(track(SfConfig.DEBUG_FORCE_SWEAT));
        tracked.add(track(SfConfig.DEBUG_FORCE_LEVEL3));
        tracked.add(track(SfConfig.DEBUG_STATE_LOG));
        tracked.add(track(SfConfig.DEBUG_FLOW_LOG));
    }

    @Override
    protected void init() {
        buildCats();
        scrollWidgets.clear();

        dividerX = CAT_X + CAT_W + 12;
        optLabelX = dividerX + 14;
        inputX = width - 26 - INPUT_W;

        scrollOffset = Mth.clamp(scrollOffset, 0, calcMaxScroll());
        treeScroll = Mth.clamp(treeScroll, 0, calcTreeMaxScroll());

        int y = viewTop() - scrollOffset;
        for (Opt opt : visibleOpts()) {
            if (opt.isHeader()) {
                y += HEADER_H;
                continue;
            }
            AbstractWidget w = opt.factory().create(y);
            w.visible = y >= viewTop() && y + 20 <= viewBottom();
            scrollWidgets.add(addRenderableWidget(w));
            y += ROW_H;
        }

        doneBtn = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> doClose())
            .bounds(width / 2 - 100, height - 32, 200, 20).build());
        exitBtn = addRenderableWidget(Button.builder(Component.translatable("sweatyfeet.config.exit"), btn -> doExit())
            .bounds(width / 2 - 104, height - 32, 100, 20).build());
        saveBtn = addRenderableWidget(Button.builder(Component.translatable("sweatyfeet.config.save"), btn -> doClose())
            .bounds(width / 2 + 4, height - 32, 100, 20).build());
    }

    // 切换 tab：看该 tab 全部分区
    private void switchCategory(int idx) {
        boolean same = idx == selectedCat && selectedSub == -1;
        selectedCat = idx;
        selectedSub = -1;
        expanded[idx] = true;
        if (!same) rebuild();
    }

    // 选中某个子分类：右侧只显示该分区的选项
    private void selectSub(int catIdx, int sub) {
        boolean same = catIdx == selectedCat && sub == selectedSub;
        selectedCat = catIdx;
        selectedSub = sub;
        expanded[catIdx] = true;
        if (!same) rebuild();
    }

    private void rebuild() {
        scrollOffset = 0;
        setFocused(null);
        clearWidgets();
        init();
    }

    // 当前右侧要显示的选项列表：selectedSub<0 全部（含分区头），否则只取目标分区的选项
    private List<Opt> visibleOpts() {
        List<Opt> all = cats.get(selectedCat).opts();
        if (selectedSub < 0) return all;
        List<Opt> out = new ArrayList<>();
        int seen = 0;
        boolean in = false;
        for (Opt o : all) {
            if (o.isHeader()) {
                if (seen == selectedSub) {
                    in = true;
                    seen++;
                    continue;
                }
                if (in) break;
                seen++;
                continue;
            }
            if (in) out.add(o);
        }
        return out;
    }

    private Button mkBoolButton(int y, ModConfigSpec.BooleanValue cfg) {
        boolean v = cfg.get();
        return Button.builder(
            v ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF,
            btn -> {
                boolean nv = !cfg.get();
                cfg.set(nv);
                btn.setMessage(nv ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
            }
        ).bounds(inputX, y, INPUT_W, 20).build();
    }

    private EditBox mkStrBox(int y, String initial, int maxLen, Consumer<String> onChange) {
        EditBox box = new EditBox(font, inputX, y, INPUT_W, 20, Component.literal(""));
        box.setValue(initial);
        box.setMaxLength(maxLen);
        box.setResponder(onChange::accept);
        return box;
    }

    private EditBox mkIntBox(int y, String initial, int min, int max, int maxLen, Consumer<Integer> onChange) {
        EditBox box = new EditBox(font, inputX, y, INPUT_W, 20, Component.literal(""));
        box.setValue(initial);
        box.setMaxLength(maxLen);
        box.setResponder(s -> {
            if (!s.matches("\\d*")) return;
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) onChange.accept(v);
            } catch (NumberFormatException ignored) {
            }
        });
        return box;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 背景：半透明深灰（世界透出）——fill 的 ARGB 顶点 alpha 在 RenderType.gui() blend 下保证生效，
        // 比纹理 blit + setShaderColor 稳（1.21.1 的 blit 走 POSITION_TEX shader，alpha 时序易丢导致全不透）。
        // 0x8F = 56% 不透明，等效 e33chat CONFIG_BG(192) × drawWithAlpha(0.75) 的视觉。
        g.fill(0, 0, width, height, 0x8F101010);
        tickAnims(); // 先推进平滑动画+同步控件 y，再画（下方绘制循环依赖更新后的 offset）
        g.drawString(font, title, width / 2 - font.width(title) / 2, 14, c().configTitle(), false);

        String tooltipKey = null;

        // 左侧标签树：可滚动 + 裁剪到视口
        g.enableScissor(CAT_X, START_Y, dividerX, viewBottom());
        int ly = START_Y - treeScroll;
        for (int i = 0; i < cats.size(); i++) {
            boolean sel = i == selectedCat;
            boolean hover = mouseX >= CAT_X && mouseX <= CAT_X + CAT_W && mouseY >= ly && mouseY < ly + CAT_ROW_H;
            if (sel || hover)
                g.blit(SfUiTextureManager.rl(SfUiElement.HOVER_BG), CAT_X, ly, CAT_W, CAT_ROW_H, 0f, 0f, 1, 1, 1, 1);
            if (sel)
                g.fill(CAT_X, ly, CAT_X + 2, ly + CAT_ROW_H, c().configTitle());
            drawTriangle(g, CAT_X + 6, ly + (CAT_ROW_H - 5) / 2, expanded[i],
                sel ? c().configTitle() : c().configLabel());
            g.drawString(font, Component.translatable(cats.get(i).key()), CAT_X + 18, ly + (CAT_ROW_H - 8) / 2,
                sel ? c().configTitle() : c().configLabel(), false);
            ly += CAT_ROW_H;
            if (expanded[i]) {
                int sub = 0;
                for (Opt o : cats.get(i).opts()) {
                    if (!o.isHeader()) continue;
                    boolean selSub = i == selectedCat && sub == selectedSub;
                    boolean sh = mouseX >= CAT_X + 14 && mouseX <= CAT_X + CAT_W && mouseY >= ly && mouseY < ly + SUB_ROW_H;
                    if (selSub || sh)
                        g.blit(SfUiTextureManager.rl(SfUiElement.HOVER_BG),
                            CAT_X + 14, ly, CAT_W - 14, SUB_ROW_H, 0f, 0f, 1, 1, 1, 1);
                    if (selSub)
                        g.fill(CAT_X + 14, ly, CAT_X + 16, ly + SUB_ROW_H, c().configTitle());
                    g.drawString(font, Component.translatable(o.key()), CAT_X + 24, ly + (SUB_ROW_H - 8) / 2,
                        (selSub || sh) ? c().configTitle() : c().configLabel(), false);
                    sub++;
                    ly += SUB_ROW_H;
                }
            }
        }
        g.disableScissor();
        drawBar(g, tTrackX(), START_Y, viewBottom(), tTotalH(), treeScroll, calcTreeMaxScroll(), mouseX, mouseY, tBarDrag);

        // Divider between categories and options
        g.blit(SfUiTextureManager.rl(SfUiElement.DIVIDER), dividerX, START_Y - 6, 1, viewBottom() - (START_Y - 6), 0f, 0f, 1, 1, 1, 1);

        // Option rows hard-clipped to the viewport
        g.enableScissor(optLabelX - 4, viewTop(), width, viewBottom());
        int y = viewTop() - scrollOffset;
        for (Opt opt : visibleOpts()) {
            if (opt.isHeader()) {
                // 分区标题：灰字左对齐 + 字右侧延伸一条细分隔线
                Component label = Component.translatable(opt.key());
                g.drawString(font, label, optLabelX, y + 11, c().configLabel(), false);
                int lineX = optLabelX + font.width(label) + 8;
                int lineEnd = width - 24; // 横线跨过输入框延伸到右边缘（e33chat 风格，原来只到 inputX 视觉显短）
                if (lineX < lineEnd)
                    g.blit(SfUiTextureManager.rl(SfUiElement.DIVIDER), lineX, y + 15, lineEnd - lineX, 1, 0f, 0f, 1, 1, 1, 1);
                y += HEADER_H;
                continue;
            }
            g.drawString(font, Component.translatable(opt.key()), optLabelX, y + 6, c().configLabel(), false);
            if (y >= viewTop() && y + 20 <= viewBottom()
                && mouseX >= optLabelX - 4 && mouseX <= inputX - 10 && mouseY >= y && mouseY <= y + 20)
                tooltipKey = opt.key() + ".desc";
            y += ROW_H;
        }
        g.disableScissor();
        drawBar(g, rTrackX(), viewTop(), viewBottom(), rTotalH(), scrollOffset, calcMaxScroll(), mouseX, mouseY, rBarDrag);

        int changed = changeCount();
        doneBtn.visible = changed == 0;
        exitBtn.visible = changed > 0;
        saveBtn.visible = changed > 0;

        super.render(g, mouseX, mouseY, partialTick);

        if (changed > 0)
            g.drawString(font, Component.translatable("sweatyfeet.config.changed", changed),
                width / 2 + 112, height - 26, c().configLabel(), false);

        if (tooltipKey != null)
            g.renderTooltip(font, font.split(Component.translatable(tooltipKey), 190), mouseX, mouseY);
    }

    private void drawTriangle(GuiGraphics g, int x, int y, boolean down, int color) {
        if (down) {
            g.fill(x, y, x + 5, y + 1, color);
            g.fill(x + 1, y + 1, x + 4, y + 2, color);
            g.fill(x + 2, y + 2, x + 3, y + 3, color);
        } else {
            g.fill(x, y, x + 1, y + 1, color);
            g.fill(x, y + 1, x + 2, y + 2, color);
            g.fill(x, y + 2, x + 3, y + 3, color);
            g.fill(x, y + 3, x + 2, y + 4, color);
            g.fill(x, y + 4, x + 1, y + 5, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 滚动条命中优先：拖 thumb 或点轨道翻页
        int w = SfScrollbar.WIDTH;
        int rMax = calcMaxScroll();
        if (rMax > 0 && mouseX >= rTrackX() && mouseX < rTrackX() + w
            && mouseY >= viewTop() && mouseY < viewBottom()) {
            int th = SfScrollbar.thumbHeight(rTrackH(), rTotalH());
            int ty = SfScrollbar.thumbY(viewTop(), rTrackH(), th, scrollOffset, rMax);
            if (mouseY < ty) startR(scrollOffset - rTrackH(), 120);
            else if (mouseY > ty + th) startR(scrollOffset + rTrackH(), 120);
            else {
                rBarDrag = true;
                rBarDragY = (int) mouseY;
                rBarDragOff = scrollOffset;
            }
            return true;
        }
        int tMax = calcTreeMaxScroll();
        if (tMax > 0 && mouseX >= tTrackX() && mouseX < tTrackX() + w
            && mouseY >= START_Y && mouseY < viewBottom()) {
            int th = SfScrollbar.thumbHeight(tTrackH(), tTotalH());
            int ty = SfScrollbar.thumbY(START_Y, tTrackH(), th, treeScroll, tMax);
            if (mouseY < ty) startT(treeScroll - tTrackH(), 120);
            else if (mouseY > ty + th) startT(treeScroll + tTrackH(), 120);
            else {
                tBarDrag = true;
                tBarDragY = (int) mouseY;
                tBarDragOff = treeScroll;
            }
            return true;
        }
        if (button == 0) {
            int ly = START_Y - treeScroll;
            for (int i = 0; i < cats.size(); i++) {
                if (mouseY >= ly && mouseY < ly + CAT_ROW_H && mouseX >= CAT_X && mouseX <= CAT_X + CAT_W) {
                    if (mouseX < CAT_X + 16) {
                        expanded[i] = !expanded[i]; // 箭头区：折叠/展开
                    } else {
                        switchCategory(i); // 标签区：看该 tab 全部
                    }
                    return true;
                }
                ly += CAT_ROW_H;
                if (expanded[i]) {
                    int sub = 0;
                    for (Opt o : cats.get(i).opts()) {
                        if (!o.isHeader()) continue;
                        if (mouseY >= ly && mouseY < ly + SUB_ROW_H && mouseX >= CAT_X + 14 && mouseX <= CAT_X + CAT_W) {
                            selectSub(i, sub); // 子分类：右侧只显示该分区
                            return true;
                        }
                        sub++;
                        ly += SUB_ROW_H;
                    }
                }
            }
        }
        // 其余交给 Screen 默认事件链：EditBox 点击 focus、Button 触发——旧版手动转发导致输入框不可输入的 bug 就出在这里
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int changeCount() {
        int n = 0;
        for (Tracked t : tracked) {
            if (t.changed()) n++;
        }
        return n;
    }

    private void revertAll() {
        for (Tracked t : tracked) t.revert();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // no-op：背景已在 render() 开头画一次（1.21.1 的 render 调 3 参版本）
    }

    private int optAreaW() {
        return inputX - optLabelX - 4;
    }

    // 选项滚动区的顶/下边界
    private int viewTop() {
        return START_Y;
    }

    private int viewBottom() {
        return height - 40;
    }

    private int calcMaxScroll() {
        int total = 0;
        for (Opt opt : visibleOpts())
            total += opt.isHeader() ? HEADER_H : ROW_H;
        return Math.max(0, viewTop() + total - viewBottom());
    }

    private int calcTreeMaxScroll() {
        int total = 0;
        for (int i = 0; i < cats.size(); i++) {
            total += CAT_ROW_H;
            if (expanded[i]) for (Opt o : cats.get(i).opts()) if (o.isHeader()) total += SUB_ROW_H;
        }
        return Math.max(0, START_Y + total - viewBottom());
    }

    // 按当前 scrollOffset 重排右侧控件的 y 与可见性
    private void relayoutWidgets() {
        int y = viewTop() - scrollOffset;
        int wi = 0;
        for (Opt opt : visibleOpts()) {
            if (opt.isHeader()) {
                y += HEADER_H;
                continue;
            }
            if (wi < scrollWidgets.size()) {
                AbstractWidget w = scrollWidgets.get(wi++);
                w.setY(y);
                w.visible = y >= viewTop() && y + 20 <= viewBottom();
            }
            y += ROW_H;
        }
    }

    // ---- 滚动条 + 平滑滚动 ----

    private int rTrackX() {
        return width - SfScrollbar.WIDTH;
    }

    private int rTrackH() {
        return viewBottom() - viewTop();
    }

    private int rTotalH() {
        return calcMaxScroll() + rTrackH();
    }

    private int tTrackX() {
        return dividerX - SfScrollbar.WIDTH - 2;
    }

    private int tTrackH() {
        return viewBottom() - START_Y;
    }

    private int tTotalH() {
        return calcTreeMaxScroll() + tTrackH();
    }

    private void startR(float target, int dur) {
        rAnimFrom = scrollOffset;
        rAnimTo = Mth.clamp(target, 0, calcMaxScroll());
        rAnimStart = net.minecraft.Util.getMillis();
        rAnimDur = dur;
        rAnimOn = true;
    }

    private void startT(float target, int dur) {
        tAnimFrom = treeScroll;
        tAnimTo = Mth.clamp(target, 0, calcTreeMaxScroll());
        tAnimStart = net.minecraft.Util.getMillis();
        tAnimDur = dur;
        tAnimOn = true;
    }

    // 每帧推进两区缓出动画并同步右侧控件 y；render 开头调一次
    private void tickAnims() {
        if (rAnimOn) {
            float t = SfAnim.progress(rAnimStart, rAnimDur, false);
            scrollOffset = Math.round(rAnimFrom + (rAnimTo - rAnimFrom) * t);
            if (t >= 1.0f) {
                scrollOffset = Math.round(rAnimTo);
                rAnimOn = false;
            }
        }
        if (tAnimOn) {
            float t = SfAnim.progress(tAnimStart, tAnimDur, false);
            treeScroll = Math.round(tAnimFrom + (tAnimTo - tAnimFrom) * t);
            if (t >= 1.0f) {
                treeScroll = Math.round(tAnimTo);
                tAnimOn = false;
            }
        }
        scrollOffset = Mth.clamp(scrollOffset, 0, calcMaxScroll());
        treeScroll = Mth.clamp(treeScroll, 0, calcTreeMaxScroll());
        relayoutWidgets();
    }

    // 常驻滚动条：maxScroll<=0 不画；track 淡、thumb 实（拖拽/悬停加亮）
    private void drawBar(GuiGraphics g, int trackX, int top, int bot,
                         int totalH, int offset, int maxScroll,
                         double mx, double my, boolean dragging) {
        if (maxScroll <= 0) return;
        int trackH = bot - top;
        int th = SfScrollbar.thumbHeight(trackH, totalH);
        int ty = SfScrollbar.thumbY(top, trackH, th, offset, maxScroll);
        int w = SfScrollbar.WIDTH;
        SfColoredTextureRenderer.drawWithAlpha(g, SfUiTextureManager.rl(SfUiElement.SCROLLBAR_TRACK),
            trackX, top, w, bot - top, 0x40 / 255f);
        int base = dragging ? 0xCC
            : SfScrollbar.isHoveringThumb(mx, my, trackX, ty, th) ? 0xAA : 0x88;
        SfColoredTextureRenderer.drawWithAlpha(g, SfUiTextureManager.rl(SfUiElement.SCROLLBAR_THUMB),
            trackX, ty, w, th, base / 255f);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        // 鼠标在左树区域滚左树，否则滚右侧选项
        if (mouseX < dividerX) {
            if (calcTreeMaxScroll() <= 0) return false;
            startT(treeScroll - (float) (deltaY * 20), 120);
            return true;
        }
        if (calcMaxScroll() <= 0) return false;
        startR(scrollOffset - (float) (deltaY * 20), 120);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (rBarDrag && calcMaxScroll() > 0) {
            int travel = rTrackH() - SfScrollbar.thumbHeight(rTrackH(), rTotalH());
            if (travel > 0) {
                int d = (int) mouseY - rBarDragY;
                startR(rBarDragOff + (float) d * calcMaxScroll() / travel, 80);
            }
            return true;
        }
        if (tBarDrag && calcTreeMaxScroll() > 0) {
            int travel = tTrackH() - SfScrollbar.thumbHeight(tTrackH(), tTotalH());
            if (travel > 0) {
                int d = (int) mouseY - tBarDragY;
                startT(tBarDragOff + (float) d * calcTreeMaxScroll() / travel, 80);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        rBarDrag = false;
        tBarDrag = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // 保存：保留改动直接关闭（写盘）
    private void doClose() {
        SfConfig.SERVER_SPEC.save();
        minecraft.setScreen(lastScreen);
    }

    // 退出：全部回滚到打开时的快照
    private void doExit() {
        revertAll();
        minecraft.setScreen(lastScreen);
    }

    @Override
    public void onClose() {
        int changed = changeCount();
        if (changed > 0) {
            minecraft.setScreen(new ConfirmScreen(confirmed -> {
                if (confirmed) doExit();
                else minecraft.setScreen(this);
            },
                Component.translatable("sweatyfeet.config.discard.title"),
                Component.translatable("sweatyfeet.config.discard.message", changed)));
        } else {
            doClose();
        }
    }
}
