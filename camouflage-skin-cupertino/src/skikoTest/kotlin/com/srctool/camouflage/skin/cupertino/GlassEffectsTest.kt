package com.srctool.camouflage.skin.cupertino

import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

// The glass effect is built once per node and rebuilt only when its inputs change (an idle glass material allocates
// nothing per frame), and it is absent, not an error, when refraction is off.
class GlassEffectsTest {
    private val size = Size(200f, 100f)

    @Test
    fun noRefractionMeansNoEffect() {
        assertNull(GlassEffects().effect(size, 24f, 14f, 6f, refract = false))
    }

    @Test
    fun theSameInputsGiveTheSameEffect() {
        val glass = GlassEffects()
        val first = glass.effect(size, 24f, 14f, 6f, refract = true)
        assertNotNull(first)
        assertSame(first, glass.effect(size, 24f, 14f, 6f, refract = true))
    }

    @Test
    fun anotherSizeRebuildsIt() {
        val glass = GlassEffects()
        val first = glass.effect(size, 24f, 14f, 6f, refract = true)
        assertNotNull(first)
        assertNotSame(first, glass.effect(Size(300f, 100f), 24f, 14f, 6f, refract = true))
    }
}
