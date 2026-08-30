# Migration to 2.0.0

## Player migration

Remove the old jar before installing a 2.0.0 loader-specific jar. This mod has no world data or configuration files to migrate.

The canonical mod ID is now `seamless_block_animations`. Fabric metadata provides the old `fresh-interactiable-animations` ID as an alias so Fabric dependency checks can continue to recognize existing integrations. Forge and NeoForge use only the canonical ID because no historical release for those loaders existed.

The mod remains client-only and safe to add or remove from an existing profile. Servers do not need it.

## Pack compatibility

Animations resolve the active baked block model at render time. Resource packs do not need mod-specific model copies, and their door, trapdoor, and fence-gate textures/models remain the geometry source.
