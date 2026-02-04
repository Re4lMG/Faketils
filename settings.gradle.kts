pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://repo.polyfrost.org/releases")
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "gg.essential.loom" -> useModule("gg.essential:architectury-loom:${requested.version}")
            }
        }
    }
}

rootProject.name = "Faketils"
rootProject.buildFileName = "root.gradle.kts"

//include(":1.8.9-forge")
include(":1.21-fabric")

//project(":1.8.9-forge").projectDir = file("versions/1.8.9-forge")
project(":1.21-fabric").projectDir = file("versions/1.21-fabric")
