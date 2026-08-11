[简体中文](README.md) | [English](README_EN.md)

<h1 align="center">Sweaty Feet</h1>

<p align="center">
  <em>穿靴子太久会脚臭，记得脱鞋洗脚！</em>
</p>

<p align="center">
  <img alt="MC" src="https://img.shields.io/badge/MC-1.21.1-green">
  <img alt="Loader" src="https://img.shields.io/badge/Loader-Fabric-orange">
  <img alt="Side" src="https://img.shields.io/badge/Side-Client%20%2B%20Server-blue">
  <img alt="Java" src="https://img.shields.io/badge/Java-21%2B-yellow">
  <img alt="Version" src="https://img.shields.io/badge/Version-0.1.2-informational">
  <img alt="License" src="https://img.shields.io/badge/License-MIT-brightgreen">
</p>

---

## 目录

- [简介](#简介)
- [安装要求](#安装要求)
- [安装方式](#安装方式)
- [玩法流程](#玩法流程)
- [常见问题](#常见问题)
- [开发与构建](#开发与构建)
- [问题反馈](#问题反馈)
- [许可证](#许可证)

---

## 简介

长时间穿靴子会把脚捂出汗：汗脚分三级推进，三级的臭脚还会感染真菌——掉血、打喷嚏、传染给朋友。把汗倒进玻璃瓶做成汗液瓶（按靴子材质带风味效果），发酵靴能酿出 buff 饮品；想治好，只能坐凳子上泡脚。

---

## 安装要求

| 依赖 | 类型 | 说明 |
|---|---|---|
| Minecraft | 必需 | 1.21.1 |
| Fabric Loader | 必需 | 0.15.11+ |
| Fabric API | 必需 | 0.116.x+ |
| Java | 必需 | 21+ |
| Patchouli | 可选 | 装了进世界自动收到《Sweaty Feet 攻略本》，没装其余功能正常 |
| player-animator | 必需 | 泡脚动画（坐凳效果），没装无法加载 |

> 这是 **Fabric 版**分支（`fabric-1.21.1`）。NeoForge 版见仓库默认分支（`neoforge-1.21.1`），两者玩法一致、jar 不通用。

---

## 安装方式

1. 从 [Releases](https://github.com/E33EPUS/sweatyfeet/releases) 下载 `sweatyfeet-Fabric-1.21.1-*.jar`
2. 放入 `.minecraft/mods/` 目录（需已装 Fabric Loader + Fabric API）
3. 启动游戏

---

## 玩法流程

```
穿靴子 → 2/4/6 分钟 → 汗脚 1/2/3 级（靴子汗化改名、冒汗粒子）
  ├─ 3 级后继续穿 60 秒 → 真菌感染（掉血 / 喷嚏 / 传染 / 光脚散臭）
  ├─ 倒汗（主手汗靴 + 副手空瓶 + 潜行右键）→ 汗液瓶，靴子还原
  │     汗液瓶按靴子材质带风味效果（见常见问题）；发酵靴 3 级倒汗 → 汗液饮品（四种 buff）
  └─ 洗脚：坐凳子 + 脱鞋 + 盆泡脚
        清水洗汗脚；花露水倒盆里变药水洗脚水 → 治真菌（唯一解法）
```

进世界按 **J** 打开配置界面（汗脚 / 真菌 / 物品与饮品 / 表现效果），或翻随身的手册看细节。

---

## 常见问题

### 真菌感染怎么治？

唯一的办法：往清水盆里倒一瓶花露水（盆水变药水），坐凳子上、脱鞋、把脚泡进去。脱鞋、倒汗、睡觉都治不好。

### 汗脚怎么消除？

洗脚能直接清掉；脱鞋只会慢慢降级（1 级保留，只能洗）。

### 汗液瓶怎么获得？

穿出汗靴后，主手持汗靴、副手持空玻璃瓶，潜行 + 右键。发酵靴（皮革靴 + 糖）到 3 级倒出来的是汗液饮品。

### 汗液瓶的风味是什么？

按汗靴材质：皮革醇厚（回饱食度翻倍）、铁锈（虚弱）、金贵（幸运）、钻凛冽（抗性提升）、下界合金硫磺（火焰抗性）。集齐五种解锁「从夯到拉」进度。

### 手册怎么获得？

进世界自动发一本《Sweaty Feet 攻略本》，需要 Patchouli 前置。没装 Patchouli 不影响其他玩法。

### 服务端要装吗？

推荐装：真菌伤害、传染、进度判定在服务端跑。单机 / 局域网不需要额外配置。

### 可以放进整合包吗？

可以，MIT 协议无需额外授权。

---

## 开发与构建

```bash
git clone -b fabric-1.21.1 https://github.com/E33EPUS/sweatyfeet.git
cd sweatyfeet
./gradlew build
```

产物：`build/libs/sweatyfeet-Fabric-1.21.1-0.1.2.jar`

运行测试：`./gradlew test`（3 个依赖 ItemStack 注册表的用例在 Fabric 纯 JUnit 下 @Disabled，其余全跑）

---

## 问题反馈

到 [Issues](https://github.com/E33EPUS/sweatyfeet/issues) 提交，尽量附上：

- 模组版本 + Minecraft 版本 + Fabric Loader / Fabric API 版本
- 相关模组列表
- `.minecraft/logs/latest.log`
- 截图 / 视频 + 复现步骤

---

## 许可证

[MIT License](LICENSE)

Copyright &copy; 2026 E33EPUS
