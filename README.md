# MTGScanner

An Android application for scanning and identifying Magic: The Gathering cards using device camera, optical character recognition (OCR), and the Scryfall API. Built with modern Android technologies following MVVM and Clean Architecture principles.

## Overview

MTGScanner captures card images via the device camera, uses ML Kit for optical text recognition to extract card names, queries the Scryfall API for card details (set images, pricing, rulings, etc.), and displays results in a clean, modern UI built with Jetpack Compose.

## Core Features

- Live camera preview with CameraX for real-time card capture
- OCR text extraction using ML Kit for accurate card name recognition
- Card lookup via Scryfall API for comprehensive card information
- Firebase authentication with Google Sign-In integration
- Image caching and loading with Coil for optimal performance
- Reactive state management with StateFlow and Coroutines
- Modern declarative UI built with Jetpack Compose and Material3
- Database persistence with Room ORM
- Wide device compatibility with Minimum SDK 24 and Target SDK 36


### Prerequisites

- Java Development Kit (JDK) 17 or newer
- Android Studio (2024.1 or newer recommended)
- Android SDK: Minimum version 24, Target version 36, Compile version 37
- Physical device or emulator with camera support

### Setup and Execution

1. Clone the Repository
   ```bash
   git clone <repository-url>
   cd MTGScanner
   ```

2. Firebase Configuration
   - Create a new project in the Firebase Console
   - Download the google-services.json configuration file
   - Place google-services.json in the app/ directory
   - Enable Firebase Authentication with Google provider

3. Gradle Synchronization
   - Open the project in Android Studio
   - Gradle dependencies will synchronize automatically
   - Verify that google-services.json is present in the app/ directory

4. Run the Application
   ```bash
   ./gradlew installDebug
   ```

   Or from Android Studio: Select Run > app

## Architecture

The project follows MVVM (Model-View-ViewModel) combined with Clean Architecture principles for maintainability, testability, and scalability.

### Architectural Layers

**Presentation Layer**
- Composable functions for UI rendering
- ViewModels for state management and business logic orchestration
- StateFlow for reactive, unidirectional data flow
- Material3 design system and theming

**Domain Layer**
- Repository interfaces defining data contracts
- Entity models representing core business concepts
- Use cases encapsulating specific business operations
- Independent of implementation details

**Data Layer**
- Repository implementations coordinating data sources
- Retrofit services for Scryfall API integration
- Room database for local persistence
- Firebase Authentication for user management
- ML Kit OCR for text recognition
- Coil image loading and caching

### Design Principles

- MVVM Pattern: Clear separation between UI, presentation logic, and data models
- Clean Architecture: Layered separation with well-defined boundaries
- Single Responsibility: Each class has one reason to change
- Dependency Inversion: Depend on abstractions, not concrete implementations
- Reactive Programming: StateFlow enables predictable, unidirectional state management

## Technologies and Versions

| Component | Version | Purpose |
|-----------|---------|---------|
| Kotlin | 2.2.10 | Primary programming language |
| JDK | 17 | Java compiler target |
| Android Gradle Plugin | 9.2.1 | Build system |
| Compose BOM | 2026.02.01 | Declarative UI framework |
| Material3 | 1.4.0 | Material Design system and components |
| CameraX | 1.6.1 | Camera capture and frame processing |
| Retrofit | 2.11.0 | HTTP REST API client |
| Gson Converter | 2.11.0 | JSON serialization for Retrofit |
| OkHttp | 4.12.0 | HTTP client with interceptors |
| Coil | 2.6.0 | Image loading and caching |
| ML Kit Text Recognition | 16.0.1 | Optical character recognition (OCR) |
| Room | 2.7.0 | Local database persistence |
| Firebase Auth | 33.1.2 | Authentication service |
| Firebase BOM | 33.1.2 | Firebase dependencies management |
| Credentials API | 1.2.2 | Credential management framework |
| Google ID | 1.1.1 | Google Identity library |
| Coroutines | 1.8.0 | Asynchronous programming |
| ViewModel Compose | 2.8.2 | ViewModel integration with Compose |
| Lifecycle Runtime | 2.6.1 | Lifecycle awareness for coroutines |
| Volley | 1.2.1 | HTTP networking library |
| Android SDK | 24–37 | Minimum 24, Target 36, Compile 37 |

