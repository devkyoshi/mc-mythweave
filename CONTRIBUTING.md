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

The full developer docs are in [docs/dev/](docs/dev/README.md):
- **[setup-and-commands.md](docs/dev/setup-and-commands.md):** setup, every launcher and Gradle
  command, run folders, test commands, asset scripts, troubleshooting;
- **[project-structure.md](docs/dev/project-structure.md):** every folder and file explained,
  how the code runs, ids and save keys, and where to put new things.

## Where things go

The code is **layered by type**, the usual Minecraft-mod convention: one package per kind of
thing, with creatures nested by name inside the packages that need it. (`...` is
`com/mythweave`.)

**Common code** (`src/main/java/...`):

| Package | What belongs there |
|---|---|
| `registry/` | Every registration, one class per kind: `ModEntities`, `ModItems`, `ModBlocks`, `ModSounds`, `ModCreativeTabs` |
| `entity/base/` | `MythlingEntity` (the base class every creature extends) and the `Mythling` interface |
| `entity/<name>/` | One creature's entity class, e.g. `entity/cindercub/CindercubEntity` |
| `evolution/` | Kindle, the evolution rules, `EvolutionLine`, and every species' line in `EvolutionLines` |
| `ability/` | The `Ability` interface, `AbilityCooldown`, and every ability (`EmberSparkAbility`, …) |
| `block/` | Block classes |
| `item/` | Custom item classes. Not created yet: today's items are vanilla `SpawnEggItem`/`BlockItem` |
| `command/` | Command trees |
| `event/` | `ModEvents`: the one place that hooks listeners and commands into the game |

**Client-only code** (`src/client/java/.../client/`):

| Package | What belongs there |
|---|---|
| `registry/` | `ModModelLayers` (model layer ids) and `ModRenderers` (entity → renderer) |
| `model/entity/` | Entity models: cube layout and animation, e.g. `CindercubModel` |
| `render/entity/` | Entity renderers, e.g. `CindercubRenderer` |
| `render/entity/state/` | Render states: per-frame data a model animates from |
| `render/entity/layer/` | Render layers shared by renderers, e.g. `PulsingGlowLayer` |

**Resources and tools:**

| Path | What lives there |
|---|---|
| `assets/mythweave/textures/entity/<name>/` | A creature's texture and glow map |
| `tools/textures/`, `tools/previews/` | Scripts that paint textures / render README images ([tools/README.md](tools/README.md)) |

Naming: registries are `Mod<Things>`; entities `<Name>Entity`; models, renderers and render
states `<Name>Model`, `<Name>Renderer`, `<Name>RenderState`; abilities `<Name>Ability`.

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
- Registries are `Mod*` classes in `registry/` (common) and `client/registry/` (client);
  everything is registered there, and `Mythweave` / `MythweaveClient` only call them in order.

## Checklist: adding a new Mythling (creature)

Use Cindercub as the template: `entity/cindercub/CindercubEntity`, `EvolutionLines.CINDERCUB`,
and on the client `CindercubModel`, `CindercubRenderer`, `CindercubRenderState`.

1. **Entity:** `entity/<name>/<Name>Entity.java`, extending `MythlingEntity`:
   - `createAttributes()`: health, speed;
   - `bondingItem()` and `evolutionLine()`;
   - sounds (`getAmbientSound` etc.);
   - optional hooks: `onOwnerUseItem` for abilities, `onBonded` for bonding effects;
   - extra saved fields: override `addAdditionalSaveData`/`readAdditionalSaveData` and call `super`.
2. **Evolution line** (new species only): add a constant to `evolution/EvolutionLines.java`.
3. **Register** in `registry/ModEntities.java`: `register(...)` with its size and eye height,
   plus its attributes in `initialize()`.
4. **Spawn egg:**
   - register it in `ModItems`, and list it in `ModCreativeTabs`;
   - add `items/<name>_spawn_egg.json`, `models/item/…` and `textures/item/…`.
5. **Sounds:** add them to `ModSounds`, `sounds.json`, and the subtitles in the lang file.
6. **Client:**
   - the model in `model/entity/<Name>Model`, its layer id in `registry/ModModelLayers`
     (registered in `register()`);
   - the render state in `render/entity/state/`;
   - the renderer in `render/entity/`, adding `PulsingGlowLayer` if it glows, and registered
     in `registry/ModRenderers`.
7. **Textures:** put them in `textures/entity/<name>/`. Ideally generate them from
   `tools/textures/<name>.ps1`, which documents the UV layout.
8. **Lang:** `entity.mythweave.<name>`, the spawn egg, and the subtitles.
9. **Test**, using the checklist below, and add a section to the README.

## Checklist: adding an evolution form

A form (e.g. Emberlynx) is a Mythling of its own: follow the creature checklist above, giving
it its own `entity/<form>/` package. Then:

- `evolutionLine()` returns the species' existing line (e.g. `EvolutionLines.CINDERCUB`);
  don't add a new line;
- the entity id must equal the name used in that line (`"emberlynx"`);
- **add it to `ModEntities.MYTHLING_FORMS`**: that's what makes it reachable by evolution.
  Until a form is there, its predecessor just tells the owner it's "ready" and waits;
- the evolution itself (Kindle threshold, transformation, the entity swap that keeps the
  owner, sit state and Kindle) is handled by `evolution/Evolution`, so the form needs no
  extra code for it. Copy any extra per-creature state you add in `Evolution.copyBond`.
  See [docs/design/evolution.md](docs/design/evolution.md).
- **Fighting forms** override `defendsOwner()` to return `true` (the base class then adds the
  bite and defend-owner goals with the vanilla wolf's target rules) and add an `ATTACK_DAMAGE`
  attribute. Passive combat effects go in a helper like `ability/MagmaBite`, called from
  `doHurtTarget` / `hurtServer`.
- **Size:** set the form's `Attributes.SCALE` in `createAttributes()` (keep the `ModEntities`
  hitbox at model size; SCALE multiplies it). Each stage should be bigger than the one before;
  evolution grows the creature to the new size and waits for room on its own.
- Use `EmberlynxEntity` / `AshenoxEntity` / `SolflareEntity` and their models as the template for
  a later form.

## Checklist: blocks and abilities

- **Block:**
  - the class goes in `block/`; register it in `ModBlocks` and its item in `ModItems`;
  - add `blockstates/`, `models/block/`, `items/`, textures, loot table (`data/mythweave/loot_table/blocks/`),
    a recipe if craftable, a mining tag, and lang.
- **Ability** (see `EmberSparkAbility`):
  - `ability/<Name>Ability.java` implementing `Ability<…>`, as constants (one per form if it grows with evolution, like `EmberSparkAbility.CINDERCUB` / `.EMBERLYNX`);
  - give the entity an `AbilityCooldown` with a save key that never changes; tick it in
    `tick()` and save/load it in the entity's save methods;
  - trigger it from the entity's `onOwnerUseItem` via `isTriggeredBy(stack)` and `use(...)`.
- **Event listener:** register it in `event/ModEvents.register()`, so every hook into the game
  stays in one place.
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
