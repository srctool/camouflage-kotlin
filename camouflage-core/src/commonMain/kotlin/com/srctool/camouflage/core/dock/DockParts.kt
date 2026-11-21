package com.srctool.camouflage.core.dock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.menu.CamoMenuAnchor
import com.srctool.camouflage.core.menu.CamoMenuEntry
import com.srctool.camouflage.core.menu.CamoMenuItem
import com.srctool.camouflage.core.menu.CamoMenuSubmenu
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.surface.CamoSurface
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.SurfaceLevel

private val StripeButtonPadding = 4.dp
private val NarrowHeader = 150.dp

// The strip of icon buttons along an edge: one toggle per tool window of that edge.
@Composable
internal fun Stripe(
    edge: DockEdge,
    windows: List<CamoToolWindow>,
    state: CamoDockState,
    size: Dp,
    strings: DockStrings,
) {
    val vertical = edge != DockEdge.Bottom
    val label = Modifier.semantics { contentDescription = strings.stripe(edge) }
    CamoSurface(
        level = SurfaceLevel.Low,
        modifier = if (vertical) Modifier.fillMaxHeight().size(width = size, height = Dp.Unspecified) else Modifier,
    ) {
        val buttons: @Composable () -> Unit = {
            windows.forEach { window ->
                CamoIconButton(
                    icon = window.icon,
                    contentDescription = window.title,
                    onClick = { state.toggle(window.id) },
                    appearance = Appearance.Ghost,
                    checked = state.isOpen(window.id),
                    tooltip = window.title,
                )
            }
        }
        if (vertical) {
            Column(
                label.padding(StripeButtonPadding),
                verticalArrangement = Arrangement.spacedBy(StripeButtonPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { buttons() }
        } else {
            Row(
                label.fillMaxWidth().padding(StripeButtonPadding),
                horizontalArrangement = Arrangement.spacedBy(StripeButtonPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) { buttons() }
        }
    }
}

// The title row of a tool window: its title, the "Move to" menu, maximize or restore, and hide. A narrow dock keeps
// the title and one menu; maximize and hide move into it.
@Composable
internal fun ToolWindowHeader(
    window: CamoToolWindow,
    edge: DockEdge,
    state: CamoDockState,
    strings: DockStrings,
) {
    val maximized = state.maximized == window.id
    val toggle = if (maximized) strings.restore else strings.maximize
    val toggleMaximize = { if (maximized) state.restore() else state.maximize(window.id) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val narrow = maxWidth < NarrowHeader
        Row(
            Modifier.fillMaxWidth().padding(start = Camouflage.spacing.m, end = Camouflage.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CamoText(
                window.title,
                Modifier.weight(1f),
                style = Camouflage.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val moves = DockEdge.entries.filter { it != edge }.map { target ->
                CamoMenuItem(strings.edge(target), onClick = { state.move(window.id, target) })
            }
            val entries = buildList<CamoMenuEntry> {
                add(CamoMenuSubmenu(strings.moveTo, moves))
                if (narrow) {
                    add(CamoMenuItem(toggle, onClick = toggleMaximize))
                    add(CamoMenuItem(strings.hide, onClick = { state.close(window.id) }))
                }
            }
            CamoMenuAnchor(items = entries) { open -> HeaderButton(Glyphs.more, strings.more, open) }
            if (!narrow) {
                HeaderButton(if (maximized) Glyphs.restore else Glyphs.maximize, toggle, toggleMaximize)
                HeaderButton(Glyphs.hide, strings.hide) { state.close(window.id) }
            }
        }
    }
}

@Composable
private fun HeaderButton(glyph: IconSource, description: String, onClick: () -> Unit) {
    CamoIconButton(
        icon = { CamoIcon(glyph, null) },
        contentDescription = description,
        onClick = onClick,
        appearance = Appearance.Ghost,
    )
}

// The header's own glyphs, so a skin without them still draws a complete header.
private const val DOT = 0.01f
private const val GLYPH_SIZE = 24f

private object Glyphs {
    val hide = glyph("hide") {
        moveTo(6f, 12f)
        horizontalLineTo(18f)
    }
    val maximize = glyph("maximize") {
        moveTo(6f, 6f)
        horizontalLineTo(18f)
        verticalLineTo(18f)
        horizontalLineTo(6f)
        close()
    }
    val restore = glyph("restore") {
        moveTo(9f, 9f)
        horizontalLineTo(18f)
        verticalLineTo(18f)
        horizontalLineTo(9f)
        close()
        moveTo(6f, 15f)
        verticalLineTo(6f)
        horizontalLineTo(15f)
    }
    val more = glyph("more", round = true) {
        for (y in listOf(6f, 12f, 18f)) {
            moveTo(12f, y)
            horizontalLineToRelative(DOT)
        }
    }

    private fun glyph(
        name: String,
        round: Boolean = false,
        draw: PathBuilder.() -> Unit,
    ): IconSource = IconSource.Vector(
        ImageVector.Builder(name, GLYPH_SIZE.dp, GLYPH_SIZE.dp, GLYPH_SIZE, GLYPH_SIZE).path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = if (round) 2.5f else 1.6f,
            strokeLineCap = if (round) StrokeCap.Round else StrokeCap.Butt,
            pathBuilder = draw,
        ).build(),
    )
}
