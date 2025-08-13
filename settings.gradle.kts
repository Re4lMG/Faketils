pluginManagement {
    repositories {
        maven("https://repo.essential.gg/repository/maven-public/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
        maven("https://repo.spongepowered.org/maven/")
        gradlePluginPortal()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "gg.essential.loom") {
                useModule("gg.essential:architectury-loom:${requested.version}")
            }
        }
    }
}

rootProject.name = "Faketils"
