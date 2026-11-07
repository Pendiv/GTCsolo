# KubeJS content

These files are the KubeJS content used by the GTCsoloEU development environment.
They target Minecraft 1.20.1, Forge, GregTech CEu Modern 1.6.4 and KubeJS 6.
They also use the mods and configuration of the intended modpack.

- `startup_scripts/` registers materials, fluids, items, blocks and multiblocks.
- `server_scripts/` contains recipes, recipe overrides, tags and world generation.
  `gtcsolo_pack/` groups the pack's additional and replacement recipes by function.
- `client_scripts/` contains translations and tooltips.
- `assets/` contains textures, models, translations and recipe UI resources.
- `data/` contains datapack definitions and recipe overrides.

Copy the required files into the corresponding `kubejs/` directories of an
instance with the matching mods. Preserve the `priority` comments in scripts;
they control ordering between registration and recipe-processing steps.
Startup registrations require a full game restart.
