plugins {
    kotlin("jvm") version "1.6.0"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "dev.nikomaru"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven ("https://papermc.io/repo/repository/maven-public/")
    maven ("https://oss.sonatype.org/content/groups/public/")
    maven ("https://repo.aikar.co/content/groups/aikar/")

}

dependencies {
    compileOnly ("io.papermc.paper:paper-api:1.17.1-R0.1-SNAPSHOT")
    implementation ("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("co.aikar:acf-paper:0.5.0-SNAPSHOT")
}


tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.javaParameters = true
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    shadowJar {
        relocate("co.aikar.commands", "dev.nikomaru.receassist.acf")
        relocate("co.aikar.locales", "dev.nikomaru.raceassist.acf.locales")
        archiveClassifier.set("")
    }
    build {
        dependsOn(shadowJar)
    }
}