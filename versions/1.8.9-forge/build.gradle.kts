@file:Suppress("UnstableApiUsage")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.9.23"
    id("gg.essential.loom") version "0.10.0.5"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("net.kyori.blossom") version "1.3.2"
    java
}

group = "com.faketils"
version = project.property("mod_version")!!
val platform = "1.8.9-forge"

base {
    archivesName.set("${project.property("mod_archives_name")}-$platform")
}

blossom {
    replaceToken("@VER@", version.toString())
    replaceToken("@NAME@", project.property("mod_name")!!)
    replaceToken("@ID@", project.property("mod_id")!!)
}

loom {
    launchConfigs {
        "client" {
            property("mixin.debug", "true")
            property("asmhelper.verbose", "true")
            arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
            arg("--mixin", "mixins.faketils.json")
        }
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        mixinConfig("mixins.faketils.json")
    }
    mixin {
        defaultRefmapName.set("mixins.faketils.refmap.json")
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")

    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    shade("org.spongepowered:mixin:0.7.11-SNAPSHOT")
    annotationProcessor("org.spongepowered:mixin:0.8.4-SNAPSHOT")

    modCompileOnly("cc.polyfrost:oneconfig-$platform:0.2.0-alpha+")
    implementation("gg.essential:essential-1.8.9-forge:3662")
    compileOnly("org.spongepowered:mixin:0.7.11-SNAPSHOT")
    shade("cc.polyfrost:oneconfig-wrapper-launchwrapper:1.0.0-beta+")
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("dev")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
