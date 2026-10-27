# Phase 0 — Project inspection notes

Recorded 2026-10-27.

## Toolchain (from the generated template — do not change silently)

| Item | Version | Where it's set |
|---|---|---|
| Minecraft | 26.3 | `gradle.properties` → `minecraft_version` |
| Fabric Loader | 0.20.1 | `gradle.properties` → `loader_version` |
| Fabric API | 0.165.0+26.3 | `gradle.properties` → `fabric_api_version` |
| Fabric Loom | 1.19-SNAPSHOT (resolved 1.19.3) | `gradle.properties` → `loom_version` |
| Gradle | 9.8.2 | `gradle/wrapper/gradle-wrapper.properties` |
| Java | 25 (Microsoft OpenJDK 25.0.5) | `build.gradle` → `release = 25` |

- Plugin id is `net.fabricmc.fabric-loom`. Minecraft 26.x ships unobfuscated, so
  there is **no mappings dependency** (no Yarn). Code uses Mojang's names directly
  (e.g. `net.minecraft.resources.Identifier`).
- `loom.splitEnvironmentSourceSets()` is on: client-only code lives in `src/client`
  and cannot be referenced from `src/main`. This is what keeps renderer classes off
  a dedicated server (spec §9).

## Origin and changes from the template

Source: Fabric's example mod (`template-mod-template-26.3`). Only renamed, no
version or build-logic changes:

- Mod id `template-mod` → `mythweave`; package `com.example` → `com.ashi.mythweave`.
- `TemplateMod` → `Mythweave`, `TemplateModClient` → `client.MythweaveClient`.
- Mixin configs → `mythweave.mixins.json` / `mythweave.client.mixins.json`.
- Assets folder → `assets/mythweave/`.
- `fabric.mod.json`: name, description, author updated. License is still the
  template's `CC0-1.0` (and `LICENSE` file) — choose your own license later.
- The two example mixins (`ExampleMixin`, `ExampleClientMixin`) are empty no-op
  injections kept from the template. They can be removed once real code exists.

## Source tree

```text
src/main/java/com/ashi/mythweave/
├── Mythweave.java            # common entry point (MOD_ID, LOGGER, id())
└── mixin/ExampleMixin.java
src/main/resources/
├── fabric.mod.json
├── mythweave.mixins.json
└── assets/mythweave/icon.png
src/client/java/com/ashi/mythweave/client/
├── MythweaveClient.java      # client entry point (renderers go here)
└── mixin/ExampleClientMixin.java
src/client/resources/mythweave.client.mixins.json
```

## Commands

```bash
.\gradlew.bat build        # → build\libs\mythweave-1.0.0.jar
.\gradlew.bat runClient    # dev client, logs in run\logs\latest.log
.\gradlew.bat runServer    # dedicated server check (Phase 5)
```

If `java -version` in a shell still shows 21, open a new terminal, or set
`$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-25.0.5.21-hotspot'` first.

## Results

- `gradlew build`: **BUILD SUCCESSFUL** (first run 6m 15s, mostly downloads).
- `gradlew runClient`: loads `mythweave 1.0.0`, logs `Mythweave initialized`.
- Known harmless log noise in dev: Perflib/OSHI system-info warnings, and
  401 / Realms errors because the dev client uses an offline account.

## Environment gotcha

This machine's IPv6 route times out to `fabricmc.net` hosts. Java/Gradle prefer
IPv4 and work. Node tools (the Fabric CLI) need
`$env:NODE_OPTIONS = '--dns-result-order=ipv4first'`.
