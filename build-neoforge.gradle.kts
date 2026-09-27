import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("dev.isxander.modstitch.base") version "0.8.5"
}

val mcVersion = property("deps.minecraft").toString()
val targetNeoForgeVersion = property("deps.neoforge").toString()
val modId = property("mod.id").toString()
val artifactVersion = "${property("mod.version")}+$mcVersion"
val targetJavaVersion = 21
val biomeCompat = providers.gradleProperty("biomeCompat").map(String::toBoolean).getOrElse(false)

group = property("mod.group").toString()
version = artifactVersion
base.archivesName = "$modId-neoforge"

modstitch {
    minecraftVersion = mcVersion
    moddevgradle {
        neoForgeVersion = targetNeoForgeVersion
        defaultRuns()
        configureNeoForge {
            runs.configureEach {
                gameDirectory = rootProject.layout.projectDirectory.dir("run/neoforge-${if (biomeCompat) "compat-" else ""}${name}")
                logLevel = org.slf4j.event.Level.INFO
                if (name == "server") programArguments.add("nogui")
            }
        }
    }
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.theillusivec4.top/")
    maven("https://maven.latvian.dev/releases") {
        content { includeGroup("dev.latvian.mods"); includeGroup("dev.latvian.apps") }
    }
    maven("https://jitpack.io") {
        content { includeGroup("com.github.rtyley") }
    }
    maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
}

// NeoForge 1.21.1 uses Mojmap at runtime; these are dev-only, not jar-in-jar dependencies.
dependencies {
    compileOnly("top.theillusivec4.curios:curios-neoforge:${property("deps.curios")}:api")
    runtimeOnly("top.theillusivec4.curios:curios-neoforge:${property("deps.curios")}")
    compileOnly("dev.latvian.mods:kubejs-neoforge:${property("deps.kubejs")}")
    if (biomeCompat) {
        runtimeOnly("maven.modrinth:biomes-o-plenty:BtZKRp69")
        runtimeOnly("maven.modrinth:terrablender:6e8GCrLb")
        runtimeOnly("maven.modrinth:glitchcore:S2TfWrZR")
        runtimeOnly("maven.modrinth:terralith:IY93YaEe")
        runtimeOnly("maven.modrinth:lithostitched:sPmtDq1X")
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        dependsOn("stonecutterGenerate")
        options.encoding = "UTF-8"
        options.release = targetJavaVersion
    }

    processResources {
        dependsOn("stonecutterGenerate")

        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "authors" to project.property("mod.authors"),
            "description" to project.property("mod.description"),
            "license" to project.property("mod.license"),
            "packFormat" to project.property("vers.packFormat"),
            "loaderRange" to project.property("vers.loaderRange"),
            "neoForgeRange" to project.property("vers.neoForgeRange"),
            "curiosRange" to project.property("vers.curiosRange"),
            "minecraftRange" to project.property("vers.minecraftRange"),
        )

        inputs.properties(props)
        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
        filesMatching("pack.mcmeta") { expand(props) }
        // IntProvider dispatch lost the nested `value` wrapper in 1.21.
        filesMatching("data/deeprealm_4th/dimension_type/fourth_layer.json") {
            filter { line -> line.replace("\"value\": {\"min_inclusive\": 0, \"max_inclusive\": 7}", "\"min_inclusive\": 0, \"max_inclusive\": 7") }
        }
        // NeoForge 1.21 uses Mojmap at runtime; the Forge SRG refmap is not applicable.
        filesMatching("$modId.mixins.json") {
            filter { line -> if (line.contains("\"refmap\"")) "" else line }
        }
        exclude("META-INF/mods.toml")
        exclude("data/curios/tags/items/**")
        exclude("data/deeprealm_4th/recipes/**")
        exclude("data/deeprealm_4th/tags/items/astral/**")
    }

    withType<Jar>().configureEach {
        archiveVersion = artifactVersion
    }
}

java {
    withSourcesJar()
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
}

val verifyTerrain by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Checks the current spiral terrain and ecology."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.tony.deeprealmtheforth.worldgen.CurrentTerrainVerification"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(targetJavaVersion) }
}

val verifyAstral by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Checks astral layouts, percentage scores, and medal formulas."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.tony.deeprealmtheforth.astral.AstralCoreVerification"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(targetJavaVersion) }
}

tasks.check { dependsOn(verifyTerrain, verifyAstral) }

// Geometry assertions are executed by verifyTerrain, not a JUnit discovery engine.
tasks.test { failOnNoDiscoveredTests = false }

val verifyHydrology by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Checks the current watershed against an exported registry climate."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.tony.deeprealmtheforth.worldgen.GlobalHydrologyVerification"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(targetJavaVersion) }
    args(rootProject.file("run/neoforge-server/biome-compat-v5/climate.tsv").absolutePath,
        rootProject.layout.buildDirectory.dir("reports/hydrology-current/neoforge").get().asFile.absolutePath)
}
