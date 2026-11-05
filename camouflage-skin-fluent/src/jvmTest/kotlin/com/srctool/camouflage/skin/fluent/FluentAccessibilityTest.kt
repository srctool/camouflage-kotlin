package com.srctool.camouflage.skin.fluent

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.srctool.camouflage.skin.testing.ActionsGallery
import com.srctool.camouflage.skin.testing.ButtonGallery
import com.srctool.camouflage.skin.testing.CollectionsGallery
import com.srctool.camouflage.skin.testing.DataDisplayGallery
import com.srctool.camouflage.skin.testing.IconButtonGallery
import com.srctool.camouflage.skin.testing.InputGallery
import com.srctool.camouflage.skin.testing.ListsGallery
import com.srctool.camouflage.skin.testing.NavigationChromeGallery
import com.srctool.camouflage.skin.testing.NavigationGallery
import com.srctool.camouflage.skin.testing.SmallDataGallery
import com.srctool.camouflage.skin.testing.StructureGallery
import com.srctool.camouflage.skin.testing.TextInputGallery
import com.srctool.camouflage.skin.testing.accessibilityProblems
import kotlin.test.Test
import kotlin.test.assertEquals

// JVM only: twelve galleries four times over are too much for the wasm browser runner, where they slowed the
// other tests into timeouts. Every interactive element in the galleries has a name, a role or state, and a
// 44 dp touch target (Testing §3.4), in both themes, at 200% text and right to left.
@OptIn(ExperimentalTestApi::class)
class FluentAccessibilityTest {
    private val galleries: List<Pair<String, @Composable () -> Unit>> = listOf(
        "buttons" to { ButtonGallery() },
        "icon buttons" to { IconButtonGallery() },
        "inputs" to { InputGallery() },
        "text inputs" to { TextInputGallery() },
        "structure" to { StructureGallery() },
        "data display" to { DataDisplayGallery() },
        "navigation" to { NavigationGallery() },
        "navigation chrome" to { NavigationChromeGallery() },
        "actions" to { ActionsGallery() },
        "small data" to { SmallDataGallery() },
        "lists" to { ListsGallery() },
        "collections" to { CollectionsGallery() },
    )

    private fun check(dark: Boolean, fontScale: Float, rtl: Boolean) {
        val problems = galleries.flatMap { (name, content) ->
            val found = mutableListOf<String>()
            runComposeUiTest {
                val theme = if (dark) fluentTheme(dark = true) else fluentTheme()
                found += accessibilityProblems(FluentSkin, theme, fontScale, rtl, content)
            }
            found.map { "$name: $it" }
        }
        assertEquals(emptyList(), problems)
    }

    @Test fun light() = check(dark = false, fontScale = 1f, rtl = false)

    @Test fun dark() = check(dark = true, fontScale = 1f, rtl = false)

    @Test fun at200PercentText() = check(dark = false, fontScale = 2f, rtl = false)

    @Test fun rightToLeft() = check(dark = false, fontScale = 1f, rtl = true)
}
