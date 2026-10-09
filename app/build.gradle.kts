import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.ScopedArtifacts
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    jacoco
}

android {
    namespace = "com.po4yka.runatal"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.po4yka.runatal"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "COROUTINES_VERSION", "\"${libs.versions.coroutines.get()}\"")
        buildConfigField("String", "ROOM_VERSION", "\"${libs.versions.room.get()}\"")
        buildConfigField("String", "DATASTORE_VERSION", "\"${libs.versions.datastore.get()}\"")
        buildConfigField("String", "HILT_VERSION", "\"${libs.versions.hilt.asProvider().get()}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "ENABLE_EXPERIMENTAL_TRANSLATE", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
            buildConfigField("boolean", "ENABLE_EXPERIMENTAL_TRANSLATE", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        getByName("main") {
            assets.directories.add(layout.buildDirectory.dir("generated/translationAssets").get().asFile.path)
        }
    }

    lint {
        // Keep warnings informational, but fail build on lint errors.
        abortOnError = true
        // Treat warnings as informational
        warningsAsErrors = false
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
}

// Room configures schema export and migration-test assets through its Gradle plugin.
room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Compose BOM
    implementation(platform(libs.compose.bom))

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Compose
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    implementation(libs.hilt.work)

    // Room
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Glance
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Navigation 3
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.lifecycle.viewmodel.navigation3)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Baseline Profiles
    implementation(libs.androidx.profileinstaller)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.konsist)
    testImplementation(libs.work.testing)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.work.testing)
}

val translationSeedDir = layout.projectDirectory.dir("src/main/translationSeed")
val translationDataDir = layout.projectDirectory.dir("src/main/translationSeed/translation")
val generatedTranslationAssetsDir = layout.buildDirectory.dir("generated/translationAssets")

@Suppress("TooManyFunctions") // validation task with many targeted checks
abstract class ValidateTranslationCurationTask : DefaultTask() {

    @get:InputDirectory
    abstract val dataDir: DirectoryProperty

