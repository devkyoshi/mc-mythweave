# Mythling evolution

Evolution is driven by **Bond XP** ("Kindle"). The Cindercub line has three stages in a
straight line, with no branching:

```text
Stage 1            Stage 2                    Stage 3
Cindercub  ──────► Ashenox  ────────────────► Emberlynx
(kit)              (horse-sized mount)        (final form)
```

Every name here is original and not taken from any other franchise. (Earlier drafts branched
stage 3 into Ashenox for combat and Solflare for exploration. That branching, and Solflare, were
removed when Ashenox became the rideable stage 2.)

## How Kindle works

Only a **bonded** Mythling earns Kindle. It's decided and saved on the server, like
everything else.

| Source | Kindle |
|---|---|
| Spending time near its owner (within 16 blocks) | +1 per minute |
| Using Ember Spark | +5 |
| Travelling with its owner (per 100 blocks walked together) | +3 |
| Entering a biome it hasn't been to before | +10 |
| A hostile mob dies near it while it's with its owner | +8 |
| Being hurt by a hostile mob and surviving (at most once per 10 s) | +2 |

- **Stage 2 at 150 Kindle** (about 2 to 3 hours of normal play together).
- **Stage 3 at 400 Kindle** (counted from zero, not on top of stage 2).
- **Status:** sneak and right-click your Mythling with an empty hand to see its Kindle and its
  next form (on an Ashenox: sneak-click while it sits).
- **Evolution moment:** it glows, rises slightly while spinning in a burst of flame, and
  lands in its new form, with a sound and a message.

## The forms

| | Cindercub | Ashenox | Emberlynx |
|---|---|---|---|
| Stage | 1 | 2 | 3 (final) |
| Look | ember fox kit: big head, cream snout, cheek ruffs, tall stepped charcoal ears, thick legs on charcoal paws, a huge glowing ember tail; built at 1.5x voxel detail to match the concept art (docs/reference/cindercub) | majestic horse-sized fire fox (docs/reference/ashenox): charcoal and volcanic-red fur, cream chest ruff and cheek fluff, tall flame-tipped ears, glowing amber eyes, glowing spirals on the flanks, three blazing tails | taller, lean ember lynx; fire tail and a small fire mane; crystal horns |
| Size (in game) | 0.6 × 0.9 | 0.9 × 1.6 | 0.69 × 1.09 (to be reworked) |
| Health | 12 | 32 (+4 armor) | 20 |
| Ember Spark | 8 blocks, 15 s | 12 blocks, 10 s | 12 blocks, 10 s |
| New ability | none | **rideable**; **Magma Bite:** sets attackers on fire (mobs only, never blocks); defends its owner | defends its owner |

Every ability stays bounded like Ember Spark: no block fire, no griefing, and all of it
checked on the server.

## How it's built

- **One entity type per form.** Evolving swaps the entity using vanilla's `Mob#convertTo`, the
  same mechanism that turns a zombie villager back into a villager. It keeps the owner, name,
  sitting state and Kindle. Each form has its own model, texture, stats and renderer, so no
  single class turns into a pile of `if (stage == …)` checks.
- **Rules live in one place.** `evolution/EvolutionRules` holds the thresholds and what each
  form becomes (`stageOf`, `nextForm`); `evolution/EvolutionLines` holds each species' line.
  The entities only report Kindle events, which keeps the spec's rule that evolution
  conditions must not be spread through entity code.
- **Sync.** Kindle is stored on the server only. Clients learn about it through the status
  message and the model swap, so no custom packets are needed.

## Phases (each one tested in game before the next)

1. **E1: Kindle.** *Done.* Earn and save Kindle, show the status message, and the
   `/mythweave kindle` test command.
2. **E2: Evolution.** *Done.* The transformation, the entity swap, the room check.
3. **E3: Ashenox as the rideable stage 2.** *Done.* Rebuilt from its design sheet; riding;
   sleep, alert, happy and ridden animations.
4. **E4: Emberlynx as the final form.** Resize and rebalance it for stage 3.
5. **E5: Balance and docs.** Tune the numbers.

## As built: evolving

- **When:** `MythlingEntity` asks `Evolution.checkReady` once a second. If
  `EvolutionRules.nextForm` returns a form, and that form exists in
  `ModEntities.mythlingForm(...)`, the transformation starts. It waits until the Mythling is on
  the ground and not swimming, riding, or being ridden.
- **Form not built yet:** if the next form isn't in the game, the owner gets a "ready to become
  X" message once per load, and Kindle keeps adding up.
- **Transformation** (`Evolution.tick`, 60 ticks):
  - the Mythling is invulnerable, without gravity, and ignores interactions;
  - it rises for the first two thirds, spinning faster and faster, inside two orbiting flames
    with embers and whooshes;
  - it grows (a transient `SCALE` modifier, `mythweave:evolution_growth`) until it's as tall as
    its new form, so the swap doesn't jump in size;
  - `EvolvingTicks` is saved, so a reload resumes the transformation.
- **Room check:** before starting, the new form's hitbox (plus the rise) must be free of
  blocks. If not, the owner gets a "needs more room" message once per load, and it waits.
- **The swap:** vanilla `Mob#convertTo` with `ConversionParams.single`. It carries position,
  name, leash, effects and team. Then `copyBond` sets the owner, tame flag, sit order and
  Kindle (`Kindle#copyFrom`), and gives full health. Gravity and invulnerability are cleared
  first, because `convertTo` copies them.
