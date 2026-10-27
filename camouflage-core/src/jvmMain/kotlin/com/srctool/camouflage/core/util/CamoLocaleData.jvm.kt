package com.srctool.camouflage.core.util

import java.text.DecimalFormatSymbols
import java.util.Locale

internal actual object CamoLocaleData {
    actual fun decimalSeparator(locale: String): Char =
        DecimalFormatSymbols.getInstance(Locale.forLanguageTag(locale)).decimalSeparator
}
