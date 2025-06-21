# Multi-loader Mod Template

This repository provides a basic template for creating Minecraft mods that target Fabric, Forge and NeoForge from the same codebase.
It is adapted from [jaredlll08's MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template) and stripped down to a minimal starting point.

## Getting started

1. Clone this repository.
2. Run `python scripts/setup_mod.py` and answer the prompts to set your package name, mod id and other details.
3. Replace the placeholder code in `TemplateMod` with your own logic.
4. Run the Gradle `build` task to produce jars for each loader.

## Directory layout

- `common/` contains code shared between all loaders.
- `fabric/`, `forge/` and `neoforge/` contain loader specific entry points and build logic.
- `buildSrc/` holds the Gradle scripts that wire everything together.

Feel free to expand upon this structure to suit the needs of your own mods.
