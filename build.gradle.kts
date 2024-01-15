import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget


plugins {
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.7"
    kotlin("jvm") version "2.0.0-Beta2"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-paper") version "2.2.2"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    kotlin("plugin.serialization") version "2.0.0-Beta2"
    id("org.jetbrains.dokka") version "1.9.10"
}

group = "dev.nikomaru.raceassist"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
    maven("https://plugins.gradle.org/m2/")
    maven("https://repo.incendo.org/content/repositories/snapshots")
    maven("https://repo.dmulloy2.net/repository/public/")
}




dependencies {
    val paperVersion = "1.20.4-R0.1-SNAPSHOT"
    val cloudVersion = "1.8.3"
    val exposedVersion = "0.42.0"
    val ktorVersion = "2.3.7"

    val koinVersion = "3.5.3"
    val ifVersion = "0.10.11"
    val junitVersion = "5.10.1"
    val mockkVersion = "1.13.8"
    val mockBukkitVersion = "3.65.0"
    val sqliteVersion = "3.44.1.0"
    val mysqlVersion = "8.0.33"
    val vaultVersion = "1.7"
    val protocolLibVersion = "5.2.0-SNAPSHOT"
    val kotlinxDataTimeVersion = "0.4.0"
    val kotlinxSerializationVersion = "1.6.0-RC"
    val mccoroutineVersion = "2.14.0"
    val kotlinCoroutinesVersion = "1.7.3"

    compileOnly("io.papermc.paper:paper-api:$paperVersion")

    compileOnly("com.github.MilkBowl:VaultAPI:$vaultVersion")
    compileOnly("com.comphenix.protocol:ProtocolLib:$protocolLibVersion")

    implementation("cloud.commandframework:cloud-core:$cloudVersion")
    implementation("cloud.commandframework:cloud-kotlin-extensions:$cloudVersion")
    implementation("cloud.commandframework:cloud-paper:$cloudVersion")
    implementation("cloud.commandframework:cloud-annotations:$cloudVersion")
    implementation("cloud.commandframework:cloud-kotlin-coroutines-annotations:$cloudVersion")
    implementation("cloud.commandframework:cloud-kotlin-coroutines:$cloudVersion")

    implementation("com.github.stefvanschie.inventoryframework:IF:$ifVersion")

    library("mysql:mysql-connector-java:$mysqlVersion")

    library("org.xerial:sqlite-jdbc:$sqliteVersion")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-network-tls-certificates:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-java:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:1.3.14")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDataTimeVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:$kotlinxSerializationVersion")

    implementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:$mccoroutineVersion")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-core:$mccoroutineVersion")

    library("com.google.api-client:google-api-client:1.35.1")
    library("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    library("com.google.apis:google-api-services-sheets:v4-rev20220606-1.32.1")

    implementation(kotlin("stdlib"))

    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.18")

    library("com.google.code.gson:gson:2.10.1")

    implementation("io.insert-koin:koin-core:$koinVersion")

    testImplementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:$mccoroutineVersion")
    testImplementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-core:$mccoroutineVersion")

    testImplementation("com.comphenix.protocol:ProtocolLib:$protocolLibVersion")

    testImplementation("org.xerial:sqlite-jdbc:$sqliteVersion")

    testImplementation("com.github.seeseemelk:MockBukkit-v1.20:$mockBukkitVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    testImplementation("io.insert-koin:koin-test-junit5:$koinVersion")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

val pluginGroup = "dev.nikomaru.raceassist"

tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile::class.java) {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
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
        relocate("cloud.commandframework", "$pluginGroup.shaded.cloud")
        relocate("io.leangen.geantyref", "$pluginGroup.shaded.typetoken")
        relocate("com.github.stefvanschie.inventoryframework", "$pluginGroup.inventoryframework")
    }
    build {
        dependsOn(shadowJar)
    }
    runServer {
        minecraftVersion("1.20.2")
    }
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
    }
    test {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
            events("passed", "skipped", "failed")
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}