## Project Structure

```
app/
├── build.gradle.kts                    # Module build configuration and dependencies
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml        # Application manifest with permissions
│   │   │   ├── java/                  # Kotlin application source
│   │   │   ├── res/                   # Android resources
│   │   │   └── assets/                # Bundled application assets
│   │   └── res/
│   │       ├── drawable/              # Vector drawables and images
│   │       ├── mipmap/                # App launcher icons
│   │       ├── values/                # String resources, colors, dimensions
│   │       └── xml/                   # Data extraction and backup rules
│   ├── androidTest/                   # Instrumentation tests (Espresso)
│   └── test/                          # Unit tests (JUnit)
└── build/                             # Generated build artifacts
```

## Permissions and Features

The application requires the following permissions declared in AndroidManifest.xml:

- CAMERA: Required to capture card images from the device camera
- INTERNET: Required to communicate with Scryfall API and Firebase services

Required device features:

- android.hardware.camera: Indicates the application requires a camera (required="true")
- android.hardware.camera.any: Supports devices with any camera configuration (required="true")

At runtime, the CAMERA permission must be explicitly requested before initiating the camera capture flow. The application handles permission requests following Android 6.0+ (API level 24) runtime permission model.

## Key Application Flows

### 1. Camera Capture Flow
The CameraView composable utilizes CameraX ImageAnalysis for real-time frame processing with optional live preview capabilities.

### 2. Optical Character Recognition and Card Recognition
ML Kit extracts text from camera frames. The extracted text is processed and sent to the Scryfall API for card matching and identification.

### 3. Card Details Lookup
The CardDetail screen displays comprehensive results from Scryfall including card images, set information, pricing, rulings, and additional metadata. Images are cached using Coil for improved performance.

### 4. User Authentication
Firebase authentication with Google Sign-In is integrated throughout the application. Credentials are managed via the androidx.credentials library to ensure seamless user experience on Android 14 and later devices.

## Reactive State Management with StateFlow

ViewModels expose UI state through StateFlow to enable reactive, unidirectional data flow patterns:

```kotlin
data class CardScreenState(
    val card: Card? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

// Usage in Composables
val uiState = viewModel.cardState.collectAsState()
```

## Scryfall API Integration

The application integrates with the Scryfall API for comprehensive card database access:

- **Base URL**: https://api.scryfall.com
- **Search Endpoint**: `/cards/search?q=<card-name>`
- **Response**: Contains card images, set information, pricing, rulings, and additional metadata

The Retrofit service is configured in the `network/` package:

```kotlin
interface ScryfallService {
    @GET("cards/search")
    suspend fun searchCards(@Query("q") query: String): SearchResponse
}
```

## Firebase Configuration

To set up Firebase authentication:

1. Navigate to Firebase Console (https://console.firebase.google.com)
2. Create a new Firebase project or select an existing one
3. Add an Android app to your Firebase project:
   - Package name: com.zaziapps.mtg_scanner
   - Download the google-services.json file
4. Place google-services.json in the app/ directory (same level as build.gradle.kts)
5. Enable Firebase Authentication:
   - Go to Authentication in Firebase Console
   - Enable Google as a sign-in provider
   - Configure the OAuth consent screen in Google Cloud Console
6. Configure app signing for release builds (recommended for production)

The google-services.json file contains all necessary API keys and configuration for Firebase integration.

## Build and Deployment

### Development Build

```bash
./gradlew assembleDebug
./gradlew installDebug
```

### Release Build

```bash
./gradlew assembleRelease
```

### Gradle Tasks

View all available Gradle tasks:
```bash
./gradlew tasks
```

Clean build artifacts:
```bash
./gradlew clean
```

## Known Limitations

- OCR accuracy depends on card image quality, lighting conditions, and camera focus
- Scryfall API imposes rate limits on requests; implement exponential backoff for production use
- Text extraction from foil or textured cards may require improved image preprocessing

## Support

For issues, feature requests, or questions:
- Open a GitHub issue with a clear description
- Include relevant error logs and device information for bug reports
- Reference the affected version and Android SDK level

## Acknowledgments

- Magic: The Gathering is a trademark of Wizards of the Coast
- Card data provided by Scryfall API
- Built with Jetpack Compose, Firebase, and Google ML Kit
