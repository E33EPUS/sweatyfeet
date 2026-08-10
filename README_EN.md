[简体中文](README.md) | [English](README_EN.md)

<h1 align="center">Sweaty Feet</h1>

<p align="center">
  <em>Wear boots too long and your feet get sweaty — take them off and wash!</em>
</p>

<p align="center">
  <img alt="MC" src="https://img.shields.io/badge/MC-1.21.1-green">
  <img alt="Loader" src="https://img.shields.io/badge/Loader-NeoForge-orange">
  <img alt="Side" src="https://img.shields.io/badge/Side-Client%20%2B%20Server-blue">
  <img alt="Java" src="https://img.shields.io/badge/Java-21%2B-yellow">
  <img alt="Version" src="https://img.shields.io/badge/Version-0.1.2-informational">
  <img alt="License" src="https://img.shields.io/badge/License-MIT-brightgreen">
</p>

---

## Table of Contents

- [Introduction](#introduction)
- [Requirements](#requirements)
- [Installation](#installation)
- [Gameplay](#gameplay)
- [FAQ](#faq)
- [Development](#development)
- [Feedback](#feedback)
- [License](#license)

---

## Introduction

Wearing boots too long makes your feet sweat. Sweaty Feet escalates through three levels, and at level 3 your stinky feet can catch a foot fungus — health drain, sneezing, and it spreads to your friends. Pour the sweat into glass bottles, brew buff drinks from fermented boots, and cure it all by soaking your feet on a stool.

---

## Requirements

| Dependency | Type | Notes |
|---|---|---|
| Minecraft | Required | 1.21.1 |
| NeoForge | Required | 21.1+ |
| Java | Required | 21+ |
| Patchouli | Optional | You'll receive the "Sweaty Feet Guide" book on joining a world; everything else works without it |
| player-animator | Required | Soaking animation (stool sitting); the mod won't load without it |

---

## Installation

1. Download the JAR from [Releases](https://github.com/E33EPUS/sweatyfeet/releases)
2. Drop it into `.minecraft/mods/`
3. Launch the game

---

## Gameplay

```
Wear boots → 2/4/6 min → Sweaty Feet level 1/2/3 (boots rename, sweat particles)
  ├─ Keep wearing at level 3 → Foot fungus (drain / sneeze / spread / smell when barefoot)
  ├─ Pour sweat (sweaty boot + empty bottle in offhand + sneak right-click) → sweat bottle, boots clean
  │     Fermented boots at level 3 pour a Sweat Drink (four buffs)
  └─ Soak feet: sit on a stool, take off boots, soak in a basin
        Clean water washes Sweaty Feet; add floral water to make medicinal water → cures fungus (only cure)
```

Press **J** in-game to open the config screen (Sweaty Feet / Fungus / Items & Drinks / Visuals), or read the handbook in your inventory for details.

---

## FAQ

### How do I cure foot fungus?

The only way: pour floral water into a clean basin (turns it medicinal), sit on a stool, take off your boots and soak your feet. Taking boots off, pouring sweat or sleeping won't cure it.

### How do I get rid of Sweaty Feet?

Soaking washes it away directly; taking your boots off only downgrades it over time (level 1 sticks until washed).

### How do I get a sweat bottle?

While wearing sweaty boots, hold a sweaty boot in your main hand + an empty glass bottle in your offhand, then sneak + right-click. Fermented boots (leather boots + sugar) at level 3 pour a Sweat Drink instead.

### How do I get the guide book?

You receive the "Sweaty Feet Guide" automatically when you join a world (requires Patchouli). Without Patchouli everything else still works.

### Do I need it on a server?

Recommended: fungus damage, spread and advancement logic run server-side. Single-player / LAN needs no extra setup.

### Can I put it in a modpack?

Yes, MIT license — no extra permission needed.

---

## Development

```bash
git clone https://github.com/E33EPUS/sweatyfeet.git
cd sweatyfeet
./gradlew build
```

Output: `build/libs/sweatyfeet-NeoForge-1.21.1-0.1.2.jar`

Run tests: `./gradlew test --offline -PrunTests`

---

## Feedback

Report issues at [Issues](https://github.com/E33EPUS/sweatyfeet/issues), ideally with:

- Mod version + Minecraft version + NeoForge version
- Relevant mod list
- `.minecraft/logs/latest.log`
- Screenshots / video + reproducible steps

---

## License

[MIT License](LICENSE)

Copyright &copy; 2026 E33EPUS