- **Size:** each form's `createAttributes()` sets vanilla's `SCALE` attribute (Cindercub 1.0,
  Ashenox 1.0 with its model built at full size, Emberlynx 1.15). Vanilla multiplies the
  hitbox, eye height, model, shadow and leash point by it. `MythlingEntity.formScale(type)`
  reads a form's value; on load the base value is reset to it, so creatures saved before a
  size change load at their form's current size.
- **Final stage:** `EvolutionRules.kindleToEvolveFrom(3)` is -1, so the Emberlynx never evolves
  again, and the status reads "fully evolved".
- **Old saves:** the `Combat` and `Exploration` keys an older Kindle save has are ignored.
  Saved Solflares are dropped when their world loads (the entity type no longer exists).

## As built: Ashenox (E3)

- **Look: generated from the design sheet.** `tools/models/ashenox_gen.ps1` builds the model
  and texture from `docs/reference/ashenox`, which is the primary authority; the spec's prose
  is in `docs/reference/prompts/ashenox.md`.
  - **Views:** it crops the sheet's front, side and back views, keys out the background, and
    scales them so the ear tips sit 63 units (2.5 per pixel) above the ground.
  - **Hull:** body, head, ruff and legs are a voxel hull, solid wherever both the side and the
    mirrored front silhouettes are. Width is capped where the front view is widened by cheek
    fur or peeking tails, so the head tapers to a snout. Ears are kept thin.
  - **Parts and boxes:** the hull is split into parts and greedily merged into boxes, about 170
    in all.
  - **Tails:** three chains of 8 tapering segments with a flame cluster at the end, shaped
    from the sheet's plumes.
  - **Texture:** the 512 × 512 texture is projected from the sheet: side faces take the side
    view's colours, front faces the front view's.
    - Every colour is snapped to the sheet's ~10-colour palette and smoothed (the majority of
      five nearby samples wins), so it reads as flat pixel art rather than copying every shade.
      This was chosen over chunkier 1×, 1.5× and 2× builds; `-Shaded` restores the full shading
      and `-Detail` changes the block size.
    - Hidden tops and the rump are plain charcoal and maroon.
    - Bright warm texels (eyes, spirals, inner ears and tips) also go into the glow map.
    - Tails are painted in clean bands, charcoal → red → orange → yellow.
  - **Outputs:** `AshenoxModelGeometry.java` (generated; `AshenoxModel` only animates), the
    textures, and `tools/models/ashenox_parts.json` for the preview renderer.
  - **Model parts:**
    - body;
    - chest ruff (hung from the throat so it hangs straight when sitting);
    - head, with ears and sleep-only eyelids;
    - three tails (`tail_center`, `tail_left`, `tail_right` → `seg1..seg7` → `flame`);
    - four two-part legs hanging from the root.
  - **Glow and light:** the glow uses `PulsingGlowLayer`, and the renderer never draws it
    darker than block light 7.
- **Stats:** 32 HP, 4 armor, 0.5 knockback resistance, 6 attack damage, speed 0.3, jump strength
  0.6, step height 1; `fireImmune()`; hitbox 0.9 × 1.6, eyes at 1.18.
- **Riding** (vanilla `LivingEntity` hooks, the way `AbstractHorse` does it):
  - `MythlingEntity.isRideable()` makes the owner's empty-handed click climb on (sit order
    cleared), a sneak-click sit it down, and a sneak-click while sitting show Kindle;
  - only the owner controls it (`getControllingPassenger`); one passenger at a time (vanilla
    `canAddPassenger`);
  - `tickRidden` faces it where the rider looks; `getRiddenInput` slows backing up and
    strafing; `getRiddenSpeed` is ×1.4 while the rider sprints (`canSprint()` is true, so
    vanilla lets riders sprint);
  - `PlayerRideableJumping` gives the horse's charged jump;
  - its goals can't move, jump or turn it while ridden (`updateControlFlags`);
  - the rider sits at `AshenoxEntity.RIDER_SEAT` (registered with `passengerAttachments`);
    tune that one constant after testing in game.
- **States:** `DATA_SLEEPING` (sitting at night or for 2 minutes; woken by being hurt, ridden
  or ordered) and `DATA_ALERT` (a target, or a hostile mob within 8 blocks, checked every
  10 ticks) are synced booleans, smoothed on the client; happy is an entity event (on mounting,
  Ember Spark, and now and then near its owner).
- **Animation** (procedural, `AshenoxModel#setupAnim`): idle breathing, independent tail sway
  with a ripple down each tail, walk (diagonal pairs with knee bends) blending into a bounding
  run, sit (tails curled around both sides), sleep (lying down, head on paws, eyes closed),
  head look with an idle scan and ear swivel, alert, happy, and a steadier carriage while
  ridden with the tails swept back from the rider.
- **Particles:** client-side embers (`ModParticles.EMBER`) from the three tail tips, more while
  running and fewer while asleep, plus a few floating around it while it stands still.
- **Magma Bite** (`ability/MagmaBite`) is passive, so it's not an `Ability`.
  `AshenoxEntity.doHurtTarget` calls `MagmaBite.onBite`, which ignites the target for 4 s.
  `hurtServer` calls `MagmaBite.onHurt`, which ignites a melee attacker for 3 s. Both skip
  players and fire-immune mobs, and neither ever places fire.
- **Ember Spark:** uses `EmberSparkAbility.EMBERLYNX`.
