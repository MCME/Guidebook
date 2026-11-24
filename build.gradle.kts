plugins {
    java
    `maven-publish`
    alias(libs.plugins.run.paper)
    alias(libs.plugins.spotless)
    checkstyle
}

group = "com.mcmiddleearth"
version = "2.1.0-SNAPSHOT"

// ./gradlew runServer -PwithoutTypewriter starts the test server without Typewriter or the Typewriter display
val withoutTypewriter = providers.gradleProperty("withoutTypewriter").isPresent

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
    testImplementation(libs.worldedit.core)
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

    // Installs the Typewriter display as an admin would: Typewriter loads extensions and Typewriter pages from its own
    // folders, which downloadPlugins can't reach
    register("installTypewriterDisplay") {
        group = "run paper"
        description = "Puts the Typewriter display, its Typewriter page and BasicExtension in run/plugins/Typewriter."
        dependsOn(":typewriter-display:jar")
        doLast {
            val typewriterFolder = file("run/plugins/Typewriter")
            val extensions = typewriterFolder.resolve("extensions").apply { mkdirs() }

            // Rebuilt every start, so the server never runs a stale build
            extensions.listFiles { f -> f.name.startsWith("typewriter-display-") }!!.forEach { it.delete() }
            project(":typewriter-display").tasks.named<Jar>("jar").get().archiveFile.get().asFile.let {
                it.copyTo(extensions.resolve(it.name))
            }

            // BasicExtension ships as an extra file in Typewriter's own Modrinth release, so it's taken from the
            // release the version catalog names
            val typewriter = libs.versions.typewriter.get()
            val basic = extensions.resolve("BasicExtension-$typewriter.jar")
            if (!basic.exists()) {
                extensions.listFiles { f -> f.name.startsWith("BasicExtension") }!!.forEach { it.delete() }
                val release = uri("https://api.modrinth.com/v2/project/typewriter/version/$typewriter").toURL()
                    .openConnection().apply { setRequestProperty("User-Agent", "gradle-run-paper") }
                    .getInputStream().use { groovy.json.JsonSlurper().parse(it) } as Map<*, *>
                val url = (release["files"] as List<*>).map { it as Map<*, *> }
                    .single { it["filename"] == "BasicExtension.jar" }["url"] as String
                uri(url).toURL().openStream().use { input -> basic.outputStream().use { input.copyTo(it) } }
            }

            // Only if missing, so changes made to it in Typewriter's panel are kept
            val page = typewriterFolder.resolve("pages/guidebook.json")
            page.parentFile.mkdirs()
            if (!page.exists()) file("typewriter-display/guidebook.json").copyTo(page)
        }
    }

    runServer {
        minecraftVersion(minecraftVersion)
        downloadPlugins {
            hangar("WorldEdit", libs.versions.worldedit.get())
            val pluginUtils = libs.versions.pluginutils.get()
            url("https://repo.mcmiddleearth.com/releases/com/mcmiddleearth/PluginUtils/$pluginUtils/PluginUtils-$pluginUtils.jar")
            // For the Typewriter display: the Typewriter build it names, and the PacketEvents Typewriter needs.
            // -PwithoutTypewriter leaves them out, to check Guidebook on a server without Typewriter
            if (!withoutTypewriter) {
                modrinth("typewriter", libs.versions.typewriter.get())
                modrinth("packetevents", libs.versions.packetevents.get())
            }
        }
        if (!withoutTypewriter) dependsOn("installTypewriterDisplay")
    }
}
