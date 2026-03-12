import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    jacoco
}

android {
    namespace = "com.po4yka.runicquotes"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.po4yka.runicquotes"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

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
            assets.srcDirs("$buildDir/generated/translationAssets")
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
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
}

// Room schema export for KSP
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
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
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
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

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.room.testing)
}

val translationSeedDir = layout.projectDirectory.dir("src/main/translationSeed")
val translationDataDir = layout.projectDirectory.dir("src/main/translationSeed/translation")
val generatedTranslationAssetsDir = layout.buildDirectory.dir("generated/translationAssets")

val validateTranslationCuration by tasks.registering {
    inputs.dir(translationDataDir)
    notCompatibleWithConfigurationCache("Uses JSON parsing from a build-script closure.")

    doLast {
        val slurper = JsonSlurper()
        val baseDir = translationDataDir.asFile
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

val generateTranslationAssets by tasks.registering(Sync::class) {
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

// JaCoCo configuration for code coverage.
val coverageExclusions = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "**/*_Factory.*",
    "**/*_HiltModules*.*",
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

val coverageSourceDirectories = files(
    "$projectDir/src/main/java",
    "$projectDir/src/main/kotlin"
)

fun coverageExecutionData() = fileTree(layout.buildDirectory) {
    include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    include("outputs/code_coverage/debugAndroidTest/connected/**/*.ec")
}

fun coverageClassTree(includes: List<String>? = null) =
    fileTree("${layout.buildDirectory.get()}/intermediates/classes/debug/transformDebugClassesWithAsm/dirs") {
        includes?.let { include(it) }
        exclude(coverageExclusions)
    }

tasks.register<JacocoReport>("jacocoProjectCoverageReport") {
    dependsOn("testDebugUnitTest")
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
    dependsOn("testDebugUnitTest")
    group = "verification"
    description = "Generates focused coverage for the transliteration domain layer."

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
    executionData.setFrom(coverageExecutionData())
}

tasks.register<JacocoCoverageVerification>("jacocoTransliterationCoverageVerification") {
    dependsOn("jacocoTransliterationCoverageReport")
    group = "verification"
    description = "Enforces the transliteration line-coverage target."

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
    executionData.setFrom(coverageExecutionData())
}

tasks.named("check") {
    dependsOn("jacocoTransliterationCoverageVerification")
}

tasks.named("preBuild") {
    dependsOn(generateTranslationAssets)
}
