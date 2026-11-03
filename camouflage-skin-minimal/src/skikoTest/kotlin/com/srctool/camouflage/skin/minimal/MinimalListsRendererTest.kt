package com.srctool.camouflage.skin.minimal

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.datatable.CamoColumn
import com.srctool.camouflage.core.datatable.CamoDataTable
import com.srctool.camouflage.core.datatable.TableTheme
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.core.tree.CamoTree
import com.srctool.camouflage.core.tree.CamoTreeNode
import com.srctool.camouflage.core.tree.TreeTheme
import com.srctool.camouflage.skin.testing.ProvideComponentThemes
import com.srctool.camouflage.skin.testing.assertNoLiterals
import kotlin.math.roundToInt
import kotlin.test.Test

// The M10 data table and tree renderers against the contract: theme fields honored.
@OptIn(ExperimentalTestApi::class)
class MinimalListsRendererTest {
    private val skin = MinimalSkin
    private val theme = minimalLight

    @Test
    fun aTableRowHonorsItsHeight() = runComposeUiTest {
        assertNoLiterals(skin, theme, TableTheme(headerHeight = 53.dp, rowHeight = 67.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(dataTable = it)) {
                CamoDataTable(
                    listOf(CamoColumn<String>("a", "A", cell = { BasicText(it) })),
                    listOf("one"), { it }, "Table", Modifier.height(400.dp),
                )
            }
        }, measure = { it.rowHeight() == px(67) })
    }

    @Test
    fun aTreeRowHonorsItsHeight() = runComposeUiTest {
        assertNoLiterals(skin, theme, TreeTheme(rowHeight = 59.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(tree = it)) {
                CamoTree(
                    listOf(CamoTreeNode("a", "A")), emptySet(), {}, emptySet(), {}, { it }, "Tree",
                    Modifier.height(200.dp),
                )
            }
        }, measure = { it.rowHeight() == px(59) })
    }

    // The height of the first row: the node that carries the collection item info.
    private fun SemanticsNode.rowHeight(): Int? = if (config.contains(SemanticsProperties.CollectionItemInfo)) {
        size.height
    } else {
        children.firstNotNullOfOrNull { it.rowHeight() }
    }

    private fun ComposeUiTest.px(dp: Int) = (dp * density.density).roundToInt()
}
