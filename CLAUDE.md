# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

EyeVector is a Minecraft Fabric client-side mod. It triangulates the in-game stronghold location from the trajectories of thrown Eyes of Ender: throw one, move away, throw another, and it computes the intersection of the two flight lines (or averages three intersections in 3-point mode) and prints the result to chat.

- Minecraft: 26.1–26.2, Fabric Loader 0.19.3+, Fabric API required, Java 25+
- Client-only mod: the single entrypoint is registered under `entrypoints.client` in `fabric.mod.json`, not `main`

## Build & run commands

```
./gradlew build          # compile + package the mod jar
./gradlew runClient       # launch a dev Minecraft client with the mod loaded (primary way to manually test changes)
./gradlew genSources      # regenerate/decompile Minecraft sources for IDE navigation, if needed
```

There is no automated test suite — verification is manual, via `runClient` (throw Eyes of Ender in a test world and check chat output/commands).

`gradle.properties` pins `org.gradle.java.home` to a local JDK 25 install; if Gradle can't find a JDK, check that path first.

## Architecture

Two classes, both under `com.eyevector`:

- **`EyeVectorClient`** (`ClientModInitializer`) — all mod state and logic lives here as static fields/methods:
  - Registers the `/eyevector` command tree via `EyeVectorCommand.register(...)`.
  - Hooks `ClientTickEvents.END_CLIENT_TICK` to run the core detection/tracking loop every tick.
  - **Detection**: scans nearby entities each tick for newly-spawned `EyeOfEnder` within 3 blocks of the player's eye position (tickCount == 1) to identify a throw as the player's own, and starts an `EyeTracker` for it. A `trackedEyes` UUID set prevents double-tracking the same eye (cleared once it grows past 100 entries).
  - **`EyeTracker`** (inner class): follows one Eye of Ender across ticks, sampling its direction of travel once per tick (once it has moved > 0.1 blocks) and storing per-tick angles. It's "valid" once enough angle samples have accumulated for the configured `Precision` (`LOW`=3, `MEDIUM`=5, `HIGH`=10 samples). The final direction is a **circular mean** of the sampled angles (`atan2(mean sin, mean cos)`), not a plain average — this matters near the ±180° wraparound.
  - When a tracked eye dies/despawns, `processCompletedTracker` records its start position + averaged angle as an `EyeThrowData`. Throws closer than `minDistance` to any prior recorded throw are rejected (kept in the list, error shown) to preserve triangulation accuracy.
  - Once `measurementMode` (2 or 3) throws are recorded, `calculateWith2Points`/`calculateWith3Points` triangulate the stronghold position via line-intersection math (cross product for parallel detection; 3-point mode averages the three pairwise intersections and tolerates one or two parallel pairs). Y is unused/zeroed in the result — only X/Z matter.
  - Successful calculation clears `throwDataList`; a parallel-lines failure keeps recorded throws so the user only needs one more throw.
  - Changing `measurementMode` via command clears `throwDataList` (recording state is tied to the mode).

- **`EyeVectorCommand`** — builds the Brigadier command tree (`/eyevector mode|distance|precision|reset|status`) using `FabricClientCommandSource`/`ClientCommands`, and calls the static setters/getters on `EyeVectorClient`. All user-facing text goes through `Component.translatable(...)` keys.

## Localization

All player-facing strings are translation keys resolved from `src/main/resources/assets/eyevector/lang/{en_us,ko_kr}.json` — never hardcode user-facing strings in Java. When adding a new message or command feedback string, add the key to **both** lang files.

## fabric.mod.json version placeholder

`version` is `"${version}"` and gets expanded from `project.version` by the `processResources` Gradle task — don't hardcode a version there.
