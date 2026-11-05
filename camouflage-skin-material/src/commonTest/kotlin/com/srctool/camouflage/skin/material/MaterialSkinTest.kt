package com.srctool.camouflage.skin.material

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class MaterialSkinTest {
    @Test
    fun theSkinIsNamedAndUsesPaperAndRipple() {
        assertEquals("material", MaterialSkin.name)
        assertSame(Paper, MaterialSkin.surfaceTreatment)
        assertSame(Ripple, MaterialSkin.feedback)
    }

    @Test
    fun expressiveProgressChangesOnlyTheBar() {
        val wavy = materialSkin(expressiveProgress = true)
        assertNotEquals(MaterialSkin.progressBar, wavy.progressBar)
        assertEquals(MaterialSkin.copy(progressBar = wavy.progressBar), wavy)
    }
}
