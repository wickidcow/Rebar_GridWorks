import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    java
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "io.github.wickidcow"
version = providers.gradleProperty("version").get()

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven("https://repo.codemc.io/repository/maven-releases/") {
        name = "CodeMC"
    }
    maven("https://repo.extendedclip.com/releases/") {
        name = "PlaceholderAPI"
    }
    maven("https://repo.xenondevs.xyz/releases") {
        name = "InvUI"
    }
    maven("https://jitpack.io") {
        name = "JitPack"
    }
}

val rebarVersion = providers.gradleProperty("rebar.version").get()
val minecraftVersion = providers.gradleProperty("minecraft.version").get()

dependencies {
    compileOnly("io.papermc.paper:paper-api:$minecraftVersion.build.+")
    compileOnly("io.github.pylonmc:rebar:$rebarVersion")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.gradle.api.tasks.bundling.AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.jar {
    archiveBaseName.set("Rebar_GridWorks")
}

bukkit {
    name = "GridWorks"
    main = "io.github.wickidcow.gridworks.GridWorks"
    version = project.version.toString()
    apiVersion = minecraftVersion
    depend = listOf("Rebar")
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
    authors = listOf("wickidcow")
    description = "Industrial automation, smart power management, and factory control systems for Pylon/Rebar."

    commands {
        register("gridworks") {
            description = "GridWorks administration and diagnostics."
            usage = "/<command> doctor"
            permission = "gridworks.admin"
        }
    }

    permissions {
        register("gridworks.admin") {
            description = "Allows GridWorks administrative diagnostics."
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}

tasks.runServer {
    downloadPlugins {
        github("pylonmc", "rebar", rebarVersion, "rebar-$rebarVersion.jar")
    }
    minecraftVersion(minecraftVersion)
}
