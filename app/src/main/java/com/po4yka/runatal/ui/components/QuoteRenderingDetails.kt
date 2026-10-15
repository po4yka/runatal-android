package com.po4yka.runatal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.po4yka.runatal.domain.model.ResolvedQuoteRendering
import com.po4yka.runatal.domain.translation.TranslationMode

/** Names the actual output and exposes historical availability and its sources. */
@Composable
fun QuoteRenderingDetails(rendering: ResolvedQuoteRendering) {
    if (rendering.mode == TranslationMode.TRANSLATE || rendering.notes.isNotEmpty()) {
        Column {
            Text(rendering.label, style = MaterialTheme.typography.labelMedium)
            rendering.notes.distinct().forEach { note ->
                Text(note, style = MaterialTheme.typography.bodySmall)
            }
            rendering.provenance.distinctBy { it.sourceId to it.referenceId }.forEach { source ->
                Text("Source: ${source.label}", style = MaterialTheme.typography.bodySmall)
                source.url?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
