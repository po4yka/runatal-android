package com.po4yka.runatal.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.po4yka.runatal.util.QuoteShareManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface QuoteShareManagerEntryPoint {
    fun quoteShareManager(): QuoteShareManager
}

@Composable
internal fun rememberQuoteShareManager(): QuoteShareManager {
    val applicationContext = LocalContext.current.applicationContext
    return remember(applicationContext) {
        EntryPointAccessors.fromApplication(
            applicationContext,
            QuoteShareManagerEntryPoint::class.java
        ).quoteShareManager()
    }
}
