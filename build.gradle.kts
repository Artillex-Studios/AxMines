plugins {
    kotlin("jvm") version "2.3.21"
    id("com.gradleup.shadow") version "9.4.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "com.artillexstudios.axmines"
version = "1.9.0"

repositories {
    mavenCentral()

    maven("https://repo.artillex-studios.com/releases/")
    maven("https://repo.alessiodp.com/releases/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://jitpack.io/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.oraxen.com/releases/")
}

dependencies {
    implementation("com.artillexstudios.axapi:axapi:2.2.0:all")
    implementation("org.bstats:bstats-bukkit:3.0.2")
    implementation("dev.triumphteam:triumph-gui:3.1.7")
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("org.slf4j:slf4j-api:2.0.9")
    compileOnly("org.apache.commons:commons-text:1.11.0")
    compileOnly("commons-io:commons-io:2.15.0")
    compileOnly("org.apache.commons:commons-math3:3.6.1")
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.3.21")
    compileOnly("com.github.oraxen:oraxen:1.161.0")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.6.1")
}

tasks {
    compileKotlin {
        compilerOptions {
            freeCompilerArgs.add("-Xno-param-assertions")
            freeCompilerArgs.add("-Xno-call-assertions")
            freeCompilerArgs.add("-Xno-receiver-assertions")
            javaParameters.set(true)
        }
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    shadowJar {
        relocate("com.artillexstudios.axapi", "com.artillexstudios.axmines.libs.axapi")
        relocate("org.bstats", "com.artillexstudios.axmines.libs.bstats")
        relocate("org.apache.commons.math3", "com.artillexstudios.axmines.libs.axapi.libs.math3")
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()

        filesMatching("plugin.yml") {
            expand(mapOf("version" to project.version))
        }
    }

    runServer {
        minecraftVersion("1.21.11")
    }

    build {
        dependsOn(shadowJar)
    }
}

kotlin {
    jvmToolchain(21)
}