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
    description = "Checks spiral geometry and fluid containment; renders terrain previews."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.tony.deeprealmtheforth.worldgen.TerrainVerification"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(targetJavaVersion) }
    args(rootProject.layout.buildDirectory.dir("reports/terrain-neoforge").get().asFile.absolutePath)
    systemProperty("java.awt.headless", "true")
}

tasks.check { dependsOn(verifyTerrain) }

// Geometry assertions are executed by verifyTerrain, not a JUnit discovery engine.
tasks.test { failOnNoDiscoveredTests = false }

val benchmarkTerrain by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Fixed-workload terrain timing, fingerprints and JFR; not client FPS."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.tony.deeprealmtheforth.worldgen.TerrainPerformanceBenchmark"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(targetJavaVersion) }
    args(rootProject.file("run/neoforge-server/hydrology-prototype/climate.tsv").absolutePath,
        rootProject.layout.buildDirectory.dir("reports/terrain-performance/neoforge").get().asFile.absolutePath,
        providers.gradleProperty("benchmarkLabel").getOrElse("current"),
        providers.gradleProperty("generationVersion").getOrElse("3"))
    maxHeapSize = "1G"
}

val verifyNaturalHydrology by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Offline v2 natural relief and asymmetric bank morphology; preserves live worldgen."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.tony.deeprealmtheforth.worldgen.NaturalHydrologyVerification"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(targetJavaVersion) }
    args(rootProject.file("run/neoforge-server/hydrology-prototype/climate.tsv").absolutePath,
        rootProject.layout.buildDirectory.dir("reports/hydrology-v2/neoforge").get().asFile.absolutePath)
    systemProperty("java.awt.headless", "true")
}

val verifyGlobalHydrology by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Verifies complete world-coordinate catchments and integrated river columns."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.tony.deeprealmtheforth.worldgen.GlobalHydrologyVerification"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(targetJavaVersion) }
    args(rootProject.file("run/neoforge-server/hydrology-prototype/climate.tsv").absolutePath,
        rootProject.layout.buildDirectory.dir("reports/hydrology-v3/neoforge").get().asFile.absolutePath)
    systemProperty("java.awt.headless", "true")
}

val verifyDetailedWorldgen by tasks.registering(JavaExec::class) {
    group = "verification"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.tony.deeprealmtheforth.worldgen.GlobalHydrologyVerification"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(targetJavaVersion) }
    args(rootProject.file("run/neoforge-server/hydrology-prototype/climate.tsv").absolutePath,
        rootProject.layout.buildDirectory.dir("reports/hydrology-v4/neoforge").get().asFile.absolutePath,"4")
    systemProperty("java.awt.headless", "true")
}

val verifyEdgeDetails by tasks.registering(JavaExec::class) {
    group = "verification"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.tony.deeprealmtheforth.worldgen.EdgeDetailVerification"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(21) }
    args(rootProject.file("run/neoforge-server/hydrology-prototype/climate.tsv").absolutePath,
        rootProject.layout.buildDirectory.dir("reports/hydrology-v4/neoforge").get().asFile.absolutePath)
    systemProperty("java.awt.headless", "true")
}

val verifyMarineArid by tasks.registering(JavaExec::class) {
    group = "verification"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.tony.deeprealmtheforth.worldgen.MarineAridVerification"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(targetJavaVersion) }
    args(rootProject.file("run/neoforge-server/hydrology-prototype/climate.tsv").absolutePath,
        rootProject.layout.buildDirectory.dir("reports/marine-v5/neoforge").get().asFile.absolutePath)
    systemProperty("java.awt.headless", "true")
}

val verifyMarineHydrology by tasks.registering(JavaExec::class) {
    group = "verification"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.tony.deeprealmtheforth.worldgen.GlobalHydrologyVerification"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(targetJavaVersion) }
    args(rootProject.file("run/neoforge-server/biome-compat-v5/climate.tsv").absolutePath,
        rootProject.layout.buildDirectory.dir("reports/hydrology-v5/neoforge").get().asFile.absolutePath,"5")
    systemProperty("java.awt.headless", "true")
}

// Offline only: first export the post-startup registry using /fourthlayer verifyclimate.
val verifyHydrologyPrototype by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Checks finite drainage prototype and draws honest algorithm previews; NOT production worldgen."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.tony.deeprealmtheforth.worldgen.HydrologyPrototypeVerification"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(targetJavaVersion) }
    args(rootProject.file("run/neoforge-server/hydrology-prototype/climate.tsv").absolutePath,
        rootProject.layout.buildDirectory.dir("reports/hydrology-prototype/neoforge").get().asFile.absolutePath)
    systemProperty("java.awt.headless", "true")
}
