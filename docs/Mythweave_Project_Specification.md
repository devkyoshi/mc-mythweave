# Mythweave --- Minecraft Fabric Mod Project Specification

**Document purpose:** Give this specification to an AI coding agent as
the source of truth for planning and implementing the project.\
**Project name:** Mythweave\
**Mod ID:** `mythweave`\
**Java package:** `com.ashi.mythweave`\
**Target:** Minecraft Java Edition 26.3 with Fabric\
**Language:** Java\
**First creature:** Cindercub\
**Project stage:** Beginner-led, step-by-step development

------------------------------------------------------------------------

## 1. Project vision

Mythweave is a Minecraft Java mod about discovering, bonding with,
raising, and eventually evolving original magical creatures called
**Mythlings**.

The mod should preserve the familiar Minecraft survival experience while
adding a creature-collecting progression loop. Mythlings should be
useful companions, not merely cosmetic pets: in later versions they can
provide exploration utility, interact with special world objects, and
use magical abilities.

The first Mythling is **Cindercub**, a small ember fox creature. The
first milestone is not a complete creature-collecting game. It is one
polished, working creature that can spawn, be interacted with, bond with
a player, follow its owner, sit, and retain its owner relationship after
saving and reloading the world.

### Design pillars

1.  **Original identity:** Create original creatures and mechanics. Do
    not copy Pokémon names, designs, models, animations, sounds, logos,
    or other protected assets.
2.  **Minecraft-first:** The mod should feel natural in Minecraft
    survival, using blocks, items, biomes, crafting, exploration, and
    familiar interaction patterns.
3.  **Small, complete increments:** Build one working feature at a time
    and verify it in-game before adding the next.
4.  **Extensible architecture:** New Mythlings should be addable without
    duplicating the entire entity, bonding, and ability systems.
5.  **Beginner-readable code:** Prefer clear classes and straightforward
    Java over clever abstractions or premature frameworks.
6.  **Reliable saves and multiplayer foundations:** Entity ownership and
    persistent state must use the correct Minecraft/Fabric mechanisms
    for the chosen version.

------------------------------------------------------------------------

## 2. Technology and version constraints

-   Minecraft Java Edition: **26.3**
-   Mod loader: **Fabric**
-   Language: **Java**
-   Build system: **Gradle using the official Fabric template**
-   IDE: IntelliJ IDEA is recommended, but not required.
-   JDK: Use the Java version required by the official Fabric template
    and Minecraft 26.3. Confirm this from current official documentation
    and generated project files; do not blindly downgrade or upgrade the
    template.
-   Dependencies: Fabric Loader and the matching Fabric API version
    generated or recommended for the target version.

### Version compatibility rules

Minecraft and Fabric APIs change between versions. Before implementing
code:

1.  Inspect the generated project's `gradle.properties`,
    `build.gradle`/`build.gradle.kts`, `settings.gradle`,
    `fabric.mod.json`, and source tree.
2.  Treat the generated template as authoritative for dependency
    versions and entry-point conventions.
3.  Verify entity registration, attributes, spawn placement,
    interaction, networking, rendering, data components, and persistence
    APIs against documentation or source matching Minecraft 26.3.
4.  Do not copy code from tutorials for older Minecraft versions without
    checking API compatibility.
5.  Do not silently change the Minecraft version, Fabric Loader, Fabric
    API, Loom, Gradle, or Java versions.
6.  If a required API is unclear, explain the uncertainty and inspect
    the matching documentation/source before implementing it.

------------------------------------------------------------------------

## 3. Creature concept: Cindercub

Cindercub is the first Mythling and the vertical-slice creature used to
prove the mod's core systems.

### Fiction and appearance

-   Species: Ember fox Mythling
-   Element: Fire / Ember
-   Rarity: Common
-   Habitat concept: Savannas, badlands, and eventually selected warm
    caves or custom magical locations
-   Personality: Curious, playful, energetic, slightly mischievous
-   Silhouette: Small fox-like body, oversized ears, short legs,
    expressive face, and a distinctive glowing ember tail tip
-   Palette: Warm orange, burnt red, charcoal accents, and a controlled
    glow effect
-   Visual direction: Readable, charming, blocky Minecraft-compatible
    style; avoid excessive particles or an overly complex model.

The first implementation may use a simple placeholder model/texture. The
creature must be functionally complete before advanced animation or
visual effects are attempted.

### Initial abilities

For the first release, implement at most one simple ability:

**Ember Spark** (preferred first ability) - Triggered by a clear,
documented interaction or command/test mechanism. - Produces a small,
controlled visual effect and/or ignites a designated test target or
special brazier. - Must have a cooldown. - Must not create uncontrolled
fires, grief player builds, or cause excessive server load. - The exact
effect should be selected after the base creature and interaction APIs
are working.

