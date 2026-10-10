package com.srctool.camouflage.skin.minimal

import kotlin.test.Test
import kotlin.test.assertEquals

class MinimalThemeTest {
    @Test
    fun minimalThemeIsACoreTheme() {
        assertEquals(0, minimalTheme.placeholder)
    }
}
