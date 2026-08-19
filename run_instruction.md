# Run Instructions

## Prerequisites
* **IDE**: Android Studio (Ladybug or newer recommended) or IntelliJ IDEA.
* **JDK**: JDK 17 or 21.
* **OS**: macOS (recommended for iOS build) or Linux/Windows (for Android build).
* **SDK**: Android SDK installed with `platform-tools` and appropriate Build Tools.

## Project Structure
* `composeApp`: Contains the Jetpack Compose Multiplatform UI code.
* `shared`: Contains business logic, database migrations (MariaDB/SQLDelight), and common models.
* `iosApp`: Native iOS wrapper project (requires Xcode).

## Build and Run

### Android
1. Open the project in Android Studio.
2. Ensure the `local.properties` file points to your Android SDK location.
3. Select the `composeApp` run configuration and click **Run**.

### iOS (requires macOS and Xcode)
1. Navigate to the `iosApp` directory.
2. Run `./gradlew :composeApp:iosBinaries` to prepare the framework.
3. Open `iosApp.xcworkspace` in Xcode.
4. Select a simulator and click **Run**.

## Database Setup
The project uses [SQLDelight/Room] for local data persistence. The initial schema migration is handled automatically upon the first application launch. If you need to reset the database during testing, clear the application data in system settings.

## Troubleshooting
* **Gradle sync issues**: Try running `./gradlew clean` and then re-syncing the project.
* **Missing imports**: Ensure all dependencies in `libs.versions.toml` are correctly resolved.