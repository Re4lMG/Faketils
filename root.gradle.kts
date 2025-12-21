import org.gradle.kotlin.dsl.*

group = "com.faketils"
version = "1.0.0"

allprojects {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://repo.polyfrost.org/releases")
        maven("https://jitpack.io")
    }
}

subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}