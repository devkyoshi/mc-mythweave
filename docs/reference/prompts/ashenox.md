# Ashenox — Character Model & Implementation Specification

## 1. Character Identity

- **Name:** Ashenox
- **Mod:** Mythweave
- **Mod ID:** `mythweave`
- **Entity ID:** `mythweave:ashenox`
- **Evolution:** Stage 2 — evolved form of Cindercub
- **Element:** Fire
- **Rarity:** Uncommon
- **Creature Type:** Mythical Fire Fox
- **Role:** Rideable companion and exploration mount
- **Visual Style:** Minecraft voxel / block-based fantasy creature

Ashenox must look like a powerful, majestic evolution of Cindercub. It should retain recognizable characteristics of its original form while having a significantly larger, more mature and intimidating appearance.

## 2. Visual Design

### Overall Appearance

Ashenox is a large mythical fox with a horse-sized body and three enormous blazing tails.

Its design should communicate strength, elegance, fire, and ancient magical energy.

**Body:**
- Dark charcoal and deep volcanic-red primary fur.
- Red-orange highlights around the shoulders, legs and back.
- Cream-colored chest, neck and lower muzzle.
- Muscular, athletic quadrupedal body.
- Longer legs than Cindercub.
- Strong shoulders and hindquarters suitable for carrying a player.
- Subtle glowing orange magical patterns on both sides.

**Head:**
- Fox-like head with a longer, more mature muzzle.
- Two tall pointed ears with dark charcoal tips.
- Glowing amber-orange eyes.
- Cream-colored muzzle.
- Dark nose.
- Layered, flame-like fur around the cheeks and neck.
- A confident and intelligent facial expression.

**Three Tails (IMPORTANT):**

Ashenox MUST have exactly THREE separate tails.

Each tail must:
- Be independently modeled.
- Have its own animation pivot.
- Be large, bushy and flame-shaped.
- Start with dark red or charcoal fur near the body.
- Transition through deep red, orange and bright yellow.
- Have a glowing ember-like tip.
- Be independently animated.
- Produce subtle ember particles.

Tail arrangement:
- One central tail extending behind the body.
- One tail extending diagonally toward the left.
- One tail extending diagonally toward the right.

The three tails must remain visually distinguishable from every viewing angle. Do not combine them into one large tail.

### Legs and Feet

- Four strong fox-like legs.
- Dark charcoal lower legs.
- Orange-red transitions around the upper legs.
- Dark claws or block-shaped paws.
- Stable quadrupedal stance.
- Feet must be positioned correctly for walking and running animations.

### Magical Details

- Glowing orange magical markings on the shoulders and sides.
- Subtle ember particles around the three tails.
- Slight glow around the eyes.
- Small floating embers when standing still.
- More intense ember effects during running.

Avoid excessive particles that could reduce performance.

## 3. Model Dimensions

Ashenox must be approximately the size of a Minecraft horse.

| Property | Target |
|---|---|
| Height | 1.6 blocks |
| Length | 2.0 blocks |
| Width | 0.9 blocks |
| Hitbox height | Approximately 1.6 blocks |
| Hitbox width | Approximately 0.9 blocks |
| Number of legs | 4 |
| Number of tails | Exactly 3 |

These are target dimensions. Adjust model geometry and hitbox alignment together so that the rendered creature fits its collision box.

The player must be able to ride Ashenox comfortably without clipping into its body.

## 4. Model Structure

Create a hierarchical model with independently animated parts.

Suggested structure:

```text
ashenox
├── root
│   ├── body
│   │   ├── chest
│   │   ├── neck
│   │   ├── head
│   │   │   ├── left_ear
│   │   │   ├── right_ear
│   │   │   ├── muzzle
│   │   │   └── eyes
│   │   ├── front_left_leg
│   │   ├── front_right_leg
│   │   ├── back_left_leg
│   │   ├── back_right_leg
│   │   ├── tail_center
│   │   ├── tail_left
│   │   └── tail_right
│   └── magical_details
```

All three tails must have separate pivots and support independent rotations.

Use clean cube geometry and Minecraft-compatible proportions. Avoid unnecessarily complex geometry.

## 5. Texture Specifications

- Texture format: PNG
- Recommended resolution: 128 × 128
- Pixel-art / voxel texture style.
- No blurry or smooth photographic textures.
- Consistent shading across all body parts.

