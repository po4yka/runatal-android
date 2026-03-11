package com.po4yka.runicquotes.ui.screens.about

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.po4yka.runicquotes.BuildConfig
import com.po4yka.runicquotes.ui.components.RunicArticleCard
import com.po4yka.runicquotes.ui.components.RunicArticleDivider
import com.po4yka.runicquotes.ui.components.RunicArticleLinkCard
import com.po4yka.runicquotes.ui.components.RunicBadge
import com.po4yka.runicquotes.ui.components.RunicBadgeRow
import com.po4yka.runicquotes.ui.components.RunicGlyphBadge
import com.po4yka.runicquotes.ui.components.RunicTopBar
import com.po4yka.runicquotes.ui.components.RunicTopBarActionStyle
import com.po4yka.runicquotes.ui.components.RunicTopBarIconAction
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val versionName = remember { BuildConfig.VERSION_NAME }
    val versionCode = remember { BuildConfig.VERSION_CODE }

    Scaffold(
        topBar = {
            AboutTopBar(onNavigateBack = onNavigateBack)
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AboutHeader(
                versionName = versionName,
                versionCode = versionCode
            )

            AboutSectionHeader(text = "Links")

            AboutLinkCard(
                title = "Source Code",
                subtitle = "github.com/runatal",
                leadingIcon = Icons.Filled.Code,
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/runatal"))
                    )
                }
            )

            AboutLinkCard(
                title = "Privacy Policy",
                subtitle = "runatal.app/privacy",
                leadingIcon = Icons.Filled.Description,
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://runatal.app/privacy")
                        )
                    )
                }
            )

            AboutLinkCard(
                title = "Acknowledgments",
                subtitle = "Contributors & translators",
                leadingIcon = Icons.Filled.FavoriteBorder,
                onClick = null
            )

            AboutSectionHeader(
                text = "Open source licenses",
                meta = "${LICENSES.size} packages"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LICENSES.forEach { license ->
                    LicenseItem(license)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AboutTopBar(onNavigateBack: () -> Unit) {
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
                text = "About",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    )
}

@Composable
private fun AboutHeader(
    versionName: String,
    versionCode: Int
) {
    RunicArticleCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        contentGap = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RunicGlyphBadge(
                size = RunicExpressiveTheme.controls.aboutBadge,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = "\u16B1",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Runatal",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                RunicBadgeRow {
                    RunicBadge(text = "v$versionName")
                    RunicBadge(
                        text = "build $versionCode",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        RunicArticleDivider()

        Text(
            text = "Transliterates quotes into ancient runic scripts. Built for readers drawn to " +
                "Norse mythology, rune history, and expressive lettering.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AboutLinkCard(
    title: String,
    subtitle: String,
    leadingIcon: ImageVector,
    onClick: (() -> Unit)?
) {
    if (onClick == null) {
        RunicArticleCard(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            contentGap = 0.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RunicGlyphBadge(
                    size = RunicExpressiveTheme.controls.leadingBadgeMedium,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    } else {
        RunicArticleLinkCard(
            title = title,
            description = subtitle,
            onClick = onClick,
            titleColor = MaterialTheme.colorScheme.onSurface,
            leadingIcon = leadingIcon,
            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew
        )
    }
}

@Composable
private fun AboutSectionHeader(text: String, meta: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (meta != null) {
            RunicBadge(text = meta)
        }
    }
}

@Composable
private fun LicenseItem(license: LicenseEntry) {
    RunicArticleCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        contentGap = 8.dp
    ) {
        Text(
            text = license.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        RunicBadgeRow {
            RunicBadge(text = license.version)
            RunicBadge(
                text = license.license,
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Text(
            text = license.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class LicenseEntry(
    val name: String,
    val version: String,
    val description: String,
    val license: String
)

private val LICENSES = listOf(
    LicenseEntry(
        name = "Material Design 3",
        version = "1.2.0",
        description = "Google's design system for Android",
        license = "Apache-2.0"
    ),
    LicenseEntry(
        name = "Noto Sans Runic",
        version = "2.003",
        description = "Unicode runic glyph font by Google Fonts",
        license = "OFL-1.1"
    ),
    LicenseEntry(
        name = "Kotlin Coroutines",
        version = "1.7.3",
        description = "Asynchronous programming framework",
        license = "Apache-2.0"
    ),
    LicenseEntry(
        name = "Jetpack Compose",
        version = "1.6.0",
        description = "Modern Android UI toolkit",
        license = "Apache-2.0"
    ),
    LicenseEntry(
        name = "Room Database",
        version = "2.6.1",
        description = "SQLite object mapping library",
        license = "Apache-2.0"
    ),
    LicenseEntry(
        name = "DataStore",
        version = "1.0.0",
        description = "Preferences and proto storage",
        license = "Apache-2.0"
    ),
    LicenseEntry(
        name = "Hilt",
        version = "2.50",
        description = "Dependency injection for Android",
        license = "Apache-2.0"
    )
)
