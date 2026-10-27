<p align="center">
  <img src="docs/images/banner.png" alt="Mythweave: discover, bond with, and raise magical Mythlings" width="100%">
</p>

<p align="center">
  <b>Minecraft Java 26.3</b> · <b>Fabric</b> · <b>Java 25</b> · <i>Early development: first creature playable</i>
</p>

---

**Mythweave** is a Fabric mod about discovering, bonding with, and raising original
magical creatures called **Mythlings**. It keeps the familiar survival experience and adds a
creature-collecting loop on top: find a Mythling, earn its trust, and it becomes a companion
that follows you, waits when told, and (in later versions) helps you explore with magical
abilities.

> Mythweave is a **single mod**, not a modpack. It needs only Fabric Loader and Fabric API.

All creatures, models, and textures are original. Mythweave does not use any Pokémon names,
designs, or assets.

## Contents

- [Meet Cindercub](#meet-cindercub)
- [How to play](#how-to-play)
- [Features and roadmap](#features-and-roadmap)
- [Installing (players)](#installing-players)
- [Building from source (developers)](#building-from-source-developers)
- [Testing checklist](#testing-checklist)
- [Project structure](#project-structure)
- [Technical notes](#technical-notes)
- [License and credits](#license-and-credits)

## Meet Cindercub

<table>
  <tr>
    <td align="center"><img src="docs/images/cindercub_standing.png" width="260" alt="Cindercub standing, front view"><br><sub>Standing</sub></td>
    <td align="center"><img src="docs/images/cindercub_sitting.png" width="260" alt="Cindercub sitting"><br><sub>Sitting (ordered to wait)</sub></td>
    <td align="center"><img src="docs/images/cindercub_back.png" width="260" alt="Cindercub from behind, showing the ember-tipped tail"><br><sub>The ember-tipped tail</sub></td>
  </tr>
</table>

The first Mythling: a small ember fox with oversized ears, short legs, and a glowing tip on
its fluffy tail. It's curious, playful, and a little mischievous.

| | |
|---|---|
| **Species** | Ember fox Mythling |
| **Element** | Fire / Ember |
| **Rarity** | Common |
| **Health** | 12 (6 hearts) |
| **Size** | 0.5 × 0.65 blocks (ears stick out above, like a fox's) |
| **Temperament** | Passive. Wanders, watches nearby players, runs away when hurt. Never attacks. |
| **Bonds with** | Blaze Powder |
| **Habitat (planned)** | Savannas, badlands, warm caves |

<sub>The images above are generated from the mod's real model layout and texture by
<code>tools/render_cindercub_preview.ps1</code>. Lighting is approximate.</sub>

## How to play

> **Finding one:** natural spawning isn't in yet. For now, use the **Cindercub Spawn Egg**
> (Creative inventory → Spawn Eggs) or the command `/summon mythweave:cindercub`.

1. **Approach a wild Cindercub.** Right-click it with an empty hand and it'll hint at what it wants.
2. **Bond with it:** right-click it holding **Blaze Powder**. One powder is used, hearts appear,
   and you'll see *"Cindercub has bonded with you!"*. Bonding always succeeds.
3. **Walk away:** your Cindercub follows you. If you get more than 12 blocks ahead and it can't
   catch up, it teleports to your side.
4. **Tell it to wait:** right-click it with an empty hand. It sits and stays put.
   Right-click again and it gets up and follows you.

### Interactions

| Who | Holding | What happens |
|---|---|---|
| Anyone (wild Cindercub) | Blaze Powder | Bonds. Uses 1 powder. |
| Anyone (wild Cindercub) | Empty hand | A hint about what it likes. |
| Owner | Empty hand (or an item with no other use on it) | Toggles sit / follow. |
| Owner | Lead, name tag, … | Works like on any vanilla animal. |
| Another player | Blaze Powder or empty hand | *"This Cindercub is already bonded to another player."* Nothing changes. |

### Good to know

- **The bond is saved.** Owner and sitting state survive saving, quitting, and reloading.
- **If you log out or change dimension**, your Cindercub sits and waits, then follows again when you're back.
- **If you die**, it stays bonded and follows you after you respawn.
- **Breeding isn't supported** (on purpose, for now).

## Features and roadmap

Mythweave is built in small, tested phases. Each one works in-game before the next starts.

- [x] **Phase 0: Project setup.** Fabric 26.3 template, builds and launches.
- [x] **Phase 1: Register Cindercub.** Entity type, attributes, spawn egg.
- [x] **Phase 2: Render Cindercub.** Original model, texture, walk/look/tail animation.
- [x] **Phase 3: Behavior.** Wander, look around, swim, flee when hurt.
- [x] **Phase 4: Bonding and companion.** Bond with Blaze Powder, follow owner, sit/stand.
- [ ] **Phase 5: Save/load and multiplayer checks.** Dedicated server, two-player tests.
- [ ] **Phase 6: Signature ability.** *Ember Spark*: a safe, cooldown-limited ember effect
  (possibly lighting a special Ember Brazier block). It won't set the world on fire.
- [ ] **Phase 7: Polish.** True glowing ember tip, sounds, custom spawn egg, finer texture.

**Later ideas** (after Cindercub is solid): more Mythlings, natural spawning in warm biomes,
the *Aether Shard* bonding item, branching evolutions (*Ashenox*, a combat-focused volcanic
fox, and *Solflare*, an exploration-focused one), magical shrines, and a creature journal.

## Installing (players)

> There are no published releases yet. Build the JAR yourself (see below).

1. Install **Java 25** (Minecraft 26.3 requires it).
2. Install **[Fabric Loader](https://fabricmc.net/use/) 0.20.1 or newer** for Minecraft **26.3**.
3. Put **[Fabric API](https://modrinth.com/mod/fabric-api)** for 26.3 in your `mods` folder.
4. Put `mythweave-1.0.0.jar` in your `mods` folder too.
5. Launch the *Fabric* profile.

The mod is required on both the client and the server when playing multiplayer.

## Building from source (developers)

**You need:** JDK 25 and Git. Gradle downloads itself through the wrapper.

```bash
git clone <this repository>
cd mythweave
```

Build the mod JAR (output: `build/libs/mythweave-1.0.0.jar`):

```bash
./gradlew build
```

Launch a development client with the mod loaded (logs in `run/logs/latest.log`):

```bash
./gradlew runClient
```

On Windows PowerShell use `.\gradlew.bat` instead of `./gradlew`. The first build downloads
Minecraft and dependencies and takes a few minutes. After that, builds take seconds.

Other useful tasks:

| Task | What it does |
|---|---|
| `./gradlew runServer` | Starts a dedicated server with the mod (checks no client-only code leaks into the server). |
| `./gradlew genSources` | Decompiles Minecraft so you can read vanilla code in your IDE. |

<details>
<summary><b>Troubleshooting</b></summary>

- **`java -version` still shows an older Java:** open a new terminal after installing JDK 25,
  or point `JAVA_HOME` at the JDK 25 folder.
- **Downloads from `fabricmc.net` hang or time out:** some networks have broken IPv6. Java and
  Gradle prefer IPv4 and are usually fine. Node-based tools such as the Fabric CLI may need
  `NODE_OPTIONS=--dns-result-order=ipv4first`.
- **Harmless log noise in the dev client:** Windows "Perflib"/OSHI warnings, and `401` or
  Realms errors (the dev client uses an offline test account).

</details>

## Testing checklist

A quick in-game test of everything that works so far. Use a Creative world with cheats on.

1. Spawn a few Cindercubs with the spawn egg. They should appear, wander, and look at you.
2. Hit one. It should run away for a few seconds. Push one into water: it swims out.
3. Right-click a wild one with an empty hand: you see the Blaze Powder hint.
4. Right-click with Blaze Powder: hearts, the "bonded" message, one powder used.
5. Walk 10+ blocks away: it follows. Sprint far: it teleports to you.
6. Right-click with an empty hand: it sits and stays. Again: it follows.
7. Sit it, save and quit, reload: still sitting, still yours. Repeat while it's standing.
8. *(Multiplayer)* A second player can't bond with or command your Cindercub.

Run `./gradlew build` before committing; it must end in `BUILD SUCCESSFUL`.

## Project structure

```text
src/main/java/com/ashi/mythweave/           Common code (runs on client and server)
├── Mythweave.java                          Mod entry point: runs all registrations
├── entity/CindercubEntity.java             Cindercub: stats, AI goals, bonding and sit logic
└── registry/
    ├── ModEntityTypes.java                 Entity types and their attributes
    └── ModItems.java                       Items (spawn egg) and creative tab placement

src/client/java/com/ashi/mythweave/client/  Client-only code (never loaded on a server)
├── MythweaveClient.java                    Client entry point: registers model + renderer
├── model/CindercubModel.java               Cube layout, walk/look/tail animation, sit pose
└── render/
    ├── CindercubRenderer.java              Connects model + texture to the entity
    └── CindercubRenderState.java           Per-frame data the model reads (e.g. sitting)

src/main/resources/
├── fabric.mod.json                         Mod metadata, entry points, dependencies
└── assets/mythweave/
    ├── textures/entity/cindercub.png       Cindercub texture (64×32)
    ├── items/cindercub_spawn_egg.json      Spawn egg appearance
    └── lang/en_us.json                     English names and messages

tools/
├── generate_cindercub_texture.ps1          Paints cindercub.png; documents the UV layout
└── render_cindercub_preview.ps1            Renders the README images in docs/images/

docs/
├── Mythweave_Project_Specification.md      Design document and phase plan
└── PHASE0_NOTES.md                         Toolchain versions and setup notes
```

### The texture

<p align="center">
  <img src="docs/images/cindercub_texture.png" width="512" alt="Cindercub texture sheet, scaled 8x">
</p>

The 64 × 32 texture, scaled 8× (the checkerboard is transparent). Each rectangle is one
body part unfolded flat: head, snout, ears, body, tail, ember tip, and legs. Edit it in any
pixel editor or Blockbench, or change the colors in `tools/generate_cindercub_texture.ps1` and
run:

```bash
powershell -ExecutionPolicy Bypass -File tools/generate_cindercub_texture.ps1
```

## Technical notes

- **Versions** (from `gradle.properties`): Minecraft 26.3, Fabric Loader 0.20.1,
  Fabric API 0.165.0+26.3, Loom 1.19, Gradle 9.8.2, Java 25.
- **No mappings step.** Minecraft 26.x ships unobfuscated, so the code uses Mojang's own names
  (e.g. `net.minecraft.resources.Identifier`) and the plugin is `net.fabricmc.fabric-loom`.
- **Client/server split.** `splitEnvironmentSourceSets()` keeps rendering code in `src/client`,
  so a dedicated server never loads it.
- **Bonding reuses vanilla.** `CindercubEntity` extends `TamableAnimal`, which stores the owner
  as a UUID, saves `Owner` and `Sitting` with the entity, and syncs both to clients. No custom
  save code or network packets.
- **Server-authoritative.** Every bond, sit toggle, and message is decided on the server.
  The client only predicts the arm swing.
- **26.3 API notes:** entity types and items must be built with their registry key; spawn eggs
  use `Item.Properties#spawnEgg`; renderers fill a *render state* each frame that the model
  animates from; Fabric's `EntityRendererRegistry` is deprecated in favor of vanilla's
  `EntityRenderers.register`; action-bar messages use `Player#sendOverlayMessage`.

The full design (pillars, phase plan, and acceptance criteria) is in
[docs/Mythweave_Project_Specification.md](docs/Mythweave_Project_Specification.md).

## License and credits

The project currently carries the Fabric example template's **CC0 1.0** license
([LICENSE](LICENSE)). A final license for Mythweave hasn't been chosen yet.

- Created by **ashi**.
- Built on the [Fabric example mod](https://github.com/FabricMC/fabric-example-mod) template.
- Cindercub's design, model, and texture are original to this project.

Mythweave is not an official Minecraft product and is not approved by or associated with Mojang or Microsoft.
