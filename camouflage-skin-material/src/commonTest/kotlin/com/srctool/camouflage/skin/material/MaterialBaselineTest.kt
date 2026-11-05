package com.srctool.camouflage.skin.material

import com.srctool.camouflage.core.theme.CamoColors
import kotlin.test.Test
import kotlin.test.assertEquals

// The baseline writes the status families out as literals so they never shift silently. If a color-library
// update changes what `fromSeed` generates, this fails and you decide whether to update the literals.
class MaterialBaselineTest {
    @Test
    fun theStatusLiteralsStillEqualWhatFromSeedGenerates() {
        for (dark in listOf(false, true)) {
            val pinned = (if (dark) materialBaselineDark else materialBaselineLight).colors
            val generated = materialColors(MaterialBaselineSeed, dark)
            assertEquals(status(generated), status(pinned), "status colors, dark = $dark")
        }
    }

    private fun status(c: CamoColors) = listOf(
        c.success, c.onSuccess, c.successContainer, c.onSuccessContainer,
        c.warning, c.onWarning, c.warningContainer, c.onWarningContainer,
        c.info, c.onInfo, c.infoContainer, c.onInfoContainer,
    )
}
