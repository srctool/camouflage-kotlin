package com.srctool.camouflage.core.textfield

import com.srctool.camouflage.core.form.CamoRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FormatTest {
    @Test
    fun filtersAcceptOrTrim() {
        assertEquals("0812", CamoInputFilter.digitsOnly().filter("", "08a12"))
        assertEquals("081", CamoInputFilter.digitsOnly(maxLength = 3).filter("", "0812"))
        assertEquals("abc", CamoInputFilter.maxLength(3).filter("", "abcd"))
        assertEquals("ab c", CamoInputFilter.pattern(Regex("[a-z ]")).filter("", "ab1 c"))
        assertEquals("12,5", CamoInputFilter.decimal(2, ',').filter("", "12,5"))
        assertEquals("12,50", CamoInputFilter.decimal(2, ',').filter("12,50", "12,507"))
        assertEquals("1,2", CamoInputFilter.decimal(2, ',').filter("1,2", "1,2,"))
    }

    @Test
    fun aPatternInsertsItsLiteralsBetweenRawCharacters() {
        val phone = CamoDisplayFormat.pattern("####-####-#####")
        assertEquals("0812-3", phone.format("08123"))
        assertEquals("0812", phone.format("0812"))
        assertEquals("0812-3456-7890", phone.format("081234567890"))
        assertEquals(6, phone.rawToShown(5, "08123"))
        assertEquals(5, phone.shownToRaw(6, "0812-3"))
    }

    @Test
    fun groupingAndDecimalGroupTheIntegerPart() {
        assertEquals("1.250.000", CamoDisplayFormat.grouping().format("1250000"))
        assertEquals("-250", CamoDisplayFormat.grouping().format("-250"))
        assertEquals("-1.250", CamoDisplayFormat.grouping().format("-1250"))
        assertEquals("1.250.000,50", CamoDisplayFormat.decimal().format("1250000,50"))
        assertEquals("999,5", CamoDisplayFormat.decimal().format("999,5"))
    }

    @Test
    fun rulesCheckAndPassEmptyTextWhereTheySay() {
        assertNotNull(CamoRules.required("x").validate(" "))
        assertNull(CamoRules.required("x").validate("a"))
        assertNotNull(CamoRules.requiredTrue("x").validate(false))
        assertNull(CamoRules.minLength(3, "x").validate(""))
        assertNotNull(CamoRules.minLength(3, "x").validate("ab"))
        assertNotNull(CamoRules.maxLength(2, "x").validate("abc"))
        assertNotNull(CamoRules.min(18.0, "x").validate("17"))
        assertNotNull(CamoRules.max(5.0, "x").validate("5,5"))
        assertNotNull(CamoRules.min(1.0, "x").validate("abc"))
        assertNull(CamoRules.email("x").validate("a@b.co"))
        assertNotNull(CamoRules.email("x").validate("a@b"))
    }
}
