# Infinite Dimensions (stage 1)

Recreation of the 20w14∞ April Fools snapshot for Minecraft 26.1.x on Fabric.

## What works in stage 1
- `/warp <text or number>`: no cheats needed. Same text → same dimension in a given world.
- Throw a written book or book & quill into a Nether portal: the portal becomes a **Neither portal**
  linked to the dimension for that text, and the book is consumed. Stand in it (4s in survival) to travel.
- Each dimension gets a seeded Overworld / Nether / End style using the **vanilla** chunk generators.
- Dimensions persist across restarts (reopened on server start).
- `/infdim home` (not in the original): back to Overworld (0, 0).

Not yet: random biomes, block swaps, mobs, sky/tint, the 43 easter-egg dimensions, new blocks/items,
the stat/advancement, `/dimensiondebug`. See the staged plan.

## Setup
1. Fill in the five `REPLACE_*` values in `gradle.properties` (Fabric versions: https://fabricmc.net/develop/,
   Fantasy: https://maven.nucleoid.xyz/xyz/nucleoid/fantasy/ , look for a `+26.1.x` build).
2. Add a Gradle wrapper: copy `gradlew`, `gradlew.bat` and `gradle/` from the Fabric example mod (26.1 branch),
   or run `gradle wrapper --gradle-version latest`.
3. JDK 25. `./gradlew build` → `build/libs/infinite-dimensions-0.1.0.jar`. `./gradlew runClient` to test.

## Optimization-mod compatibility
There are **no mixins** in stage 1. Nothing touches chunk meshing, rendering, ticking or worldgen internals.
Worldgen is vanilla's generators; dimension parameters are pure functions of (world seed, id).

## Unverified API spots (written without being able to compile)
If the build fails, it will almost certainly be one of these:
| File | Risk |
|---|---|
| `DimensionHost.open()` | Fantasy method names for 26.x (`RuntimeLevelConfig`, `getOrOpenPersistentLevel`, `asLevel`) |
| `DimensionHost` / `BookPortals` | Fabric attachment API names (`AttachmentRegistry.create`, `getAttachedOrCreate`, `setAttached`) |
| `DimensionHost.sendTo()` | `ServerPlayer.teleportTo(...)` signature |
| `ModBlocks` / `NeitherPortalBlock` | `Properties.setId`, `getShape` visibility/signature |
| `DimensionIds` | `WrittenBookContent.pages()` / `Filterable.raw()` |
| `InfiniteDimensionsClient` | `BlockRenderLayerMap` / `ChunkSectionLayer` names |
| any file | `Identifier` (renamed from `ResourceLocation`), `getMinY()/getMaxY()` |
