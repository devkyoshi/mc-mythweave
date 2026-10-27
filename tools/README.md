# tools

PowerShell scripts that generate art, so textures can be reviewed and changed as code. Run
them from the project root (Windows; `powershell` 5.1 or later):

```bash
powershell -ExecutionPolicy Bypass -File tools/textures/cindercub.ps1
```

| Script | Writes | Re-run when |
|---|---|---|
| `textures/cindercub.ps1` | `textures/entity/cindercub/cindercub.png` + `cindercub_glow.png` | you change Cindercub's colors or its model's UV layout |
| `textures/emberlynx.ps1` | `textures/entity/emberlynx/emberlynx.png` + `emberlynx_glow.png` | you change Emberlynx's colors or its model's UV layout |
| `textures/ashenox.ps1` | `textures/entity/ashenox/ashenox.png` + `ashenox_glow.png` | you change Ashenox's colors or its model's UV layout |
| `textures/spawn_eggs.ps1` | `textures/item/*_spawn_egg.png` | you change an egg or add a form (add a `New-Egg` line) |
| `textures/ember_brazier.ps1` | `textures/block/ember_brazier_*.png` (+ flame `.mcmeta`) | you change the brazier |
| `previews/mythlings.ps1` | `docs/images/*.png` (README renders and banner) | a Mythling model or texture changes |

- Output is deterministic: the same script always writes byte-identical files, so `git diff`
  only shows real changes.
- Each texture script documents the UV layout it paints. If you change a model's cubes in
  Java, update the script and `previews/mythlings.ps1`, which duplicates each cube list.
- Commit the regenerated PNGs together with the script change.
- Hand-painting in Blockbench or a pixel editor is fine too. If you do, say so in the script
  header, so nobody overwrites your work by re-running it.
- New creature? Copy `textures/cindercub.ps1` to `textures/<name>.ps1` as a starting point.
