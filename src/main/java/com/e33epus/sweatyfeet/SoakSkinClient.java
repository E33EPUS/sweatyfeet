package com.e33epus.sweatyfeet;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 坐下脱裤观感（纯客户端）：骑 SeatEntity 时把皮肤下半身（腿+腿overlay）涂成肤色。
 * 借鉴 needsofnature 但更轻：遮罩固定、骑乘状态 vanilla 已同步 → 各客户端本地改图，
 * 不上传改后皮肤（它上传是因为破损随机，我们不需要）。
 * 基础图走 PlayerSkin.textureUrl() 自下载 + 磁盘缓存；拉取失败不替换（走原皮肤）。
 * 新像素 = 肤色 × 原像素亮度系数，避免纯色塑料感。
 */
public final class SoakSkinClient {
    /** 下半身主层区（渲染第一层，右腿+左腿）——整区替换成肤色。
     *  注意：身体上衣 overlay 在 (16,32)-(32,48)，绝不能涂——之前误当左腿主层
     *  涂了，导致躯干腰线被染色（实测截图实锤）。 */
    private static final int[][] LEG_MAIN = {
        {0, 16, 16, 32},   // 右腿主
        {16, 32, 48, 64},  // 左腿主
    };

    /** 下半身 overlay 区（裤子第二层，x0-16 的两条）——alpha 必须清 0，
     *  否则第二层仍叠在主层上：提取色叠裤子色 = 呈现的不是提取色（"叠加不是覆盖"根因） */
    private static final int[][] LEG_OVERLAY = {
        {0, 16, 32, 48},   // 右裤
        {0, 16, 48, 64},   // 左裤
    };

    /** 用户指定首次自动采样像素：皮肤展开图第 8 行第 4、5 列（面部侧下）。
     *  第一个通过候选过滤的即为肤色；都不通过 → 回退分区采样 → 再不行回退浅肤色。 */
    private static final int[][] FIRST_TINT_PIXELS = {
        {4, 8},
        {5, 8},
    };

    /** 回退采样区域 {x0,y0,x1,y1,weight}：脸下半+前胸（指定像素失败时的兜底） */
    private static final int[][] TINT_REGIONS = {
        {9, 12, 15, 16, 5},
        {8, 12, 16, 16, 2},
        {20, 17, 28, 24, 3},
    };

    /** 采样失败回退色（needsofnature FALLBACK_SKIN_TINT_RGB 同款浅肤色） */
    static final int FALLBACK_SKIN_TINT = 0xD2A079;

    private record Entry(ResourceLocation base, ResourceLocation generated, String tint) {
    }

