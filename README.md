# TV Floating Widget Source

This is a complete Android Studio project for a floating widget application designed for TV interfaces.

## Project Structure
- `app/`: Main Android application module.
- `.github/workflows/`: GitHub Actions configuration for automated CI/CD builds.

## How to Build
1. Open this folder in Android Studio (Iguana or newer recommended).
2. Sync Project with Gradle Files.
3. Build -> Build Bundle(s) / APK(s) -> Build APK(s).

## CI/CD
The project includes a GitHub Action (`.github/workflows/android.yml`) that automatically builds the APK on every push to main. It uses Gradle 8.4 to ensure compatibility.