A later utility ability may let Cindercub activate a custom **Ember
Brazier** block. This is a better first utility target than letting the
creature ignite arbitrary blocks in the world.

### Future evolution (not in the first milestone)

-   **Ashenox:** A combat-focused, volcanic fox evolution.
-   **Solflare:** A utility/exploration-focused evolution.
-   Evolution should eventually be driven by explicit conditions, not by
    hard-coded assumptions spread across entity code.

Do not implement evolution in the first vertical slice.

------------------------------------------------------------------------

## 4. First milestone: a working Cindercub

The first milestone is complete when all of the following work in a
development client:

1.  The mod loads without errors.
2.  Cindercub is registered as a custom entity type using the APIs for
    Minecraft 26.3 and Fabric.
3.  Cindercub has an entity definition, sensible dimensions, and
    suitable attributes for its role.
4.  Cindercub can be spawned reliably for testing, preferably through a
    development command or spawn egg.
5.  A visible model and texture are present. A temporary placeholder is
    acceptable only during early entity registration work.
6.  It has basic idle and movement behavior.
7.  A player can bond with it through a documented interaction using a
    sensible item, such as a suitable food or a future Aether Shard.
8.  On successful bonding, the entity records the correct owner.
9.  An owned Cindercub follows its owner when appropriate.
10. The owner can toggle sitting/standing using a clear interaction.
11. The creature does not keep following a different player after it has
    been bonded.
12. Ownership and sitting state persist correctly across save, quit, and
    reload.
13. The mod builds successfully into a JAR.
14. The implementation includes simple testing instructions and has no
    known severe console errors.

If a feature depends on an API or behavior that is different in
Minecraft 26.3, adapt it to the current supported API and document the
choice.

------------------------------------------------------------------------

## 5. Suggested player experience

### Initial test flow

1.  Start a test world with the mod.
2.  Obtain or spawn a Cindercub using a documented development
    mechanism.
3.  Approach it and use the bonding item.
4.  Receive clear feedback when bonding succeeds or fails.
5.  Walk away and verify that Cindercub follows the owner.
6.  Toggle sitting and verify it stays in place.
7.  Toggle standing and verify it resumes following.
8.  Save and quit the world.
9.  Reload the world and verify that the same owner relationship and
    sitting state remain.
10. Test interactions with a second player in a local/dedicated
    multiplayer environment if multiplayer is supported by the selected
    implementation stage.

### Bonding rules

For the initial implementation: - A wild Cindercub can be bonded with a
defined item. - Bonding has a reasonable success rule. For a first
prototype, guaranteed success on valid interaction is acceptable;
randomness is not required. - Successful bonding records the player UUID
as owner. - A creature already owned by a player cannot be claimed by
another player through the normal bonding interaction. - Provide visible
feedback for success, failure, and already-owned state. - Do not add
breeding, trading, hunger simulation, loyalty decay, or complex trust
scores yet.

A more advanced trust system can be designed after the basic owner
relationship works reliably.

------------------------------------------------------------------------

## 6. Interaction and behavior requirements

### Wild behavior

-   Behave like a small passive or neutral creature.
-   Wander naturally without moving too quickly.
-   Avoid needless attacks on players.
-   Have a basic idle state.
-   Use appropriate navigation and movement for its dimensions.

### Owned behavior

-   Follow its owner within a sensible distance.
-   Stop following while sitting.
-   Resume following when standing.
-   Avoid teleporting constantly or getting stuck in a tight loop.
-   Handle owner logout or temporary absence safely.
-   Do not interfere with the player's normal movement.
-   If owner death/respawn behavior needs special handling, choose and
    document a simple reliable default.

### Interaction priority

Define interaction handling carefully so that: - The owner can toggle
sitting. - A wild creature can be bonded. - Other players cannot steal
an owned creature. - The ability interaction, if added, does not
override bonding or sitting accidentally.

Use the interaction APIs available in the target version. Avoid relying
on client-only logic for authoritative gameplay state.

------------------------------------------------------------------------

## 7. Architecture guidelines

Keep the initial codebase simple. The names below are suggestions; align
them with the generated template and current Fabric conventions.

``` text
src/main/java/com/ashi/mythweave/
├── Mythweave.java
├── registry/
│   ├── ModEntityTypes.java
│   ├── ModItems.java
│   └── ModBlocks.java              # Add when needed
├── entity/
│   └── CindercubEntity.java
├── entity/ai/                      # Only when separate AI classes are justified
├── client/
│   └── MythweaveClient.java        # Client entry point if required
├── client/render/
│   └── CindercubRenderer.java      # Names/API depend on target version
├── client/model/
│   └── CindercubModel.java         # Names/API depend on target version
├── item/
│   └── AetherShardItem.java        # Add when bonding item is implemented
└── ability/
    └── CindercubAbility.java       # Add after core companion behavior
```

