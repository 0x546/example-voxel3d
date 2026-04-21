# Voxelgame

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

This project is a 3D Minecraft-like sample game. It was originally a 2D game that was used as an engine template, but has since been refactored into a 3D voxel-based game. The project is a multiplayer game utilizing the LibGDX library for the game and Kryonet for server-side networking.

## How to run

The recommended way to run the game is by using the Gradle wrapper scripts provided in the project.

1.  **Run the server:**
    ```bash
    ./gradlew server:run
    ```
2.  **Run the client:**
    ```bash
    ./gradlew lwjgl3:run
    ```

Alternatively, you can run the game from your IDE by running the `ServerLauncher.java` and `Lwjgl3Launcher.java` files.

## Lombok library

This project uses Lombok, a Java library that helps reduce boilerplate code by automatically generating getters, setters (and even constructors, but not in this project) for any class.
For example look at `...Message` classes that use @Data annotation, which generates both getters and setters.
This makes the code cleaner and more readable.

Learn more about Lombok here: [Introduction to Lombok on GeeksforGeeks](https://www.geeksforgeeks.org/introduction-to-project-lombok-in-java-and-how-to-get-started/)

## Platforms / main modules

- `core`: Main module with the application logic shared by all platforms. Contains mainly client-side logic.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs. For running client side of the game.
- `server`: A separate application without access to the `core` module.
- `shared`: A common module shared by `core` (client) and `server` platforms. Contains shared constants, messages, etc.

## Other files and directories

- `assets`: Contains game resources like sprites, images, fonts, and sounds used by the client.
- `gradle/wrapper`: No need to modify. Contains files for the Gradle wrapper, ensuring consistent Gradle version across different environments.
- `.editorconfig`: No need to modify. Defines coding style rules, such as indentation and line endings in the whole project.
- `.gitattributes`: No need to modify. This .gitattributes file ensures consistent line endings for .bat files.
- `.gitignore`: Tells Git which files and directories to exclude from version control.
- `build.gradle`: No need to modify. Defines the build configuration for a Gradle project, including dependencies, plugins, and tasks.
- `gradle.properties`: No need to modify. Contains project-specific properties and configuration settings, such as version numbers and JVM options.
- `gradlew`: No need to modify. Script that runs Gradle tasks with a specific version.
- `gradlew.bat`: No need to modify. Script that runs Gradle tasks on Windows without a pre-installed Gradle.
- `settings.gradle`: No need to modify. Configures the structure of a multi-project Gradle build, specifying which subprojects are included.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `server:run`: runs the server application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
