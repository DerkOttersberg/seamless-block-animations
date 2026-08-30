# Porting architecture

## Modules

- `common` owns animation state, timing, kinematics, vanilla model filtering, render submission, mixins, and unit tests.
- `fabric`, `forge`, and `neoforge` contain only loader entrypoints, client lifecycle adapters, and loader metadata.
- `gradle/libs.versions.toml` is the single source for Minecraft, Java, loader, Loom, and test dependency versions.

Common bootstrap receives a `ClientPlatformServices` implementation explicitly. Runtime reflection and `ServiceLoader` discovery are intentionally not used.

## Rendering model

Minecraft's active `BlockStateModel` supplies the baked quads. During a transition, static chunk compilation suppresses the moving door/trapdoor geometry and preserves fence-gate posts. A dynamic render submission transforms the same resource-pack-aware model around vanilla hinges. No loader renderer API or raw graphics API is used in common code.

## Version ports

For a future Minecraft line:

1. Change versions only in `gradle/libs.versions.toml` and the release number in `gradle.properties`.
2. Compile `common` first and adapt official-name Minecraft API changes there.
3. Verify both client block-update paths. Normal server packets may bypass the public `ClientLevel.setBlock` override.
4. Compile and boot each loader independently.
5. Run `clean check build` and `:fabric:runClientGameTest`.
6. Inspect all four client screenshots, then test both OpenGL and Vulkan in the combined mod suite.

The root verification tasks reject loader imports in `common` and reject jars containing another loader's metadata.
