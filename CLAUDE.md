# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

EyeVector is a Minecraft client-side mod, shipped for both **Fabric** and **NeoForge**. It triangulates the in-game stronghold location from the trajectories of thrown Eyes of Ender: throw one, move away, throw another, and it computes the intersection of the two flight lines (or averages three intersections in 3-point mode) and prints the result to chat.

- Minecraft: 26.1–26.2, Java 25+
- Fabric: Fabric Loader 0.19.3+, Fabric API required
- NeoForge: 26.2.0.87+ (ModDevGradle)
- Client-only mod on both loaders

## Project layout

Multi-project Gradle build, one root `settings.gradle`/`gradle.properties` shared by two independent subprojects with **duplicated** (not shared-module) logic, since each loader's event/command APIs are incompatible and the codebase is tiny enough that duplication beats an abstraction layer:

- `fabric/` — Fabric Loom build. Entry point `EyeVectorClient` (`ClientModInitializer`), commands via `net.fabricmc.fabric.api.client.command.v2`.
- `neoforge/` — ModDevGradle build. Entry point `EyeVectorMod` (`@Mod(dist = Dist.CLIENT)`), listens on `NeoForge.EVENT_BUS` for `ClientTickEvent.Post` and `RegisterClientCommandsEvent`, commands via vanilla `Commands`/`CommandSourceStack`.
- `neoforge/src/main/templates/META-INF/neoforge.mods.toml` — placeholder-expanded by the `generateModMetadata` task (mirrors what `processResources` does for `fabric.mod.json`'s `${version}`).

Both subprojects read shared version numbers (`mod_version`, `minecraft_version`, etc.) from the root `gradle.properties` — Gradle applies the root project's `gradle.properties` to all subprojects automatically.

The two `EyeVectorClient`/`EyeVectorMod` and `EyeVectorCommand` classes are near-mirrors of each other. **When fixing a bug or changing triangulation/tracking logic, apply the change to both `fabric/src/.../EyeVectorClient.java` and `neoforge/src/.../EyeVectorMod.java`.**

## Build & run commands

```
./gradlew build                  # compile + package both mod jars
./gradlew :fabric:runClient      # launch a dev client with the Fabric build loaded
./gradlew :neoforge:runClient    # launch a dev client with the NeoForge build loaded
./gradlew :fabric:genSources     # regenerate/decompile Minecraft sources for IDE navigation, if needed
```

There is no automated test suite — verification is manual, via `runClient` (throw Eyes of Ender in a test world and check chat output/commands).

`gradle.properties` pins `org.gradle.java.home` to a local JDK 25 install; if Gradle can't find a JDK, check that path first (or override with `-Dorg.gradle.java.home=...` for a one-off build on a different machine).

## Architecture (per loader)

Two classes per loader, both under `com.eyevector`:

- **`EyeVectorClient`** (Fabric, `ClientModInitializer`) / **`EyeVectorMod`** (NeoForge, `@Mod`) — all mod state and logic lives here as static fields/methods:
  - Registers the `/eyevector` command tree via `EyeVectorCommand.register(...)`.
  - Hooks the client tick (Fabric: `ClientTickEvents.END_CLIENT_TICK`; NeoForge: `ClientTickEvent.Post` on `NeoForge.EVENT_BUS`) to run the core detection/tracking loop every tick.
  - **Detection**: scans nearby entities each tick for newly-spawned `EyeOfEnder` within 3 blocks of the player's eye position (tickCount == 1) to identify a throw as the player's own, and starts an `EyeTracker` for it. A `trackedEyes` UUID set prevents double-tracking the same eye (cleared once it grows past 100 entries).
  - **`EyeTracker`** (inner class): follows one Eye of Ender across ticks, sampling its direction of travel once per tick (once it has moved > 0.1 blocks) and storing per-tick angles. It's "valid" once enough angle samples have accumulated for the configured `Precision` (`LOW`=3, `MEDIUM`=5, `HIGH`=10 samples). The final direction is a **circular mean** of the sampled angles (`atan2(mean sin, mean cos)`), not a plain average — this matters near the ±180° wraparound.
  - When a tracked eye dies/despawns, `processCompletedTracker` records its start position + averaged angle as an `EyeThrowData`. Throws closer than `minDistance` to any prior recorded throw are rejected (kept in the list, error shown) to preserve triangulation accuracy.
  - Once `measurementMode` (2 or 3) throws are recorded, `calculateWith2Points`/`calculateWith3Points` triangulate the stronghold position via line-intersection math (cross product for parallel detection; 3-point mode averages the three pairwise intersections and tolerates one or two parallel pairs). Y is unused/zeroed in the result — only X/Z matter.
  - Successful calculation clears `throwDataList`; a parallel-lines failure keeps recorded throws so the user only needs one more throw.
  - Changing `measurementMode` via command clears `throwDataList` (recording state is tied to the mode).

- **`EyeVectorCommand`** — builds the Brigadier command tree (`/eyevector mode|distance|precision|reset|status`). Fabric: `FabricClientCommandSource`/`ClientCommands`, feedback via `context.getSource().sendFeedback(...)`. NeoForge: `CommandSourceStack`/`Commands`, feedback via `context.getSource().sendSuccess(() -> ..., false)`. Calls the static setters/getters on the loader's main class. All user-facing text goes through `Component.translatable(...)` keys.

## Localization

All player-facing strings are translation keys resolved from `assets/eyevector/lang/{en_us,ko_kr}.json` — never hardcode user-facing strings in Java. The lang files are duplicated under both `fabric/src/main/resources/` and `neoforge/src/main/resources/` (identical content). When adding a new message or command feedback string, add the key to **all four** files.

## Version placeholders

- Fabric: `fabric.mod.json`'s `version` is `"${version}"`, expanded from `project.version` by the `processResources` Gradle task.
- NeoForge: `neoforge.mods.toml` lives under `src/main/templates/` and is placeholder-expanded (`${mod_version}`, `${neo_version}`, etc.) by the `generateModMetadata` task into `build/generated/sources/modMetadata`.

Don't hardcode a version in either file.
