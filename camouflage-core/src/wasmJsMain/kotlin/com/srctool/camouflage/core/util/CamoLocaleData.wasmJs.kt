@file:Suppress("MatchingDeclarationName") // the actual object plus its JavaScript helper

package com.srctool.camouflage.core.util

internal actual object CamoLocaleData {
    actual fun decimalSeparator(locale: String): Char = jsDecimalSeparator(locale).firstOrNull() ?: '.'
}

// Intl.NumberFormat's decimal part for 1.5 in `locale`; `locale` is read by the JavaScript.
@Suppress("UnusedParameter")
private fun jsDecimalSeparator(locale: String): String = js(
    "(new Intl.NumberFormat(locale).formatToParts(1.5)" +
        ".find(function (p) { return p.type === 'decimal' }) || { value: '.' }).value",
)