bukkit {
    name = "RaceAssist"
//minecraft_plugin_version
    version = "minecraft_plugin_version"
    website = "https://docs-raceassist.nikomaru.dev/"

    main = "dev.nikomaru.raceassist.RaceAssist"

    apiVersion = "1.19"
    depend = listOf("Vault:ProtocolLib")

    permissions {
        register("RaceAssist.admin") {
            default = Default.OP
            children = listOf(
                "raceassist.commands.audience.leave",
                "raceassist.commands.place.degree",
                "raceassist.commands.bet.sheet",
                "raceassist.commands.race.start",
                "raceassist.commands.player.add",
                "raceassist.commands.horse.ownerdelete",
                "raceassist.commands.bet.can",
                "raceassist.commands.setting.copy",
                "raceassist.commands.player.remove",
                "raceassist.commands.bet.list",
                "raceassist.commands.bet.delete",
                "raceassist.commands.setting.view",
                "raceassist.commands.place.central",
                "raceassist.commands.audience.list",
                "raceassist.commands.setting.staff",
                "raceassist.commands.setting.create",
                "raceassist.commands.bet.open",
                "raceassist.commands.place.reverse",
                "raceassist.commands.bet.revert.jockey",
                "raceassist.commands.reload",
                "raceassist.commands.web",
                "raceassist.commands.place.set",
                "raceassist.commands.player.delete",
                "raceassist.commands.player.list",
                "raceassist.commands.race.debug",
                "raceassist.commands.race.horse",
                "raceassist.commands.bet.revert.row",
                "raceassist.commands.place.finish",
                "raceassist.commands.bet.return.jockey",
                "raceassist.commands.setting.delete",
                "raceassist.commands.race.stop",
                "raceassist.commands.bet.unit",
                "raceassist.commands.bet.revert.all",
                "raceassist.commands.place.lap",
                "raceassist.commands.bet.rate",
                "raceassist.commands.player.replacement",
                "raceassist.commands.audience.join",
                "raceassist.command.help"
            )
        }
        register("RaceAssist.user") {
            default = Default.TRUE
            children = listOf(
                "raceassist.commands.audience.join",
                "raceassist.commands.audience.leave",
                "raceassist.commands.bet.open",
                "raceassist.commands.web",
                "raceassist.commands.horse.ownerdelete",
            )
        }
    }
}

//tasks.withType<DokkaTask>().configureEach {
//    dokkaSourceSets {
//        configureEach {
//            perPackageOption {
//                matchingRegex.set(".*")
//                suppress.set(true)
//            }
//            perPackageOption {
//                matchingRegex.set("dev.nikomaru.raceassist.api.core.*")
//                suppress.set(false)
//            }
//            perPackageOption {
//                matchingRegex.set(".*\\.data.*")
//                suppress.set(false)
//            }
//        }
//    }
//}

// enable k2 compiler
tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
    }
}

tasks.register("depsize") {
    description = "Prints dependencies for \"default\" configuration"
    doLast {
        listConfigurationDependencies(configurations["default"])
    }
}

tasks.register("depsize-all-configurations") {
    description = "Prints dependencies for all available configurations"
    doLast {
        configurations.filter { it.isCanBeResolved }.forEach { listConfigurationDependencies(it) }
    }
}

fun listConfigurationDependencies(configuration: Configuration) {
    val formatStr = "%,10.2f"

    val size = configuration.sumOf { it.length() / (1024.0 * 1024.0) }

    val out = StringBuffer()
    out.append("\nConfiguration name: \"${configuration.name}\"\n")
    if (size > 0) {
        out.append("Total dependencies size:".padEnd(65))
        out.append("${String.format(formatStr, size)} Mb\n\n")

        configuration.sortedBy { -it.length() }.forEach {
                out.append(it.name.padEnd(65))
                out.append("${String.format(formatStr, (it.length() / 1024.0))} kb\n")
            }
    } else {
        out.append("No dependencies found")
    }
    println(out)
}
