# Mythling evolution: plan (draft for review)

Decisions so far: evolution is driven by **Bond XP** ("Kindle"), and the line **branches at
stage 3** into the two forms named in the spec.

```text
Stage 1            Stage 2              Stage 3
Cindercub  ──────► Emberlynx* ──┬────► Ashenox   (combat: volcanic, armored)
(kit)              (young)      └────► Solflare  (exploration: radiant, swift)
```

\* *Emberlynx* is a placeholder name for stage 2. Alternatives: *Pyrekit*, *Flarewisp*,
*Cinderlynx*. Every name here is original and not taken from any other franchise.

## How Kindle works

Only a **bonded** Mythling earns Kindle. It's decided and saved on the server, like
everything else.

| Source | Kindle | Branch affinity |
|---|---|---|
| Spending time near its owner (within 16 blocks) | +1 per minute | none |
| Using Ember Spark | +5 | Solflare |
| Travelling with its owner (per 100 blocks walked together) | +3 | Solflare |
| Entering a biome it hasn't been to before | +10 | Solflare |
| A hostile mob dies near it while it's with its owner (later: by its own attacks) | +8 | Ashenox |
| Being hurt and recovering (surviving fights) | +2 | Ashenox |

- **Stage 2 at 150 Kindle** (about 2 to 3 hours of normal play together).
- **Stage 3 at 400 Kindle.** The form is whichever affinity is higher, so the way you play
  decides it. Fighting together leads to Ashenox; exploring together leads to Solflare.
- **Status:** sneak and right-click your Mythling with an empty hand to see its Kindle, its
  stage, and which way it's leaning. Normal clicks still toggle sit.
- **Evolution moment:** it glows, rises slightly while spinning in a burst of flame, and
  lands in its new form, with a sound and a message.

## The forms

| | Cindercub | Emberlynx | Ashenox | Solflare |
|---|---|---|---|---|
| Look | chibi kit (current) | taller, lean; fire tail and a small fire mane; horns grow | heavy, obsidian and magma plates, cracked glowing veins, big curved horns | sleek, sun-gold, radiant crest and a long fire tail like a comet trail |
| Size | 0.5 × 0.7 | 0.6 × 0.9 | 0.8 × 1.1 | 0.7 × 1.0 |
| Health | 12 | 20 | 32 | 24 |
| Ember Spark | 8 blocks, 15 s | 12 blocks, 10 s | same as stage 2 | 16 blocks, 6 s, lights several braziers |
| New ability | none | defends its owner (attacks mobs that hurt you) | **Magma Bite:** sets attackers on fire (mobs only, never blocks) | **Sunwarden aura:** you get Fire Resistance and a little Speed while it's near |

Every ability stays bounded like Ember Spark is now: no block fire, no griefing, and all of
it checked on the server.

## How it's built

- **One entity type per form.** Evolving swaps the entity using vanilla's `Mob#convertTo`, the
  same mechanism that turns a zombie villager back into a villager. It keeps the owner, name,
  sitting state, Kindle and affinity. Each form has its own model, texture, stats and
  renderer, so no single class turns into a pile of `if (stage == …)` checks.
- **Rules live in one place.** `evolution/EvolutionRules` holds the thresholds, the branch
  logic and what each form becomes. The entities only report Kindle events to it, which keeps
  the spec's rule that evolution conditions must not be spread through entity code.
- **Sync.** Kindle and affinity are stored on the server only. Clients learn about them
  through the status message and the model swap, so no custom packets are needed.

## Phases (each one tested in game before the next)

1. **E1: Kindle.** *Done.* Earn and save Kindle and affinity, show the status message, and
   the `/mythweave kindle` test command.
2. **E2: Evolution and Emberlynx.** *Done.* The transformation and
   the full stage-2 form: model, texture, stats, and the defend-owner AI.
3. **E3: Ashenox.** Model, texture, stats, Magma Bite.
4. **E4: Solflare.** Model, texture, stats, Sunwarden aura.
5. **E5: Balance and docs.** Tune the numbers and update the README.

## As built (E2)

- **When:** `MythlingEntity` asks `Evolution.checkReady` once a second. If
  `EvolutionRules.nextForm` returns a form, and that form exists in
  `ModEntities.mythlingForm(...)`, the transformation starts. It waits until the Mythling is on
  the ground and not swimming or riding.
- **Branch with no leaning:** at stage 2 with a 0–0 tie, it waits until there is a leaning.
- **Form not built yet:** if the next form isn't in the game (Ashenox and Solflare before
  E3/E4), the owner gets a "ready to become X" message once per load, and Kindle keeps adding up.
- **Transformation** (`Evolution.tick`, 60 ticks):
  - the Mythling is invulnerable, without gravity, and ignores interactions;
  - it rises for the first two thirds, spinning faster and faster, inside two orbiting flames
    with embers and whooshes.
  - `EvolvingTicks` is saved, so a reload resumes the transformation.
- **The swap:** vanilla `Mob#convertTo` with `ConversionParams.single`. It carries position,
  name, leash, effects and team. Then `copyBond` sets the owner, tame flag, sit order and
  Kindle (`Kindle#copyFrom`), and gives full health. Gravity and invulnerability are cleared
  first, because `convertTo` copies them.
- **Emberlynx:**
  - 20 HP, speed 0.3, 4 attack damage;
  - doesn't panic when hurt; `MeleeAttackGoal`, `OwnerHurtByTargetGoal` and `HurtByTargetGoal`;
  - `wantsToAttack` uses the vanilla wolf's rules;
  - Ember Spark is `EmberSparkAbility.EMBERLYNX` (12 blocks, 10 s);
  - it has its own spawn egg, mainly for testing.
