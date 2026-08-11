[简体中文](README.md) | [English](README_EN.md)

<h1 align="center">Sweaty Feet</h1>

<p align="center">
  <em>Wearing boots too long makes your feet stinky — take them off and wash up!</em>
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

## Table of Contents

- [About](#about)
- [Requirements](#requirements)
- [Installation](#installation)
- [Gameplay Loop](#gameplay-loop)
- [FAQ](#faq)
- [Development](#development)
- [Feedback](#feedback)
- [License](#license)

---

## About

Wearing boots too long makes your feet sweat. Sweaty Feet escalates through three levels, and at level 3 your stinky feet can catch a foot fungus — health drain, sneezing, and it spreads to your friends. Pour the sweat into glass bottles (each carries a flavor effect based on the boot material), brew buff drinks from fermented boots, and cure it all by soaking your feet on a stool.

---

## Requirements

| Dependency | Type | Notes |
|---|---|---|
| Minecraft | Required | 1.21.1 |
| Fabric Loader | Required | 0.16+ |
| Fabric API | Required | 0.116.x+ |
| Java | Required | 21+ |
| Patchouli | Optional | Receives the Sweaty Feet Guide on join; everything else works without it |
| player-animator | Required | Soaking animation (stool); mod will not load without it |

> This is the **Fabric** branch (`fabric-1.21.1`). The NeoForge version lives on the default branch (`neoforge-1.21.1`) with identical gameplay; the jars are not interchangeable.

---

## Installation

1. Download `sweatyfeet-Fabric-1.21.1-*.jar` from [Releases](https://github.com/E33EPUS/sweatyfeet/releases)
2. Put it in `.minecraft/mods/` (Fabric Loader + Fabric API required)
3. Launch the game

---

## Gameplay Loop

```
Wear boots → 2/4/6 minutes → Sweaty Feet level 1/2/3 (boots renamed, sweat particles)
  ├─ 60s more at level 3 → foot fungus (health drain / sneezing / contagion / stink aura when barefoot)
  ├─ Pour sweat (sweaty boot + empty bottle in offhand + sneak right-click) → sweat bottle, boots clean
  │     Bottles carry a flavor effect by boot material (see FAQ); fermented boots at level 3 pour a Sweat Drink (four buffs)
  └─ Wash up: sit on a stool + barefoot + soak in the basin
        Clean water washes sweaty feet; floral water turns the basin medicinal → cures fungus (only cure)
```

Press **J** in-game for the config screen (sweat / fungus / items & drinks / visuals), or flip through the guide book.

---

## FAQ

### How do I cure the fungus?

The only cure: pour a floral water into a clean-water basin (turns it medicinal), sit on a stool, take off your boots and soak. Taking boots off, pouring sweat, or sleeping won't cure it.

### How do I get rid of sweaty feet?

Washing directly clears them; taking boots off only degrades them slowly (level 1 stays — washing is the only full clear).

### How do I get a sweat bottle?

While wearing sweaty boots, hold a sweaty boot in your main hand + an empty glass bottle in your offhand, then sneak + right-click. Fermented boots (leather boots + sugar) at level 3 pour a Sweat Drink instead.

### What are the sweat bottle flavors?

Based on the boot material: leather is Rich (double hunger), iron Rusty (Weakness), gold Gilded (Luck), diamond Crisp (Resistance), netherite Sulfurous (Fire Resistance). Collect all five to unlock the "From Funky to Rank" advancement.

### How do I get the guide book?

You receive one automatically on join if Patchouli is installed. Everything else works without it.

### Do I need the mod on the server?

Recommended: fungus damage, contagion and advancement checks run server-side. Singleplayer / LAN needs no extra setup.

### Can I put this in a modpack?

Yes, MIT licensed — no permission needed.

---

## Development

```bash
git clone -b fabric-1.21.1 https://github.com/E33EPUS/sweatyfeet.git
cd sweatyfeet
./gradlew build
```

Output: `build/libs/sweatyfeet-Fabric-1.21.1-0.1.2.jar`

Run tests: `./gradlew test` (3 ItemStack-registry-dependent cases are @Disabled under plain Fabric JUnit; the rest run)

---

## Feedback

Open an [Issue](https://github.com/E33EPUS/sweatyfeet/issues) with:

- Mod version + Minecraft version + Fabric Loader / Fabric API version
- Mod list
- `.minecraft/logs/latest.log`
- Screenshot / video + reproduction steps

---

## License

[MIT License](LICENSE)

Copyright &copy; 2026 E33EPUS
