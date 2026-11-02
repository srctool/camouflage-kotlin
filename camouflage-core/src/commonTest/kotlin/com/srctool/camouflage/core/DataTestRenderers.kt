package com.srctool.camouflage.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.breadcrumb.BreadcrumbColors
import com.srctool.camouflage.core.breadcrumb.BreadcrumbItemState
import com.srctool.camouflage.core.breadcrumb.BreadcrumbRenderer
import com.srctool.camouflage.core.breadcrumb.BreadcrumbSlots
import com.srctool.camouflage.core.breadcrumb.BreadcrumbState
import com.srctool.camouflage.core.breadcrumb.BreadcrumbStyle
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.image.ImageColors
import com.srctool.camouflage.core.image.ImageRenderer
import com.srctool.camouflage.core.image.ImageState
import com.srctool.camouflage.core.image.ImageStyle
import com.srctool.camouflage.core.pagination.PageArrowState
import com.srctool.camouflage.core.pagination.PageButtonState
import com.srctool.camouflage.core.pagination.PageDotState
import com.srctool.camouflage.core.pagination.PaginationColors
import com.srctool.camouflage.core.pagination.PaginationRenderer
import com.srctool.camouflage.core.pagination.PaginationState
import com.srctool.camouflage.core.pagination.PaginationStyle
import com.srctool.camouflage.core.progresssteps.ProgressStepsRenderer
import com.srctool.camouflage.core.progresssteps.StepItemState
import com.srctool.camouflage.core.progresssteps.StepSlots
import com.srctool.camouflage.core.progresssteps.StepsColors
import com.srctool.camouflage.core.progresssteps.StepsStyle
import com.srctool.camouflage.core.rating.RatingColors
import com.srctool.camouflage.core.rating.RatingRenderer
import com.srctool.camouflage.core.rating.RatingState
import com.srctool.camouflage.core.rating.RatingStyle
import com.srctool.camouflage.core.theme.CamoTheme

// The M10 image, rating, pagination, breadcrumb and progress steps in the plain test skin: boxes, rows and text.

private val PlainDataIcon = IconSource.Vector(ImageVector.Builder("t", 24.dp, 24.dp, 24f, 24f).build())

/** The last state each plain renderer drew, for tests. */
internal var lastImage: ImageState? = null
internal var lastRating: RatingState? = null
internal var lastPagination: PaginationState? = null
internal var lastBreadcrumb: BreadcrumbState? = null

/** The state each plain page button and step last drew, by index, for tests. */
internal val pageButtons = mutableMapOf<Int, PageButtonState>()
internal val steps = mutableMapOf<Int, StepItemState>()

internal object PlainImage : ImageRenderer {
    override fun defaults(theme: CamoTheme) =
        ImageStyle(PlainDataIcon, PlainDataIcon, 0, 0.dp, ImageColors(Color.LightGray, Color.Gray, Color.Gray))

    @Composable
    override fun Frame(state: ImageState, theme: CamoTheme, style: ImageStyle, modifier: Modifier,
        content: @Composable () -> Unit) {
        lastImage = state
        Box(modifier.background(style.colors.placeholder)) {
            content()
            BasicText(state.phase.name)
        }
    }
}

internal object PlainRating : RatingRenderer {
    override fun defaults(theme: CamoTheme) = RatingStyle(
        PlainDataIcon, 16.dp, 20.dp, 28.dp, 4.dp, RatingColors(Color.Black, Color.Gray, Color.DarkGray,
            Color.LightGray),
    )

    @Composable
    override fun Render(state: RatingState, theme: CamoTheme, style: RatingStyle, modifier: Modifier) {
        lastRating = state
        Row(modifier.padding(4.dp)) { repeat(state.max) { Box(Modifier.size(24.dp)) } }
    }
}

internal object PlainPagination : PaginationRenderer {
    override fun defaults(theme: CamoTheme) = PaginationStyle(
        32.dp, RectangleShape, 8.dp, 4.dp, TextStyle.Default, PlainDataIcon, PlainDataIcon,
        PaginationColors(Color.LightGray, Color.Black, Color.Black, Color.Gray, Color.Black),
    )

    @Composable
    override fun Container(
        state: PaginationState,
        theme: CamoTheme,
        style: PaginationStyle,
        modifier: Modifier,
        content: @Composable RowScope.() -> Unit,
    ) {
        lastPagination = state
        Row(modifier, content = content)
    }

    @Composable
    override fun Button(state: PageButtonState, label: String, theme: CamoTheme, style: PaginationStyle,
        modifier: Modifier) {
        pageButtons[state.index] = state
        Box(modifier.size(32.dp)) { BasicText(label) }
    }

    @Composable
    override fun Arrow(state: PageArrowState, theme: CamoTheme, style: PaginationStyle, modifier: Modifier) {
        Box(modifier.size(32.dp)) { BasicText(state.direction.name) }
    }

    @Composable
    override fun Gap(theme: CamoTheme, style: PaginationStyle) {
        BasicText("…")
    }

    @Composable
    override fun Dot(state: PageDotState, theme: CamoTheme, style: PaginationStyle, modifier: Modifier) {
        Box(modifier.size(16.dp))
    }

    @Composable
    override fun Summary(text: String, theme: CamoTheme, style: PaginationStyle) {
        BasicText(text)
    }
}

internal object PlainBreadcrumb : BreadcrumbRenderer {
    override fun defaults(theme: CamoTheme) = BreadcrumbStyle(
        PlainDataIcon, 4.dp, PlainDataIcon, 16.dp, TextStyle.Default, BreadcrumbColors(Color.Blue, Color.Black,
            Color.Gray, Color.Black),
    )

    @Composable
    override fun Container(
        state: BreadcrumbState,
        theme: CamoTheme,
        style: BreadcrumbStyle,
        modifier: Modifier,
        content: @Composable RowScope.() -> Unit,
    ) {
        lastBreadcrumb = state
        Row(modifier, content = content)
    }

    @Composable
    override fun Item(state: BreadcrumbItemState, slots: BreadcrumbSlots, theme: CamoTheme, style: BreadcrumbStyle,
        modifier: Modifier) {
        Row(modifier.padding(4.dp)) {
            slots.icon?.invoke()
            BasicText(slots.label)
        }
    }

    @Composable
    override fun Separator(theme: CamoTheme, style: BreadcrumbStyle) {
        BasicText("/")
    }
}

internal object PlainProgressSteps : ProgressStepsRenderer {
    override fun defaults(theme: CamoTheme) = StepsStyle(
        20.dp, 2.dp, true, PlainDataIcon, PlainDataIcon, TextStyle.Default, TextStyle.Default,
        StepsColors(Color.Green, Color.White, Color.Blue, Color.White, Color.Gray, Color.Red, Color.Gray, Color.Black,
            Color.DarkGray),
    )

    @Composable
    override fun Step(state: StepItemState, slots: StepSlots, theme: CamoTheme, style: StepsStyle, modifier: Modifier) {
        steps[state.index] = state
        Column(modifier.padding(4.dp)) {
            BasicText(slots.label)
            slots.description?.let { BasicText(it) }
        }
    }
}
