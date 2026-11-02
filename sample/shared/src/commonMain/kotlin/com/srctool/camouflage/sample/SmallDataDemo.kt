package com.srctool.camouflage.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.breadcrumb.CamoBreadcrumb
import com.srctool.camouflage.core.breadcrumb.CamoBreadcrumbItem
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.image.CamoImage
import com.srctool.camouflage.core.image.ImageSource
import com.srctool.camouflage.core.pagination.CamoPagination
import com.srctool.camouflage.core.progresssteps.CamoProgressSteps
import com.srctool.camouflage.core.progresssteps.CamoStep
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.rating.CamoRating
import com.srctool.camouflage.core.rating.RatingStep
import com.srctool.camouflage.core.text.CamoText

// The M10 small data components together: a rating that changes, pages, a path and a wizard's steps.

private val Thumb = 64.dp
private val Blue = Color(0xFF6D8FB5)
private const val PAGES = 12
private val Path = listOf("Home", "Projects", "Camouflage", "Design", "Notes")
private val Wizard = listOf("Account", "Shipping", "Payment", "Review")

@Composable
internal fun SmallDataDemo() {
    var rating by remember { mutableFloatStateOf(3.5f) }
    var page by remember { mutableIntStateOf(0) }
    var depth by remember { mutableIntStateOf(Path.size - 1) }
    var step by remember { mutableIntStateOf(1) }
    Column(verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.m)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CamoImage(ImageSource.Loaded(ColorPainter(Blue)), "A blue square", Modifier.size(Thumb))
            CamoImage(ImageSource.Failed(), "A picture that failed to load", Modifier.size(Thumb))
            Column {
                CamoRating(rating, "Quality", onValueChange = { rating = it }, step = RatingStep.Half)
                CamoText("Quality: $rating of 5")
            }
        }
        CamoBreadcrumb(Path.take(depth + 1).mapIndexed { i, name -> CamoBreadcrumbItem(name, name,
            onClick = { depth = i }) })
        CamoPagination(PAGES, page, { page = it })
        CamoText("Page ${page + 1} of $PAGES")
        CamoProgressSteps(
            Wizard.mapIndexed { i, name -> CamoStep(name, name, onClick = if (i < step) ({ step = i }) else null) },
            current = step,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.s)) {
            CamoButton("Back", { step = (step - 1).coerceAtLeast(0) }, enabled = step > 0)
            CamoButton("Next", { step = (step + 1).coerceAtMost(Wizard.lastIndex) }, enabled = step < Wizard.lastIndex)
        }
    }
}
