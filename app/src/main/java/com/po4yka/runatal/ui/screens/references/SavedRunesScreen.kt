package com.po4yka.runatal.ui.screens.references

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
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.ui.components.RunicText
import com.po4yka.runatal.ui.components.RunicTopBar
import com.po4yka.runatal.ui.components.RunicTopBarIconAction

/** Bookmarked runes open their authoritative reference detail. */
@Composable
internal fun SavedRunesScreen(
    onNavigateBack: () -> Unit,
    onSelectRune: (Long) -> Unit,
    viewModel: SavedRunesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        RunicTopBar(
            navigationIcon = { RunicTopBarIconAction(Icons.AutoMirrored.Filled.ArrowBack, "Back", onNavigateBack) },
            titleContent = { Text("Saved runes") }
        )
    }) { padding ->
        when (val current = state) {
            SavedRunesUiState.Loading -> CircularProgressIndicator(Modifier.padding(padding))
            is SavedRunesUiState.Error -> Text(current.message, Modifier.padding(padding))
            is SavedRunesUiState.Ready -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp)
            ) {
                if (current.runes.isEmpty()) item { Text("Save a rune from its reference page to find it here.") }
                items(current.runes, key = { it.id }) { rune ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                            .clickable { onSelectRune(rune.id) },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            RunicText(rune.character, script = when (rune.script) {
                                "younger_futhark" -> RunicScript.YOUNGER_FUTHARK
                                "cirth" -> RunicScript.CIRTH
                                else -> RunicScript.ELDER_FUTHARK
                            })
                            Text(rune.name, style = MaterialTheme.typography.titleMedium)
                            Text(rune.meaning)
                        }
                    }
                }
            }
        }
    }
}
