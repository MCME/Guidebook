plugins {
    java
    `maven-publish`
    alias(libs.plugins.run.paper)
    alias(libs.plugins.spotless)
}

group = "com.mcmiddleearth"
version = "2.0.1"

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
    // Other plugins compile against Guidebook (to listen for GuidebookSendEvent), so the sources are published too
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    // Publishes com.mcmiddleearth:Guidebook to MCME's Reposilite, where the other MCME plugins and libraries live.
    // The version suffix picks the repository: releases refuses to redeploy a version, snapshots doesn't
    //
    // Credentials come from the MCME_MAVEN_NAME / MCME_MAVEN_SECRET org secrets in CI, or the same-named
    // environment variables locally. Without them only `publish` fails, build and test are unaffected
    repositories {
        maven {
            name = "mcme"
            val repository = if (version.toString().endsWith("SNAPSHOT")) "snapshots" else "releases"
            url = uri("https://repo.mcmiddleearth.com/$repository")
            credentials {
                username = System.getenv("MCME_MAVEN_NAME")
                password = System.getenv("MCME_MAVEN_SECRET")
            }
        }
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

    // Dynamically adding the version and api-version to the paper plugin.yml
    processResources {
        val props = mapOf("version" to version, "apiVersion" to minecraftVersion)
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
