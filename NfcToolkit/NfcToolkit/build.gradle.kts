plugins {
    id("com.android.application") version "8.7.0" apply true
    id("org.jetbrains.kotlin.android") version "2.0.21" apply true
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply true
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply true
}

android {
    compileSdk 35
    defaultConfig {
        minSdk 24
        targetSdk 35
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    