Resources will include the relevant `fabric.mod.json`, assets, entity
texture/model files, item models, language entries, and any required
data files. Exact paths and formats must follow Minecraft 26.3 and the
chosen model/rendering approach.

### Responsibility boundaries

-   **Main mod entry point:** Initialize registrations and common-side
    setup.
-   **Registries:** Register entity types and later items/blocks in one
    predictable place.
-   **Cindercub entity:** Own creature-specific state and behavior, such
    as owner relationship and sitting state, using supported Minecraft
    entity/taming mechanisms where suitable.
-   **Client entry point:** Register client-only renderer/model code.
    Never load client-only classes on a dedicated server.
-   **Renderer/model:** Handle appearance only; avoid placing bonding or
    gameplay logic here.
-   **Ability code:** Keep ability rules separate from rendering and
    entity registration once there is enough code to justify a separate
    class.

### Future extensibility

When a second Mythling is introduced, identify reusable behavior and
extract it into a shared base class or composition pattern only if it
genuinely reduces duplication. Do not build a generic creature framework
before the first creature works.

Potential future concepts: - `MythlingEntity` base class - reusable
owner/bonding behavior - a spell/ability definition system - data-driven
creature definitions - evolution definitions and conditions -
habitat/spawn configuration - creature traits and rarity - a player
collection or journal

These are future directions, not mandatory first-release classes.

------------------------------------------------------------------------

## 8. Rendering and assets

For a beginner-friendly workflow: 1. Start with the simplest
renderer/model supported by the chosen version. 2. Verify that the
entity can spawn and render before polishing textures. 3. Use Blockbench
if a custom blocky model is needed. 4. Keep the model modest: body,
head, ears, legs, tail, and ember detail. 5. Make sure textures are
placed in the correct resource path and named consistently. 6. Test the
model from several angles and under different lighting. 7. Add animation
only after the static model renders reliably.

Do not generate or include copyrighted Pokémon models, sprites, sounds,
or copied character designs. All Mythweave creature art should be
original or properly licensed.

------------------------------------------------------------------------

## 9. Multiplayer and persistence requirements

Minecraft entity state must be handled using the current version's
supported synchronization and persistence mechanisms.

-   Owner identity should be stored as a UUID, not a display name.
-   Owner and sitting state must be saved and restored.
-   Client visuals should reflect authoritative server state.
-   Bonding and ability effects must be validated server-side.
-   Client-only rendering registration must be isolated from common
    code.
-   Do not trust a client to decide whether a player owns a creature or
    whether a cooldown has elapsed.
-   Avoid sending unnecessary network updates every tick.
-   Test dedicated-server startup to catch accidental client-class
    references.
-   Do not implement custom packets unless the feature actually needs
    them; use vanilla/Fabric-supported mechanisms where possible.

The exact persistence and synchronization implementation must be
verified for Minecraft 26.3.

------------------------------------------------------------------------

## 10. Development phases and acceptance criteria

### Phase 0 --- Inspect the project

-   Confirm target Minecraft version, Java version, Fabric Loader,
    Fabric API, Loom, and Gradle.
-   Run the generated development client.
-   Run the generated build task.
-   Record the actual generated project structure.

**Done when:** the untouched template launches and builds.

### Phase 1 --- Register Cindercub

-   Register the entity type.
-   Define dimensions and required attributes.
-   Add a safe testing spawn mechanism.
-   Confirm entity creation and removal work.

**Done when:** a Cindercub can be spawned repeatedly without crashing.

### Phase 2 --- Render Cindercub

-   Register the client renderer.
-   Add a basic model and texture.
-   Fix missing texture, model, and resource errors.

**Done when:** the creature is visibly rendered in-world and in relevant
debug/test views.

### Phase 3 --- Add basic behavior

-   Add idle and wander behavior.
-   Tune movement speed and dimensions.
-   Verify it behaves correctly around blocks and players.

**Done when:** it behaves like a believable small creature without
jittering or constant pathfinding failures.

### Phase 4 --- Add bonding and companion behavior

-   Add a bonding item or use a deliberate prototype interaction.
-   Store owner UUID.
-   Add follow and sit/stand behavior.
-   Prevent normal interactions from reassigning an owned creature.

**Done when:** the full bonding/follow/sit loop works.

### Phase 5 --- Save/load and multiplayer checks

-   Save owner and sitting state.
-   Verify state after relogging and world reload.
-   Test server-side interaction validation.
-   Verify the mod does not crash on a dedicated server.

**Done when:** state is stable and interactions are authoritative.

### Phase 6 --- Add one signature ability

-   Implement Ember Spark or Ember Brazier interaction.
-   Add cooldown and clear feedback.
-   Ensure the effect is bounded and server-validated.

