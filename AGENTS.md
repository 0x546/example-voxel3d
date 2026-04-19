# AI Agent Guide for example-game

Welcome to the `example-game` project, a Minecraft-like 3D multiplayer game utilizing LibGDX and Kryonet. Note: Anything 2D-related in the codebase is old leftover template code and should be ignored. This guide contains essential project-specific patterns to ensure you're quickly productive.

## Project Structure & Architecture

The application is split into three main modules:
- **`core`**: Contains client-side LibGDX logic, rendering (`VoxelScreen`, `VoxelMeshBuilder`), and game state management (`GameStateManager`).
- **`server`**: Headless server application driven by Kryonet. Accesses game logic via listeners such as `ServerListener` and `PlayerMovementListener`.
- **`shared`**: Shared DTOs, messages, physics logic, and constants. Shared by both `core` and `server`.

## Networking & Kryonet

- **Message Envelopes**: All network payloads should be named `*Message` (e.g., `GameStateMessage`, `PlayerMovementMessage`) and placed in `shared/src/main/java/message/`.
- **Kryo Registration Is Mandatory**: Every new class transmitted over the network MUST be registered in `shared/src/main/java/network/KryoHelper.java` via `kryo.register(MyNewMessage.class)`. Unregistered classes will cause runtime serialization errors.
- **DTOs**: Data transfer objects (like `PlayerState`) should be placed in `shared/src/main/java/message/dto/`.

## Coding Conventions

- **Lombok `@Data`**: When creating network messages or DTOs, strictly use the Lombok `@Data` annotation to generate getters, setters, equals, hashcode, and toString methods. Avoid generating manual boilerplate.
- **Singletons**: Components often rely on simplified static singletons (e.g., `ServerConnection.getInstance()`). Follow this convention for creating global access if DI is not introduced.
- **Client/Server Isolation**: The `server` module never depends on LibGDX graphics components. Do not import `com.badlogic.gdx.*` in the `server` module or it will break the headless server build.

## Developer Workflows

- **Run the Server**: Execute `./gradlew server:run` or launch `ServerLauncher.java` in the `server` module via IDE.
- **Run the Client**: Execute `./gradlew lwjgl3:run` or start `Lwjgl3Launcher.java` in the `lwjgl3` module. *Note: When running locally, start the Server before any client.*
- **Build the Game**: Use `./gradlew lwjgl3:jar` to compile a runnable Java archive (JAR) for distribution.
- **Clean the Build**: Use `./gradlew clean` to clean cached build artifacts.

## File References

- **Kryo Registry**: `shared/src/main/java/network/KryoHelper.java`
- **Server Entrypoint**: `server/src/main/java/ee/taltech/examplegame/server/ServerLauncher.java`
- **Client Connection**: `core/src/main/java/ee/taltech/examplegame/network/ServerConnection.java`
- **Server Netcode Listeners**: `server/src/main/java/ee/taltech/examplegame/server/listener/ServerListener.java`


