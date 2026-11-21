plugins {
    java
    `maven-publish`
    alias(libs.plugins.run.paper)
    alias(libs.plugins.spotless)
    checkstyle
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
        // Every failure in full, and a total at the end, so one run says what passed and why anything failed
        testLogging {
            events("failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
        addTestListener(object : TestListener {
            override fun beforeSuite(suite: TestDescriptor) {}

            override fun beforeTest(testDescriptor: TestDescriptor) {}

            override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}

            override fun afterSuite(suite: TestDescriptor, result: TestResult) {
                if (suite.parent == null) {
                    println(
                        "${result.testCount} tests: ${result.successfulTestCount} passed, " +
                            "${result.failedTestCount} failed, ${result.skippedTestCount} skipped",
                    )
                }
            }
        })
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
            // For the Typewriter display: the Typewriter build it names, and the PacketEvents Typewriter needs
            modrinth("typewriter", "0.9.0-beta-177")
            modrinth("packetevents", "2.14.0+spigot")
        }

        // Typewriter loads extensions and pages from its own folders, which downloadPlugins can't reach, so the
        // Typewriter display, its Typewriter page and the BasicExtension Typewriter needs are put there before each start
        dependsOn(":typewriter-display:jar")
        doFirst {
            val run = runDirectory.get().asFile
            val extensions = run.resolve("plugins/Typewriter/extensions")
            extensions.mkdirs()
            extensions.listFiles { f -> f.name.startsWith("typewriter-display-") }!!.forEach { it.delete() }
            project(":typewriter-display").tasks.named<Jar>("jar").get().archiveFile.get().asFile.let {
                it.copyTo(extensions.resolve(it.name), overwrite = true)
            }
            val basic = extensions.resolve("BasicExtension.jar")
            if (!basic.exists()) {
                // The BasicExtension built for the Typewriter version above
                uri("https://cdn.modrinth.com/data/Vm7B3ymm/versions/728uFaZd/BasicExtension.jar")
                    .toURL()
                    .openStream()
                    .use { input -> basic.outputStream().use { input.copyTo(it) } }
            }

            // Only if missing, so changes made to it in Typewriter's panel are kept
            val page = run.resolve("plugins/Typewriter/pages/guidebook.json")
            page.parentFile.mkdirs()
            if (!page.exists()) rootDir.resolve("typewriter-display/guidebook.json").copyTo(page)

            // Close the box with F (swap hands) instead of Typewriter's default, sneak
            val typewriterConfig = run.resolve("plugins/Typewriter/config.yml")
            if (!typewriterConfig.exists() || "confirmationKey:" !in typewriterConfig.readText()) {
                typewriterConfig.appendText("\nconfirmationKey: SWAP_HANDS\n")
            }

            // Choose the Typewriter display, unless the config already chooses one
            val guidebookConfig = run.resolve("plugins/Guidebook/config.yml")
            guidebookConfig.parentFile.mkdirs()
            if (!guidebookConfig.exists() || "display:" !in guidebookConfig.readText()) {
                guidebookConfig.appendText("\ndisplay: typewriter\n")
            }
        }
    }
}
