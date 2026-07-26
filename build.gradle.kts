plugins {
    id("net.neoforged.moddev") version "2.0.107"
}

// Toolchain versions - these must line up with the TerraFirmaCraft build we depend on
val minecraftVersion: String = "1.21.1"
val neoForgeVersion: String = "21.1.234"
val parchmentVersion: String = "2024.11.17"
val parchmentMinecraftVersion: String = "1.21.1"

// Dependency versions
val jeiVersion: String = "19.25.0.321"
val patchouliVersion: String = "1.21.1-92-NEOFORGE"

val modId: String = "terravera"
val modVersion: String = System.getenv("VERSION") ?: "0.1.0-indev"
val modJavaVersion: String = "21"
val modDataOutput: String = "src/generated/resources"

// TerraFirmaCraft is either a local jar dropped into libs/, or pulled from CurseForge
val tfcCurseProjectId: String = providers.gradleProperty("tfcCurseProjectId").getOrElse("302973")
val tfcCurseFileId: String = providers.gradleProperty("tfcCurseFileId").getOrElse("")
val tfcLocalJars = fileTree("libs") { include("*.jar") }

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val properties = mapOf(
        "modId" to modId,
        "modVersion" to modVersion,
        "minecraftVersionRange" to "[$minecraftVersion]",
        "neoForgeVersionRange" to "[$neoForgeVersion,)",
        "patchouliVersionRange" to "[$patchouliVersion,)",
        "jeiVersionRange" to "[$jeiVersion,)"
    )
    inputs.properties(properties)
    expand(properties)
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

neoForge {
    version = neoForgeVersion
}

base {
    archivesName.set("TerraVera-NeoForge-$minecraftVersion")
    group = "com.terravera"
    version = modVersion
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
}

repositories {
    mavenCentral()
    mavenLocal()
    exclusiveContent {
        forRepository { maven("https://maven.blamejared.com/") }
        filter { includeGroup("mezz.jei") }
    }
    exclusiveContent {
        forRepository { maven("https://maven.blamejared.com") }
        filter { includeGroup("vazkii.patchouli") }
    }
    exclusiveContent {
        forRepository { maven("https://www.cursemaven.com") }
        filter { includeGroup("curse.maven") }
    }
}

sourceSets {
    main {
        resources {
            srcDir(modDataOutput)
            srcDir(generateModMetadata)
        }
    }
    create("data")
}

neoForge {
    addModdingDependenciesTo(sourceSets["data"])

    parchment {
        minecraftVersion.set(parchmentMinecraftVersion)
        mappingsVersion.set(parchmentVersion)
    }

    runs {
        configureEach {
            jvmArguments.addAll("-XX:+IgnoreUnrecognizedVMOptions", "-ea")
        }
        register("client") {
            client()
            gameDirectory = file("run/client")
        }
        register("server") {
            server()
            gameDirectory = file("run/server")
            programArgument("--nogui")
        }
        register("data") {
            data()
            sourceSet = sourceSets["data"]
            programArguments.addAll(
                "--all", "--mod", modId,
                "--output", file(modDataOutput).absolutePath,
                "--existing", file("src/main/resources").absolutePath
            )
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets["data"])
        }
    }

    unitTest {
        enable()
        testedMod = mods[modId]
    }

    ideSyncTask(generateModMetadata)
}

dependencies {
    // TerraFirmaCraft. We compile and run against it - TerraVera is an addon, not a fork.
    if (!tfcLocalJars.isEmpty) {
        logger.lifecycle("TerraVera: using local TerraFirmaCraft jar(s) ${tfcLocalJars.files}")
        implementation(files(tfcLocalJars))
        "dataImplementation"(files(tfcLocalJars))
    } else {
        require(tfcCurseFileId.isNotBlank()) {
            "No TerraFirmaCraft jar found. Either drop one into libs/, or set tfcCurseFileId in gradle.properties."
        }
        implementation(group = "curse.maven", name = "terrafirmacraft-$tfcCurseProjectId", version = tfcCurseFileId)
        "dataImplementation"(group = "curse.maven", name = "terrafirmacraft-$tfcCurseProjectId", version = tfcCurseFileId)
    }

    // TFC hard-depends on Patchouli, so we get it at runtime regardless
    implementation("vazkii.patchouli:Patchouli:$patchouliVersion")

    // JEI, optional
    compileOnly("mezz.jei:jei-$minecraftVersion-common-api:$jeiVersion")
    compileOnly("mezz.jei:jei-$minecraftVersion-neoforge-api:$jeiVersion")
    runtimeOnly("mezz.jei:jei-$minecraftVersion-neoforge:$jeiVersion")

    "dataImplementation"(sourceSets["main"].output)

    testImplementation(sourceSets["data"].output)
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}

tasks.test {
    useJUnitPlatform()
}
