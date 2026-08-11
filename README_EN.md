[简体中文](README.md) | [English](README_EN.md)

<h1 align="center">Sweaty Feet</h1>

<p align="center">
  <em>Wear boots too long and your feet stink. Take them off and wash up!</em>
</p>

<p align="center">
  <img alt="MC" src="https://img.shields.io/badge/MC-1.21.1-green">
  <img alt="Loader" src="https://img.shields.io/badge/Loader-Fabric-blueviolet">
  <img alt="Side" src="https://img.shields.io/badge/Side-Client%20%2B%20Server-blue">
  <img alt="Java" src="https://img.shields.io/badge/Java-21%2B-yellow">
  <img alt="Version" src="https://img.shields.io/badge/Version-0.1.5-informational">
  <img alt="License" src="https://img.shields.io/badge/License-MIT-brightgreen">
</p>

---

## Changelog

### 0.1.5
- Sweat bottles now carry a food component: the tooltip shows "Restores X Hunger" (1 point, doubled to 2 by the Rich flavor), exactly like vanilla food
- Drink item tooltips trimmed: effects are auto-shown by the potion component, flavor lore stays as pure flavor text, details moved to the guidebook
- Guidebook updated: transmutation, hunger, fermented-boots armor

### 0.1.4
- **Tier-3 transmutation**: tiers 1-2 always pour plain bottles; at tier 3 the sweat transmutes and the bottle takes on the flavor of the boot material (no level in the name, full tier-3 effects kept); boots from other mods still pour plain tier-3 bottles (compat)
- Flavored bottles are tinted per flavor (Sulfurous = golden potion, Crisp = cyan, ...); the drink is wheat-gold
- Pouring now goes through a custom network packet (use-event cancellation meant the server never heard about it); creative tab has all variants; fixed basin/stool collision boxes (they were one chunk big)

### 0.1.3
- ⚙ config button on the inventory screen (no prerequisites); I/II/III tier badge on the sweaty-feet HUD icon
- All creative-tab variants; removed the dropped "thrown bottle debuff" config

## Table of Contents

