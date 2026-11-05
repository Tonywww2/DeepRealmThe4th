import org.gradle.api.tasks.compile.JavaCompile
import net.fabricmc.loom.api.remapping.RemapperParameters
import com.tony.build.BopTreeDevRemapper
import java.io.File
import java.util.Properties

plugins {
    id("dev.architectury.loom") // pinned in buildSrc for its dev-remapper API
    id("me.modmuss50.mod-publish-plugin") version "2.1.1"
}

val loader = property("loom.platform").toString()
val mcVersion = property("vers.mcVersion").toString()
val modId = property("mod.id").toString()
val artifactVersion = "${property("mod.version")}+$mcVersion"
val targetJavaVersion = 17
val biomeCompat = providers.gradleProperty("biomeCompat").map(String::toBoolean).getOrElse(false)
val processRecipeSource = rootProject.file("scripts/GenerateAstralProcessRecipes.java")
val generatedProcessRecipes = layout.buildDirectory.dir("generated/astral-process-recipes")
val generateAstralProcessRecipes by tasks.registering(Exec::class) {
    group = "datagen"
    description = "Generates combination forging and projection combining recipes."
    inputs.file(processRecipeSource)
    outputs.dir(generatedProcessRecipes)
    commandLine(javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(targetJavaVersion) }
            .get().executablePath.asFile.absolutePath,
        "--source", "17", processRecipeSource.absolutePath,
        generatedProcessRecipes.get().asFile.absolutePath, "recipes")
}

group = property("mod.group").toString()
version = artifactVersion
base.archivesName = "$modId-$loader"

loom {
    silentMojangMappingsLicense()
    mixin { defaultRefmapName = "$modId.refmap.json" }
    forge { mixinConfig("$modId.mixins.json") }
    if (biomeCompat) addRemapperExtension(BopTreeDevRemapper::class.java, RemapperParameters.None::class.java) {}

    runConfigs.all {
        ideConfigGenerated(stonecutter.current.isActive)
        runDir("../../run/forge-${if (biomeCompat) "compat-" else ""}${name}")
    }
}

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net/")
    maven("https://maven.theillusivec4.top/")
    maven("https://maven.blamejared.com/")
    maven("https://maven.latvian.dev/releases") {
        content { includeGroup("dev.latvian.mods"); includeGroup("dev.latvian.apps") }
    }
    maven("https://maven.architectury.dev/") {
        content { includeGroup("dev.architectury") }
    }
    maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.officialMojangMappings())
    "forge"("net.minecraftforge:forge:$mcVersion-${property("vers.deps.fml")}")
    "modCompileOnly"("top.theillusivec4.curios:curios-forge:${property("deps.curios")}:api")
    "modRuntimeOnly"("top.theillusivec4.curios:curios-forge:${property("deps.curios")}")
    "modCompileOnly"("mezz.jei:jei-$mcVersion-forge-api:${property("deps.jei")}")
    "modRuntimeOnly"("mezz.jei:jei-$mcVersion-forge:${property("deps.jei")}") {
        exclude(group = "mezz.jei") // The full JEI jar already contains these split modules.
    }
    // JEI 15.62 declares MezzConfig as a required runtime mod; its API jar is not sufficient.
    "modRuntimeOnly"("net.mezzdev.config:mezz_config-1.20.1-forge:0.6.3")
    // Loom rewrites JEI's shaded library references during dev remapping; expose the originals to Forge's runtime.
    "forgeRuntimeLibrary"("net.mezzdev:deduplicating-runner:0.1.0")
    "forgeRuntimeLibrary"("net.mezzdev:baked-substring-index:0.1.0")
    "forgeRuntimeLibrary"("net.mezzdev:suffixtree:1.4.0")
    "modCompileOnly"("dev.latvian.mods:kubejs-forge:${property("deps.kubejs")}")
    if (biomeCompat) {
        "modRuntimeOnly"("maven.modrinth:biomes-o-plenty:jxUqRzSD")
        "modRuntimeOnly"("maven.modrinth:terrablender:zGconCHG")
        "modRuntimeOnly"("maven.modrinth:glitchcore:pYPZ5MNI")
        "modRuntimeOnly"("maven.modrinth:terralith:WeYhEb5d")
    }
}

