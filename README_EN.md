[简体中文](README.md) | [English](README_EN.md)

<h1 align="center">Sweaty Feet</h1>

<p align="center">
  <em>Wear boots too long and your feet get sweaty! A prank-focused survival mod</em>
</p>

<p align="center">
  <img alt="MC" src="https://img.shields.io/badge/MC-1.21.1-green">
  <img alt="Loader" src="https://img.shields.io/badge/Loader-NeoForge-orange">
  <img alt="Side" src="https://img.shields.io/badge/Side-Client%20%2B%20Server-blue">
  <img alt="Java" src="https://img.shields.io/badge/Java-21%2B-yellow">
  <img alt="Version" src="https://img.shields.io/badge/Version-0.1.0-informational">
  <img alt="License" src="https://img.shields.io/badge/License-MIT-brightgreen">
</p>

Sweaty Feet is a prank-focused survival mod: wearing boots too long makes your feet sweaty, progressing through three levels of sweaty feet. Level 3 leads to a fungal infection that deals damage, spreads to nearby players, and makes you sneeze. Pour the sweat into a glass bottle to craft tiered Sweat Bottles, then brew them into buff drinks. All mischief, no seriousness - perfect for messing with friends on a server.

---

## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Gameplay Loop](#gameplay-loop)
- [Sweaty Feet Levels](#sweaty-feet-levels)
- [Fungal Infection](#fungal-infection)
- [Sweat Bottles & Drinks](#sweat-bottles--drinks)
- [Configuration](#configuration)
- [Compatibility](#compatibility)
- [Known Limitations](#known-limitations)
- [Development & Build](#development--build)
- [Feedback](#feedback)
- [License](#license)

---

## Requirements

| Dependency | Type | Notes |
|---|---|---|
| Minecraft | Required | 1.21.1 |
| Java | Required | 21+ |
| NeoForge | Required | 21.1.x |
| Cloth Config | Optional | v15 (works without it, just no config GUI) |

---

## Installation

1. Download the JAR from [Releases](https://github.com/E33EPUS/sweatyfeet/releases)
2. Put it into `.minecraft/mods/`
3. Launch the game

---

## Quick Start

1. Wear any boots (leather, iron, diamond, netherite, ...)
2. Keep wearing them for **2 minutes**: the boots become "Sweaty xxx" with Sweaty Feet level 1
3. Hold the sweaty boots + glass bottle in offhand + **crouch + right-click**: pour the sweat out! The bottle becomes a Sweat Bottle and the boots are restored
4. Craft buff drinks: Sweat Bottle + sugar / gold nugget in a crafting table

---

## Gameplay Loop

```
Wear boots → 2/4/6 min → Sweaty Feet level 1/2/3 (boots sweatified, renamed, sweat particles)
  ├─ Take boots off → timer resets, debuffs expire naturally
  ├─ Keep wearing 30s past level 3 → fungal infection (slow + damage + sneeze + contagious)
  └─ Pour sweat (crouch + right-click + offhand glass bottle) → Sweat Bottle + boots restored
        ↓ Bottle tier follows current sweaty-feet level (I / II / III)
        Tier I: restores half a hunger shank
        Tier II: + Nausea 10s
        Tier III: + Poison 3s
        ↓ Crafting table
        Speed Drink (+sugar) / Strength Drink (+gold nugget): +30s buff
```

---

## Sweaty Feet Levels

| Level | Trigger | Effect |
|---|---|---|
| 1 | 2 min worn | Water particles only; boots sweatified & renamed |
| 2 | 4 min worn | **Slide** (retains momentum after releasing keys; ice-like but easier to control) |
| 3 | 6 min worn | Slide + Slowness |

- Works with any item in the `foot_armor` tag (vanilla + other mods' boots)
- Sweatifying does not change the item: icon, attributes, enchantments and durability are kept - only a data component + rename are applied
- Custom player-given boot names are stored and restored after pouring

---

## Fungal Infection

- Triggers 30s after reaching level 3 while still wearing boots
- Constant -15% movement speed + 1 magic damage every 3 seconds (ignores armor, can be fatal) + sneeze particles
- **Contagious**: standing near an infected player spreads it, chain-reaction style
- Taking boots off lets the debuff expire naturally; without treatment it can kill you

---

## Sweat Bottles & Drinks

| Item | How to get | Effect when drunk |
|---|---|---|
| Sweat Bottle I | Pour at sweaty-feet level 1 | Restores half a hunger shank |
| Sweat Bottle II | Pour at level 2 | Hunger + Nausea 10s |
| Sweat Bottle III | Pour at level 3 | Hunger + Nausea + Poison 3s |
| Speed Drink | Bottle + sugar | Speed I × 30s |
| Strength Drink | Bottle + gold nugget | Strength I × 30s |

- Throw a bottle while crouching: hit target gets 5s of Sweaty Feet (prank your friends)
- All drinks use the vanilla potion-style: drink animation, glug sound, returns an empty glass bottle

---

## Configuration

In-game: **Mods list → Sweaty Feet → Config button** (requires Cloth Config), or edit `config/sweatyfeet.json` directly.

| Category | Options |
|---|---|
| Timing | Seconds for level 1/2/3 |
| Fungus | Enable/delay/duration, damage toggle & interval, contagion toggle/range/interval |
| Durations | Sweaty-feet effect, throw debuff, poison/nausea, drink buff |
| Movement | Slide toggle, momentum retention percentage |
| Visual | Sweat/sneeze particles, particle scale |
| Debug | Show wear ticks, force fungus |

---

## Compatibility

- Cold Sweat (temperature system) does not conflict gameplay-wise - it handles body temperature, we handle stinky feet; both act on boots though, test coexistence in a modpack
- Crafting recipes use the vanilla JSON recipe system and can be overridden by resource packs

---

## Known Limitations

- Dedicated servers read default config values (Cloth Config registers client-side only); single-player and LAN work fine
- Won't load on NeoForge below 21.1 (mods.toml version range)
- v0.1 values are placeholders; slide feel and fungus pacing should be tuned after playtesting

---

## Development & Build

```bash
# Compile & package
gradlew build
# Output: build/libs/sweatyfeet-NeoForge-1.21.1-0.1.0.jar

# Unit tests (every mechanic has a regression test)
gradlew test -PrunTests
```

**Windows gotcha**: if the global gradle proxy `127.0.0.1:26561` is not running, build with `-Dhttp.proxyHost= -Dhttps.proxyHost= -Dhttp.proxyPort= -Dhttps.proxyPort=` or disable the proxy in the user-level `gradle.properties`.

---

## Feedback

GitHub Issues: https://github.com/E33EPUS/sweatyfeet/issues

---

## License

MIT License, Copyright (c) 2026 E33EPUS
