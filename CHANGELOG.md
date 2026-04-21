# Changelog

## [1.2.1] - Water Physics & Rendering Enhancements

### Features
- **Dynamic Water Flow:** Formulated a chunk-aware water update sub-loop inside the server handling automated 3D fluid expansion according to a priority downwards-vector framework. Added concurrent thread protections for global modifications.
- **Gravity Blocks:** Implemented `updateFallingBlocks` behavior simulating sand physics.
- **Enhanced Procedural Shader:** Refined global underwater tinting algorithms to explicitly verify positional block submersion states using binary markers mapped in vertex textures (`a_texCoord1.y`) instead of an absolute world-plane Y-value limit.

### Technical & System Improvements
- **Block Iteration Restructure:** Consolidated heavily nested x/y/z world loops traversing global bounds inside `GameInstance` into a clean single-method approach (`processWorldBlocks`) utilizing Java `@FunctionalInterface` interfaces.
- **Input Robustness:** Stopped UI tables in `TitleScreen` from constantly regenerating their memory references resolving undetected click-events.

### Fixes
- **Render Pipeline Layering:** Fixed culling logic hiding opaque models when peering through liquid surfaces. Split the master rendering loop into distinct passes separating opaque geometry from transparent. Incorporated `glDepthMask(false)` guaranteeing subsequent semi-opaque instances won't overwrite local z-buffers hiding other elements (like the Pause Menu).

## [1.2.0] - Creative Mode & New Procedural Blocks

### Features
- **Creative Mode Block Interaction:** Implemented 3D raycasting for block placement (right-click) and removal (left-click). Includes a highlighted bounding box wireframe around the selected block.
- **Interactive Action Bar:** Added a scrollable 2D HUD at the bottom of the screen (`VoxelHud`) to cycle through available building materials.
- **New Procedural Blocks:** Added Sand, Brick, and Glass. 
- **World Persistence & Dynamic Title Menu:** Server instances stay alive when empty instead of terminating instantly. `TitleScreen` now actively polls the server with `ServerStatusRequestMessage` to reveal context-sensitive buttons (Continue, New Game, Reset World) and a live player counter.
- **Transparent Rendering Pass:** Expanded `VoxelMeshBuilder` with a third discrete rendering pass specifically for transparent blocks (Glass) preventing internal geometry culling and fixing depth buffer blending against water and solids.

### Technical & System Improvements
- **Continuous Block Placement:** Added a `blockActionTimer` enabling continuous smooth block placement/breaking while holding the mouse button.
- **Player Placement Safety:** Added spatial intersection checks against the player bounding box to prevent trapping the player inside newly constructed blocks.
- **Indestructible Bedrock:** Added a bedrock limit protecting the world floor (`Y=0`) from being broken.

### Fixes
- **Procedural Shader Details & Alignment:** Improved value noise shading for Dirt, Grass, and Stone, and fixed the block UV projection math locking side faces to prevent Brick and Wood textures from stretching or misaligning horizontally.
- **UV Edge Artifacts:** Resolved Z-fighting and edge flickering on brick/wood by implementing `safeUV` rounding offsets inside the fragment shader. 
- **Client Prediction Rubber-Banding:** Fixed continuous one-block wide teleports caused by local chunks generating. Enforced a rigid `Math.min(delta, 0.1f)` maximum time-step in `VoxelScreen` ensuring the client doesn't over-predict movements past the server during frame drops.

## [1.1.0] - Multiplayer Chunk Streaming & Creative Flight

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
