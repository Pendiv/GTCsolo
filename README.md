# GTCsoloEU

A GregTech CEu Modern addon for Minecraft 1.20.1 and Forge, developed for the
All The Greg Tech modpack. This repository contains the addon, its bundled
Tiered Torcherino extension, and the accompanying KubeJS content.

The current version is **2.2.6-beta**. The development environment targets
GregTech CEu 1.6.4 and KubeJS 6.

## Runtime requirements

The addon requires GregTech CEu, Mekanism, Applied Energistics 2, Apotheosis,
L2Hostility, L2DamageTracker and L2Library. The bundled Tiered Torcherino
extension also requires Torcherino 20.0.0. Version ranges and optional mods are
listed in `src/main/resources/META-INF/mods.toml` and the extension's mod metadata.
The KubeJS content expects the other mods used by the intended modpack.

## Repository layout

- `src/` contains the addon source and resources.
- `extensions/tiered-torcherino/` contains the bundled extension.
- `run/kubejs/` contains the pack's KubeJS scripts, assets and datapack data.

The Gradle setup also references locally managed CmdExtra, CRedit and optional
ATGbattle projects, and local dependency JARs under `libs/`. Those projects and
JARs are not included here, so additional setup is needed to reproduce the
author's development build.

## License and credits

See [LICENSE](LICENSE), [LICENSE-ASSETS.md](LICENSE-ASSETS.md) and
[CREDITS.md](CREDITS.md) for licensing and asset attribution.
