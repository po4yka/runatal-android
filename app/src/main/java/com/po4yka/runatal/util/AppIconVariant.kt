package com.po4yka.runatal.util

import android.content.pm.PackageManager
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.po4yka.runatal.R

/**
 * Launcher icon variants aligned to the Figma icon palette.
 */
@Suppress("LongParameterList")
enum class AppIconVariant(
    val persistedValue: String,
    val title: String,
    val subtitle: String,
    val aliasClassName: String,
    @param:DrawableRes val foregroundDrawableRes: Int,
    @param:ColorRes val backgroundColorRes: Int,
    val enabledState: Int
) {
    STORM_SLATE(
        persistedValue = "storm_slate",
        title = "Storm Slate",
        subtitle = "Default graphite icon from the main Figma board.",
        aliasClassName = "com.po4yka.runatal.launcher.StormSlateAlias",
        foregroundDrawableRes = R.drawable.ic_launcher_foreground_storm,
        backgroundColorRes = R.color.ic_launcher_bg_storm,
        enabledState = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
    ),
    DARK(
        persistedValue = "dark",
        title = "Dark",
        subtitle = "Deeper night surface with the high-contrast rune mark.",
        aliasClassName = "com.po4yka.runatal.launcher.DarkAlias",
        foregroundDrawableRes = R.drawable.ic_launcher_foreground_dark,
        backgroundColorRes = R.color.ic_launcher_bg_dark,
        enabledState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    ),
    FJORD_BLUE(
        persistedValue = "fjord_blue",
        title = "Fjord Blue",
        subtitle = "Cool blue-toned icon variant from the themed palette.",
        aliasClassName = "com.po4yka.runatal.launcher.FjordBlueAlias",
        foregroundDrawableRes = R.drawable.ic_launcher_foreground_fjord,
        backgroundColorRes = R.color.ic_launcher_bg_fjord,
        enabledState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    ),
    PINE_GREEN(
        persistedValue = "pine_green",
        title = "Pine Green",
        subtitle = "Muted evergreen icon variant from the themed palette.",
        aliasClassName = "com.po4yka.runatal.launcher.PineGreenAlias",
        foregroundDrawableRes = R.drawable.ic_launcher_foreground_pine,
        backgroundColorRes = R.color.ic_launcher_bg_pine,
        enabledState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    ),
    WARM_STONE(
        persistedValue = "warm_stone",
        title = "Warm Stone",
        subtitle = "Warm mineral variant with softer neutral contrast.",
        aliasClassName = "com.po4yka.runatal.launcher.WarmStoneAlias",
        foregroundDrawableRes = R.drawable.ic_launcher_foreground_warm,
        backgroundColorRes = R.color.ic_launcher_bg_warm,
        enabledState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    );

    /**
     * Maps stored preference values back to the supported icon variants.
     */
    companion object {
        /**
         * Returns the matching icon variant or the default Storm Slate icon.
         */
        fun fromPersistedValue(value: String): AppIconVariant {
            return entries.firstOrNull { it.persistedValue == value } ?: STORM_SLATE
        }
    }
}
