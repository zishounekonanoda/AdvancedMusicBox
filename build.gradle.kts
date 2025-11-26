plugins {
    `java-library`
    id("io.github.goooler.shadow") version "8.1.7"
}

group = "pl.nekorin"
version = "1.1.0"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.codemc.org/repository/maven-public")
    maven("https://jitpack.io")
}

java {
    targetCompatibility = JavaVersion.VERSION_21
    sourceCompatibility = JavaVersion.VERSION_21
}
dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("com.github.koca2000:NoteBlockAPI:1.6.1")

    implementation("com.github.cryptomorin:XSeries:11.0.0")
    implementation("io.github.bananapuncher714:nbteditor:7.19.0")
    implementation("org.bstats:bstats-bukkit:3.0.2")

    api(project(":nms"))
    implementation(project(mapOf("path" to ":nms:versions:21", "configuration" to "reobf")))
    implementation(project(mapOf("path" to ":nms:versions:21_2", "configuration" to "reobf")))

    compileOnly("org.yaml:snakeyaml:2.0")
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    compileOnly("org.jetbrains:annotations:24.0.1")

}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("org.bstats", "pl.nekorin.musicbox.shadow.bstats")
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
