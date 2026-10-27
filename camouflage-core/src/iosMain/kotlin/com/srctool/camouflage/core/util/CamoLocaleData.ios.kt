package com.srctool.camouflage.core.util

import platform.Foundation.NSLocale
import platform.Foundation.NSNumberFormatter

internal actual object CamoLocaleData {
    actual fun decimalSeparator(locale: String): Char {
        val formatter = NSNumberFormatter().apply { this.locale = NSLocale(localeIdentifier = locale) }
        return formatter.decimalSeparator.firstOrNull() ?: '.'
    }
}
