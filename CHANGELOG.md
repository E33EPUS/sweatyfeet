# Changelog

Sweaty Feet 版本变更记录。中文 + English。

## 0.1.6

### 中文
- 修复泡脚坐下动画在 Fabric 端不生效（移植漏调动画初始化，脱裤正常但腿不动）
- 脱裤皮肤下载链路优化：进世界预下载、离线玩家默认皮肤兜底（离线也有脱裤）、8 秒下载超时、失败 60 秒冷却自动重试
- 修复坐凳瞬间皮肤闪成默认皮肤（默认皮肤兜底仅限真离线玩家）
- debug_undress 日志按状态变化输出（不再每帧刷屏）
- 清理：tooltip 死引用、孤儿语言键、未用 GUI 贴图；修复配置界面悬停显示原始键

### English
- Fixed soak sit animation not playing on Fabric (the port missed the animation init call — undress worked but legs didn't move)
- Undress skin pipeline optimized: prefetch on login, offline default-skin fallback (offline players get undress too), 8s download timeout, 60s cooldown auto-retry
- Fixed skin flashing to the default skin while sitting (default-skin fallback now limited to truly offline players)
- debug_undress logs only on state change (no per-frame spam)
- Cleanup: dead tooltip refs, orphan lang keys, unused GUI textures; fixed raw key showing on config hover

## 0.1.5

### 中文
- 汗液瓶注册饱食度组件：tooltip 直接显示「恢复 X 饥饿值」（1 点，醇厚风味翻倍 2 点），与原版食物一致
- 饮食物品 tooltip 精简：效果由药水组件自动显示，风味 lore 保留纯描写，细节移入手册
- 手册更新：三级质变、饱食度、发酵靴护甲说明

### English
- Sweat bottles now carry a food component: the tooltip shows "Restores X Hunger" (1 point, doubled to 2 by the Rich flavor), exactly like vanilla food
- Drink item tooltips trimmed: effects are auto-shown by the potion component, flavor lore stays as pure flavor text, details moved to the guidebook
- Guidebook updated: transmutation, hunger, fermented-boots armor

## 0.1.4

### 中文
- **三级质变**：1-2 级倒汗永远出普通瓶；汗脚 3 级倒汗按靴子材质出风味瓶（名字无等级、保留完整三级效果）；其他 mod 靴子仍出普通三级瓶（兼容）
- 风味瓶按风味染色（硫磺=金色药水、凛冽=青蓝……），饮品为麦金色
- 倒汗改走自定义网络包（修复事件取消后服务端收不到的问题）；创造标签页全变体；修复洗脚盆/凳子碰撞箱（曾为整区块大小）

### English
- **Tier-3 transmutation**: tiers 1-2 always pour plain bottles; at tier 3 the sweat transmutes and the bottle takes on the flavor of the boot material (no level in the name, full tier-3 effects kept); boots from other mods still pour plain tier-3 bottles (compat)
- Flavored bottles are tinted per flavor (Sulfurous = golden potion, Crisp = cyan, ...); the drink is wheat-gold
- Pouring now goes through a custom network packet (use-event cancellation meant the server never heard about it); creative tab has all variants; fixed basin/stool collision boxes (they were one chunk big)

## 0.1.3

### 中文
- 背包界面右上角加 ⚙ 配置按钮（不依赖任何前置）；汗脚 buff 图标右上角显示 I/II/III 等级角标
- 创造标签页全物品；删除已移除的「汗液瓶投掷」配置项

### English
- ⚙ config button on the inventory screen (no prerequisites); I/II/III tier badge on the sweaty-feet HUD icon
- All creative-tab variants; removed the dropped "thrown bottle debuff" config
