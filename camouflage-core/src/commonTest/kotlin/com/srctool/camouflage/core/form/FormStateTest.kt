package com.srctool.camouflage.core.form

import androidx.compose.runtime.saveable.SaverScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormStateTest {
    @Test
    fun theSaverKeepsValuesErrorsAndInteraction() {
        val form = CamoFormState()
        form.declare("email", "", listOf(CamoRules.email("Enter an email")))
        form.entry("email", "").apply {
            value = "not"
            hasInteracted = true
        }
        form.validate()
        val saved = with(CamoFormState.Saver) { SaverScope { true }.save(form) }!!
        val restored = CamoFormState.Saver.restore(saved)!!
        val entry = restored.entry("email", "")
        assertEquals("not", entry.value)
        assertEquals("Enter an email", entry.error)
        assertTrue(entry.hasInteracted)
    }

    @Test
    fun dirtyComparesWithTheInitialValue() {
        val form = CamoFormState()
        form.entry("name", "Ann").value = "Bob"
        assertTrue(form.isDirty && form.isDirty("name"))
        form.entry("name", "Ann").value = "Ann"
        assertFalse(form.isDirty)
    }
}
