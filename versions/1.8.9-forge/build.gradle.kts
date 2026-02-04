import org.apache.commons.lang3.SystemUtils

plugins {
    kotlin("jvm") version "2.0.21"
    idea
    id("gg.essential.loom") version "0.10.0.5"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("net.kyori.blossom") version "1.3.2"
    java
}

group = "com.faketils"
version = project.property("mod_version")!!
val platform = "1.8.9-forge"

val devEnv: Configuration by configurations.creating {
    configurations.runtimeClasspath.get().extendsFrom(this)
    isCanBeResolved = false
    isCanBeConsumed = false
    isVisible = false
}

blossom {
    replaceToken("@VER@", version.toString())
    replaceToken("@NAME@", project.property("mod_name")!!)
    replaceToken("@ID@", project.property("mod_id")!!)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}
kotlin {
    jvmToolchain { languageVersion.set(JavaLanguageVersion.of(8)) }
}

loom {
    log4jConfigs.from(file("log4j2.xml"))
    launchConfigs {
        "client" {
            property("mixin.debug", "true")
            arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
            arg("--tweakClass", "cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker")
        }
    }
    runConfigs {
        "client" {
            if (SystemUtils.IS_OS_MAC_OSX) {
                vmArgs.remove("-XstartOnFirstThread")
            }
        }
        remove(getByName("server"))
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        mixinConfig("mixins.faketils.json")
    }
    mixin {
        defaultRefmapName.set("mixins.faketils.refmap.json")
    }
}

sourceSets.main {
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
}

val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

repositories {
    mavenCentral()
    maven("https://repo.essential.gg/repository/maven-public/")
    maven("https://repo.spongepowered.org/maven/")
    maven("https://repo.polyfrost.cc/releases")
    maven("https://mvnrepository.com/repos/central")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    compileOnly("cc.polyfrost:oneconfig-1.8.9-forge:0.2.2-alpha+")
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    runtimeOnly("cc.polyfrost:oneconfig-wrapper-launchwrapper:1.0.0-beta+")

    implementation("org.slick2d:slick2d-core:1.0.1")
    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")
    devEnv("me.djtheredstoner:DevAuth-forge-legacy:1.2.1")
}

tasks.withType(org.gradle.jvm.tasks.Jar::class) {
    archiveBaseName.set("${project.property("mod_name")}-Forge-1.8.9")
    manifest.attributes.run {
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"
        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        this["MixinConfigs"] = "mixins.faketils.json"
    }
}

tasks {
    processResources {
        filesMatching("mcmod.info") {
            expand(
                mapOf(
                    "name" to project.property("mod_name")!!,
                    "modid" to project.property("mod_id")!!,
                    "version" to project.property("mod_version")!!,
                    "mcversion" to "1.8.9"
                )
            )
        }
    }
}

val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveClassifier.set("")
    from(tasks.shadowJar)
    input.set(tasks.shadowJar.get().archiveFile)
}

tasks.jar {
    archiveClassifier.set("without-deps")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    manifest.attributes += mapOf(
        "ModSide" to "CLIENT",
        "TweakOrder" to 0,
        "ForceLoadAsMod" to true,
        "TweakClass" to "cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker"
    )
}

tasks.shadowJar {
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    archiveClassifier.set("non-obfuscated-with-deps")
    configurations = listOf(shadowImpl)
    doLast {
        configurations.forEach {
            println("Copying dependencies into mod: ${it.files}")
        }
    }
    fun relocate(name: String) = relocate(name, "com.faketils.deps.$name")
}

tasks.assemble.get().dependsOn(tasks.remapJar)