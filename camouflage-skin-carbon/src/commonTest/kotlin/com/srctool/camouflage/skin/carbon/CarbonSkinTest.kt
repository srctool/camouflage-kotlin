package com.srctool.camouflage.skin.carbon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CarbonSkinTest {
    @Test
    fun theSkinIsNamedAndUsesLayeredSurfacesAndCarbonStates() {
        assertEquals("carbon", CarbonSkin.name)
        assertSame(Layered, CarbonSkin.surfaceTreatment)
        assertSame(CarbonStates, CarbonSkin.feedback)
    }
}
