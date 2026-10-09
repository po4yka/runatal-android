package com.po4yka.runatal.ui.widget

import kotlinx.coroutines.sync.Mutex

/** Serializes quote loading with cache invalidation after committed library changes. */
internal object WidgetContentLock {
    val mutex = Mutex()
}
