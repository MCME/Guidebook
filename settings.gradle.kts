pluginManagement {
    repositories {
        gradlePluginPortal()
        // Typewriter's module-plugin 2.2.0 (needed for engine 0.9.0) is only published here
        maven("https://maven.typewritermc.com/beta")
    }
}

rootProject.name = "Guidebook"

// The Typewriter display: a Typewriter extension with its own jar and version (ADR 0008)
include("typewriter-display")
