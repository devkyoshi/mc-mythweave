# Creature journal

The journal is the player's in-game record of Mythlings: the ones they have bonded with,
and every form that exists in the world. It is a reading UI, not a management UI: it never
changes the world, and everything it shows is decided by the server.

## Opening it

- **J** (rebindable in *Options → Controls → Mythweave*). Pressing it again while the
  journal is open closes it.
- **`/mythweave journal`**, available to every player. The command asks the server to send a
  tiny "open journal" packet, so it also works when the player is not the one running the
  world.

While the search field has keyboard focus, the journal key types into it instead of closing;
**Esc** still closes the screen.

The screen is a non-pausing in-game UI (`isPauseScreen()` is false), like the inventory or a
book, so the world stays visible and tamed progress keeps updating while it is open.

## The two tabs

| Tab | Shows |
|---|---|
| **Tamed** | Every Mythling currently bonded to this player, including ones in unloaded chunks or another dimension. |
| **Mythlings** | Every Mythling form in the mod, in evolution-line order, discovered or not. |

A tamed card uses the companion's custom name when it has one; otherwise it uses the form's
name. The detail page adds the form name, stage, stats, abilities, health, and evolution
progress.

## Discovery

A form is discovered for a player when either:

- the player comes within **24 blocks** of a loaded Mythling of that form (checked once a
  second), or
- the player bonds with one.

Discovery is per player and permanent. Undiscovered forms are listed as `???`: the list card
hides the name and icon, and the detail page hides the model, description, stats, abilities,
bonding item, and evolution targets. The `Mythlings` tab's counter only counts forms the
player has actually seen.

## Search and filters

A name search sits in the tab row, and two cycle chips (`Rarity`, `Element`) sit above the
left-hand list. Both tabs share the same search and filter state.

- **Search** matches the companion's custom name and the translated form name,
  case-insensitively.
- **Rarity** cycles `Any → each known rarity → Any`.
- **Element** cycles `Any → each known element → Any`. Elements are stored as tags
  (`fire`, `earth`, `light`), not as the combined display line, so picking `Fire` also finds
  `Fire and Earth` and `Fire and Light`.
- Filter options come only from forms the player has discovered or tamed, so the chips do
  not hint at unseen lore.
- While a search or filter is active, undiscovered forms are hidden from the list. Without
  one, the `Mythlings` tab still shows them as `???`.
- Changing the search or a filter resets paging. The tamed counter becomes `x / y tamed` so
  it is not mistaken for the total.
- If nothing matches, the list and detail pane show a "no matches" message instead of the
  normal empty/select text.

## Where the data lives

```text
Server                                   Client
──────                                   ──────
MythlingJournalService                   JournalData (attachment copy)
  ├─ discovery scan (every second)  ──►   MythlingJournalScreen
  ├─ tamed snapshots (every second)        ├─ Tamed tab
  └─ bond / evolve / death hooks           └─ Mythlings tab
        │
        └─ JournalData on the player's data attachment
           (persistent, copy-on-death, target-only sync)
```

- **`JournalData`** is an immutable record of discovered form ids plus a list of
  `TamedMythling` snapshots. Changing it means replacing the attachment value.
- **`TamedMythling`** caches the last-known name, form, Kindle, affinity, sit/evolving state,
  and health, so an unloaded companion can still be listed. The entity remains the source of
  truth; the snapshot is refreshed whenever it is loaded.
- **`JournalAttachments`** registers the attachment with Fabric: persistent, copied on death,
  and synced only to its owner.
- **`MythlingCatalog`** is the static form list: the hand-written element tags, rarity,
  habitat, description, abilities, and the item icon. Stats and evolution rules are derived
  from the entity type and `EvolutionRules`, not duplicated here.
- **`OpenJournalPayload`** is the only packet. It carries no data; the journal itself arrives
  through the attachment sync.

## Evolution details

The detail page reads `EvolutionRules` directly, so the journal can never drift from the real
rules:

- the form's stage (`stageOf`),
- the Kindle threshold for the next stage (`kindleToEvolveFrom`),
- the current Kindle and combat/exploration affinity for a tamed Mythling,
- the form it is ready to become (`nextForm`), or the branch it is leaning toward
  (`leaning`), or both stage-3 options when it has no leaning yet.

The page scrolls with the mouse wheel when the window is too short for all of it.

## Files

- `src/main/java/com/mythweave/journal/` — data, catalog, attachment, service, events.
- `src/main/java/com/mythweave/network/OpenJournalPayload.java` — open packet.
- `src/client/java/com/mythweave/client/journal/MythlingJournalScreen.java` — the screen.
- `src/client/java/com/mythweave/client/MythweaveClient.java` — keybind and packet receiver.
