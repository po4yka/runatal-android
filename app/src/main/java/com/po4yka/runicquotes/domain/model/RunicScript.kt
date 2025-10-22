package com.po4yka.runicquotes.domain.model

/**
 * Represents the different runic scripts supported by the app.
 */
enum class RunicScript {
    /**
     * Elder Futhark - The oldest runic alphabet, used from ~150-800 AD
     */
    ELDER_FUTHARK,

    /**
     * Younger Futhark - Used during the Viking Age, ~800-1100 AD
     */
    YOUNGER_FUTHARK,

    /**
     * Cirth (Angerthas) - Tolkien's fictional runic script from Middle-earth
     */
    CIRTH;

    companion object {
        /**
         * Default script to use when none is specified.
         */
        val DEFAULT = ELDER_FUTHARK
    }
}
