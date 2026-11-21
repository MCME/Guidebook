// The Typewriter display (ADR 0008): a Kotlin Typewriter extension that registers a Display with Guidebook.
// Its own jar and version, released on typewriter-v* tags, so a Typewriter update never forces a Guidebook release
plugins {
    kotlin("jvm") version "2.3.20"
    id("com.typewritermc.module-plugin") version "2.2.0"
}

group = "com.mcmiddleearth"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.mcmiddleearth.com/releases")
}

// The beta Maven repo only publishes the engine as "0.9.0", not per beta build, so compile against that
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.typewritermc" && requested.version!!.startsWith("0.9.0-beta")) {
            useVersion("0.9.0")
        }
    }
}

dependencies {
    // Guidebook is installed as its own plugin, so it's never bundled into this jar
    compileOnly(project(":"))

    // The tests render real Descriptions with Guidebook's AreaText, which needs Adventure from the Paper API
    testImplementation(project(":"))
    testImplementation(libs.paper.api)
    testImplementation(libs.pluginutils)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

typewriter {
    namespace = "mcmiddleearth"

    extension {
        name = "GuidebookTypewriter"
        shortDescription = "Guidebook's Typewriter display"
        description = "Types each Guidebook Welcome into a framed box over the chat, in Pages the player turns " +
            "and closes with a key."
        // Typewriter refuses an extension unless this names its running build exactly
        engineVersion = "0.9.0-beta-177"
        channel = com.typewritermc.moduleplugin.ReleaseChannel.BETA

        paper {
            dependency("Guidebook")
        }
    }
}

kotlin {
    // Guidebook is compiled for Java 25, so this has to read Java 25 class files
    jvmToolchain(25)
}

// Typewriter's annotation processor only reads the extension's own entries, and its incremental run over the tests
// fails on a changed test file
tasks.matching { it.name == "kspTestKotlin" }.configureEach { enabled = false }

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
