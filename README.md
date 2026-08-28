<p align="center">
  <img src="logo.png" alt="StarShack Logo" width="600" height="337">
</p>

<h1 align="center">StarShack</h1>

<p align="center">
  <b>A modern, privacy-first Minecraft utility mod for 1.8.9 (Forge)</b><br>
  <i>Configuration-persistent · Extensible · Built on Novoline-bS / Raven-bS foundations</i>
</p>

<p align="center">
  <a href="https://github.com/vliantwo/starshack/releases"><img src="https://img.shields.io/github/v/release/vliantwo/starshack?style=flat-square&color=blue" alt="Releases"></a>
  <a href="https://github.com/vliantwo/starshack/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square" alt="License: GPLv3"></a>
  <a href="https://github.com/vliantwo/starshack/issues"><img src="https://img.shields.io/github/issues/vliantwo/starshack?style=flat-square" alt="Issues"></a>
</p>

---

## ✨ Features

- **🛡️ Privacy-First** — No telemetry, no analytics, no account data sent anywhere. What happens on your machine stays
  on your machine.

- **💾 Configuration-Persistent** — All settings auto-save on change (`saved` dirty-flag → tick-based flush)
  and re-load on startup. Change CPS, toggle a module, restart — your config is exactly as you left it. **No more "
  settings reset after every launch."**

- **🔌 Extensible** — Full scripting API + a clean module/setting architecture (`Setting` base class, `ModeSetting`,
  `SliderSetting`, `ButtonSetting`, …)
  makes adding new modules straightforward.

- **⚔️ Combat** — KillAura, AutoClicker (Vape V4-style: Normal / Extra / Extra+ randomization, Trigger modes, Jitter,
  Break-blocks coordination), Reach, Velocity, Criticals, AimAssist, BackTrack, and more.

- **🎨 Render** — Clean, customizable HUD with module list, TargetHUD, ESP (player / mob / item / chest / bed), Tracers,
  TNT timer, Trajectories, Chams, Nametags, Break progress, and a polished ClickGUI.

- **🚀 Movement** — Speed, Fly, BHop, LongJump, Sprint, WTap, NoSlow, Timer, Velocity, Strafe, Teleport, and movement-fix
  helpers.

- **🧱 World / Player** — Scaffold, FastMine, FastPlace, AutoTool, AutoSwap, NoFall, SafeWalk, GhostHand, AntiFireball,
  Freecam, and many more.

- **🎮 Minigames** — Dedicated modules for BedWars, SkyWars, MurderMystery, Duels, Sumo, SpeedBuilders, WoolWars,
  BridgeInfo, AutoRequeue, and stats tracking.

- **💬 Other** — AntiAFK, ChatBypass, FakeChat, NameHider, Relationships (player relations manager), HideWindow,
  Disabler, IRC, and an Anticheat utility.

> **NOTE:** StarShack is a *utility mod* intended for **private servers,
> single-player, and learning/modding research**. Use on public servers
> (especially competitive ones) is **at your own risk** and may violate
> their terms of service. The author (s) are not responsible for any bans
> or consequences.

---

## 🚀 Getting Started

### Prerequisites

| Requirement   | Version                                  |
|---------------|------------------------------------------|
| **Java JDK**  | 8 (1.8) — *other versions will NOT work* |
| **Minecraft** | 1.8.9 (Forge)                            |
| **OS**        | Windows / Linux / macOS                  |
| **RAM**       | ≥ 4 GB recommended                       |

### Clone & Build

```bash
# 1. Clone the repository
git clone https://github.com/vliantwo/starshack.git
cd starshack

# 2. Build the mod (produces build/libs/*.jar)
./gradlew build
#  (Windows: gradlew.bat build)

# 3. Grab the jar
#  → build/libs/starshack-<version>.jar
```

### Run in IDEA (recommended)

1. Open **IntelliJ IDEA** → `File → Open` → select the `starshack` folder.
2. Wait for Gradle import to finish (watch the progress bar bottom-right).
3. Run the **`runClient`** Gradle task (`Gradle` tool window → `starshack → Tasks → forgegradle → runClient`).
4. The game launches with StarShack pre-loaded; open the GUI with the configured keybind (default `RSHIFT`).

> **TIP:** On first run, IDEA may prompt to **import the Gradle project** —
> accept and let it sync. If `runClient` isn't listed, run `./gradlew genIntellijRuns`
> once, then refresh the Gradle tool window.

### Install into an existing Minecraft instance

1. Build the jar (see above).
2. Drop the jar into your `.minecraft/mods/` folder.
3. Launch Minecraft with the matching **Forge 1.8.9** profile.
4. StarShack loads automatically — no config needed to start.

---

## ⚙️ Configuration

StarShack saves **all module settings** so they persist across restarts.

| Item                   | Location                                     |
|------------------------|----------------------------------------------|
| **Profiles directory** | `.minecraft/starshack/profiles/`             |
| **Default profile**    | `.minecraft/starshack/profiles/default.json` |
| **Auto-save**          | On every setting change (tick-based flush)   |
| **Auto-load**          | On game startup, before the main menu        |

