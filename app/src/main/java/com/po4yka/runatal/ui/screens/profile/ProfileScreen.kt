package com.po4yka.runatal.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runatal.ui.components.RunicArticleCard
import com.po4yka.runatal.ui.components.RunicArticleLinkCard
import com.po4yka.runatal.ui.components.RunicBadge
import com.po4yka.runatal.ui.components.RunicBadgeRow
import com.po4yka.runatal.ui.components.RunicGlyphBadge
import com.po4yka.runatal.ui.components.RunicInfoCard
import com.po4yka.runatal.ui.components.RunicTopBar
import com.po4yka.runatal.ui.components.RunicTopBarActionStyle
import com.po4yka.runatal.ui.components.RunicTopBarIconAction
import com.po4yka.runatal.ui.theme.RunicExpressiveTheme

/**
 * Profile screen displaying user statistics and menu items.
 */
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ProfileTopBar(onNavigateBack = onNavigateBack)
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileHeroCard(streakDays = state.streakDays)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    value = state.totalQuotes,
                    label = "Quotes",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.Favorite,
                    value = state.favoriteCount,
                    label = "Favorites",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.Folder,
                    value = state.createdCount,
                    label = "Created",
                    modifier = Modifier.weight(1f)
                )
            }

            ProfileSectionHeader("Account")

            ProfileMenuItem(
                icon = Icons.Default.Share,
                title = "Share Profile",
                subtitle = "Invite friends to Runatal",
                onClick = {}
            )
            ProfileMenuItem(
                icon = Icons.Default.History,
                title = "Streak History",
                subtitle = "View your daily activity",
                onClick = {}
            )
            ProfileMenuItem(
                icon = Icons.Default.BookmarkBorder,
                title = "Saved Runes",
                subtitle = "Your bookmarked runes",
                onClick = {}
            )
        }
    }
}

@Composable
private fun ProfileTopBar(onNavigateBack: () -> Unit) {
    RunicTopBar(
        navigationIcon = {
            RunicTopBarIconAction(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onNavigateBack,
                style = RunicTopBarActionStyle.Tonal
            )
        },
        titleContent = {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    )
}

@Composable
private fun ProfileHeroCard(streakDays: Int) {
    RunicArticleCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        contentGap = 14.dp,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RunicGlyphBadge(
            size = 96.dp,
            shape = RunicExpressiveTheme.shapes.heroCard,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile avatar",
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "Rune Seeker",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        RunicBadgeRow {
            RunicBadge(text = "Member since February 2026")
            if (streakDays > 0) {
                RunicBadge(
                    text = "$streakDays-day streak",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        if (streakDays > 0) {
            RunicBadgeRow {
                RunicGlyphBadge(
                    size = RunicExpressiveTheme.controls.leadingBadgeMedium,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Text(
                    text = "Consistency matters more than volume. Keep the runes moving.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    RunicInfoCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RunicGlyphBadge(
                size = RunicExpressiveTheme.controls.leadingBadgeMedium,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    RunicArticleLinkCard(
        title = title,
        description = subtitle,
        onClick = onClick,
        titleColor = MaterialTheme.colorScheme.onSurface,
        leadingIcon = icon,
        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight
    )
}

@Composable
private fun ProfileSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
    )
}
