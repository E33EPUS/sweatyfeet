[简体中文](README.md) | [English](README_EN.md)

<h1 align="center">Sweaty Feet</h1>

<p align="center">
  <em>Wear boots too long and your feet stink. Take them off and wash up!</em>
</p>

<p align="center">
  <img alt="MC" src="https://img.shields.io/badge/MC-1.21.1-green">
  <img alt="Loader" src="https://img.shields.io/badge/Loader-NeoForge-orange">
  <img alt="Side" src="https://img.shields.io/badge/Side-Client%20%2B%20Server-blue">
  <img alt="Java" src="https://img.shields.io/badge/Java-21%2B-yellow">
  <img alt="Version" src="https://img.shields.io/badge/Version-0.1.6-informational">
  <img alt="License" src="https://img.shields.io/badge/License-MIT-brightgreen">
</p>

> Version history: see [CHANGELOG.md](CHANGELOG.md)

---

## Table of Contents

- [Overview](#overview)
- [Gameplay at a Glance](#gameplay-at-a-glance)
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

## Gameplay at a Glance

- **Three-tier sweat**: 2/4/6 minutes of cumulative boot-wearing; tier 2 slides, tier 3 nauseates while held and pollutes the ground; the Nether doubles the rate; pouring does not cure it — only soaking does
- **Tier-3 transmutation**: at tier 3, poured sweat transmutes into five flavored bottles by boot material — different colors and bonus effects, the core of the collection
- **Fungus infection**: keep wearing boots past tier 3 and you catch a fungus — damage over time, sneezes, barefoot stink, contagion; the only cure is floral-water soaking
- **Soaking & washing**: sit on a stool and soak for 15 s to clear sweaty feet and cure fungus; throw sweaty boots into water for 15 s to wash them clean
- **Fermented drink**: wearing Fermented Boots ferments your sweat; tier-3 pouring yields a positive buff drink (Speed / Jump Boost / Strength / Luck)

> Full mechanics, values and recipes (Flavor Almanac, per-tier effects) live in the in-game **Guidebook** — press **H**. This page is just the gameplay overview.

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

Press **J** to open the config screen (or the ⚙ button in the inventory, or Mods list → Config). Four categories:

- **Sweat**: wear timings (120/240/360 s), degradation, sliding, particles
- **Fungus**: toggle, damage, contagion range/interval, stink
- **Bottles & Drinks**: effect durations (nausea/poison/buff), boot washing, drink duration
- **Visuals**: soak undress, skin tint, eyedropper, debug toggles

> Server config is authoritative: in multiplayer the server's config wins (stored in `config/sweatyfeet-server.toml`).

---

## Guidebook

You get a **Sweaty Feet Guidebook** (Patchouli) on entering the world: 7 chapters, 29 entries — mechanics overview, items & drinks, recipes, advancements, visuals, config. Every mechanic detail lives in the book; press H in-game.

---

## Requirements

- Minecraft **1.21.1**
- **NeoForge** 21.1.x (Java 21)
- **player-animator** ≥ 2.0.4 (soaking animation prerequisite, [Modrinth](https://modrinth.com/mod/playeranimator))
- **Patchouli** (optional — installs the guidebook; gameplay works without it)

## Installation

1. Download `sweatyfeet-NeoForge-1.21.1-0.1.6.jar` from the `Neoforge-1.21.1` branch
2. Drop it into `.minecraft/mods/`
3. Launch (install on **both** client and server)

---

## Modpacks & Servers

- **Modpack-friendly** (MIT, credit the source)
- Install on both client and server (gameplay logic runs server-side, animation/rendering client-side)
- **Server config is authoritative**: in multiplayer the server's `config/sweatyfeet-server.toml` wins; client config only affects local display (e.g. skin tint)
- LAN / offline servers: undress automatically falls back to the player's default skin, so it works offline too
- Failed skin downloads auto-retry with a 60 s cooldown — no restart needed
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
One potion color per flavor (Sulfurous = golden, Crisp = cyan, ...); the color matches the bonus effect — see the Flavor Almanac in the guidebook.

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

- Repo: `github.com/E33EPUS/sweatyfeet`, default branch `Neoforge-1.21.1` (Fabric port on `Fabric-1.21.1`)
- Toolchain: JDK 21 + ModDevGradle
- Build + test: `gradlew build test -PrunTests`
- Tests: 26 unit tests (sweat degradation state machine, pour tier/flavor branches, soak-skin resolution, ...), JUnit 5
- Proxy 26561 connection refused? `gradlew build -Dhttp.proxyHost= -Dhttps.proxyHost= -Dhttp.proxyPort= -Dhttps.proxyPort=`

## Feedback

GitHub Issues: `github.com/E33EPUS/sweatyfeet/issues` (attach latest.log and the crash report).

## License

MIT