    @TaskAction
    fun validate() {
        val slurper = groovy.json.JsonSlurper()
        val baseDir = dataDir.get().asFile
        val requiredFiles = listOf(
            "dataset_manifest.json",
            "source_manifest.json",
            "old_norse_lexicon.json",
            "proto_norse_lexicon.json",
            "paradigm_tables.json",
            "fallback_templates.json",
            "grammar_rules.json",
            "name_adaptations.json",
            "younger_phrase_templates.json",
            "elder_attested_forms.json",
            "runic_corpus_refs.json",
            "erebor_tables.json",
            "gold_examples.json"
        )
        requiredFiles.forEach { fileName ->
            check(baseDir.resolve(fileName).isFile) {
                "Missing curated translation file: $fileName"
            }
        }

        fun parseArray(fileName: String): List<Map<String, Any?>> {
            @Suppress("UNCHECKED_CAST")
            return slurper.parse(baseDir.resolve(fileName)) as List<Map<String, Any?>>
        }

        @Suppress("UNCHECKED_CAST")
        val sourceManifest = slurper.parse(baseDir.resolve("source_manifest.json")) as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val sources = sourceManifest["sources"] as List<Map<String, Any?>>
        check(sources.isNotEmpty()) { "source_manifest.json must contain at least one source." }
        val sourceIds = sources.map { (it["id"] as String).trim() }.toSet()
        val missingLicenses = sources.filter { (it["license"] as? String).isNullOrBlank() }
        check(missingLicenses.isEmpty()) { "Every source entry must include a non-empty license." }

        fun validateUniqueIds(fileName: String, rows: List<Map<String, Any?>>) {
            val ids = rows.map { (it["id"] as? String).orEmpty() }
            check(ids.none { it.isBlank() }) { "$fileName contains rows without an id." }
            check(ids.size == ids.toSet().size) { "$fileName contains duplicate ids." }
        }

        fun validateSourceIds(fileName: String, rows: List<Map<String, Any?>>) {
            val invalidRows = rows.filter {
                val sourceId = (it["sourceId"] as? String).orEmpty()
                sourceId.isNotBlank() && sourceId !in sourceIds
            }
            check(invalidRows.isEmpty()) { "$fileName contains unknown sourceId values." }
        }

        fun validateStrictCitations(fileName: String, rows: List<Map<String, Any?>>) {
            val invalidRows = rows.filter { row ->
                val strictEligible = row["strictEligible"] as? Boolean ?: false
                @Suppress("UNCHECKED_CAST")
                val citations = row["citations"] as? List<Any?> ?: emptyList()
                strictEligible && citations.none { !it.toString().isNullOrBlank() }
            }
            check(invalidRows.isEmpty()) { "$fileName contains strict rows without citations." }
        }

        val oldNorseLexicon = parseArray("old_norse_lexicon.json")
        val protoNorseLexicon = parseArray("proto_norse_lexicon.json")
        val youngerTemplates = parseArray("younger_phrase_templates.json")
        val elderTemplates = parseArray("elder_attested_forms.json")
        val corpusRefs = parseArray("runic_corpus_refs.json")
        val goldExamples = parseArray("gold_examples.json")
        @Suppress("UNCHECKED_CAST")
        val ereborTables = slurper.parse(baseDir.resolve("erebor_tables.json")) as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val ereborPhraseMappings = ereborTables["phraseMappings"] as? List<Map<String, Any?>> ?: emptyList()

        validateUniqueIds("old_norse_lexicon.json", oldNorseLexicon)
        validateUniqueIds("proto_norse_lexicon.json", protoNorseLexicon)
        validateUniqueIds("younger_phrase_templates.json", youngerTemplates)
        validateUniqueIds("elder_attested_forms.json", elderTemplates)
        validateUniqueIds("runic_corpus_refs.json", corpusRefs)
        validateUniqueIds("gold_examples.json", goldExamples)
        validateUniqueIds("erebor_tables.json#phraseMappings", ereborPhraseMappings)
        validateSourceIds("old_norse_lexicon.json", oldNorseLexicon)
        validateSourceIds("proto_norse_lexicon.json", protoNorseLexicon)
        validateSourceIds("runic_corpus_refs.json", corpusRefs)
        validateStrictCitations("old_norse_lexicon.json", oldNorseLexicon)
        validateStrictCitations("proto_norse_lexicon.json", protoNorseLexicon)

        oldNorseLexicon.forEach { row ->
            val formFields = listOf("nounForms", "adjectiveForms", "presentForms", "pastForms")
            val hasForms = formFields.any { (row[it] as? Map<*, *>)?.isNotEmpty() == true }
            if (hasForms) {
                check(row["inflectionSourceId"] in sourceIds) { "Unknown inflection source in ${row["id"]}." }
                check((row["inflectionCitations"] as? List<*>)?.any { !it.toString().isBlank() } == true) {
                    "Missing inflection citations in ${row["id"]}."
                }
                formFields.forEach { field ->
                    val forms = row[field] as? Map<*, *> ?: emptyMap<Any, Any>()
                    check(forms.values.all { it is String && it.isNotBlank() }) { "Blank forms in ${row["id"]}." }
                }
            }
        }
        @Suppress("UNCHECKED_CAST")
        val grammarRules = slurper.parse(baseDir.resolve("grammar_rules.json")) as Map<String, Any?>
        val government = grammarRules["governedPrepositions"] as? Map<*, *> ?: emptyMap<Any, Any>()
        government.values.forEach { value ->
            val rule = value as? Map<*, *> ?: error("Invalid preposition government row.")
            check(rule["sourceId"] in sourceIds) { "Unknown preposition government source." }
            check(rule["grammaticalCase"] in setOf("NOMINATIVE", "ACCUSATIVE", "GENITIVE", "DATIVE")) {
                "Invalid governed case."
            }
            check((rule["citations"] as? List<*>)?.any { !it.toString().isBlank() } == true) {
                "Missing preposition-government citation."
            }
        }

        val corpusRefIds = corpusRefs.map { it["id"] as String }.toSet()

        fun validateTemplateRows(fileName: String, rows: List<Map<String, Any?>>) {
            val invalidRows = rows.filter { row ->
                (row["script"] as? String).isNullOrBlank() ||
                    (row["fidelity"] as? String).isNullOrBlank() ||
                    (row["derivationKind"] as? String).isNullOrBlank()
            }
            check(invalidRows.isEmpty()) {
                "$fileName contains rows without script, fidelity, or derivationKind metadata."
            }
            val brokenRefs = rows.flatMap { row ->
                @Suppress("UNCHECKED_CAST")
                val refs = row["referenceIds"] as? List<String> ?: emptyList()
                refs.filterNot(corpusRefIds::contains)
            }
            check(brokenRefs.isEmpty()) { "$fileName contains unknown runic corpus references." }
        }

        validateTemplateRows("younger_phrase_templates.json", youngerTemplates)
        validateTemplateRows("elder_attested_forms.json", elderTemplates)

        val invalidGoldResults = goldExamples.flatMap { example ->
            @Suppress("UNCHECKED_CAST")
            val results = example["results"] as List<Map<String, Any?>>
            results.filter {
                (it["script"] as? String).isNullOrBlank() ||
                    (it["fidelity"] as? String).isNullOrBlank() ||
                    (it["derivationKind"] as? String).isNullOrBlank()
            }
        }
        check(invalidGoldResults.isEmpty()) {
            "gold_examples.json contains results without script, fidelity, or derivationKind metadata."
        }

        val invalidGoldProvenance = goldExamples.flatMap { example ->
            @Suppress("UNCHECKED_CAST")
            val results = example["results"] as List<Map<String, Any?>>
            results.filter { (it["fidelity"] as? String) == "STRICT" }.flatMap { result ->
                @Suppress("UNCHECKED_CAST")
                val provenance = result["provenance"] as? List<Map<String, Any?>> ?: emptyList()
                provenance.filter { provenanceEntry ->
                    val sourceId = (provenanceEntry["sourceId"] as? String).orEmpty()
                    val referenceId = provenanceEntry["referenceId"] as? String
                    sourceId !in sourceIds || (referenceId != null && referenceId !in corpusRefIds)
                }
            }
        }
        check(invalidGoldProvenance.isEmpty()) {
            "gold_examples.json contains strict provenance entries with broken source or reference ids."
        }

        val invalidEreborRefs = ereborPhraseMappings.flatMap { row ->
            @Suppress("UNCHECKED_CAST")
            val refs = row["referenceIds"] as? List<String> ?: emptyList()
            refs.filterNot(corpusRefIds::contains)
        }
        check(invalidEreborRefs.isEmpty()) {
            "erebor_tables.json contains phrase mappings with unknown runic corpus references."
        }
    }
}

