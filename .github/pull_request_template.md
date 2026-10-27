## What this changes

<!-- One feature or one phase per PR. What does a player (or a developer) notice? -->

## How I tested it

- [ ] `./gradlew build` passes
- [ ] Tried it in game (`dev.cmd client`), including an **existing** world
- [ ] Dedicated server starts (`dev.cmd server`), no `ClassNotFound`
- [ ] Multiplayer checked if ownership or interactions changed (`dev.cmd multiplayer`)

## Checklist

- [ ] No save keys or ids renamed (entity/item/block ids, NBT keys)
- [ ] Gameplay decided on the server; no `net.minecraft.client` imports in `src/main`
- [ ] New text is in `lang/en_us.json`
- [ ] Regenerated textures committed with their `tools/` script change
- [ ] README / CONTRIBUTING / docs updated if behaviour or structure changed
