plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.20.1-forge"

stonecutter parameters {
    val loader = current.project.substringAfterLast('-')
    constants {
        match(loader, "forge", "neoforge")
    }
}

tasks.register("buildAllVersions") {
    group = "build"
    description = "Packages the Forge and NeoForge release jars without running verification tasks."
    dependsOn(":1.20.1-forge:remapJar", ":1.21.1-neoforge:jar")
}

tasks.register("publishAllVersions") {
    group = "publishing"
    description = "Builds both loader jars and publishes them to CurseForge."
    dependsOn("buildAllVersions")
    dependsOn(stonecutter.tasks.named("publishMods").map { it.values })
}

// Publish the loader files serially, after both release jars have been built.
stonecutter.tasks.order("publishCurseforge")

