import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default

plugins {
    id("java")
    id("eclipse")
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.0.1"
    kotlin("jvm") version "1.6.0"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("xyz.jpenilla.run-paper") version "1.0.6"
    id("org.sonarqube") version "3.3"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
    kotlin("plugin.serialization") version "1.6.10"
}

group = "dev.nikomaru"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
    maven("https://plugins.gradle.org/m2/")
    maven("https://repo.incendo.org/content/repositories/snapshots")
}

val cloudVersion = "1.6.2"
val exposedVersion = "0.38.2"
dependencies {
    compileOnly("io.papermc.paper", "paper-api", "1.19-R0.1-SNAPSHOT")

    library(kotlin("stdlib"))

    compileOnly("com.github.MilkBowl", "VaultAPI", "1.7")

    implementation("cloud.commandframework", "cloud-core", cloudVersion)
    implementation("cloud.commandframework", "cloud-kotlin-extensions", cloudVersion)
    implementation("cloud.commandframework", "cloud-paper", cloudVersion)
    implementation("cloud.commandframework", "cloud-annotations", cloudVersion)
    implementation("cloud.commandframework", "cloud-kotlin-coroutines-annotations", cloudVersion)
    implementation("cloud.commandframework", "cloud-kotlin-coroutines", cloudVersion)

    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-java-time", exposedVersion)

    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.6.2")

    implementation("com.github.shynixn.mccoroutine", "mccoroutine-bukkit-api", "2.2.0")
    implementation("com.github.shynixn.mccoroutine", "mccoroutine-bukkit-core", "2.2.0")

    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.3.3")
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-hocon", "1.3.3")

    implementation("com.google.api-client", "google-api-client", "1.35.1")
    implementation("com.google.oauth-client", "google-oauth-client-jetty", "1.34.1")
    implementation("com.google.apis", "google-api-services-sheets", "v4-rev20220606-1.32.1")

    bukkitLibrary("com.google.code.gson", "gson", "2.8.7")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
        kotlinOptions.javaParameters = true
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    shadowJar {
        relocate("cloud.commandframework", "dev.nikomaru.receassist.shaded.cloud")
        relocate("io.leangen.geantyref", "dev.nikomaru.receassist.shaded.typetoken")
    }
    build {
        dependsOn(shadowJar)
    }
    runServer {
        minecraftVersion("1.19")
    }
}


bukkit {
    name = "RaceAssist"
    version = "miencraft_plugin_version"
    website = "https://github.com/Nlkomaru/RaceAssist-core"

    main = "dev.nikomaru.raceassist.RaceAssist"

    apiVersion = "1.19"
    depend = listOf("Vault")
    libraries = listOf("com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:2.2.0", "com.github.shynixn.mccoroutine:mccoroutine-bukkit-core:2.2.0")


    permissions {
        register("RaceAssist.admin") {
            default = Default.OP
            children = listOf("RaceAssist.commands.audience.join",
                "RaceAssist.commands.audience.leave",
                "RaceAssist.commands.audience.list",
                "RaceAssist.commands.bet.can",
                "RaceAssist.commands.bet.delete",
                "RaceAssist.commands.bet.list",
                "RaceAssist.commands.bet.open",
                "RaceAssist.commands.bet.rate",
                "RaceAssist.commands.bet.revert",
                "RaceAssist.commands.bet.return",
                "RaceAssist.commands.bet.remove",
                "RaceAssist.commands.bet.sheet",
                "RaceAssist.commands.place.reverse",
                "RaceAssist.commands.place.central",
                "RaceAssist.commands.place.degree",
                "RaceAssist.commands.place.lap",
                "RaceAssist.commands.place.set",
                "RaceAssist.commands.place.finish",
                "RaceAssist.commands.player.add",
                "RaceAssist.commands.player.remove",
                "RaceAssist.commands.player.delete",
                "RaceAssist.commands.player.list",
                "RaceAssist.commands.race.start",
                "RaceAssist.commands.race.debug",
                "RaceAssist.commands.race.stop",
                "RaceAssist.commands.setting.create",
                "RaceAssist.commands.setting.delete",
                "RaceAssist.commands.setting.copy",
                "RaceAssist.commands.setting.staff")
        }
        register("RaceAssist.user") {
            default = Default.TRUE
            children = listOf("RaceAssist.commands.audience.join", "RaceAssist.commands.audience.leave", "RaceAssist.commands.bet.open")
        }
    }
}