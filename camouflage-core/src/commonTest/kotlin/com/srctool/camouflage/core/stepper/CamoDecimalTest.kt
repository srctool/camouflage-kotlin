package com.srctool.camouflage.core.stepper

import com.srctool.camouflage.core.slider.snap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CamoDecimalTest {
    @Test
    fun parsesEitherSeparatorAndKeepsTheScale() {
        assertEquals(CamoDecimal(1250, 2), CamoDecimal("12.50"))
        assertEquals(CamoDecimal(25, 2), CamoDecimal("0,25"))
        assertEquals(CamoDecimal(-3, 0), CamoDecimal("-3"))
        assertEquals(CamoDecimal(5, 1), CamoDecimal(".5"))
        assertNull(CamoDecimal.parseOrNull("1.2.3"))
        assertNull(CamoDecimal.parseOrNull("abc"))
        assertNull(CamoDecimal.parseOrNull(""))
        assertFailsWith<IllegalArgumentException> { CamoDecimal(1, -1) }
    }

    @Test
    fun stepsNeverDrift() {
        val tenth = CamoDecimal("0.1")
        assertEquals(0, (tenth + tenth + tenth).compareTo(CamoDecimal("0.3")))
        assertEquals("0.3", (tenth * 3).format())
        assertEquals("1.75", (CamoDecimal("2") - CamoDecimal("0.25")).format())
    }

    @Test
    fun comparesByValueButEqualsByRepresentation() {
        assertEquals(0, CamoDecimal("1.0").compareTo(CamoDecimal("1.00")))
        assertTrue(CamoDecimal("1.0") != CamoDecimal("1.00"))
        assertTrue(CamoDecimal("-0.5") < CamoDecimal.Zero)
    }

    @Test
    fun formatsWithTheSeparatorAndRoundsHalfAwayFromZero() {
        assertEquals("12,50", CamoDecimal("12.5").withScale(2).format(','))
        assertEquals("0.05", CamoDecimal("0.05").format())
        assertEquals("-0.05", CamoDecimal("-0.05").format())
        assertEquals("1.3", CamoDecimal("1.25").withScale(1).format())
        assertEquals("-1.3", CamoDecimal("-1.25").withScale(1).format())
    }

    @Test
    fun snapsToTheNearestStepFromTheStart() {
        assertEquals("1.5", snapToStep(CamoDecimal("1.6"), CamoDecimal("0.5"), CamoDecimal("0.5")).format())
        assertEquals(0, snapToStep(CamoDecimal("7"), CamoDecimal("1"), CamoDecimal("5")).compareTo(CamoDecimal("6")))
    }

    @Test
    fun sliderSnapsToItsStops() {
        assertEquals(0.5f, snap(0.45f, steps = 3))
        assertEquals(0.25f, snap(0.3f, steps = 3))
        assertEquals(0.3f, snap(0.3f, steps = 0))
    }
}
