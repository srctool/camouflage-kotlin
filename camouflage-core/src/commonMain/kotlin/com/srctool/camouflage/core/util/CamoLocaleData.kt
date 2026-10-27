package com.srctool.camouflage.core.util

import androidx.compose.ui.text.intl.Locale

/** Locale facts the platform knows and Compose doesn't expose. `locale` is a BCP 47 tag ("id-ID"). */
internal expect object CamoLocaleData {
    /** The decimal separator: '.' for en-US, ',' for id-ID. */
    fun decimalSeparator(locale: String): Char
}

/** The current locale's BCP 47 tag. */
internal fun currentLocaleTag(): String = Locale.current.toLanguageTag()
