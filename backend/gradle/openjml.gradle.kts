import java.net.URI

val openjmlVersion = "21-0.21"
val openjmlDir = layout.projectDirectory.dir(".openjml")
val openjmlBinary = openjmlDir.file("openjml")

// ESC is scoped to state/ package only (pure enums, no framework annotations).
// - model/ classes: JPA no-arg constructors produce NullField false positives
// - exception/ classes: OpenJML's own CharSequence.jml spec has invariant bugs
//   when verifying String parameters passed to super constructors
// JML contracts on excluded files are still enforced at L1 (test pairs).
val escSrcDirs = listOf(
    "src/main/java/com/keplerops/groundcontrol/domain/requirements/state",
    "src/main/java/com/keplerops/groundcontrol/domain/adrs/state"
)
val racSrcDir = "src/main/java/com/keplerops/groundcontrol/domain"

tasks.register("downloadOpenJml") {
    group = "verification"
    description = "Downloads OpenJML $openjmlVersion to .openjml/"

    outputs.dir(openjmlDir)

    doLast {
        val dir = openjmlDir.asFile
        if (dir.resolve("openjml").exists()) {
            logger.lifecycle("OpenJML already present at ${dir.absolutePath}, skipping download.")
            return@doLast
        }

        val platform = if (System.getProperty("os.name").lowercase().contains("mac")) {
            "macos-14"
        } else {
            "ubuntu-latest"
        }
        val zipName = "openjml-$platform-$openjmlVersion.zip"
        val url = "https://github.com/OpenJML/OpenJML/releases/download/$openjmlVersion/$zipName"

        logger.lifecycle("Downloading OpenJML $openjmlVersion from $url")

        val zipFile = layout.buildDirectory.file("tmp/$zipName").get().asFile
        zipFile.parentFile.mkdirs()

        URI(url).toURL().openStream().use { input ->
            zipFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        logger.lifecycle("Extracting to ${dir.absolutePath}")
        dir.mkdirs()

        project.exec {
            commandLine("unzip", "-o", "-q", zipFile.absolutePath, "-d", dir.absolutePath)
        }

        // The zip extracts into a subdirectory — move contents up if needed
        val nested = dir.listFiles()?.singleOrNull { it.isDirectory && it.name.startsWith("openjml") }
        if (nested != null && nested.resolve("openjml").exists()) {
            nested.listFiles()?.forEach { it.renameTo(dir.resolve(it.name)) }
            nested.delete()
        }

        dir.resolve("openjml").setExecutable(true)

        logger.lifecycle("OpenJML $openjmlVersion installed successfully.")
    }
}

tasks.register<Exec>("openjmlEsc") {
    group = "verification"
    description = "Run OpenJML Extended Static Checking on domain source code"
    dependsOn("downloadOpenJml", "compileJava")

    // Gradle up-to-date checking: skip if sources haven't changed
    escSrcDirs.forEach { inputs.dir(it) }
    outputs.file(layout.buildDirectory.file("openjml/esc.marker"))

    doFirst {
        val binary = openjmlDir.asFile.resolve("openjml")
        if (!binary.exists()) {
            throw GradleException("OpenJML binary not found at ${binary.absolutePath}. Run ./gradlew downloadOpenJml first.")
        }

        val compileCp = project.configurations.getByName("compileClasspath").asPath +
            File.pathSeparator + layout.buildDirectory.dir("classes/java/main").get().asFile.absolutePath

        val sourceFiles = escSrcDirs.flatMap { dir ->
            layout.projectDirectory.dir(dir).asFile.walkTopDown()
                .filter { it.isFile && it.extension == "java" }
                .map { it.absolutePath }
                .toList()
        }

        if (sourceFiles.isEmpty()) {
            throw GradleException("No Java source files found in ESC source dirs: $escSrcDirs")
        }

        commandLine(
            listOf(binary.absolutePath, "--esc", "-classpath", compileCp) + sourceFiles
        )
    }

    doLast {
        val marker = layout.buildDirectory.file("openjml/esc.marker").get().asFile
        marker.parentFile.mkdirs()
        marker.writeText("ESC passed at ${java.time.Instant.now()}")
    }
}

tasks.register<Exec>("openjmlRac") {
    group = "verification"
    description = "Run OpenJML Runtime Assertion Checking compilation on domain source code"
    dependsOn("downloadOpenJml")

    doFirst {
        val binary = openjmlDir.asFile.resolve("openjml")
        if (!binary.exists()) {
            throw GradleException("OpenJML binary not found at ${binary.absolutePath}. Run ./gradlew downloadOpenJml first.")
        }

        val compileCp = project.configurations.getByName("compileClasspath").asPath

        val racOutputDir = layout.buildDirectory.dir("classes/rac").get().asFile
        racOutputDir.mkdirs()

        val domainDir = layout.projectDirectory.dir(racSrcDir).asFile
        val sourceFiles = domainDir.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .map { it.absolutePath }
            .toList()

        if (sourceFiles.isEmpty()) {
            throw GradleException("No Java source files found in $racSrcDir")
        }

        commandLine(
            listOf(binary.absolutePath, "--rac", "-classpath", compileCp, "-d", racOutputDir.absolutePath) + sourceFiles
        )
    }
}
