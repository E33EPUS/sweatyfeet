[简体中文](README.md) | [English](README_EN.md)

<h1 align="center">Sweaty Feet</h1>

<p align="center">
  <em>Wear boots too long and your feet get sweaty! A prank survival mod</em>
</p>

<p align="center">
  <img alt="MC" src="https://img.shields.io/badge/MC-1.21.1-green">
  <img alt="Loader" src="https://img.shields.io/badge/Loader-NeoForge-orange">
  <img alt="Side" src="https://img.shields.io/badge/Side-Client%20%2B%20Server-blue">
  <img alt="Java" src="https://img.shields.io/badge/Java-21%2B-yellow">
  <img alt="Version" src="https://img.shields.io/badge/Version-0.1.1-informational">
  <img alt="License" src="https://img.shields.io/badge/License-MIT-brightgreen">
</p>

Wearing boots too long makes your feet sweat. Sweaty Feet escalates through three levels, and at level 3 your stinky feet can catch a foot fungus — health drain, sneezing, and it spreads to your friends. Pour the sweat into glass bottles, brew buff drinks from fermented boots, and cure it all by soaking your feet on a stool. All prank, no serious — perfect for griefing your friends on a server.

---

## Installation

- Requirements: Minecraft **1.21.1** + NeoForge **21.1.x** + Java 21
- Download the JAR from [Releases](https://github.com/E33EPUS/sweatyfeet/releases), drop it into `.minecraft/mods/`, and launch the game
- You'll receive a "Sweaty Feet Guide" book on joining a world (requires [Patchouli](https://www.curseforge.com/minecraft/mc-mods/patchouli)); all mechanics live inside the book

---

## Gameplay loop

```
Wear boots → 30/60/90 s → Sweaty Feet level 1/2/3 (boots rename, sweat particles)
  ├─ Keep wearing at level 3 → Foot fungus (drain / sneeze / spread / smell when barefoot)
  ├─ Pour sweat (sweaty boot + empty bottle in offhand + sneak right-click) → sweat bottle, boots clean
  │     Fermented boots at level 3 pour a Sweat Drink (four buffs)
  └─ Soak feet: sit on a stool, take off boots, soak in a basin
        Clean water washes Sweaty Feet; add floral water to make medicinal water → cures fungus (only cure)
```

---

## Configuration

Press **J** in-game to open the config screen: Sweaty Feet / Fungus / Items & Drinks / Visuals, plus an eyedropper to pick your seated skin tint.

---

## Development

```bash
gradlew build
```

Output: `build/libs/sweatyfeet-NeoForge-1.21.1-0.1.1.jar`

---

## License

MIT License, Copyright (c) 2026 E33EPUS