- **Change a setting** (e.g. AutoClicker CPS) → it's written to
  `default.json` within ~1–2 seconds, **no manual save button needed**.
- **Restart the game** → settings are restored exactly as you left them.
- **Multiple profiles** are supported; switch via the profile command (`/p load <name>`) or the GUI.

> **Migrating from a `keystrokes/` folder?**
> If you have an older config under `.minecraft/keystrokes/`, simply
> **rename that folder to `starshack`** — the internal `profiles/` layout
> is identical, so your existing `default.json` carries over unchanged.

---

## 📁 Project Structure

```
starshack/
├── build.gradle                  # ForgeGradle build script
├── gradlew / gradlew.bat        # Gradle wrapper
├── settings.gradle
├── README.md
├── LICENSE
├── CONTRIBUTING.md
│
├── src/main/java/starshack/
│   ├── Stars.java                # Main mod class (@Mod), init & tick
│   ├── module/
│   │   ├── Module.java           # Base module + categories
│   │   ├── ModuleManager.java    # Registers all modules
│   │   └── impl/
│   │       ├── combat/           # KillAura, AutoClicker, Velocity, …
│   │       │   └── autoclicker/  # Vape V4-style auto-clicker
│   │       ├── movement/         # Speed, Fly, BHop, …
│   │       ├── render/           # ESP, Tracers, HUD, …
│   │       ├── player/           # Scaffold, FastMine, …
│   │       ├── world/            # …
│   │       ├── minigames/        # BedWars, SkyWars, …
│   │       ├── other/            # AntiAFK, ChatBypass, …
│   │       └── client/           # Settings, Gui, Relationships
│   ├── setting/                  # Setting base + Slider/Button/Mode
│   ├── clickgui/                 # ClickGUI (Novoline-style)
│   ├── event/                    # Custom events
│   ├── mixin/                    # Mixin injection (FullBody, etc.)
│   ├── helper/                   # RotationHelper, MouseHelper, …
│   ├── utility/                  # Profile, Reflection, Font, …
│   └── script/                   # Scripting API
│
├── src/main/resources/
│   ├── assets/starshack/         # Textures, cape images, lang
│   └── mcmod.info
│
└── .github/workflows/build.yml    # CI: auto-build on push/PR
```

---

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) first.

1. **Fork** the repo and create a branch:
   ```bash
   git checkout -b feat/my-new-module
   ```
2. **Code** your changes. Follow the existing style:
    - New modules extend `Module` and are registered in `ModuleManager.register()`.
    - New settings use the `Setting` subclasses (`SliderSetting`, `ButtonSetting`, `ModeSetting`).
    - Keep mixins minimal and document any `@Overwrite`.
3. **Test** by running `./gradlew build` (CI must pass).
4. **Commit** with a clear message:
   ```bash
   git commit -m "feat(combat): add new aim-assist mode"
   ```
5. **Push** and open a **Pull Request** against `main`.

> **Code style:** 4-space indent, braces on new lines (Java convention),
> meaningful names, no dead code. Run `./gradlew build` before pushing
> to catch compilation errors early.

---

## 📜 Legal & License

StarShack is free software, licensed under the **GNU General Public License v3.0 (GPLv3)**.

> **What this means:**
> - You are free to use, copy, modify, and redistribute StarShack.
> - Any distribution (including precompiled jars) **must** also be
>   released under GPLv3 with full source available.
> - You must preserve the original copyright and license notices.
> - The authors are NOT liable for any damages arising from use.

See the full license text in [LICENSE](LICENSE).

**SPDX identifier:** `GPL-3.0-only`

---

## 🙏 Credits & Acknowledgements

StarShack is built upon the foundations laid by the **Raven-bS / Novoline-bS**
family of Minecraft utility mods. Huge thanks to the original developers:

- **[Novoline-bS](https://github.com/Ij1chi-Nijika/Novoline-bS)**
  by [Ij1chi-Nijika](https://github.com/Ij1chi-Nijika) — the base this project was forked from. Much of the module and
  setting architecture, ClickGUI, and Mixin work originates here.

- **Raven-bS / Raven-XD** — the original "Raven" lineage that Novoline-bS itself extended. Core concepts (esp, aura,
  scaffolds, etc.) trace back to this community.

- **[LiquidBounce](https://github.com/CCBlueX/LiquidBounce)** — an inspiration for clean module architecture and
  scripting design.

- **Minecraft Forge / ForgeGradle** — the modding framework this project builds on top of.

- **SpongePowered Mixin** — the bytecode transformation library used for runtime patches.

This project **inherits the GPLv3 license** from its predecessors and **preserves all applicable copyright notices**. If
you believe any attribution is missing, please open an issue and it will be corrected promptly.

---

**StarShack** — *Privacy-First. Configuration-Persistent. Yours to extend.*