val validateTranslationCuration = tasks.register<ValidateTranslationCurationTask>("validateTranslationCuration") {
    dataDir.set(translationDataDir)
}

val generateTranslationAssets = tasks.register<Sync>("generateTranslationAssets") {
    dependsOn(validateTranslationCuration)
    from(translationSeedDir)
    into(generatedTranslationAssetsDir)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/detekt.yml"))
    source.setFrom(
        "src/main/java",
        "src/main/kotlin"
    )
}

// Use the same JaCoCo version for AGP instrumentation and Gradle reports.
android {
    testCoverage {
        jacocoVersion = libs.versions.jacoco.get()
    }
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

// Consume AGP's public, uninstrumented project classes rather than an internal task output path.
abstract class CollectCoverageClasses : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val classDirectories: ListProperty<Directory>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val classJars: ListProperty<RegularFile>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun collect() {
        fileSystemOperations.sync {
            from(classDirectories)
            classJars.get().forEach { from(archiveOperations.zipTree(it)) }
            into(outputDirectory)
            include("**/*.class")
            duplicatesStrategy = DuplicatesStrategy.FAIL
        }
        check(outputDirectory.get().asFile.resolve("com/po4yka/runatal/MainActivity.class").isFile) {
            "AGP did not provide application classes for coverage."
        }
    }
}

val collectDebugCoverageClasses = tasks.register<CollectCoverageClasses>("collectDebugCoverageClasses") {
    outputDirectory.set(layout.buildDirectory.dir("coverage/classes/debug"))
}

androidComponents.onVariants(androidComponents.selector().withBuildType("debug")) { variant ->
    variant.artifacts.forScope(ScopedArtifacts.Scope.PROJECT)
        .use(collectDebugCoverageClasses)
        .toGet(
            ScopedArtifact.CLASSES,
            CollectCoverageClasses::classJars,
            CollectCoverageClasses::classDirectories
        )
}

// JaCoCo configuration for code coverage.
val coverageExclusions = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "**/*_Factory.*",
    "**/*_AssistedFactory.*",
    "**/*_HiltModules*.*",
    "**/*_HiltComponents*.*",
    "**/*_MembersInjector.*",
    "**/*_Impl*.*",
    "**/*ComponentTreeDeps.*",
    "**/*GeneratedInjector.*",
    "**/ComposableSingletons*.*",
    "**/Dagger*.*",
    "**/Hilt_*.*",
    "**/*\$Companion*.*",
    "**/*\$WhenMappings*.*",
    "android/**/*.*",
    "dagger/hilt/internal/**/*.*",
    "hilt_aggregated_deps/**/*.*"
)

val transliterationCoverageIncludes = listOf(
    "**/domain/transliteration/**"
)

