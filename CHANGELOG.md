# Changelog

## [1.1.0] - Multiplayer Chunk Streaming & Creative Flight (2026-04-19)

### Features
- **Dynamic Chunk-Based World Generation:** Upgraded the single-chunk limitation to a procedural multi-chunk streaming system.
  - Implemented `World` and `Chunk` management in the `shared` module with `HashMap` addressing.
  - Added new networking messages: `ChunkRequestMessage` and `ChunkDataMessage`.
  - Upgraded Client map rendering natively requesting and assembling independent chunk arrays around the camera radially.
- **Creative Flight System:**
  - Players can toggle flight mode with the `F` key.
  - Flight mode ignores vertical gravity, doubles horizontal maneuverability speed, and negates friction/inertia to allow precise aerial building.
  - Implemented dedicated `handleFlyMovement` mapping within the `VoxelPhysics` update structure.

### Technical & System Improvements
- **Network Buffering:** Raised Client & Server Kryonet object serialization limits from 512KB to 16MB to comfortably broadcast array packets unconstrained without buffering errors during mass chunk-loading.
- **Physics Tick Accuracy:** Migrated `GameInstance` core loop to `System.nanoTime()` variable iteration deltas to exactly mirror the client's high FPS framerate simulation, halting widespread local prediction rubber-banding issues.
- **Memory Optimization:** Mended memory leaks crashing `VoxelScreen` upon return to the Main Menu by explicitly de-activating persistent connection listener listeners.
- **Physics Refactoring:** Abstracted `VoxelPhysics.java`'s massive update chain into singular, specific methods (`handleWalkMovement`, `handleFlyMovement`, `applyPhysics`, `applyDamping`) radically simplifying future feature expansions.
- **Deterministic Slicing:** Modified `TreeGenerator` coordinates mapping using deterministic hashing across boundary zones rather than `nextFloat()` sequencing per block; fixing graphical anomalies where trees generated near borders appeared sliced open.

### Fixes
- `VoxelScreen` no longer misses `Sneak` inputs within local prediction causing downward-flight stutters against the server state.
- Duplicate invocations of `gatherInput` checking triggers sequentially each frame have been deleted to fix single-frame toggles immediately relocking.
