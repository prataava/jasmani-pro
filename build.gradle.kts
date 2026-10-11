// Versi plugin dikunci di satu tempat.
// Pasangan versi: Kotlin 2.2.0, Android Gradle Plugin 8.13.2 (butuh Gradle >= 8.13), Gradle 8.14.3, JDK 17.
plugins {
    id("com.android.application") version "8.13.2" apply false
    kotlin("jvm") version "2.2.0" apply false
    kotlin("android") version "2.2.0" apply false
    kotlin("plugin.serialization") version "2.2.0" apply false
    kotlin("plugin.compose") version "2.2.0" apply false
}
