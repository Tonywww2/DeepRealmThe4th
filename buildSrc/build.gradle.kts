plugins { java }
repositories {
    mavenCentral()
    maven("https://maven.architectury.dev/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.minecraftforge.net/")
    maven("https://libraries.minecraft.net/")
    gradlePluginPortal()
}
dependencies {
    implementation("dev.architectury:architectury-loom:1.11.458")
    compileOnly("org.ow2.asm:asm-commons:9.8")
}