- [Overview](#overview)
- [Gameplay](#gameplay)
- [Item Guide](#item-guide)
- [Configuration](#configuration)
- [Guidebook](#guidebook)
- [Requirements](#requirements)
- [Installation](#installation)
- [Modpacks & Servers](#modpacks--servers)
- [FAQ](#faq)
- [Development](#development)
- [Feedback](#feedback)
- [License](#license)
- [Screenshots (WIP)](#screenshots-wip)

---

## Overview

Wearing boots too long makes your feet sweat: three escalating tiers, and a tier-3 foot can even catch a fungus — damage, sneezing, and it spreads to friends. Pour the sweat into a glass bottle to make Sweat Bottles (at tier 3 the sweat transmutes and takes on the flavor of the boot material), ferment rich Sweat Drinks, and cure everything by sitting on a stool and soaking your feet.

**Highlights**

- **Three-tier sweat**: 2/4/6 minutes of cumulative boot-wearing; higher tiers hurt more (tier 2 = sliding, tier 3 = nausea while held + pollutes the ground); the Nether's heat doubles the rate
- **Tier-3 transmutation**: at tier 3, poured sweat transmutes into five flavored bottles by boot material — different colors, different bonus effects, the heart of the "From Funky to Rank" collection
- **Fungus infection**: keep wearing boots past tier 3 and you catch a fungus — damage over time, sneeze particles, barefoot stink, contagion; the only cure is floral-water soaking
- **Soaking animation**: sit on a stool, take off your boots, soak your feet — the lower-body skin shows your skin tone (eyedropper picker + cross-client sync)
- **Prank material**: hand sweat bottles to friends (nausea at tier 2, poison at tier 3), and bucket up the basin water to gift it

---

## Gameplay

### Three-tier sweat

Wear any boots long enough and they "sweatify" — renamed to "〈Player's〉Sweaty 〈Original Name〉 (Tier X)", with sweat particles and the Sweaty Feet effect:

| Tier | Wear time | Effects |
| --- | --- | --- |
| 1 | 120 s | Sweat particles; pouring makes a tier-1 bottle |
| 2 | 240 s | + sliding (reduced friction, ice-like) |
| 3 | 360 s | + nausea while holding the sweaty boots; dropping them spreads green stink particles and nauseates nearby players (including you) |

- **Nether**: the heat doubles the sweat rate (prank easter egg)
- **Taking the boots off**: the effect stays; it degrades one tier every 60 s and vanishes from tier 1
- **Pouring does not cure the effect** — only soaking does

### Pouring & transmutation

Sweaty boots in the main hand + empty glass bottle in the offhand + sneak + right-click → boots restored, bottle produced.

**Transmutation (0.1.4+)**:

| Sweat tier | Result |
| --- | --- |
| 1-2 | Plain bottle (tier I/II in the name, no flavor) |
| 3 + vanilla/this-mod boots | **Flavored bottle** (transmuted: no tier in the name, full tier-3 effects + flavor bonus, tinted per flavor) |
| 3 + other mods' boots | Plain tier-3 bottle (tier III in the name, unknown-material compat) |
| 3 + Fermented Boots | Sweat Drink (positive buffs, below) |

### Flavor Almanac

| Boot material | Flavor | Bottle color | Bonus effect |
| --- | --- | --- | --- |
| Leather | Rich | Brown | Double hunger restored (2 points) |
| Iron | Rusty | Rust brown | Weakness for 15 s (the only negative flavor — cheap material, cheap sweat) |
| Gold | Gilded | Bright yellow | Luck for 60 s |
| Diamond | Crisp | Cyan | Resistance for 20 s |
| Netherite | Sulfurous | Golden orange (fire-resistance potion color) | Fire Resistance for 30 s |

Collect all five flavors to unlock the "From Funky to Rank" advancement.

### Bottle effects

| Tier | Drinking |
| --- | --- |
| 1 | Restores 1 hunger (half a shank), some saturation |
| 2 | + Nausea for 10 s |
| 3 | + Poison for 3 s (flavored bottles stack the flavor bonus on top) |

Effects and hunger show right on the bottle's tooltip, exactly like vanilla potions.

### Fungus infection

Keep wearing boots 60 s past tier 3 → fungus infection:

- 1 damage every 3 s (magic damage, ignores armor)
- Sneeze particles (toggleable)
- **Contagious**: players standing within 3 blocks catch it (checked every 3 s); barefoot stink nauseates within 5 blocks
- **Only cure**: pour Floral Water into a basin with water → medicinal wash water → soak your feet

### Soaking & washing

1. Place a **Wash Basin**, fill it with a **water bucket** (or scoop with an empty bucket; potions go in directly)
2. Sit on a **Stool** (right-click, boots come off automatically)
3. Soak barefoot in the water for 15 s → sweat cleared, basin turns dirty (scoop it with an empty bucket for a Wash Water Bucket)
4. Floral Water (water bottle + any two small flowers) poured into the basin → medicinal wash water that cures fungus

**Washing boots**: throw sweaty boots into water for 15 s and they wash clean (configurable).

### Fermented Boots & Sweat Drink

- **Craft**: leather boots + sugar
- Wearing them ferments your sweat; **tier-3 pouring** produces "〈Player's〉 Sweat Drink" (not a bottle)
- Armor: 1 (same as leather), shown on the tooltip
- **Drink**: 30 s of Speed + Jump Boost + Strength + Luck

---

## Item Guide

| Item | How to get | Use |
| --- | --- | --- |
| Sweat Bottle (tiers 1/2/3 + 5 flavors) | Pour from sweaty boots | Drink (hunger/nausea/poison/flavor effects), pranks |
| Sweat Drink | Pour from tier-3 fermented boots | Four positive buffs |
| Fermented Boots | Leather boots + sugar | Brews the drink |
| Floral Water | Water bottle + any two small flowers | Cures fungus when poured into the basin |
| Diluted Floral Water | Scooped medicinal water | Same, diluted |
| Wash Water Bucket | Scoop the dirty basin water | Pranks (gift it) |
| Wash Basin / Stool | Crafting | Soaking set |

---

## Configuration

Press **J** to open the config screen (or the ⚙ button in the inventory, or ModMenu → Config). Four categories:

- **Sweat**: wear timings (120/240/360 s), degradation, sliding, particles
- **Fungus**: toggle, damage, contagion range/interval, stink
- **Bottles & Drinks**: effect durations (nausea/poison/buff), boot washing, drink duration
- **Visuals**: soak undress, skin tint, eyedropper, debug toggles

> Server config is authoritative: in multiplayer the server's config wins (stored in `config/sweatyfeet-server.json`).

---

## Guidebook

You get a **Sweaty Feet Guidebook** (Patchouli) on entering the world: 7 chapters, 29 entries — mechanics overview, items & drinks, recipes, advancements, visuals, config. Every mechanic detail lives in the book; press H in-game.

---

## Requirements

- Minecraft **1.21.1**
- **Fabric Loader** ≥ 0.15.11 (Java 21)
- **Fabric API** ≥ 0.116.0
- **player-animator** ≥ 2.0.4 (soaking animation prerequisite)
- **ModMenu** (optional, recommended — one of the config screen entries)
- **Patchouli** (optional — installs the guidebook; gameplay works without it)

## Installation

1. Download `sweatyfeet-Fabric-1.21.1-0.1.5.jar` from the `Fabric-1.21.1` branch
2. Drop it into the pack's `.minecraft/mods/` (e.g. 1.21.1-CCB)
3. Launch (install on **both** client and server)

---

## Modpacks & Servers

- **Modpack-friendly** (MIT, credit the source)
- Install on both client and server (gameplay logic runs server-side, animation/rendering client-side)
- The server config file is authoritative; client config only affects local display (e.g. skin tint)
- Config is reachable without ModMenu (J key / inventory ⚙)
- No known conflicts with common mods (e33chat bubbles, Modern UI, Xaero's maps, ...)

---

## FAQ

**How do I cure fungus?**
Pour Floral Water into a basin with water → medicinal wash water → sit and soak. No other way (short of commands).

**How do I clear sweaty feet?**
Soak for 15 s. Pouring doesn't clear it; taking the boots off only degrades it.

**How do I get Sweat Bottles?**
Sweaty boots in the main hand + empty glass bottle in the offhand + sneak + right-click. The boots must be sweaty (2+ minutes worn).

**Why did my tier 1-2 bottle have no flavor?**
That's the transmutation mechanic (0.1.4+): tiers 1-2 pour plain bottles only. **Tier-3** pouring transmutes the bottle by boot material.

**What do the bottle colors mean?**
One potion color per flavor (Sulfurous = golden, Crisp = cyan, ...); the color matches the bonus effect — see the Flavor Almanac above.

**Do Fermented Boots have armor?**
Yes — 1 point, same as leather, shown on the tooltip.

**How much hunger does a Sweat Bottle restore?**
1 point (half a shank); the Rich flavor doubles it to 2.

**How do I get the guidebook?**
It's given on entering the world (with Patchouli installed); drop extras.

**Do I need the mod on the server?**
Yes. It's a client + server mod; without the server side the gameplay logic won't run.

**Can I put it in a modpack?**
Yes — MIT license, credit the source.

**J does nothing?**
The key may be taken by another mod — rebind it in Options → Controls, or use the ⚙ button in the inventory.

---

## Development

- Repo: `github.com/E33EPUS/sweatyfeet`, branch `Fabric-1.21.1` (ported from the NeoForge build, Yarn mappings)
- Toolchain: JDK 21 + Loom 1.7.4 (player-animator via local libs-repo)
- Build + test: `gradlew build test`
- Tests: 26 unit tests (sweat degradation state machine, pour tier/flavor branches, soak-skin resolution, ...; 3 needing a full Loader environment are kept on the NeoForge side)
- Proxy 26561 connection refused? `gradlew build -Dhttp.proxyHost= -Dhttps.proxyHost= -Dhttp.proxyPort= -Dhttps.proxyPort=`

## Feedback

GitHub Issues: `github.com/E33EPUS/sweatyfeet/issues` (attach latest.log and the crash report).

## License

MIT

---

## Screenshots (WIP)

> Placeholder for screenshots: sweat particles / transmuted flavored bottles / fungus infection / soaking animation / config screen
