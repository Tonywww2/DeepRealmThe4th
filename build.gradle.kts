import org.gradle.api.tasks.compile.JavaCompile
import net.fabricmc.loom.api.remapping.RemapperParameters
import com.tony.build.BopTreeDevRemapper

plugins {
    id("dev.architectury.loom") // pinned in buildSrc for its dev-remapper API
}

val loader = property("loom.platform").toString()
val mcVersion = property("vers.mcVersion").toString()
val modId = property("mod.id").toString()
val artifactVersion = "${property("mod.version")}+$mcVersion"
val targetJavaVersion = 17
val biomeCompat = providers.gradleProperty("biomeCompat").map(String::toBoolean).getOrElse(false)

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
    args(rootProject.file("run/forge-server/biome-compat-v5/climate.tsv").absolutePath,
        rootProject.layout.buildDirectory.dir("reports/hydrology-current/forge").get().asFile.absolutePath)
}
