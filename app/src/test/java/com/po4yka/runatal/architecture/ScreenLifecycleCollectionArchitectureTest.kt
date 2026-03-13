package com.po4yka.runatal.architecture

import org.junit.Test

class ScreenLifecycleCollectionArchitectureTest {

    @Test
    fun `screen files use collectAsStateWithLifecycle for ViewModel state`() {
        val violations = screenSourceFiles().mapNotNull { path ->
            val source = path.toFile().readText()
            if (plainCollectAsStatePattern.containsMatchIn(source)) {
                buildString {
                    appendLine(path)
                    appendLine("Rule: Screen files must collect ViewModel state with collectAsStateWithLifecycle().")
                    appendLine("Found: plain collectAsState() usage in a screen source file.")
                    append("Expected: replace collectAsState() with collectAsStateWithLifecycle().")
                }
            } else {
                null
            }
        }

        assertNoArchitectureViolations(violations)
    }

    private companion object {
        val plainCollectAsStatePattern = Regex("""\bcollectAsState\s*\(""")
    }
}
