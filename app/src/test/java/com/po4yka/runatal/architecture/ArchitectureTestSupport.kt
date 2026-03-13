package com.po4yka.runatal.architecture

import com.google.common.truth.Truth.assertWithMessage
import com.lemonappdev.konsist.api.Konsist
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

internal const val UI_SCREENS_PACKAGE = "com.po4yka.runatal.ui.screens.."

internal val appProductionScope = Konsist.scopeFromProject(
    moduleName = "app",
    sourceSetName = "main"
)

internal fun assertNoArchitectureViolations(violations: List<String>) {
    val message = if (violations.isEmpty()) {
        "Expected no architecture violations."
    } else {
        violations.joinToString(separator = "\n\n")
    }
    assertWithMessage(message).that(violations).isEmpty()
}

internal fun sourcePathFromLocation(location: String): Path {
    val match = LOCATION_PATTERN.matchEntire(location)
    return if (match != null) {
        Paths.get(match.groupValues[1])
    } else {
        Paths.get(location)
    }
}

internal fun readSourceFile(location: String): String = sourcePathFromLocation(location).toFile().readText()

internal fun screenSourceFiles(): List<Path> {
    val screensRoot = resolveProductionSourcesRoot().resolve("com/po4yka/runatal/ui/screens")
    require(screensRoot.exists()) {
        "Unable to locate screen sources at $screensRoot"
    }

    val paths = mutableListOf<Path>()
    java.nio.file.Files.walk(screensRoot).use { stream ->
        stream
            .filter { java.nio.file.Files.isRegularFile(it) && it.fileName.toString().endsWith("Screen.kt") }
            .sorted()
            .forEach { paths.add(it) }
    }
    return paths
}

private fun resolveProductionSourcesRoot(): Path {
    val candidates = listOf(
        Paths.get("app", "src", "main", "java"),
        Paths.get("src", "main", "java")
    ).map { it.toAbsolutePath().normalize() }

    return candidates.firstOrNull(Path::exists)
        ?: error("Unable to locate production sources. Checked: ${candidates.joinToString()}")
}

private val LOCATION_PATTERN = Regex("^(.*):\\d+:\\d+$")
