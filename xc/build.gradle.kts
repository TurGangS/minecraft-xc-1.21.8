plugins {
    // Kotlin 2.1.0
    kotlin("jvm") version "2.1.0"

    // NEW SHADOW PLUGIN (Required for Gradle 9)
    id("com.gradleup.shadow") version "8.3.5"

    // PAPERWEIGHT BETA 19 (Works now because you are on Gradle 9!)
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

group = "phonon"
version = "0.0.5"

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    // 1.21.8 Bundle (or 1.21.4-R0.1-SNAPSHOT if 1.21.8 isn't published)
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.tomlj:tomlj:1.1.0")

    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.14") {
        exclude(group = "com.google.guava", module = "guava")
    }

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

// Modern Java 21 Toolchain
kotlin {
    jvmToolchain(21)
}

tasks {
    shadowJar {
        archiveClassifier.set("")
    }

    build {
        dependsOn(shadowJar)
    }
}