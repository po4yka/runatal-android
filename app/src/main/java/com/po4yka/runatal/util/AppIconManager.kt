package com.po4yka.runatal.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Applies the selected launcher icon by toggling launcher aliases.
 */
object AppIconManager {

    /**
     * Enables the selected launcher alias and disables the remaining variants.
     */
    fun apply(
        context: Context,
        variant: AppIconVariant
    ) {
        val packageManager = context.packageManager

        AppIconVariant.entries.forEach { candidate ->
            val componentName = ComponentName(context, candidate.aliasClassName)
            val desiredState = if (candidate == variant) {
                candidate.enabledState
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            if (packageManager.getComponentEnabledSetting(componentName) == desiredState) {
                return@forEach
            }

            runCatching {
                packageManager.setComponentEnabledSetting(
                    componentName,
                    desiredState,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}
