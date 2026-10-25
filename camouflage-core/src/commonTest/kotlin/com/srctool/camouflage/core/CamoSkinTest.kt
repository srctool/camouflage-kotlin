package com.srctool.camouflage.core

import androidx.compose.ui.graphics.Color
import com.srctool.camouflage.core.provider.CamouflageMissingProviderException
import com.srctool.camouflage.core.skin.InteractionFeedback
import com.srctool.camouflage.core.util.disabled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class CamoSkinTest {
    @Test
    fun copyKeepsTheOtherFieldsIdentical() {
        val copy = skinA.copy(feedback = object : InteractionFeedback by NoFeedback {})
        assertSame(skinA.surfaceTreatment, copy.surfaceTreatment)
        assertSame(skinA.name, copy.name)
        assertNotSame(skinA.feedback, copy.feedback)
        // A skin that leaves out a field doesn't compile: CamoSkin has no defaults (Skin Contract §3.3).
    }

    @Test
    fun disabledScalesTheAlpha() {
        assertEquals(0.38f, Color.Black.disabled(0.38f).alpha, 0.01f)
        assertEquals(0.19f, Color.Black.copy(alpha = 0.5f).disabled(0.38f).alpha, 0.01f)
    }

    @Test
    fun theErrorNamesTheComponentAndTheMissingHalf() {
        assertEquals(
            "No CamouflageProvider found above CamoButton: missing skin. " +
                "Wrap your app in CamouflageProvider(skin = …, theme = …).",
            CamouflageMissingProviderException("CamoButton", "skin").message,
        )
    }
}