**Done when:** the ability works repeatedly, respects its cooldown, and
cannot cause uncontrolled world damage.

### Phase 7 --- Polish the vertical slice

-   Improve texture/model.
-   Add language strings and basic sounds if feasible.
-   Test normal gameplay and edge cases.
-   Update README with setup, build, and testing instructions.

**Done when:** a new developer can build the project and a player can
understand how to find, bond with, and use Cindercub.

------------------------------------------------------------------------

## 11. Testing checklist

Test each phase in a fresh development world and keep a short record of
the result.

### Entity

-   [ ] Entity registration succeeds.
-   [ ] Entity spawns without crashes.
-   [ ] Model and texture load correctly.
-   [ ] Entity moves and navigates reasonably.
-   [ ] Entity can be removed and respawned safely.

### Bonding

-   [ ] Valid bonding interaction succeeds.
-   [ ] Invalid interaction gives appropriate feedback.
-   [ ] Owner is saved as a UUID.
-   [ ] Another player cannot claim an already-owned creature.
-   [ ] Owner can toggle sitting and standing.

### Persistence

-   [ ] Owner survives save/reload.
-   [ ] Sitting state survives save/reload.
-   [ ] Missing or invalid owner data does not crash the game.
-   [ ] Entity remains usable after the owner reconnects.

### Multiplayer and release

-   [ ] Server owns authoritative gameplay state.
-   [ ] Client renderer classes are not loaded by a dedicated server.
-   [ ] No severe log errors occur during normal use.
-   [ ] Gradle build succeeds.
-   [ ] Release JAR is produced and its installation steps are
    documented.

------------------------------------------------------------------------

## 12. AI coding agent instructions

The AI agent must follow these working rules.

### Work incrementally

-   Implement only one phase or a small subtask at a time.
-   Explain the purpose of each file and important code section.
-   Do not generate the entire mod in one large, unreviewable change.
-   After each phase, give the exact command to build or run the project
    and the expected result.
-   Stop at the phase boundary and ask the user to run the project and
    share any error before proceeding.

### Inspect before editing

-   Read the existing files before changing them.
-   Preserve the generated project's valid versions and configuration.
-   Never replace the whole build configuration just to fix one
    compilation error.
-   Do not invent Fabric API methods, event names, or registry
    signatures.
-   Verify version-specific APIs using the project sources and matching
    documentation.

### Code quality

-   Use Java, consistent naming, small methods, and clear
    responsibilities.
-   Avoid unnecessary dependencies and premature abstractions.
-   Add comments where Minecraft-specific behavior or version-specific
    decisions are non-obvious.
-   Keep gameplay code server-safe.
-   Do not swallow exceptions or hide build failures.
-   Do not claim the project works unless the relevant build or runtime
    check has actually passed.

### When an error occurs

1.  Read the complete error and identify the first meaningful cause.
2.  Inspect the exact source line and relevant dependency/API.
3.  Propose the smallest targeted fix.
4.  Re-run the relevant build or test.
5.  Explain what caused the issue in beginner-friendly terms.

### Before each task

The agent should report: - Goal of the current phase - Files it intends
to create or modify - Implementation approach - How success will be
verified

### At the end of each task

The agent should report: - Files changed - What was implemented - Exact
build/run command - Expected in-game behavior - Known limitations - What
the user should test before continuing

------------------------------------------------------------------------

## 13. First task for the coding agent

**Do not start by implementing the full creature. Begin with project
inspection.**

Ask the user to provide or open the generated Fabric project. Then:

1.  Inspect the project configuration and source tree.
2.  Confirm the actual Minecraft 26.3, Java, Fabric Loader, Fabric API,
    Loom, and Gradle versions.
3.  Identify the mod entry point and any client entry point.
4.  Run the existing build and, if feasible, the development client.
5.  Report any problems without changing unrelated configuration.
6.  Propose the smallest next step to register a Cindercub entity.
7.  Wait for confirmation before proceeding to the next phase.

The first coding change should be only the minimum required for **Phase
1: register and spawn Cindercub**. Rendering, bonding, abilities, and
evolution must wait until that foundation builds and runs.

------------------------------------------------------------------------

## 14. Definition of the first playable release

The first playable release is successful when a player can:

-   Launch Minecraft with Mythweave installed.
-   Spawn or discover Cindercub using the currently implemented
    test/discovery mechanism.
-   See the creature rendered correctly.
-   Bond with it.
-   Have it follow them and sit on command.
-   Save and reload the world without losing the owner relationship.
-   Use one simple, safe ember-themed ability, if that feature has been
    completed.
-   Build the mod from source using the documented instructions.

**Long-term vision:** expand Mythweave with additional original
Mythlings, elemental abilities, habitats, rare variants, training,
branching evolution, magical shrines, and a creature journal---but only
after the Cindercub vertical slice is reliable.
