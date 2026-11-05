package com.srctool.camouflage.skin.cupertino

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CupertinoSkinTest {
    @Test
    fun theSkinIsNamedAndUsesOpacityFeedback() {
        assertEquals("cupertino", CupertinoSkin.name)
        assertSame(Opacity, CupertinoSkin.feedback)
    }

    @Test
    fun theSurfaceOptionPicksTheTreatment() {
        assertSame(Classic, cupertinoSkin().surfaceTreatment)
        assertSame(Classic, cupertinoSkin(CupertinoSurface.Classic).surfaceTreatment)
        assertSame(LiquidGlass, cupertinoSkin(CupertinoSurface.LiquidGlass).surfaceTreatment)
    }
}