val translationCoverageIncludes = listOf(
    "**/domain/translation/**",
    "**/data/repository/TranslationRepository*",
    "**/data/translation/AssetTranslationDatasetProvider*",
    "**/worker/TranslationBackfillWorker*",
    "**/ui/screens/translation/TranslationViewModel*"
)

val translationCoverageExcludes = listOf(
    "**/ui/screens/translation/TranslationScreen*",
    "**/ui/screens/translation/TranslationAccuracyScreen*"
)

val coverageSourceDirectories = files(
    "$projectDir/src/main/java",
    "$projectDir/src/main/kotlin"
)

fun coverageExecutionData() = fileTree(layout.buildDirectory) {
    include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    include("outputs/code_coverage/debugAndroidTest/connected/**/*.ec")
}

fun unitCoverageExecutionData() = fileTree(layout.buildDirectory) {
    include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
}

fun coverageClassTree(
    includes: List<String>? = null,
    extraExcludes: List<String> = emptyList()
) =
    fileTree(collectDebugCoverageClasses.flatMap { it.outputDirectory }) {
        includes?.let { include(it) }
        exclude(coverageExclusions + extraExcludes)
    }

tasks.register<JacocoReport>("jacocoProjectCoverageReport") {
    dependsOn("testDebugUnitTest", collectDebugCoverageClasses)
    mustRunAfter("connectedDebugAndroidTest")
    group = "verification"
    description = "Generates project coverage from unit tests and any available Android test coverage."

    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/projectCoverage/projectCoverage.xml"))
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/projectCoverage/html"))
    }

    sourceDirectories.setFrom(coverageSourceDirectories)
    classDirectories.setFrom(files(coverageClassTree()))
    executionData.setFrom(coverageExecutionData())
}

tasks.register<JacocoReport>("jacocoTransliterationCoverageReport") {
    dependsOn("testDebugUnitTest", collectDebugCoverageClasses)
    mustRunAfter("connectedDebugAndroidTest")
    group = "verification"
    description = "Generates focused JVM coverage for the transliteration domain layer."

    reports {
        xml.required.set(true)
        xml.outputLocation.set(
            layout.buildDirectory.file("reports/jacoco/transliterationCoverage/transliterationCoverage.xml")
        )
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/transliterationCoverage/html"))
    }

    sourceDirectories.setFrom(coverageSourceDirectories)
    classDirectories.setFrom(files(coverageClassTree(transliterationCoverageIncludes)))
    executionData.setFrom(unitCoverageExecutionData())
}

tasks.register<JacocoCoverageVerification>("jacocoTransliterationCoverageVerification") {
    dependsOn("jacocoTransliterationCoverageReport", collectDebugCoverageClasses)
    mustRunAfter("connectedDebugAndroidTest")
    group = "verification"
    description = "Enforces the transliteration JVM line-coverage target."

    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
    }

    sourceDirectories.setFrom(coverageSourceDirectories)
    classDirectories.setFrom(files(coverageClassTree(transliterationCoverageIncludes)))
    executionData.setFrom(unitCoverageExecutionData())
}

tasks.register<JacocoReport>("jacocoTranslationCoverageReport") {
    dependsOn("testDebugUnitTest", collectDebugCoverageClasses)
    group = "verification"
    description = "Generates focused JVM coverage for the translation feature."

    reports {
        xml.required.set(true)
        xml.outputLocation.set(
            layout.buildDirectory.file("reports/jacoco/translationCoverage/translationCoverage.xml")
        )
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/translationCoverage/html"))
    }

    sourceDirectories.setFrom(coverageSourceDirectories)
    classDirectories.setFrom(files(coverageClassTree(translationCoverageIncludes, translationCoverageExcludes)))
    executionData.setFrom(unitCoverageExecutionData())
}

tasks.register<JacocoCoverageVerification>("jacocoTranslationCoverageVerification") {
    dependsOn("jacocoTranslationCoverageReport", collectDebugCoverageClasses)
    group = "verification"
    description = "Enforces the translation JVM line-coverage target."

    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.85".toBigDecimal()
            }
        }
    }

    sourceDirectories.setFrom(coverageSourceDirectories)
    classDirectories.setFrom(files(coverageClassTree(translationCoverageIncludes, translationCoverageExcludes)))
    executionData.setFrom(unitCoverageExecutionData())
}

tasks.named("check") {
    dependsOn("jacocoTransliterationCoverageVerification")
    dependsOn("jacocoTranslationCoverageVerification")
}

tasks.named("preBuild") {
    dependsOn(generateTranslationAssets)
}

// Robolectric's Android 17 environment accesses public JDK FileDescriptor internals.
tasks.withType<Test>().configureEach {
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
}
