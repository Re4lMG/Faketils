import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.jvm.toolchain.JavaLanguageVersion
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

    plugins.withId("java") {
        extensions.configure(org.gradle.api.plugins.JavaPluginExtension::class.java) {
            val legacy = project.name.contains("1.8")
            toolchain.languageVersion.set(JavaLanguageVersion.of(if (legacy) 8 else 21))
        }
    }

    configurations.maybeCreate("shade")
}