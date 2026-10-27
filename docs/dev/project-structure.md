# Project structure

A map of the repository: every folder and file, what it does, and how the pieces connect at
runtime. For how to run things, see [setup-and-commands.md](setup-and-commands.md). For the
rules and the step-by-step "add a creature / ability / block" checklists, see
[CONTRIBUTING.md](../../CONTRIBUTING.md).

- [Big picture](#big-picture)
- [Repository root](#repository-root)
- [Common code: `src/main/java`](#common-code-srcmainjava)
- [Client code: `src/client/java`](#client-code-srcclientjava)
- [Resources: `src/main/resources`](#resources-srcmainresources)
- [Tools and docs](#tools-and-docs)
- [How it runs](#how-it-runs)
- [Ids, save keys and translation keys](#ids-save-keys-and-translation-keys)
- [Where do I put…?](#where-do-i-put)

## Big picture

- **Two source sets.** `src/main` is common code: it runs on the client and on the dedicated
  server. `src/client` is client-only code: rendering, models and anything from
  `net.minecraft.client`. Loom's `splitEnvironmentSourceSets()` enforces this, so `src/main`
  can't reference `src/client`.
- **Layered by type.** One Java package per kind of thing (entities, abilities, evolution,
  blocks, registries, events), with creatures nested by name, e.g. `entity/cindercub/`.
- **Registries in one place.** Everything the game must know about (entity types, items,
  blocks, sounds, model layers, renderers) is registered in a `Mod*` class under `registry/`
  (common) or `client/registry/` (client). The entry points only call those in order.
- **Server-authoritative.** Game state (owner, sit, cooldowns, Kindle) is decided and saved on
  the server. The client renders what the server syncs.

## Repository root

```text
mythweave/
├── build.gradle              Gradle build: Loom plugin, dependencies, Java 25, run configs (runClient, runClient2, runServer)
├── gradle.properties         Versions (Minecraft, Loader, Fabric API, Loom) and the mod version
├── settings.gradle           Project name (mythweave) and plugin repositories
├── gradlew, gradlew.bat      Gradle wrapper: always use these, never a system Gradle
├── gradle/wrapper/           Wrapper JAR and the pinned Gradle version (9.8.2)
├── dev.cmd                   Windows launcher: double-click for a menu (runs dev.ps1)
├── dev.ps1                   Launcher logic: client, client2, server, multiplayer, build
├── README.md                 Player-facing overview, how to play, roadmap, testing checklist
├── CONTRIBUTING.md           Rules, conventions and checklists for contributors
├── LICENSE                   CC0 1.0 (from the template; final license not chosen yet)
├── .editorconfig             Indentation and encoding rules for editors
├── .gitattributes            Line endings (LF in the repo) and binary files
├── .gitignore                Ignores build output, IDE files, run folders
├── .github/
│   ├── workflows/build.yml   CI: ./gradlew build on JDK 25 for every push and PR
│   └── pull_request_template.md   Checklist shown on every PR
├── src/                      The mod (below)
├── tools/                    Scripts that generate textures and README images
└── docs/                     Design documents, developer docs, README images
```

Generated or local only, and git-ignored: `build/` (compiled output and JARs), `.gradle/`
(Gradle and Loom caches, including Minecraft itself), `run/`, `run-client2/` and
`run-server/` (game folders, see the setup guide), and `.idea/` (IntelliJ).

## Common code: `src/main/java`

Package root `com.ashi.mythweave` (written `…` below).

```text
…/
├── Mythweave.java                     Mod entry point (fabric.mod.json "main"). Calls the registries, then ModEvents.
│                                      Also holds MOD_ID, the LOGGER and id("path") → mythweave:path.
├── registry/                          Everything registered with the game, one class per kind
│   ├── ModSounds.java                 Sound events: entity.<cindercub|emberlynx|ashenox>.ambient / hurt / death
│   ├── ModEntities.java               Entity types (CINDERCUB, EMBERLYNX, ASHENOX: size, eye height) + attributes,
│   │                                  and mythlingForm(id): form id → entity type, used by evolution
│   ├── ModBlocks.java                 Blocks (EMBER_BRAZIER: strength, sound, light while lit)
│   ├── ModItems.java                  Items: spawn eggs, block items
│   └── ModCreativeTabs.java           Which Creative tab each item appears in
├── entity/
│   ├── base/
│   │   ├── Mythling.java              Interface: kindle(), evolutionLine()
│   │   └── MythlingEntity.java        Base class of every creature: bonding, refusing non-owners,
│   │                                  owner controls (status, abilities, sit), companion AI, Kindle saving,
│   │                                  evolution ticking, shared ember particles (emitEmbers), and the
│   │                                  defend-owner AI for fighting forms (defendsOwner, wolf targeting rules)
│   ├── cindercub/
│   │   └── CindercubEntity.java       Cindercub (stage 1): stats, sounds, bonding item, Ember Spark trigger,
│   │                                  ignition burst on bonding, tail embers (client particles)
│   ├── emberlynx/
│   │   └── EmberlynxEntity.java       Emberlynx (stage 2): stronger stats, defends its owner,
│   │                                  stronger Ember Spark, tail and mane embers
│   └── ashenox/
│       └── AshenoxEntity.java         Ashenox (stage 3, combat): armored, fire-immune, Magma Bite, lava drips
├── evolution/
│   ├── EvolutionRules.java            All evolution numbers and rules: thresholds, Kindle sources, ranges,
│   │                                  branches, stageOf / nextForm, transformation length
│   ├── EvolutionLine.java             One species' forms: base → stage 2 → combat form | exploration form
│   ├── EvolutionLines.java            Every species' line as a constant (CINDERCUB)
│   ├── Evolution.java                 Evolving: readiness check, the 3 s transformation, the entity swap
│   ├── Kindle.java                    One Mythling's evolution progress: earning, saving, copying, status
│   └── KindleEvents.java              Kindle from the world: hostile mobs dying nearby, survived hits
├── ability/
│   ├── Ability.java                   Interface: id, cooldown, trigger item, use(mythling, cooldown, ...) (server)
│   ├── AbilityCooldown.java           One ability's cooldown on one creature: tick, save, load
│   ├── EmberSparkAbility.java         Ember Spark: light the nearest visible unlit brazier; one constant per
│   │                                  form (CINDERCUB 8 blocks / 15 s, EMBERLYNX 12 blocks / 10 s)
│   └── MagmaBite.java                 Ashenox's passive ability: bites ignite mobs; mobs that hit it burn
├── block/
│   └── EmberBrazierBlock.java         Ember Brazier: LIT state, shape, empty-hand extinguish, particles
├── command/
│   └── MythweaveCommands.java         /mythweave kindle get|add (operators; for testing)
└── event/
    └── ModEvents.java                 Hooks into the game: registers KindleEvents and the commands
```

The mixin package `…/mixin/` is declared in `mythweave.mixins.json` but empty for now; add
mixins there when one is really needed.

## Client code: `src/client/java`

Package root `com.ashi.mythweave.client`.

```text
client/
├── MythweaveClient.java               Client entry point (fabric.mod.json "client"). Calls the client registries.
├── registry/
│   ├── ModModelLayers.java            Model layer ids (CINDERCUB, EMBERLYNX, ASHENOX = mythweave:<name>#main)
│   └── ModRenderers.java              Entity type → renderer (every entity type needs one)
├── model/entity/
│   ├── CindercubModel.java            Cube layout and all animation: walk, head look, idle tilt, ear twitch,
│   │                                  sit pose, fur tail (wild) vs fire tail (bonded) with its flicker
│   ├── EmberlynxModel.java            Taller lynx: fire mane, flame ear tufts, long rippling fire tail, sit pose
│   └── AshenoxModel.java              Heavy volcanic fox: obsidian plates, magma mane, curved horns, thick fire tail
└── render/entity/
    ├── CindercubRenderer.java         Ties model + textures to the entity; fills the render state each frame
    ├── EmberlynxRenderer.java         The same for Emberlynx
    ├── AshenoxRenderer.java           The same for Ashenox
    ├── state/
    │   ├── CindercubRenderState.java  Per-frame snapshot the model reads (isSitting, isTame, + vanilla data)
    │   ├── EmberlynxRenderState.java  isSitting + vanilla data
    │   └── AshenoxRenderState.java    isSitting + vanilla data
    └── layer/
        └── PulsingGlowLayer.java      Draws any creature's glow map full-bright with a slow "breathing" pulse
```

The client mixin package `client/mixin/` is declared in `mythweave.client.mixins.json` and is
empty.

## Resources: `src/main/resources`

```text
resources/
├── fabric.mod.json                    Mod id, name, version, entry points, mixin configs, dependencies
├── mythweave.mixins.json              Common mixin config (empty list)
├── assets/mythweave/                  Client resources (resource pack)
│   ├── icon.png                       Mod icon in the mod list
│   ├── lang/en_us.json                Every player-visible text: names, messages, subtitles
│   ├── sounds.json                    What each sound event plays (currently vanilla fox sounds, by event)
│   ├── blockstates/ember_brazier.json Picks the model per LIT state
│   ├── models/block/                  ember_brazier.json (unlit), ember_brazier_lit.json (with flame planes)
│   ├── models/item/                   <form>_spawn_egg.json for cindercub, emberlynx, ashenox
│   ├── items/                         Item appearances: the three spawn eggs, ember_brazier.json
│   └── textures/
│       ├── entity/cindercub/          cindercub.png (64×32 texture), cindercub_glow.png (glow map)
│       ├── entity/emberlynx/          emberlynx.png (64×64 texture), emberlynx_glow.png (glow map)
│       ├── entity/ashenox/            ashenox.png (64×64 texture), ashenox_glow.png (glow map)
│       ├── block/                     ember_brazier_*.png; ember_brazier_flame.png (+ .mcmeta = animation)
│       └── item/                      <form>_spawn_egg.png for cindercub, emberlynx, ashenox
└── data/                              Server data (data pack)
    ├── mythweave/recipe/ember_brazier.json          Crafting recipe
    ├── mythweave/loot_table/blocks/ember_brazier.json   Drops itself when mined
    └── minecraft/tags/block/mineable/pickaxe.json   Adds the brazier to the pickaxe tag
```

`src/client/resources/mythweave.client.mixins.json` is the client mixin config (empty list).

**Texture conventions:**
- entity textures go in `textures/entity/<creature>/`, with a `<creature>_glow.png` if parts glow;
- block and item textures are flat, vanilla-style, named after the block or item;
- in 26.3 the render layer (solid or cutout) comes from the texture itself: pixels that are
  either fully transparent or fully opaque render as cutout, and nothing needs registering.

## Tools and docs

```text
tools/
├── README.md                          What each script does and when to re-run it
├── textures/cindercub.ps1             Paints the Cindercub texture + glow map; documents its UV layout
├── textures/emberlynx.ps1             Paints the Emberlynx texture + glow map; documents its UV layout
├── textures/ashenox.ps1               Paints the Ashenox texture + glow map; documents its UV layout
├── textures/spawn_eggs.ps1            Paints every spawn egg (same shape, per-form colors)
├── textures/ember_brazier.ps1         Paints the brazier textures and flame animation
└── previews/mythlings.ps1             Renders docs/images/*.png from the models (duplicates their cube lists)

docs/
├── design/specification.md            The original design document: pillars, phases, rules
├── design/evolution.md                Evolution design: Kindle, forms, branch, phases E1–E5
├── dev/README.md                      Index of developer docs
├── dev/setup-and-commands.md          Setup, run commands, tasks, troubleshooting
├── dev/project-structure.md           This file
├── dev/setup-notes.md                 Phase 0 notes from when the project was created (historical)
└── images/                            README images (generated by tools/previews/)
```

## How it runs

**Startup (both sides), from `Mythweave.onInitialize()`:**
1. `ModSounds`, then `ModEntities`, `ModBlocks`, `ModItems`, `ModCreativeTabs`. Order matters:
   items refer to entity types and blocks.
2. `ModEvents.register()`, which hooks the Kindle listeners (Fabric `ServerLivingEntityEvents`)
   and the `/mythweave` command (Fabric `CommandRegistrationCallback`).

**Startup (client only), from `MythweaveClient.onInitializeClient()`:**
1. `ModModelLayers.register()`: model cube layouts.
2. `ModRenderers.register()`: renderers, which bake those layers.

**A right-click on a Mythling** (`MythlingEntity.mobInteract`, run on both sides; only the
server changes anything):
1. **Wild:**
   - with its bonding item, it bonds: `tame`, hearts, the `onBonded` hook (Cindercub's tail ignites);
   - with an empty hand, you get a hint.
2. **Someone else's:** refused with a message.
3. **Owner:**
   - sneaking with an empty hand shows the Kindle status;
   - an item the creature reacts to goes to `onOwnerUseItem` (Cindercub: Blaze Powder runs `EmberSparkAbility`);
   - anything else gets vanilla handling (leads, name tags) and then the sit/stand toggle.

**Every server tick** for a bonded Mythling: `Kindle.tick` pays out for time together,
distance travelled together and new biomes; ability cooldowns tick down.

**Once a second**, `Evolution.checkReady` asks `EvolutionRules.nextForm` whether it should
evolve. If so, and the next form exists in `ModEntities.mythlingForm`, `Evolution.tick` runs
a 60-tick transformation and then swaps the entity (`Mob#convertTo`), carrying over the owner,
sit order and Kindle.

**Every rendered frame:**
1. `CindercubRenderer.extractRenderState` copies `isSitting` and `isTame` into the render state.
2. `CindercubModel.setupAnim` poses the parts from that state.
3. `PulsingGlowLayer` draws the glow map on top.

## Ids, save keys and translation keys

These are in players' worlds and resource packs. **Never rename them**; add new ones instead.

| Kind | Values |
|---|---|
| Entities | `mythweave:cindercub`, `mythweave:emberlynx`, `mythweave:ashenox` |
| Items | `mythweave:cindercub_spawn_egg`, `mythweave:emberlynx_spawn_egg`, `mythweave:ashenox_spawn_egg`, `mythweave:ember_brazier` |
| Block | `mythweave:ember_brazier` (state `lit=true/false`) |
| Sounds | `mythweave:entity.<cindercub/emberlynx/ashenox>.ambient`, `.hurt`, `.death` |
| Model layers | `mythweave:cindercub#main`, `mythweave:emberlynx#main`, `mythweave:ashenox#main` |
| Entity save data | `Owner`, `Sitting` (vanilla), `EmberCooldown`, `EvolvingTicks` (only mid-transformation), `Kindle` → `Points`, `Combat`, `Exploration`, `TicksWithOwner`, `BlocksTravelled`, `Biomes` |

**Translation key namespaces** (`lang/en_us.json`):

| Prefix | Used for |
|---|---|
| `entity.mythweave.*` | Creature and form names (`cindercub`, `emberlynx`, `ashenox`; `solflare` is ready for E4) |
| `item.mythweave.*`, `block.mythweave.*` | Item and block names |
| `subtitles.mythweave.*` | Sound subtitles |
| `message.mythweave.mythling.*` | Messages shared by every creature (bonded, sitting, not yours, …; `%s` = its name) |
| `message.mythweave.<creature>.*` | Creature-specific messages (e.g. Ember Spark) |
| `message.mythweave.kindle.*` | Kindle status and events |
| `commands.mythweave.*` | Command feedback |

## Where do I put…?

| I want to add… | Put it in | And register it in |
|---|---|---|
| A new creature | `entity/<name>/<Name>Entity.java` (extends `MythlingEntity`) | `ModEntities`; spawn egg in `ModItems` + `ModCreativeTabs` |
| Its evolution line | a constant in `evolution/EvolutionLines.java` | none needed |
| Its model / renderer / render state | `client/model/entity/`, `client/render/entity/`, `…/state/` | `ModModelLayers`, `ModRenderers` |
| An ability | `ability/<Name>Ability.java` (implements `Ability`) | triggered from the creature's `onOwnerUseItem` |
| A block | `block/<Name>Block.java` | `ModBlocks` + its item in `ModItems` + `ModCreativeTabs` |
| A custom item class | `item/<Name>Item.java` (create the package) | `ModItems` + `ModCreativeTabs` |
| A sound | `ModSounds` + `sounds.json` + subtitle in `lang/en_us.json` | none needed |
| An event listener | the feature's own class (e.g. `KindleEvents`) | `event/ModEvents.register()` |
| A command | `command/` | `event/ModEvents.register()` |
| A Kindle source | `EvolutionRules.KindleSource` | report it with `kindle().gain(...)` |
| Text a player sees | `lang/en_us.json` | use `Component.translatable(key)` |

Full step-by-step versions of these are in [CONTRIBUTING.md](../../CONTRIBUTING.md).
