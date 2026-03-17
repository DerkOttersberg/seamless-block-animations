# Fresh interactiable animations

Fresh interactiable animations is a client-side Fabric mod for Minecraft 1.21.11 that adds smooth open and close animations to several vanilla interactive blocks while keeping normal gameplay behavior intact.

![Open Door Animation](opendoor.gif)

## Features
- Smooth opening and closing animations for vanilla doors
- Smooth opening and closing animations for vanilla trapdoors
- Smooth opening and closing animations for vanilla fence gates
- Client-side only behavior
- Works with Fabric on Minecraft 1.21.11
- Designed to stay compatible with resource packs by using the baked block models already provided by the game
- Lightweight rendering approach with no gameplay changes, no new blocks, and no server-side requirement

## Supported blocks
- All vanilla doors
- All vanilla trapdoors
- All vanilla fence gates

## How it works
- Doors are hidden briefly and re-rendered with a smooth swing animation
- Trapdoors use a contained animation path so the panel stays visually inside the block space while moving into the open pose
- Fence gates animate their moving halves while preserving the overall vanilla model layout
- The mod only changes visuals on the client and does not change block states, timings, or redstone behavior on the server

## Requirements
- Minecraft 1.21.11
- Fabric Loader 0.16.14 or newer
- Fabric API
- Java 21

## Installation
1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API.
3. Download the mod jar from the releases you build or publish.
4. Place the jar in your Minecraft `mods` folder.
5. Launch the game with the Fabric profile.

## Building from source
1. Clone this repository.
2. Open the project in a Java 21 environment.
3. Run `gradlew build` on Windows or `./gradlew build` on Linux or macOS.
4. Find the built jar in `build/libs`.

## Notes
- This mod is client-side.
- Servers do not need to install it.
- The mod focuses on visual animation only and should preserve normal vanilla interaction behavior.

## License
This project is licensed under CC0-1.0.
