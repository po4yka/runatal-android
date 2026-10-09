package com.po4yka.runatal.util

/** Limits an existing UTF-16 unit budget without splitting a paired supplementary character at the boundary. */
internal fun String.takeUtf16Budget(maxUnits: Int): String {
    require(maxUnits >= 0)
    if (length <= maxUnits) return this
    val end = if (maxUnits > 0 && Character.isHighSurrogate(this[maxUnits - 1]) &&
        Character.isLowSurrogate(this[maxUnits])) {
        maxUnits - 1
    } else {
        maxUnits
    }
    return substring(0, end)
}
