plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "com.mcmiddleearth"
version = "1.2.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.mcmiddleearth.com/releases")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.129-stable")
    compileOnly("com.mcmiddleearth:PluginUtils:2.0.4")
    compileOnly("com.sk89q.worldedit:worldedit-core:7.4.5")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.4.5")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks {
    // Dynamically adding the name and version to the paper plugin.yml
    processResources {
        val props = mapOf("name" to project.name, "version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    runServer {
        minecraftVersion("26.2")
        downloadPlugins {
            hangar("WorldEdit", "7.4.5")
            url("https://repo.mcmiddleearth.com/releases/com/mcmiddleearth/PluginUtils/2.0.4/PluginUtils-2.0.4.jar")
        }
    }
}
