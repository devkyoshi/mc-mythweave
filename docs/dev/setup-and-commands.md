# Developer setup and commands

Everything you need to build, run and test Mythweave locally. For where code goes and what
each file does, see [project-structure.md](project-structure.md). For the rules and
checklists, see [CONTRIBUTING.md](../../CONTRIBUTING.md).

- [Prerequisites](#prerequisites)
- [First-time setup](#first-time-setup)
- [Toolchain versions](#toolchain-versions)
- [Running the game](#running-the-game)
- [Gradle tasks](#gradle-tasks)
- [Run folders: logs, worlds, screenshots](#run-folders-logs-worlds-screenshots)
- [In-game commands for testing](#in-game-commands-for-testing)
- [Asset scripts](#asset-scripts)
- [Reading Minecraft's code](#reading-minecrafts-code)
- [Building a release JAR](#building-a-release-jar)
- [Continuous integration](#continuous-integration)
- [Troubleshooting](#troubleshooting)

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | **25** | Minecraft 26.3 needs Java 25. Windows: `winget install Microsoft.OpenJDK.25` |
| Git | any recent | |
| IDE | IntelliJ IDEA (recommended), or VS Code / Eclipse with Java support | |
| PowerShell | 5.1+ (built into Windows) | Only needed for `dev.cmd` and the `tools/` scripts |

Gradle is **not** needed: the wrapper (`gradlew` / `gradlew.bat`) downloads the right version.

`dev.cmd` and `tools/` are Windows scripts. On macOS or Linux, use the Gradle commands
directly. They do the same thing, apart from the multiplayer setup helpers (EULA prompt,
offline mode, operators); that section says how to do those by hand.

## First-time setup

1. **Clone:**
   ```bash
   git clone https://github.com/devkyoshi/mc-mythweave.git
   ```
2. **Check Java** (open a *new* terminal after installing the JDK):
   ```bash
   java -version
   ```
   It must say 25. If an older version shows, set `JAVA_HOME` to the JDK 25 folder. `dev.cmd`
   also finds JDK 25 on its own.
3. **First build.** This downloads Minecraft, Fabric and dependencies, which takes a few
   minutes once; after that, builds take seconds:
   ```bash
   ./gradlew build
   ```
   On Windows PowerShell, write `.\gradlew.bat` wherever this guide says `./gradlew`.
4. **Open in your IDE.** In IntelliJ: *File → Open* and choose the project folder. It imports
   as a Gradle project. Set the project SDK to JDK 25 (*File → Project Structure → SDK*).
   Loom generates run configurations: **Minecraft Client**, **Minecraft Client 2** and
   **Minecraft Server**.
5. **Decompile Minecraft** once, so you can open vanilla classes with sources (see
   [Reading Minecraft's code](#reading-minecrafts-code)):
   ```bash
   ./gradlew genSources
   ```
6. **Run it:** `.\dev.cmd client` (Windows) or `./gradlew runClient`.

## Toolchain versions

Change these only on purpose, in their own commit, and test the game afterwards.

| Item | Version | Set in |
|---|---|---|
| Minecraft | 26.3 | `gradle.properties` → `minecraft_version` |
| Fabric Loader | 0.20.1 | `gradle.properties` → `loader_version` |
| Fabric API | 0.165.0+26.3 | `gradle.properties` → `fabric_api_version` |
| Fabric Loom | 1.19-SNAPSHOT | `gradle.properties` → `loom_version` |
| Gradle | 9.8.2 | `gradle/wrapper/gradle-wrapper.properties` |
| Java | 25 | `build.gradle` (`release = 25`) |
| Mod version | 1.0.0 | `gradle.properties` → `version` |

Minecraft 26.x ships unobfuscated, so there is no mappings step: code uses Mojang's names
(`net.minecraft.resources.Identifier`, …) directly. Latest versions are listed at
<https://fabricmc.net/develop>.

## Running the game

### The launcher (Windows): `dev.cmd`

Double-click `dev.cmd` for a menu, or run it with a command:

| Command | What it does |
|---|---|
| `.\dev.cmd` | Menu: pick 1–5 |
| `.\dev.cmd client` | Minecraft with the mod, signed in as **Player1** (singleplayer) |
| `.\dev.cmd client2` | A second Minecraft as **Player2**, with its own folder |
| `.\dev.cmd server` | The dedicated server only (`run-server/`) |
| `.\dev.cmd multiplayer` (or `mp`) | Server + Player1 + Player2, each in its own window. Waits for the server, then both clients join it automatically |
| `.\dev.cmd build` | Builds the JAR |
| `.\dev.cmd help` | Built-in help |

- **Add `-Join`** to `client` or `client2` to make that client join `localhost:25565` on start,
  e.g. after you close a client window by accident: `.\dev.cmd client -Join`.
- **Finds Java itself:** it uses `JAVA_HOME` if that's JDK 25; otherwise it searches the usual
  install folders (Microsoft, Adoptium, Oracle).
- **First server start:** it shows the Minecraft EULA link and asks *y/N* (accepting is your
  decision). It also sets `online-mode=false` in `run-server/server.properties`, because dev
  clients have no Microsoft login, and writes `run-server/ops.json` making Player1 and Player2
  operators, so `/summon` and the test commands work.
- **Execution policy:** `dev.cmd` runs `dev.ps1` with `-ExecutionPolicy Bypass`, so you don't
  have to change your PowerShell policy. Prefer `dev.cmd` over calling `dev.ps1` directly.

**Stopping:** close a client window as normal. For the server, type `stop` in its window.

### Fixed dev usernames

The clients always sign in as **Player1** and **Player2**. Minecraft's default dev name is
random (`Player123`), and an offline player's UUID is derived from the name, so with random
names every launch is a "different player": your bonded Mythlings would claim to belong to
someone else. Keep the names fixed.

### Multiplayer by hand (macOS / Linux, or without the launcher)

1. `./gradlew runServer` once. It stops and asks you to accept the EULA: set `eula=true` in
   `run-server/eula.txt` (only if you agree to the Minecraft EULA).
2. Set `online-mode=false` in `run-server/server.properties`.
3. Run `./gradlew runServer` again, and in its console type `op Player1` and `op Player2`.
4. In two more terminals:
   ```bash
   ./gradlew runClient -PjoinServer=localhost
   ```
   ```bash
   ./gradlew runClient2 -PjoinServer=localhost
   ```

## Gradle tasks

| Task | What it does |
|---|---|
| `./gradlew build` | Compiles and packages: `build/libs/mythweave-1.0.0.jar` (+ `-sources.jar`) |
| `./gradlew clean build` | The same, from scratch (use if something seems stale) |
| `./gradlew runClient` | Dev client as Player1, folder `run/` |
| `./gradlew runClient2` | Second dev client as Player2, folder `run-client2/` |
| `./gradlew runServer` | Dedicated server, folder `run-server/` |
| `./gradlew runClient -PjoinServer=localhost` | Client that joins that server on start (works for `runClient2` too) |
| `./gradlew genSources` | Decompiles Minecraft so the IDE shows vanilla source |
| `./gradlew --stop` | Stops background Gradle daemons (frees memory, clears odd lock issues) |
| `./gradlew build --refresh-dependencies` | Re-downloads dependencies (after a version change or a broken download) |

Run configurations live in `build.gradle` → `loom { runs { … } }`.

## Run folders: logs, worlds, screenshots

Each run has its own folder. All three are git-ignored, so they're yours to break.

| Folder | Used by | Useful contents |
|---|---|---|
| `run/` | `runClient` / Player1 | `logs/latest.log`, `saves/` (worlds), `screenshots/` (F2), `options.txt` |
| `run-client2/` | `runClient2` / Player2 | same as above |
| `run-server/` | `runServer` | `logs/latest.log`, `world/`, `server.properties`, `eula.txt`, `ops.json` |

- **Fresh start:** delete a world (`run/saves/<name>` or `run-server/world`) or the whole folder.
- **Test with an existing world** as well as new ones, so broken saves get caught.
- **Harmless noise in the logs:**
  - Windows "Perflib" / OSHI warnings;
  - `401` and Realms errors on the client (offline dev account);
  - "SERVER IS RUNNING IN OFFLINE/INSECURE MODE" on the server.

## In-game commands for testing

Your dev player needs to be an operator. In singleplayer, create the world with *Allow Commands*.

| Command | Use |
|---|---|
| `/summon mythweave:cindercub` | Spawn a Cindercub (or use the spawn egg from the Spawn Eggs tab) |
| `/mythweave kindle get @e[type=mythweave:cindercub,limit=1,sort=nearest]` | Show a Mythling's Kindle status |
| `/mythweave kindle add @e[type=mythweave:cindercub,distance=..5] 150` | Add Kindle (1–1000). Add `combat` or `exploration` at the end to push the branch |
| `/summon mythweave:cindercub ~ ~ ~ {Owner:[I;1,2,3,4]}` | A Cindercub owned by a nonexistent player (tests ownership handling) |
| `/time set midnight`, `/weather rain` | Check glow and fire effects at night, steam in rain |

The full manual test list is the README's *Testing checklist*.

## Asset scripts

Textures and README images are generated by PowerShell scripts, so art changes are reviewable
and reproducible. Run them from the project root:

```bash
powershell -ExecutionPolicy Bypass -File tools/textures/cindercub.ps1
```

| Script | Regenerates |
|---|---|
| `tools/textures/cindercub.ps1` | Cindercub texture + glow map |
| `tools/textures/cindercub_spawn_egg.ps1` | Spawn egg texture |
| `tools/textures/ember_brazier.ps1` | Ember Brazier textures (+ animated flame) |
| `tools/previews/cindercub.ps1` | README images in `docs/images/` |

Output is byte-identical between runs, so `git status` shows only real changes. Commit the PNGs
together with the script change. Details: [tools/README.md](../../tools/README.md).

## Reading Minecraft's code

Never guess an API from a tutorial for another version. Find the vanilla class that does what
you need and follow it:

1. `./gradlew genSources` (once per Minecraft version).
2. In IntelliJ, *Navigate → Class* (Ctrl+N) finds vanilla classes, e.g. `Wolf`, `TamableAnimal`,
   `CandleBlock`, `EyesLayer`. They open with readable source.
3. Without an IDE, the sources are in
   `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-common-*/26.3/*-sources.jar`
   (common) and `minecraft-clientOnly-*` (client). Extract a single file with `jar xf`.

## Building a release JAR

```bash
./gradlew clean build
```

The result is `build/libs/mythweave-1.0.0.jar` (the `-sources.jar` next to it is for
developers). To install it, put the JAR together with Fabric API in a Fabric 26.3 `mods`
folder; see the README's *Installing (players)*. Bump `version` in `gradle.properties` for
each release.

## Continuous integration

`.github/workflows/build.yml` runs `./gradlew build` on JDK 25 (Ubuntu) for every push and pull
request, and uploads the JARs as a build artifact. A pull request should only be merged when
it's green. CI does not start the game, so in-game testing is still on you (see the PR
template).

## Troubleshooting

| Problem | Fix |
|---|---|
| `java -version` shows an old Java | Open a new terminal after installing, or set `JAVA_HOME` to the JDK 25 folder |
| `JDK 25 not found` from `dev.cmd` | Install it (`winget install Microsoft.OpenJDK.25`) or set `JAVA_HOME` |
| Downloads from `fabricmc.net` hang | Some networks have broken IPv6. Gradle and Java are usually fine; Node tools (e.g. the Fabric CLI) need `NODE_OPTIONS=--dns-result-order=ipv4first` |
| "You need to agree to the EULA" | First server run: answer the `dev.cmd` prompt, or set `eula=true` in `run-server/eula.txt` if you agree |
| Client can't join the dev server (a login or authentication error) | Set `online-mode=false` in `run-server/server.properties` |
| "Unknown or incomplete command" for `/mythweave` | You aren't an operator: `op Player1` in the server console, or *Allow Commands* in singleplayer |
| Port 25565 already in use | A server is still running (maybe a hidden one). Type `stop` in its window, or end the Java process |
| Old classes or resources still show up, or odd build errors | `./gradlew --stop`, then `./gradlew clean build` |
| Bonded Mythling says it's "already bonded to another player" after relaunch | You launched with a different username. Use `dev.cmd` or the Gradle tasks (fixed names) |
| `.ps1 cannot be loaded because running scripts is disabled` | Use `dev.cmd`, or run the script with `powershell -ExecutionPolicy Bypass -File …` |
| Git warns "LF will be replaced by CRLF" | Harmless on Windows; `.gitattributes` keeps the repository consistent |
| Dedicated server crashes with `ClassNotFoundException: net.minecraft.client…` | Something in `src/main` uses client-only code. Move it to `src/client` |
