package com.srctool.camouflage.sample

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.Camouflage
import com.srctool.camouflage.core.skin.IconStyle
import com.srctool.camouflage.core.skin.ProvideContentStyle
import com.srctool.camouflage.core.skin.rememberSkinIndication

/**
 * A labelled placeholder drawn through the current skin, the way a renderer will draw a component (M3):
 * the behavior modifier first, then the skin's surface treatment and feedback, then the content style.
 */
@Composable
fun Placeholder(label: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val skin = Camouflage.skin
    val theme = Camouflage.theme
    val colors = theme.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val shape = theme.shapes.medium
    Box(
        modifier
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .then(skin.surfaceTreatment.modifier(colors.primaryContainer, shape, 0.dp, theme))
            .indication(source, rememberSkinIndication(colors.onPrimaryContainer))
            .border(if (focused) 2.dp else 0.dp, if (focused) Color.Magenta else Color.Transparent, shape)
            .heightIn(min = 48.dp)
            .padding(horizontal = theme.spacing.l, vertical = theme.spacing.s),
        contentAlignment = Alignment.CenterStart,
    ) {
        ProvideContentStyle(colors.onPrimaryContainer, IconStyle(18.dp, colors.onPrimaryContainer)) {
            BasicText(
                "[${skin.name}] $label",
                style = theme.typography.labelLarge.copy(color = Camouflage.contentColor),
            )
        }
    }
}
