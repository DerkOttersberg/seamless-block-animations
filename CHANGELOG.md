# Changelog

## 2.0.0+mc26.2

- Ported the mod to Minecraft 26.2 and Java 25.
- Added Fabric, Forge, and NeoForge releases from one multi-loader project.
- Renamed the public product and canonical mod ID to Seamless Block Animations / `seamless_block_animations`.
- Kept the old Fabric ID `fresh-interactiable-animations` as a compatibility alias.
- Replaced the Fabric renderer wrapper with loader-neutral vanilla baked-quad filtering and Minecraft's render submission pipeline.
- Added smooth rapid-reversal handling without endpoint snaps.
- Added automated animation math/kinematics tests and a real Fabric client GameTest with QA screenshots.
- Fixed ordinary server block updates, which bypass `ClientLevel.setBlock` in Minecraft 26.2, by observing the server-verified update path as well.
- Fixed unintended white outlines on animated models by passing no outline color to the model submitter.
- Expanded combined-suite client QA to start, render, resource-reload, and stop a live meteor shower on both graphics backends.
