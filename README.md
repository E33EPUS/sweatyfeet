[简体中文](README.md) | [English](README_EN.md)

<h1 align="center">Sweaty Feet</h1>

<p align="center">
  <em>穿靴子太久会脚臭！整蛊向生存模组</em>
</p>

<p align="center">
  <img alt="MC" src="https://img.shields.io/badge/MC-1.21.1-green">
  <img alt="Loader" src="https://img.shields.io/badge/Loader-NeoForge-orange">
  <img alt="Side" src="https://img.shields.io/badge/Side-Client%20%2B%20Server-blue">
  <img alt="Java" src="https://img.shields.io/badge/Java-21%2B-yellow">
  <img alt="Version" src="https://img.shields.io/badge/Version-0.1.1-informational">
  <img alt="License" src="https://img.shields.io/badge/License-MIT-brightgreen">
</p>

长时间穿靴子会把脚捂出汗，汗脚分三级推进，三级的臭脚还会感染真菌——掉血、打喷嚏、传染给朋友。把汗倒进玻璃瓶做成汗液瓶，发酵靴能酿出 buff 饮品；想治好，只能坐凳子上泡脚。全是恶搞，没有正经，适合联机互坑。

---

## 安装

- 环境：Minecraft **1.21.1** + NeoForge **21.1.x** + Java 21
- 从 [Releases](https://github.com/E33EPUS/sweatyfeet/releases) 下载 JAR，扔进 `.minecraft/mods/`，启动游戏
- 进世界自动收到一本《Sweaty Feet 攻略本》（需 [Patchouli](https://www.curseforge.com/minecraft/mc-mods/patchouli) 前置），所有机制细节都在书里

---

## 玩法循环

```
穿靴子 → 30/60/90 秒 → 汗脚 1/2/3 级（靴子汗化改名、冒汗粒子）
  ├─ 3 级后继续穿 → 真菌感染（掉血 / 喷嚏 / 传染 / 光脚散臭）
  ├─ 倒汗（主手汗靴 + 副手空瓶 + 潜行右键）→ 汗液瓶，靴子还原
  │     发酵靴 3 级倒汗 → 汗液饮品（四种 buff）
  └─ 洗脚：坐凳子 + 脱鞋 + 盆泡脚
        清水洗汗脚；花露水倒盆里变药水洗脚水 → 治真菌（唯一解法）
```

---

## 配置

游戏内按 **J** 打开配置界面：汗脚 / 真菌 / 物品与饮品 / 表现效果四个分类，还能滴管选坐凳时的皮肤色。

---

## 开发构建

```bash
gradlew build
```

产物：`build/libs/sweatyfeet-NeoForge-1.21.1-0.1.1.jar`

---

## 许可证

MIT License, Copyright (c) 2026 E33EPUS
