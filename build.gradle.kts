plugins {
    java
    alias(libs.plugins.run.paper)
    alias(libs.plugins.spotless)
}

group = "com.mcmiddleearth"
version = "1.2.1"

// "26.2.build.129-stable" -> "26.2"
val minecraftVersion = libs.versions.paper.get().substringBefore(".build")

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.mcmiddleearth.com/releases")
    maven("https://maven.enginehub.org/repo/")
    maven("https://eldonexus.de/repository/maven-public/")
    maven("https://repo.mikeprimm.com/")
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.pluginutils)
    compileOnly(libs.worldedit.core)
    compileOnly(libs.worldedit.bukkit)
    compileOnly(libs.dynmap.api)
    compileOnly(libs.strokk.commands.annotations)
    annotationProcessor(libs.strokk.commands.processor)

    testImplementation(libs.paper.api)
    testImplementation(libs.pluginutils)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

// Points git at the repo's hooks so the pre-commit formatter is enabled for everyone who builds
if (file(".git").exists()) {
    providers.exec { commandLine("git", "config", "core.hooksPath", ".githooks") }.result.get()
}

spotless {
    java {
        palantirJavaFormat()
        removeUnusedImports()
    }
}

tasks {
    test {
        useJUnitPlatform()
    }

    // Dynamically adding the name, version and api-version to the paper plugin.yml
    processResources {
        val props = mapOf("name" to project.name, "version" to version, "apiVersion" to minecraftVersion)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    runServer {
        minecraftVersion(minecraftVersion)
        downloadPlugins {
            hangar("WorldEdit", libs.versions.worldedit.get())
            val pluginUtils = libs.versions.pluginutils.get()
            url("https://repo.mcmiddleearth.com/releases/com/mcmiddleearth/PluginUtils/$pluginUtils/PluginUtils-$pluginUtils.jar")
        }
    }
}
