import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import xyz.jpenilla.resourcefactory.bukkit.Permission


plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
    alias(libs.plugins.resource.factory)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
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
    implementation(kotlin("stdlib"))
    compileOnly(libs.paper.api)

    compileOnly(libs.vault.api)
    compileOnly(libs.protocol.lib)

    implementation(libs.bundles.command)

    implementation(libs.inventoryframework)
    implementation(libs.mysql.connector.java)
    implementation(libs.sqlite.jdbc)

    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.ktor.server)

    implementation(libs.bundles.exposed)

    implementation(libs.bundles.coroutines)


    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.hocon)

    implementation(libs.kotlin.result)

    implementation(libs.gson)

    implementation(libs.koin.core)

    testImplementation(libs.bundles.coroutines)
    testImplementation(libs.protocol.lib)
    testImplementation(libs.sqlite.jdbc)

    testImplementation(libs.mockBukkit)
    testImplementation(libs.mockk)
    testImplementation(libs.junitJupiter)
    testImplementation(libs.koinTest)
    testImplementation(libs.koinTestJunit5)
}

kotlin {
    jvmToolchain {
        (this).languageVersion.set(JavaLanguageVersion.of(21))
    }
    jvmToolchain(21)
}

val pluginGroup = "dev.nikomaru.raceassist"

tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile::class.java) {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}


tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "21"
        kotlinOptions.javaParameters = true
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "21"
    }
    build {
        dependsOn("shadowJar")
    }
    shadowJar
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
    shadowJar {
        relocate("cloud.commandframework", "$pluginGroup.shaded.cloud")
        relocate("io.leangen.geantyref", "$pluginGroup.shaded.typetoken")
        relocate("com.github.stefvanschie.inventoryframework", "$pluginGroup.inventoryframework")
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
sourceSets.main {
    resourceFactory {
        bukkitPluginYaml {
            name = rootProject.name
            version = project.version.toString()
            website = "https://github.com/RaceAssist/RaceAssist-core"
            main = "$group.RaceAssist"
            apiVersion = "1.20"
            libraries = libs.bundles.coroutines.asString()
            softDepend = listOf("Vault", "ProtocolLib")
            permissions {
                register("RaceAssist.admin") {
                    default = Permission.Default.OP
                    children(
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
                    default = Permission.Default.TRUE
                    children(
                        "raceassist.commands.audience.join",
                        "raceassist.commands.audience.leave",
                        "raceassist.commands.bet.open",
                        "raceassist.commands.web",
                        "raceassist.commands.horse.ownerdelete",
                    )
                }
            }
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

fun Provider<MinimalExternalModuleDependency>.asString(): String {
    val dependency = this.get()
    return dependency.module.toString() + ":" + dependency.versionConstraint.toString()
}

fun Provider<ExternalModuleDependencyBundle>.asString(): List<String> {
    return this.get().map { dependency ->
        "${dependency.group}:${dependency.name}:${dependency.version}"
    }
}