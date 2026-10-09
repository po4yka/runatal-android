package com.po4yka.runatal.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runatal.domain.model.displayName
import com.po4yka.runatal.ui.components.RunicTopBar
import com.po4yka.runatal.ui.components.RunicTopBarIconAction
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Real reading history reachable from Today and Profile. */
@Composable
internal fun ReadingHistoryScreen(
    onNavigateBack: () -> Unit,
    onShareQuote: (Long) -> Unit,
    viewModel: ReadingHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        RunicTopBar(
            navigationIcon = {
                RunicTopBarIconAction(Icons.AutoMirrored.Filled.ArrowBack, "Back", onNavigateBack)
            },
            titleContent = { Text("Reading history") }
        )
    }) { padding ->
        when (val current = state) {
            ReadingHistoryUiState.Loading -> CircularProgressIndicator(Modifier.padding(padding))
            is ReadingHistoryUiState.Error -> Text(current.message, Modifier.padding(padding))
            is ReadingHistoryUiState.Ready -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp)
            ) {
                item {
                    Text("${current.stats.streakDays}-day streak · ${current.stats.totalDays} reading days")
                    if (current.readings.isEmpty()) Text("Open a quote on Today to start your reading history.")
                }
                items(current.readings) { reading ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                            .clickable { onShareQuote(reading.quote.id) },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(reading.quote.textLatin, style = MaterialTheme.typography.bodyLarge)
                            Text("— ${reading.quote.author}")
                            Text(
                                reading.script.displayName + " · " + Instant.ofEpochMilli(reading.readAt)
                                    .atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
