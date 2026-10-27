# Contributing to Mythweave

Thanks for helping! This guide covers getting set up, where things live, the rules every
change follows, and step-by-step checklists for the common jobs: adding a creature, an
evolution form, a block or an ability.

The design source of truth is [docs/design/specification.md](docs/design/specification.md),
and evolution is described in [docs/design/evolution.md](docs/design/evolution.md).

## Setup

1. Install **JDK 25** (e.g. `winget install Microsoft.OpenJDK.25`) and Git.
2. Clone the repo and open it in IntelliJ IDEA (recommended) or any Java IDE. The first
   Gradle import downloads Minecraft and takes a few minutes.
3. Run the game:
   - Windows: double-click **`dev.cmd`** (menu) or run `.\dev.cmd client`.
   - Anywhere: `./gradlew runClient`.
4. Read Minecraft's own code when you need to know how something works:
   `./gradlew genSources` decompiles it, so you can open vanilla classes in your IDE.

More detail (versions, the IPv6 download issue): [docs/dev/setup-notes.md](docs/dev/setup-notes.md)
and the README's *Building from source* section.

## Where things go

The code is organised **by feature**, not by kind of class:

| Path | What lives there |
|---|---|
| `src/main/java/.../mythling/` | Shared by every creature: `MythlingEntity` (base class), `Mythling` |
| `src/main/java/.../mythling/evolution/` | Kindle and evolution rules, shared by every species |
| `src/main/java/.../creature/<name>/` | Everything for one creature: entity, evolution line, its abilities |
| `src/main/java/.../registry/` | Every registration (entity types, items, blocks, sounds), in one place |
| `src/main/java/.../block/` | Blocks (shared across creatures) |
| `src/main/java/.../command/` | Commands |
| `src/client/java/.../client/creature/<name>/` | That creature's model, renderer, render state |
| `src/client/java/.../client/mythling/` | Shared client code (e.g. `PulsingGlowLayer`) |
| `assets/mythweave/textures/entity/<name>/` | That creature's texture and glow map |
| `tools/textures/`, `tools/previews/` | Scripts that paint textures / render README images ([tools/README.md](tools/README.md)) |

`src/main` runs on both the client and the dedicated server; `src/client` runs only on the
client. That split is enforced by the build (`splitEnvironmentSourceSets`).

## Rules every change follows

1. **The server decides.** Bonding, ownership, cooldowns, Kindle and abilities are checked and
   changed on the server only. The client only draws things.
2. **No client code in `src/main`.** Never import `net.minecraft.client.*` from `src/main`: the
   dedicated server doesn't have those classes and will crash. Check with `.\dev.cmd server`.
3. **Verify APIs against Minecraft 26.3.** Don't trust tutorials for older versions. Find a
   vanilla class that does the same thing (`genSources`) and copy its pattern.
4. **Never rename save keys or ids.** Entity ids (`mythweave:cindercub`), item/block ids and NBT
   keys (`Owner`, `Sitting`, `EmberCooldown`, `Kindle`, …) are in players' worlds. Renaming
   one silently breaks saves. Add new keys instead.
5. **Abilities stay bounded.** No uncontrolled fire, no griefing, a cooldown, and server checks.
6. **Original designs only.** No Pokémon (or other franchise) names, designs, sprites or
   sounds. A multi-tailed fire fox, for example, is too close to an existing design.
7. **Text goes in the lang file.** Players see `Component.translatable(...)`, never hard-coded
   strings; add the key to `assets/mythweave/lang/en_us.json`.

## Code style

- **Tabs** for indentation in Java, Gradle and PowerShell; 2 spaces in JSON, Markdown and YAML
  (`.editorconfig` sets this up in most editors).
- Imports in groups: this mod, then other libraries (Mojang, Fabric), then Minecraft, then Java.
- Comments explain **why** (rules, numbers, vanilla quirks); the code says what. Every class
  gets a short Javadoc saying what it's for.
- Registries are `Mod*` classes in `registry/`; everything is registered there.

