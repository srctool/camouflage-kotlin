package com.srctool.camouflage.core

import kotlin.test.Test
import kotlin.test.assertEquals

class CamoThemeTest {
    @Test
    fun themesWithTheSameValuesAreEqual() {
        assertEquals(CamoTheme(placeholder = 1), CamoTheme(placeholder = 1))
    }
}
