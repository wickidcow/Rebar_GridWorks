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

    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.test {
    useJUnitPlatform()
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
}

tasks.runServer {
    downloadPlugins {
        github("pylonmc", "rebar", rebarVersion, "rebar-$rebarVersion.jar")
    }
    minecraftVersion(minecraftVersion)
}