tasks {
    configureEach {
        if (name == "createMinecraftArtifacts") {
            dependsOn("stonecutterGenerate")
        }
    }

    withType<JavaCompile>().configureEach {
        dependsOn("stonecutterGenerate")
        options.encoding = "UTF-8"
        options.release = targetJavaVersion
    }

    processResources {
        dependsOn("stonecutterGenerate")
        dependsOn(generateAstralProcessRecipes)
        from(generatedProcessRecipes)

        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "authors" to project.property("mod.authors"),
            "description" to project.property("mod.description"),
            "license" to project.property("mod.license"),
            "packFormat" to project.property("vers.packFormat"),
            "loaderRange" to project.property("vers.loaderRange"),
            "forgeRange" to project.property("vers.forgeRange"),
            "curiosRange" to project.property("vers.curiosRange"),
            "minecraftRange" to project.property("vers.minecraftRange"),
        )

        inputs.properties(props)
        filesMatching("META-INF/mods.toml") { expand(props) }
        filesMatching("pack.mcmeta") { expand(props) }
        exclude("META-INF/neoforge.mods.toml")
        exclude("data/curios/tags/item/**")
        exclude("data/deeprealm_4th/recipe/**")
        exclude("data/deeprealm_4th/tags/item/astral/**")
    }

    withType<Jar>().configureEach {
        archiveVersion = artifactVersion
        manifest.attributes("MixinConfigs" to "$modId.mixins.json")
    }
}

java {
    withSourcesJar()
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
}

publishMods {
    // Loom's raw jar is a development artifact; publish the remapped Forge jar.
    file = tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar").flatMap { it.archiveFile }
    version = artifactVersion
    displayName = "${property("mod.name")} ${property("mod.version")} - MC $mcVersion (Forge)"
    modLoaders.add("forge")
    type = STABLE
    dryRun = providers.gradleProperty("publish.dryRun").map(String::toBoolean).orElse(false)
    changelog = providers.environmentVariable("CHANGELOG").filter { it.isNotBlank() }
        .orElse(providers.provider { rootProject.file("CHANGELOG.md").takeIf { it.isFile }?.readText(Charsets.UTF_8) })
        .orElse("Release ${property("mod.version")}. See https://github.com/Tonywww2/DeepRealmThe4th")

    curseforge {
        projectId = providers.gradleProperty("curseforge.projectId")
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN").filter { it.isNotBlank() }
            .orElse(providers.gradleProperty("curseforge.token").filter { it.isNotBlank() })
            // Also support ~/.gradle when GRADLE_USER_HOME points to the local cache.
            .orElse(providers.provider {
                File(System.getProperty("user.home"), ".gradle/gradle.properties")
                    .takeIf { it.isFile }?.inputStream()?.use { input ->
                        Properties().apply { load(input) }.getProperty("curseforge.token")
                            ?.takeIf { it.isNotBlank() }
                    }
            })
        minecraftVersions.add(mcVersion)
        javaVersions.add(JavaVersion.toVersion(targetJavaVersion))
        client = true
        server = true
        requires("curios")
        optional("jei", "kubejs")
    }
}

tasks.named("publishCurseforge") { mustRunAfter(":buildAllVersions") }

val verifyTerrain by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Checks the current spiral terrain and ecology."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.tonywww.deeprealm4th.worldgen.CurrentTerrainVerification"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(targetJavaVersion) }
}

val verifyAstral by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Checks astral layouts, percentage scores, and medal formulas."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.tonywww.deeprealm4th.astral.AstralCoreVerification"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(targetJavaVersion) }
}

val verifyWorldgenOptimization by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Checks exact parity of optimized worldgen calculations."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.tonywww.deeprealm4th.worldgen.WorldgenOptimizationVerification"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(targetJavaVersion) }
}

tasks.check { dependsOn(verifyTerrain, verifyAstral, verifyWorldgenOptimization) }

// Geometry assertions are executed by verifyTerrain, not a JUnit discovery engine.
tasks.test { failOnNoDiscoveredTests = false }

val verifyHydrology by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Checks the current watershed against an exported registry climate."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.tonywww.deeprealm4th.worldgen.GlobalHydrologyVerification"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(targetJavaVersion) }
    args(rootProject.file("run/forge-server/biome-compat-v5/climate.tsv").absolutePath,
        rootProject.layout.buildDirectory.dir("reports/hydrology-current/forge").get().asFile.absolutePath)
}