    private static final Map<UUID, Entry> CACHE = new HashMap<>();
    private static final Map<UUID, String> LAST_LOG_STATE = new HashMap<>();
    private static final Set<UUID> PENDING = new HashSet<>();
    /** 失败时间戳（ms）：冷却期内不重试，过期后自动重试——网络瞬时故障可自愈 */
    private static final Map<UUID, Long> FAILED = new HashMap<>();
    private static final long RETRY_COOLDOWN_MS = 60_000L;
    private static final Map<UUID, NativeImage> READY = new HashMap<>();
    private static final ExecutorService FETCHER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sweatyfeet-skin-fetch");
        t.setDaemon(true);
        return t;
    });

    private SoakSkinClient() {
    }

    /** Mixin 入口：坐着且改图就绪 → 返回替换后的 PlayerSkin；否则 null 走原版。
     *  skin 由 mixin 传入（getSkin() 返回值）——绝不能在这里再调 player.getSkin()，
     *  否则 mixin→resolve→getSkin→mixin 递归。返回的 PlayerSkin 只换 texture，
     *  url/cape/elytra/model/secure 保持原样（skinlayers3d 等拿 texture() 时看到改图）。 */
    public static PlayerSkin resolve(Player player, PlayerSkin skin) {
        if (!SfConfig.SOAK_UNDRESS_ENABLED.get() || skin == null || skin.texture() == null) {
            return null;
        }
        if (!(player.getVehicle() instanceof SeatEntity)) {
            return null;
        }
        ResourceLocation base = skin.texture();
        UUID id = player.getUUID();
        // 颜色优先级：服务端广播来的（其他玩家/自己坐凳时的上报）> 本地配置。
        // 本地玩家自己最终也走广播回环（== 自己配置），保证各端一致。
        String tintStr = SfConfig.SOAK_UNDRESS_TINT.get();
        String synced = ModNetworking.syncedTint(id);
        if (synced != null) {
            tintStr = synced;
        }
        // 本地玩家坐着且有显式色：每 20 tick 上报一次给服务端（广播给所有人）。
        // 离凳停报（本来就不显示）；新玩家最多等 1 秒收到；滴管选色后立即另发。
        if (player.level().isClientSide
            && player.getUUID().equals(Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null)
            && tintStr != null && !tintStr.isBlank()
            && player.tickCount % 20 == 0) {
            ModNetworking.reportTint(tintStr);
        }
        drainReady(id, base, tintStr);
        Entry e = CACHE.get(id);
        // 配置肤色变了：旧改图作废（之前 CACHE 不记 tint，改配置永不生效——取色器"没用"根因）
        if (e != null && e.base().equals(base) && !e.tint().equals(tintStr)) {
            CACHE.remove(id);
            FAILED.remove(id);
            e = null;
        }
        if (SfConfig.DEBUG_UNDRESS.get()) {
            // 只打状态变化（none→pending→ready/failed），避免每帧刷屏日志爆炸
            String state = (e != null && e.base().equals(base)) ? "ready"
                : PENDING.contains(id) ? "pending"
                : FAILED.containsKey(id) ? "failed" : "none";
            if (!state.equals(LAST_LOG_STATE.get(id))) {
                LAST_LOG_STATE.put(id, state);
                com.mojang.logging.LogUtils.getLogger().info(
                    "[SF] undress resolve {} seat={} tint={} state={} (base={})",
                    player.getName().getString(),
                    player.getVehicle() instanceof SeatEntity,
                    tintStr,
                    state,
                    base);
            }
        }
        if (e != null && e.base().equals(base)) {
            return new PlayerSkin(e.generated(), skin.textureUrl(), skin.capeTexture(),
                skin.elytraTexture(), skin.model(), skin.secure());
        }
        if (e != null) {
            CACHE.remove(id); // 换皮肤了：旧改图作废，重拉
            FAILED.remove(id);
        }
        requestFetch(id, player, skin.textureUrl(), skin.texture());
        return null;
    }

    /** 后台下载完成的图在渲染线程注册成 DynamicTexture（resolve 顺带驱动）。
     *  tint 用 resolve 算好的最终色（广播 > 本地配置）——之前误读 SfConfig，
     *  跨端同步的广播色根本没进改图，且缓存键永远对不上 → 每 20 tick 重拉重生成。 */
    private static void drainReady(UUID id, ResourceLocation base, String tintStr) {
        NativeImage img;
        synchronized (READY) {
            img = READY.remove(id);
        }
        if (img == null) {
            return;
        }
        int tintRgb = parseTint(tintStr);
        if (tintRgb == 0) {
            tintRgb = autoSampleAndPersist(img); // 首次：读指定像素并写入配置
            tintStr = SfConfig.SOAK_UNDRESS_TINT.get(); // 采样成功会更新配置；失败保持空
        }
        NativeImage undressed = buildUndressed(img, tintRgb);
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(SweatyFeet.MOD_ID, "soak_skin/" + id);
        Minecraft.getInstance().getTextureManager().register(rl, new DynamicTexture(undressed));
        CACHE.put(id, new Entry(base, rl, tintStr));
    }

    private static void requestFetch(UUID id, Player player, String url, ResourceLocation textureRl) {
        synchronized (PENDING) {
            if (PENDING.contains(id)) {
                return;
            }
            // 失败冷却：60s 内不重试，过期自动重试（网络瞬时故障可自愈）
            Long failedAt = FAILED.get(id);
            if (failedAt != null && System.currentTimeMillis() - failedAt < RETRY_COOLDOWN_MS) {
                return;
            }
            FAILED.remove(id);
            PENDING.add(id);
        }
        String resolvedUrl = url;
        // CSL/离线场景：getSkin().textureUrl() 可能为 null（本地导入皮肤无 mojang URL）。
        // 学 e33chat：用名字走 getInsecureSkin，CSL 拦截底层 SkinManager 按名解析。
        if (resolvedUrl == null && player != null) {
            try {
                resolvedUrl = Minecraft.getInstance().getSkinManager()
                    .getInsecureSkin(new com.mojang.authlib.GameProfile(
                        java.util.UUID.nameUUIDFromBytes(player.getGameProfile().getName().getBytes(StandardCharsets.UTF_8)),
                        player.getGameProfile().getName()))
                    .textureUrl();
            } catch (Exception ignored) {
            }
        }
        final String fetchUrl = resolvedUrl;
        FETCHER.submit(() -> {
            NativeImage img = null;
            // ① CSL 本地导入皮肤：textureUrl() 是空串（API 不暴露本地路径，反编译实锤），
            //    直接读 CSL 默认本地皮肤目录（LocalSkin/skins/<玩家名>.png）——离线可用
            if (player != null) {
                try {
                    java.nio.file.Path local = Minecraft.getInstance().gameDirectory.toPath()
                        .resolve("CustomSkinLoader/LocalSkin/skins")
                        .resolve(player.getGameProfile().getName() + ".png");
                    if (java.nio.file.Files.isRegularFile(local)) {
                        img = NativeImage.read(java.nio.file.Files.newInputStream(local));
                    }
                } catch (Exception ignored) {
                }
            }
            // ② 资源包内默认皮肤：离线玩家（textureUrl 为 null）的 skin.texture() 直接指向
            //    minecraft:textures/entity/player/{slim,wide}/... —— 零网络直读，离线也有脱裤
            if (img == null) {
                img = tryResource(textureRl);
            }
            // ③ 磁盘缓存 → 带超时下载（在线玩家；国内网络差不挂死，靠缓存/①/②兜底）
            if (img == null && fetchUrl != null && !fetchUrl.isBlank()) {
                try {
                    img = loadCached(fetchUrl);
                } catch (Exception ignored) {
                }
            }
            synchronized (READY) {
                if (img != null) {
                    READY.put(id, img);
                }
            }
            synchronized (PENDING) {
                PENDING.remove(id);
                if (img == null) {
                    FAILED.put(id, System.currentTimeMillis()); // 冷却后自动重试
                    if (SfConfig.DEBUG_UNDRESS.get()) {
                        com.mojang.logging.LogUtils.getLogger().info(
                            "[SF] undress fetch FAILED for {} (url={}, local={})",
                            id, fetchUrl,
                            player != null ? Minecraft.getInstance().gameDirectory.toPath()
                                .resolve("CustomSkinLoader/LocalSkin/skins")
                                .resolve(player.getGameProfile().getName() + ".png") : "n/a");
                    }
                }
            }
        });
    }

    /** 从资源包内读取玩家皮肤展开图（离线默认皮肤 textures/entity/player/...），零网络。
     *  在线玩家的 texture RL 是 minecraft:skins/<hash>（非资源路径），读不到返回 null 走下载。 */
    private static NativeImage tryResource(ResourceLocation rl) {
        if (rl == null) {
            return null;
        }
        try {
            var res = Minecraft.getInstance().getResourceManager().getResource(rl);
            if (res.isPresent()) {
                try (var in = res.get().open()) {
                    return NativeImage.read(in);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** 进世界预热：后台拉取本地玩家皮肤（不要求坐凳），坐下时改图通常已就绪。
     *  走 getInsecureSkin（底层 SkinManager，不经 getSkin mixin——无递归风险）。 */
    public static void prefetch(Player player) {
        if (player == null) {
            return;
        }
        UUID id = player.getUUID();
        synchronized (CACHE) {
            if (CACHE.containsKey(id)) {
                return;
            }
        }
        PlayerSkin skin = null;
        try {
            skin = Minecraft.getInstance().getSkinManager().getInsecureSkin(player.getGameProfile());
        } catch (Exception ignored) {
        }
        if (skin != null && skin.texture() != null) {
            requestFetch(id, player, skin.textureUrl(), skin.texture());
        }
    }

    /** 供取色器用：同步加载玩家皮肤展开图（NativeImage），失败返回 null。调用方负责 close。
     *  优先级：① 已注册纹理对象直接读像素（兼容 CSL FakeHttpTexture 本地缓存 + DynamicTexture）
     *  ② textureUrl() 下载 + 磁盘缓存。返回的是原始皮肤展开图，未经任何屏幕加工。
     *  不调 player.getSkin()——那会触发 getSkin mixin，坐凳时取到的是改图而不是原始皮肤；
     *  走 getInsecureSkin（CSL 拦截底层 SkinManager 按名解析，与 mixin 无关）。 */
    public static NativeImage loadBaseImageSync(Player player) {
        PlayerSkin skin = null;
        try {
            skin = Minecraft.getInstance().getSkinManager().getInsecureSkin(player.getGameProfile());
        } catch (Exception ignored) {
        }
        if (skin != null && skin.texture() != null) {
            NativeImage img = textureImage(skin.texture());
            if (img != null) {
                return img;
            }
        }
        String url = skin != null ? skin.textureUrl() : null;
        if (url != null) {
            try {
                return loadCached(url);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /** 从已注册纹理对象拷一份像素（copy：返回副本，调用方可安全 close，不碰正式纹理）。
     *  只认 DynamicTexture（getPixels 是 vanilla 公开 API）；CSL 的 FakeHttpTexture 是
     *  SimpleTexture 子类，内部图不公开（skinlayers3d 靠自己 mixin accessor 才拿到）→
     *  这类走 textureUrl() 下载兜底（CSL 的 url 是真实皮肤站地址，可下载）。
     *  渲染线程调用（picker init）。 */
    private static NativeImage textureImage(ResourceLocation rl) {
        try {
            AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(rl);
            if (tex instanceof DynamicTexture dt) {
                NativeImage src = dt.getPixels();
                if (src != null) {
                    NativeImage copy = new NativeImage(src.getWidth(), src.getHeight(), true);
                    copy.copyFrom(src);
                    return copy;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static NativeImage loadCached(String url) throws Exception {
        Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("sweatyfeet_skins");
        Files.createDirectories(dir);
        Path file = dir.resolve(sha1(url) + ".png");
        byte[] bytes;
        if (Files.isRegularFile(file)) {
            bytes = Files.readAllBytes(file);
        } else {
            // 下载带 8s 连接/读取超时：网络差不挂死（后台线程，不卡主线程）
            java.net.URLConnection conn = java.net.URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            try (var in = conn.getInputStream()) {
                bytes = in.readAllBytes();
            }
            Files.write(file, bytes);
        }
        return NativeImage.read(new ByteArrayInputStream(bytes));
    }

    /** 64x64 皮肤下半身像素判定（含主层+overlay）。纯逻辑可单测 */
    static boolean isLegPixel(int x, int y, int height) {
        for (int[] r : LEG_MAIN) {
            if (r[2] < height && x >= r[0] && x < r[1] && y >= r[2] && y < r[3]) {
                return true;
            }
        }
        for (int[] r : LEG_OVERLAY) {
            if (r[2] < height && x >= r[0] && x < r[1] && y >= r[2] && y < r[3]) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构建改图：整个下半身 = 提取色号（主层替换为色号，alpha 255）+ overlay 层彻底清除。
     * 皮肤渲染是主层+overlay 两层叠加——只改 RGB 不清 overlay alpha，第二层裤子仍会
     * 叠上来：提取色叠裤子色 = "感觉是叠加不是覆盖"（实测根因）。纯逻辑可单测。
     */
    public static NativeImage buildUndressed(NativeImage base, int tintColor) {
        int w = base.getWidth();
        int h = base.getHeight();
        NativeImage out = new NativeImage(w, h, false);
        out.copyFrom(base);
        int skin = tintColor != 0 ? tintColor : sampleSkinColor(base);
        // 主层：整区替换为纯色块（alpha 255）——用户要求绝对纯色，不要亮度明暗
        for (int[] r : LEG_MAIN) {
            for (int y = r[2]; y < r[3] && y < h; y++) {
                for (int x = r[0]; x < r[1] && x < w; x++) {
                    int orig = base.getPixelRGBA(x, y);
                    if (((orig >>> 24) & 0xFF) == 0) {
                        continue; // 主层透明像素不动（老 64x32 皮肤下半区可能透明）
                        // ABGR：alpha 在最高字节——之前写 (orig&0xFF)==0 查的是 R 通道，
                        // 蓝裤(R=0)等腿部像素被误判透明跳过不涂（P1 字节序修正遗漏）
                    }
                    // 0xRRGGBB → ABGR（A 最高字节，然后 B,G,R）——之前按 RRGGBBAA 写，蓝变紫红+半透明（实测实锤）
                    out.setPixelRGBA(x, y, (0xFF << 24) | ((skin & 0xFF) << 16) | (((skin >>> 8) & 0xFF) << 8) | ((skin >>> 16) & 0xFF));
                }
            }
        }
        // overlay（裤子第二层）：alpha 全清 → 渲染时第二层消失
        for (int[] r : LEG_OVERLAY) {
            for (int y = r[2]; y < r[3] && y < h; y++) {
                for (int x = r[0]; x < r[1] && x < w; x++) {
                    out.setPixelRGBA(x, y, 0x00000000);
                }
            }
        }
        return out;
    }

    /** 肤色候选判定：排除透明/过暗过亮/高饱和/非暖色（抄 needsofnature isSkinTintCandidate） */
    static boolean isSkinTintCandidate(int argb) {
        // NativeImage 字节序 = ABGR（R 最低字节），反编译 Format.RGBA redOffset=0 实锤
        int r = argb & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = (argb >>> 16) & 0xFF;
        int a = (argb >>> 24) & 0xFF;
        if (a < 32) {
            return false;
        }
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        int lum = (r + g + b) / 3;
        if (lum < 35 || lum > 245) {
            return false;
        }
        if (max > 0 && (max - min) * 255 / max > 178) {
            return false; // 饱和度 > 0.7：彩色装饰/衣服排除
        }
        if (r < b * 72 / 100 || g < b * 50 / 100) {
            return false; // 非暖色倾向（蓝/青）
        }
        if (b > r * 85 / 100) {
            return false; // 蓝色占比过高（粉紫/偏蓝肤色也拒）；真正防粉靠采样区域只留脸胸
        }
        return true;
    }

    /** 指定像素采样（纯逻辑）：读 (4,8)(5,8)，第一个通过候选过滤的即肤色；全败回退分区采样 */
    static int sampleSpecifiedPixels(NativeImage base) {
        for (int[] p : FIRST_TINT_PIXELS) {
            if (p[1] >= base.getHeight() || p[0] >= base.getWidth()) {
                continue;
            }
            int c = base.getPixelRGBA(p[0], p[1]);
            if (isSkinTintCandidate(c)) {
                // ABGR：R 在最低字节 → 0xRRGGBB
                return (c & 0xFF) << 16 | ((c >>> 8) & 0xFF) << 8 | ((c >>> 16) & 0xFF);
            }
        }
        return sampleSkinColor(base);
    }

    /** 首次自动采样：指定像素通过候选过滤即写入配置持久化（之后直接用配置色） */
    static int autoSampleAndPersist(NativeImage base) {
        int rgb = sampleSpecifiedPixels(base);
        if (rgb != sampleSkinColor(base) || rgb != FALLBACK_SKIN_TINT) {
            SfConfig.SOAK_UNDRESS_TINT.set(String.format("#%06X", rgb));
            SfConfig.SERVER_SPEC.save(); // 持久化：重启后直接用配置色
        }
        return rgb;
    }

    /** 自动肤色：多区域加权采样 + 32^3 归桶取前 3 大桶加权平均。
     *  之前的全脸均值会把头发/装饰色（如粉色）拉进结果 → 腿部变粉（实测） */
    static int sampleSkinColor(NativeImage base) {
        int w = base.getWidth();
        int h = base.getHeight();
        int[] count = new int[32768];
        long[] sumR = new long[32768];
        long[] sumG = new long[32768];
        long[] sumB = new long[32768];
        for (int[] rg : TINT_REGIONS) {
            int weight = rg[4];
            for (int y = rg[1]; y < rg[3] && y < h; y++) {
                for (int x = rg[0]; x < rg[2] && x < w; x++) {
                    int p = base.getPixelRGBA(x, y);
                    if (!isSkinTintCandidate(p)) {
                        continue;
                    }
                    int r = p & 0xFF;
                    int g = (p >>> 8) & 0xFF;
                    int b = (p >>> 16) & 0xFF;
                    int idx = (r / 8 << 10) | (g / 8 << 5) | (b / 8);
                    count[idx] += weight;
                    sumR[idx] += (long) r * weight;
                    sumG[idx] += (long) g * weight;
                    sumB[idx] += (long) b * weight;
                }
            }
        }
        // 取计数前 3 的桶加权平均
        int topA = -1, topB = -1, topC = -1;
        for (int i = 0; i < count.length; i++) {
            if (count[i] == 0) {
                continue;
            }
            if (topA < 0 || count[i] > count[topA]) {
                topC = topB;
                topB = topA;
                topA = i;
            } else if (topB < 0 || count[i] > count[topB]) {
                topC = topB;
                topB = i;
            } else if (topC < 0 || count[i] > count[topC]) {
                topC = i;
            }
        }
        if (topA < 0) {
            return FALLBACK_SKIN_TINT;
        }
        if (count[topA] < 12) {
            return FALLBACK_SKIN_TINT; // 候选太少：区域里没有像样的肤色，回退浅肤色
        }
        long r = sumR[topA] + (topB >= 0 ? sumR[topB] : 0) + (topC >= 0 ? sumR[topC] : 0);
        long g = sumG[topA] + (topB >= 0 ? sumG[topB] : 0) + (topC >= 0 ? sumG[topC] : 0);
        long b = sumB[topA] + (topB >= 0 ? sumB[topB] : 0) + (topC >= 0 ? sumB[topC] : 0);
        long n = count[topA] + (topB >= 0 ? count[topB] : 0) + (topC >= 0 ? count[topC] : 0);
        return (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
    }

    /** 配置 hex 解析；空/坏 = 0（走自动采样） */
    static int parseTint(String hex) {
        if (hex == null || hex.isBlank()) {
            return 0;
        }
        try {
            return (int) Long.parseLong(hex.replace("#", "").trim(), 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String sha1(String s) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte x : d) {
            sb.append(String.format("%02x", x));
        }
        return sb.toString();
    }

    /** 玩家下线清理缓存（防 UUID 复用后 FAILED/PENDING 卡死 + 长期会话内存泄漏） */
    public static void clearFor(UUID id) {
        synchronized (READY) {
            READY.remove(id);
        }
        synchronized (PENDING) {
            PENDING.remove(id);
            FAILED.remove(id);
        }
        CACHE.remove(id);
        LAST_LOG_STATE.remove(id);
    }
}
