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
    archivesName.set("${project.property("mod_name")}-Forge-1.8.9")
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

sourceSets.main {
    output.setResourcesDir(file("$buildDir/classes/java/main"))
}

val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")

    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT")
    annotationProcessor("org.spongepowered:mixin:0.8.4-SNAPSHOT")

    modCompileOnly("cc.polyfrost:oneconfig-$platform:0.2.0-alpha+")
    implementation("gg.essential:essential-1.8.9-forge:3662")
    compileOnly("org.spongepowered:mixin:0.7.11-SNAPSHOT")
    shadowImpl("cc.polyfrost:oneconfig-wrapper-launchwrapper:1.0.0-beta+")
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

tasks.withType<JavaCompile> { options.encoding = "UTF-8" }

tasks.withType<Jar> {
    manifest.attributes.run {
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"
        this["MixinConfigs"] = "mixins.faketils.json"
        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
    }
}

val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveClassifier.set("")
    from(tasks.shadowJar)
    input.set(tasks.shadowJar.get().archiveFile)
}

tasks.shadowJar {
    archiveClassifier.set("dev")
    configurations = listOf(shadowImpl)
    doLast {
        configurations.forEach {
            println("Config: ${it.files}")
        }
    }
}

tasks.assemble.get().dependsOn(tasks.remapJar)