## Checklist: adding a new Mythling (creature)

Use Cindercub (`creature/cindercub/`) as the template.

1. **Entity:** `creature/<name>/<Name>Entity.java`, extending `MythlingEntity`:
   - `createAttributes()`: health, speed;
   - `bondingItem()` and `evolutionLine()`;
   - sounds (`getAmbientSound` etc.);
   - optional hooks: `onOwnerUseItem` for abilities, `onBonded` for bonding effects;
   - extra saved fields: override `addAdditionalSaveData`/`readAdditionalSaveData` and call `super`.
2. **Evolution line** (new species only): `creature/<name>/<Name>Line.java` holding its
   `EvolutionLine`.
3. **Register** in `registry/ModEntityTypes.java`: `register(...)` with its size and eye
   height, plus its attributes in `initialize()`.
4. **Spawn egg:** register it in `ModItems`, add it to the Spawn Eggs tab, and add
   `items/<name>_spawn_egg.json`, `models/item/…` and `textures/item/…`.
5. **Sounds:** add them to `ModSounds`, `sounds.json`, and the subtitles in the lang file.
6. **Client** in `client/creature/<name>/`:
   - a model with a `LAYER`;
   - a render state;
   - a renderer, plus `PulsingGlowLayer` if it glows.

   Register both in `MythweaveClient` (`ModelLayerRegistry` and `EntityRenderers`).
7. **Textures:** put them in `textures/entity/<name>/`. Ideally generate them from
   `tools/textures/<name>.ps1`, which documents the UV layout.
8. **Lang:** `entity.mythweave.<name>`, the spawn egg, and the subtitles.
9. **Test**, using the checklist below, and add a section to the README.

## Checklist: adding an evolution form

A form (e.g. Emberlynx) is a Mythling of its own: follow the creature checklist above, in the
**same** `creature/<line>/` folder. Then:

- `evolutionLine()` returns the species' existing line (e.g. `CindercubLine.LINE`);
- the entity id must equal the name used in that line (`"emberlynx"`);
- the evolution itself (Kindle threshold → transformation) comes from
  `mythling/evolution/`; see [docs/design/evolution.md](docs/design/evolution.md) for which
  phase adds what.

## Checklist: blocks and abilities

- **Block:**
  - the class goes in `block/`; register it in `ModBlocks` and its item in `ModItems`;
  - add `blockstates/`, `models/block/`, `items/`, textures, loot table (`data/mythweave/loot_table/blocks/`),
    a recipe if craftable, a mining tag, and lang.
- **Ability:**
  - put it next to its creature (`creature/<name>/`) and trigger it from `onOwnerUseItem`;
  - store its cooldown on the entity and save it;
  - when a second creature needs the same ability, move it to a shared `mythling/ability/` package.
- **Kindle source:** add it to `EvolutionRules.KindleSource` and report it with
  `mythling.kindle().gain(mythling, KindleSource.X)`.

## Testing

Before opening a pull request:

1. `./gradlew build` ends in `BUILD SUCCESSFUL` (CI checks this too).
2. `.\dev.cmd client`: try what you changed in game, and run the README's *Testing
   checklist* items it touches.
3. `.\dev.cmd server`: the dedicated server reaches `Done`, with no `ClassNotFound` errors.
4. For multiplayer behaviour, run `.\dev.cmd multiplayer` (server, Player1 and Player2).
5. Load an **existing** world, not only a fresh one, so you catch broken saves.

`/mythweave kindle add <mythlings> <amount> [combat|exploration]` (operators) speeds up
evolution testing.

## Git workflow

- Branch from `main`: `feature/emberlynx`, `fix/sit-toggle`, …
- Keep each pull request to **one feature or one phase**; small PRs get reviewed faster.
- Write commit messages as a short summary line, a blank line, then what and why.
- Open a PR and fill in the template. **CI must be green** before merging.
- Generated files (textures from `tools/`) are committed **together with** the script change
  that made them.
