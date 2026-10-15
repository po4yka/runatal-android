package com.po4yka.runatal.data.translation

import android.content.Context
import android.content.res.AssetManager
import io.mockk.every
import io.mockk.mockk
import java.io.File

/** Opens the real generated assets (or their checked-in source when no generated directory exists). */
internal fun bundledTranslationTestData(): AssetTranslationDatasetProvider {
    val directory = sequenceOf(
        File("build/generated/translationAssets/translation"),
        File("app/build/generated/translationAssets/translation"),
        File("src/main/translationSeed/translation"), File("app/src/main/translationSeed/translation")
    ).first { it.isDirectory }
    val context = mockk<Context>()
    val assets = mockk<AssetManager>()
    every { context.assets } returns assets
    every { assets.open(any()) } answers {
        directory.resolve(firstArg<String>().removePrefix("translation/")).inputStream()
    }
    return AssetTranslationDatasetProvider(context)
}
