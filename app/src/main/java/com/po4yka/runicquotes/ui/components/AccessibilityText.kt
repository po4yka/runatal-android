package com.po4yka.runicquotes.ui.components

internal fun toggleStateDescription(checked: Boolean): String = if (checked) "On" else "Off"

internal fun selectionStateDescription(selected: Boolean): String = if (selected) "Selected" else "Not selected"

internal fun buildRunicAccessibilityText(
    latinText: String,
    author: String? = null,
    scriptLabel: String? = null,
    prefix: String? = null
): String {
    val normalizedText = latinText
        .trim()
        .trim('"', '\u201C', '\u201D')

    return buildString {
        prefix
            ?.trim()
            ?.removeSuffix(".")
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                append(it)
                append(". ")
            }

        if (normalizedText.isNotEmpty()) {
            append(normalizedText)
        }

        author
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                if (isNotEmpty()) append(". ")
                append("By ")
                append(it)
            }

        scriptLabel
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                if (isNotEmpty()) append(". ")
                append("Script: ")
                append(it)
            }
    }
}
