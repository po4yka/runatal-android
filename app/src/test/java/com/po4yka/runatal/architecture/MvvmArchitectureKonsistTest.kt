package com.po4yka.runatal.architecture

import dagger.hilt.android.lifecycle.HiltViewModel
import org.junit.Test

class MvvmArchitectureKonsistTest {

    private val viewModels by lazy {
        appProductionScope.classes().filter { declaration ->
            declaration.name.endsWith("ViewModel") && declaration.resideInPackage(UI_SCREENS_PACKAGE)
        }
    }

    @Test
    fun `screen ViewModels extend lifecycle ViewModel`() {
        val violations = viewModels
            .filterNot { declaration ->
                readSourceFile(declaration.location).let { source ->
                    lifecycleViewModelInheritancePattern.containsMatchIn(source)
                }
            }
            .map { declaration ->
                buildString {
                    appendLine(declaration.location)
                    appendLine("Rule: Screen ViewModels must extend androidx.lifecycle.ViewModel.")
                    append("Expected: declare the ViewModel class with `: ViewModel()`.")
                }
            }

        assertNoArchitectureViolations(violations)
    }

    @Test
    fun `screen ViewModels are annotated with HiltViewModel`() {
        val violations = viewModels
            .filterNot { it.hasAnnotationOf(HiltViewModel::class) }
            .map { declaration ->
                buildString {
                    appendLine(declaration.location)
                    appendLine("Rule: Screen ViewModels must be annotated with @HiltViewModel.")
                    append("Expected: add @HiltViewModel to the ViewModel class declaration.")
                }
            }

        assertNoArchitectureViolations(violations)
    }

    @Test
    fun `screen ViewModels do not expose public mutable stream primitives`() {
        val violations = viewModels.flatMap { declaration ->
            declaration.properties()
                .filter { it.hasPublicOrDefaultModifier }
                .mapNotNull { property ->
                    val forbiddenType = forbiddenPublicStreamPatterns.keys.firstOrNull { typeName ->
                        forbiddenPublicStreamPatterns.getValue(typeName).containsMatchIn(property.text)
                    }
                    forbiddenType?.let {
                        buildString {
                            appendLine(property.location)
                            appendLine("Rule: ViewModels must not expose public mutable stream primitives.")
                            appendLine("Found: $forbiddenType in `${property.text.trim()}`")
                            append("Expected: expose StateFlow, SharedFlow, or Flow via asStateFlow() / receiveAsFlow().")
                        }
                    }
                }
        }

        assertNoArchitectureViolations(violations)
    }

    @Test
    fun `screen ViewModels do not expose public callback parameters`() {
        val violations = viewModels.flatMap { declaration ->
            declaration.functions()
                .filter { it.hasPublicOrDefaultModifier }
                .mapNotNull { function ->
                    val signature = function.text.substringBefore("{").substringBefore("=")
                    if (callbackParameterPattern.containsMatchIn(signature)) {
                        buildString {
                            appendLine(function.location)
                            appendLine("Rule: ViewModel APIs must not accept UI callback parameters.")
                            appendLine("Found: `${signature.trim()}`")
                            append("Expected: emit UI events from the ViewModel instead of accepting lambdas.")
                        }
                    } else {
                        null
                    }
                }
        }

        assertNoArchitectureViolations(violations)
    }

    @Test
    fun `screen ViewModels do not depend on UI or platform-facing types`() {
        val violations = viewModels.flatMap { declaration ->
            val source = readSourceFile(declaration.location)
            val importLines = source.lineSequence()
                .map(String::trim)
                .filter { it.startsWith("import ") }
                .toList()

            val forbiddenImports = importLines.filter { importLine ->
                forbiddenImportPrefixes.any(importLine::startsWith) ||
                    importLine == "import com.po4yka.runatal.util.QuoteShareManager"
            }
            val fullyQualifiedReferences = forbiddenQualifiedReferencePatterns.keys.filter { pattern ->
                pattern.containsMatchIn(declaration.text)
            }

            buildList {
                if (forbiddenImports.isNotEmpty()) {
                    add(
                        buildString {
                            appendLine(declaration.location)
                            appendLine("Rule: ViewModels must not import UI or platform-facing types.")
                            appendLine("Found imports:")
                            forbiddenImports.forEach { appendLine("- $it") }
                            append(
                                "Expected: keep ViewModels limited to lifecycle, domain, data, and coroutine types."
                            )
                        }
                    )
                }
                if (fullyQualifiedReferences.isNotEmpty()) {
                    add(
                        buildString {
                            appendLine(declaration.location)
                            appendLine("Rule: ViewModels must not reference UI or platform-facing types by FQCN.")
                            appendLine("Found references:")
                            fullyQualifiedReferences.forEach { appendLine("- ${forbiddenQualifiedReferencePatterns.getValue(it)}") }
                            append(
                                "Expected: remove UI/platform dependencies and surface behavior through state or events."
                            )
                        }
                    )
                }
            }
        }

        assertNoArchitectureViolations(violations)
    }

    private companion object {
        val forbiddenPublicStreamPatterns = linkedMapOf(
            "MutableStateFlow" to Regex("""\bMutableStateFlow\s*<"""),
            "MutableSharedFlow" to Regex("""\bMutableSharedFlow\s*<"""),
            "Channel" to Regex("""\bChannel\s*<""")
        )

        val lifecycleViewModelInheritancePattern = Regex(
            pattern = """class\s+\w*ViewModel\b[\s\S]*?:\s*ViewModel\s*\(""",
            options = setOf(RegexOption.MULTILINE)
        )

        val callbackParameterPattern = Regex(
            pattern = """fun\s+\w+\s*\([^)]*(->|Function\d)""",
            options = setOf(RegexOption.DOT_MATCHES_ALL)
        )

        val forbiddenImportPrefixes = listOf(
            "import android.content.",
            "import android.app.",
            "import android.widget.",
            "import androidx.compose.",
            "import androidx.navigation.",
            "import androidx.activity.",
            "import androidx.fragment.",
            "import com.po4yka.runatal.ui."
        )

        val forbiddenQualifiedReferencePatterns = linkedMapOf(
            Regex("""\bandroid\.content\.""") to "android.content..",
            Regex("""\bandroid\.app\.""") to "android.app..",
            Regex("""\bandroid\.widget\.""") to "android.widget..",
            Regex("""\bandroidx\.compose\.""") to "androidx.compose..",
            Regex("""\bandroidx\.navigation\.""") to "androidx.navigation..",
            Regex("""\bandroidx\.activity\.""") to "androidx.activity..",
            Regex("""\bandroidx\.fragment\.""") to "androidx.fragment..",
            Regex("""\bcom\.po4yka\.runicquotes\.ui\.""") to "com.po4yka.runatal.ui..",
            Regex("""\bcom\.po4yka\.runicquotes\.util\.QuoteShareManager\b""") to
                "com.po4yka.runatal.util.QuoteShareManager"
        )
    }
}