Suggested palette:

| Feature | Color |
|---|---|
| Primary fur | `#29212D` |
| Secondary fur | `#49232B` |
| Volcanic red | `#A52F27` |
| Flame red | `#E84B20` |
| Ember orange | `#FF831F` |
| Flame yellow | `#FFC34D` |
| Chest cream | `#E8D6B9` |
| Eye glow | `#FFB52E` |

Use emissive rendering or an appropriate glow effect where supported. Do not rely on ordinary texture brightness alone to create actual light emission.

## 6. Required Animations

Implement the following animations:

| Animation | Expected behaviour |
|---|---|
| Idle | Subtle breathing and independent tail movement |
| Walk | Natural four-legged walking cycle |
| Run | Faster quadrupedal movement with animated tails |
| Sit | Ashenox sits while keeping its three tails visible |
| Sleep | Relaxed resting pose |
| Look Around | Head and ears turn naturally |
| Happy | Tail movement and excited body animation |
| Alert | Head raised, ears upright and tails slightly elevated |
| Mount Idle | Standing while a player is riding |
| Mount Walk | Walking while carrying a player |
| Mount Run | Running while carrying a player |

The three tails should have subtle independent movement instead of behaving as a single rigid object.

## 7. Mount / Riding Functionality

Ashenox must be a rideable creature.

Required functionality:

- Allow a player to mount Ashenox.
- Allow the player to dismount.
- Support forward movement.
- Support jumping if compatible with the selected mount implementation.
- Support walking and sprinting.
- Use appropriate movement speed for a fantasy mount.
- Prevent the player from being positioned inside the creature.
- Position the player correctly above the creature's back.
- Prevent multiple players from riding simultaneously.
- Support multiplayer synchronization.
- Ensure the riding position remains correct during movement and animation.

**Mount Position:**

The rider should sit approximately above the middle of Ashenox's back, slightly behind the shoulders.

The rider must not intersect the head, neck or three tails.

Keep the riding position configurable so it can be adjusted after testing in Minecraft.

Do not require a saddle unless the mod's gameplay design later introduces one.

## 8. In-Game Appearance

The final model must look natural in Minecraft.

Create reference views for:

1. Front view.
2. Back view.
3. Left side view.
4. Right side view.
5. Top view.
6. Three-quarter perspective view.
7. Player riding Ashenox.
8. Walking animation.
9. Running animation.
10. Idle animation.

The model should remain recognizable from a normal Minecraft third-person camera distance.

## 9. Asset Files

Use a clean asset structure:

```text
assets/mythweave/
├── textures/
│   └── entity/
│       └── ashenox.png
├── models/
│   └── entity/
│       └── ashenox.json
└── animations/
    └── entity/
        └── ashenox.animation.json
```

Adapt the exact paths and model format to the animation/model technology selected for the existing Fabric project.

## 10. Implementation Instructions for the AI Agent

1. Inspect the existing Mythweave project before changing anything.
2. Confirm the Minecraft 26.3 and Fabric API versions from the actual project files.
3. Inspect the existing Cindercub model and follow the established architecture.
4. Create Ashenox as a separate entity and model.
5. Use the provided visual reference as the primary design authority.
6. Preserve exactly three independently animated tails.
7. Implement horse-sized dimensions and an appropriately aligned hitbox.
8. Implement rideable functionality.
9. Implement the required animations.
10. Add texture and particle effects.
11. Register the entity and its renderer correctly.
12. Ensure multiplayer compatibility.
13. Build and test the project.
14. Do not break existing Cindercub functionality.

Do not invent unsupported Minecraft APIs. Use the APIs and model formats compatible with the existing project.

## 11. Important Design Restrictions

- Do not redesign Ashenox into a horse.
- Do not reduce the number of tails.
- Do not merge the three tails into one.
- Do not make Ashenox look like an enlarged Cindercub without meaningful evolution details.
- Do not use smooth, realistic animal meshes.
- Preserve the Minecraft voxel aesthetic.
- Do not add wings or unrelated body parts.
- Maintain the charcoal, volcanic red, orange and yellow fire palette.
- Keep the model optimized for real-time Minecraft gameplay.

**Final Goal:** Create a recognizable, horse-sized, three-tailed mythical fire fox that evolves from Cindercub and becomes the player's rideable exploration companion in Mythweave.