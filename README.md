# Sweaty Feet

穿靴子太久会脚臭！整蛊向 Minecraft 模组（NeoForge 1.21.1）。

## 玩法

- **汗脚**：穿上任何靴子（`foot_armor` 标签，兼容其他 mod）2/4/6 分钟后触发 1/2/3 级汗脚 debuff，靴子会变成「充满汗液的xxx」（名称可还原）
- **倒汗**：手持汗靴 + 副手玻璃瓶 + 潜行右键 → 玻璃瓶变成汗液瓶，靴子恢复原样
- **汗液瓶**：普通右键喝（轻毒，整蛊自己）；潜行右键投掷（砸中人挂汗脚，整蛊别人）
- **真菌感染**：汗脚 3 级后继续穿靴子会打喷嚏，脱鞋即愈
- **配置**：Mods 列表 → Sweaty Feet → Config 按钮（Cloth Config GUI），所有数值可调

## 前置

- NeoForge 21.1+
- Cloth Config v15（optional，没装也能玩，只是没有配置界面）

## 构建

```bash
gradlew build
# 产物：build/libs/sweatyfeet-NeoForge-1.21.1-<version>.jar
```

## 许可

MIT License，Copyright (c) 2026 E33EPUS
