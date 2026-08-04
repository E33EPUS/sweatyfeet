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
  <img alt="Version" src="https://img.shields.io/badge/Version-0.1.0-informational">
  <img alt="License" src="https://img.shields.io/badge/License-MIT-brightgreen">
</p>

Sweaty Feet 是一款整蛊向生存模组：长时间穿靴子会把脚捂出汗，汗脚分三级推进，三级的臭脚还会感染真菌——真菌会掉血、会传染、会打喷嚏。把汗倒进玻璃瓶做成汗液瓶，还能酿成各种 buff 饮品。全是恶搞，没有正经，适合联机互坑。

---

## 目录

- [安装要求](#安装要求)
- [安装方法](#安装方法)
- [快速开始](#快速开始)
- [玩法循环](#玩法循环)
- [汗脚分级](#汗脚分级)
- [真菌感染](#真菌感染)
- [汗液瓶与饮品](#汗液瓶与饮品)
- [配置](#配置)
- [兼容性](#兼容性)
- [已知限制](#已知限制)
- [开发与构建](#开发与构建)
- [问题反馈](#问题反馈)
- [许可证](#许可证)

---

## 安装要求

| 依赖 | 类型 | 说明 |
|---|---|---|
| Minecraft | 必需 | 1.21.1 |
| Java | 必需 | 21+ |
| NeoForge | 必需 | 21.1.x |
| Cloth Config | 可选 | v15（没装也能玩，只是没有配置界面） |

---

## 安装方法

1. 从 [Releases](https://github.com/E33EPUS/sweatyfeet/releases) 下载 JAR
2. 放入 `.minecraft/mods/` 目录
3. 启动游戏

---

## 快速开始

1. 穿上任何靴子（原版皮革/铁/钻/下界合金等都行）
2. 连续穿 **2 分钟**：靴子变成「充满汗液的xxx」，挂上汗脚 1 级
3. 手持汗靴 + 副手玻璃瓶 + **潜行右键**：倒汗！玻璃瓶变成汗液瓶，靴子还原
4. 拿汗液瓶 + 糖/金粒 → 工作台合成 buff 饮品

---

## 玩法循环

```
穿靴子 → 2/4/6 分钟 → 汗脚 1/2/3 级（靴子汗化、改名、冒汗粒子）
  ├─ 脱鞋 → 计时清零，debuff 自然走完剩余时长
  ├─ 3 级后继续穿 30 秒 → 真菌感染（减速 + 缓慢扣血 + 喷嚏 + 可传染）
  └─ 倒汗（潜行右键 + 副手玻璃瓶）→ 汗液瓶 + 靴子还原
        ↓ 汗液瓶按倒汗时的等级产出（一级/二级/三级）
        一级瓶：喝回半格饱食度
        二级瓶：+ 反胃 10 秒
        三级瓶：+ 中毒 3 秒
        ↓ 工作台合成
        速度饮品（+糖）/ 力量饮品（+金粒）：喝 + buff 30 秒
```

---

## 汗脚分级

| 等级 | 触发 | 表现 |
|---|---|---|
| 1 级 | 穿靴 2 分钟 | 冒水粒子，无额外效果；靴子汗化改名 |
| 2 级 | 穿靴 4 分钟 | **脚滑**（松键后保留动量，类似冰面但更好控制） |
| 3 级 | 穿靴 6 分钟 | 脚滑 + 减速 |

- 兼容一切 `foot_armor` 标签物品（原版 + 其他 mod 的靴子）
- 汗化不改变物品本身：图标、属性、附魔、耐久全保留，只加组件 + 改名
- 玩家自定义的靴子名会存下来，倒汗后原样还原，不丢

---

## 真菌感染

- 汗脚 3 级后继续穿 30 秒触发
- 持续减速 15% + 每 3 秒掉 1 点血（无视护甲，可致死）+ 喷嚏粒子
- **可传染**：站在感染者附近会被传染，链式扩散
- 脱鞋后 debuff 走完剩余时长自然消失，不治疗会死

---

## 汗液瓶与饮品

| 物品 | 获得 | 效果 |
|---|---|---|
| 一级汗液瓶 | 汗脚 1 级倒汗 | 喝：回半格饱食度 |
| 二级汗液瓶 | 汗脚 2 级倒汗 | 喝：回饱食度 + 反胃 10 秒 |
| 三级汗液瓶 | 汗脚 3 级倒汗 | 喝：回饱食度 + 反胃 + 中毒 3 秒 |
| 速度饮品 | 汗液瓶 + 糖 | 喝：速度 I × 30 秒 |
| 力量饮品 | 汗液瓶 + 金粒 | 喝：力量 I × 30 秒 |

- 汗液瓶潜行右键投掷：砸中人挂汗脚 5 秒（整蛊别人）
- 所有饮品都是原版药水式：仰头喝动画 + 咕嘟音效 + 回空玻璃瓶

---

## 配置

游戏内 **Mods 列表 → Sweaty Feet → Config 按钮**（需要 Cloth Config），或直接改 `config/sweatyfeet.json`。

| 分类 | 可调项 |
|---|---|
| 穿戴计时 | 1/2/3 级所需秒数 |
| 真菌感染 | 开关、延迟、时长、扣血开关/间隔、传染开关/范围/间隔 |
| 效果时长 | 汗脚时长、投掷 debuff、瓶中毒/反胃、饮品 buff 时长 |
| 移动效果 | 脚滑开关、动量保留百分比 |
| 表现效果 | 汗/喷嚏粒子开关、粒子数量 |
| 调试 | 显示穿戴计时、强制真菌 |

---

## 兼容性

- **冷知识**：与 Cold Sweat（温度系统大 mod）玩法不冲突——它管体温，我们管脚臭，但两者都会作用在靴子上，整合包共存需实测
- 合成配方走原版 JSON 配方系统，可被资源包覆盖

---

## 已知限制

- 专用服务器（Dedicated Server）：服务端进程读配置默认值（Cloth Config 只在客户端注册），单机/局域网正常
- 低版本 NeoForge（< 21.1）不会加载（mods.toml 依赖范围限制）
- 数值为 v0.1 雏形默认值，脚滑手感、真菌节奏都建议实测后调

---

## 开发与构建

```bash
# 编译打包
gradlew build
# 产物：build/libs/sweatyfeet-NeoForge-1.21.1-0.1.0.jar

# 单元测试（e33chat 哲学：每个功能有测试兜底）
gradlew test -PrunTests
```

**环境坑**（Windows）：gradle 全局代理 `127.0.0.1:26561` 未开启时，构建需追加 `-Dhttp.proxyHost= -Dhttps.proxyHost= -Dhttp.proxyPort= -Dhttps.proxyPort=` 或临时关闭用户级 gradle.properties 代理。

---

## 问题反馈

GitHub Issues: https://github.com/E33EPUS/sweatyfeet/issues

---

## 许可证

MIT License，Copyright (c) 2026 E33EPUS
