package com.po4yka.runatal.data.translation

import android.content.Context
import android.content.res.AssetManager
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertThrows
import org.junit.Test

class AssetTranslationDatasetProviderTest {

    @Test
    fun `loads packaged translation assets and caches parsed values`() {
        val translationDir = sequenceOf(
            File("build/generated/translationAssets/translation"),
            File("app/build/generated/translationAssets/translation")
        ).first { it.isDirectory }
        val context = mockk<Context>()
        val assetManager = mockk<AssetManager>()

        every { context.assets } returns assetManager
        every { assetManager.open(any()) } answers {
            val path = firstArg<String>().removePrefix("translation/")
            translationDir.resolve(path).inputStream()
        }

        val provider = AssetTranslationDatasetProvider(context)

        val manifest = provider.datasetManifest()
        val sourceManifest = provider.sourceManifest()

        assertThat(manifest.version).isNotEmpty()
        assertThat(sourceManifest.sources).isNotEmpty()
        assertThat(provider.oldNorseLexicon()).isNotEmpty()
        assertThat(provider.protoNorseLexicon()).isNotEmpty()
        assertThat(provider.paradigmTables().nounParadigms).isNotEmpty()
        assertThat(provider.youngerPhraseTemplates()).isNotEmpty()
        assertThat(provider.elderAttestedForms()).isNotEmpty()
        assertThat(provider.runicCorpusReferences()).isNotEmpty()
        assertThat(provider.goldExamples()).isNotEmpty()
        assertThat(provider.ereborTables().singleCharacters).isNotEmpty()

        assertThat(provider.datasetManifest()).isSameInstanceAs(manifest)
        assertThat(provider.sourceManifest()).isSameInstanceAs(sourceManifest)
    }

    @Test
    fun `throws serialization error when a dataset asset is malformed`() {
        val context = mockk<Context>()
        val assetManager = mockk<AssetManager>()

        every { context.assets } returns assetManager
        every { assetManager.open("translation/dataset_manifest.json") } returns ByteArrayInputStream(
            """{"version": }""".toByteArray()
        )

        val provider = AssetTranslationDatasetProvider(context)

        assertThrows(SerializationException::class.java) {
            provider.datasetManifest()
        }
    }
}
