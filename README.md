# Seamless Block Animations

Seamless Block Animations is a client-only Minecraft 26.2 mod that gives vanilla doors, trapdoors, and fence gates smooth open/close motion. One shared implementation is released for Fabric, Forge, and NeoForge.

![Door animation](opendoor.gif)

## Features

- Smooth motion for every vanilla door, trapdoor, and fence gate.
- Rapid direction changes continue from the current angle instead of snapping.
- Vanilla baked models remain the source of the animated geometry, including models supplied by resource packs.
- No blocks, registry entries, packets, server logic, or gameplay rules are added.
- Uses Minecraft's render submission pipeline and does not call raw OpenGL, so the renderer is suitable for both OpenGL and Vulkan backends.

## Requirements

- Minecraft Java Edition 26.2
- Java 25
- One of:
  - Fabric Loader 0.19.3 or newer plus Fabric API
  - Forge 26.2-65.1.3 or compatible
  - NeoForge 26.2.0.75 or compatible

The mod is client-only. It does not need to be installed on a server.

## Installation

Download the jar matching your loader and copy it into that profile's `mods` directory. Do not install more than one loader jar in the same profile.

Release filenames use this format:

```text
seamless-block-animations-2.0.0+mc26.2-fabric.jar
seamless-block-animations-2.0.0+mc26.2-forge.jar
seamless-block-animations-2.0.0+mc26.2-neoforge.jar
```

## Building and testing

Use Java 25 and the included Gradle wrapper:

```text
./gradlew clean check build
```

The loader jars are written to `fabric/build/libs`, `forge/build/libs`, and `neoforge/build/libs`. The Fabric end-to-end client test creates a real world, animates all three block families, tests a rapid reversal, and captures screenshots:

```text
./gradlew :fabric:runClientGameTest
```

Screenshots are written to `fabric/build/run/clientGameTest/screenshots`.

When all five Seamless Suite repositories are checked out as siblings and their Fabric jars have been built, run the same interaction test with the entire suite loaded:

```text
./gradlew :fabric:runClientGameTest -PsuiteTest=true -PgraphicsBackend=opengl
./gradlew :fabric:runClientGameTest -PsuiteTest=true -PgraphicsBackend=vulkan
```

Suite mode asserts that all five compatibility IDs are present, starts a large
meteor shower, captures its wide sky field, reloads resources while it is
active, captures it again, and then stops it. Vulkan mode also asserts
Minecraft's live device backend, so an OpenGL fallback fails the test.

NeoForge 26.2.0.75's early loading screen creates an OpenGL-owned window. Disable it with `earlyWindowControl = false` in the generated `config/fml.toml` before selecting Vulkan; otherwise NeoForge cannot hand that window to Minecraft's Vulkan surface. This is a loader startup constraint and does not affect the mod's renderer.

See [PORTING.md](PORTING.md) for the module boundaries and [MIGRATION.md](MIGRATION.md) for compatibility details.

## License

This project remains licensed under CC0-1.0.
