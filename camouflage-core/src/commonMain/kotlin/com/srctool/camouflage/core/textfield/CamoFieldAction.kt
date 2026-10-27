package com.srctool.camouflage.core.textfield

import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.util.rememberSkinIndication

private const val DISABLED = 0.38f

/**
 * An icon action inside a field (clear, show password, open a picker). Unlike a `CamoIconButton` it has no
 * container of its own: it takes the field's icon size and color (the ambient icon style the field's renderer
 * sets for its icons) and a small square around the icon, so a field with an action is as tall as one without.
 * It's its own focusable button, after the input, named by [contentDescription].
 */
@Composable
public fun CamoFieldAction(
    icon: @Composable () -> Unit,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val style = Camouflage.iconStyle
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .semantics { this.contentDescription = contentDescription }
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .indication(source, rememberSkinIndication(style.color, CircleShape))
            .size(style.size + Camouflage.spacing.s)
            .alpha(if (enabled) 1f else DISABLED),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.clearAndSetSemantics {}) { icon() }
    }
}
