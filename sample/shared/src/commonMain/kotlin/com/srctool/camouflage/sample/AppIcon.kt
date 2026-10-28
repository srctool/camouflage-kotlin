package com.srctool.camouflage.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.sample.resources.Res
import com.srctool.camouflage.sample.resources.ic_back
import com.srctool.camouflage.sample.resources.ic_chevron
import com.srctool.camouflage.sample.resources.ic_delete
import com.srctool.camouflage.sample.resources.ic_edit
import com.srctool.camouflage.sample.resources.ic_more
import com.srctool.camouflage.sample.resources.ic_search
import com.srctool.camouflage.sample.resources.ic_share
import com.srctool.camouflage.sample.resources.ic_sort
import com.srctool.camouflage.sample.resources.ic_star
import com.srctool.camouflage.sample.resources.ic_star_outline
import org.jetbrains.compose.resources.DrawableResource

/**
 * The app's icons (CamoIcon §3, the `AppIcon` pattern): one composable per asset, with no size or color unless
 * given, so each takes the style of the component it's placed in. Decorative by default.
 */
object AppIcon {
    @Composable fun Back(modifier: Modifier = Modifier) = Icon(Res.drawable.ic_back, modifier, autoMirror = true)

    @Composable fun Search(modifier: Modifier = Modifier, size: Dp = Dp.Unspecified, tint: Color = Color.Unspecified) =
        Icon(Res.drawable.ic_search, modifier, size = size, tint = tint)

    @Composable fun Star(modifier: Modifier = Modifier) = Icon(Res.drawable.ic_star, modifier)

    @Composable fun StarOutline(modifier: Modifier = Modifier) = Icon(Res.drawable.ic_star_outline, modifier)

    @Composable fun Delete(modifier: Modifier = Modifier) = Icon(Res.drawable.ic_delete, modifier)

    @Composable fun ChevronRight(modifier: Modifier = Modifier) =
        Icon(Res.drawable.ic_chevron, modifier, autoMirror = true)

    @Composable fun More(modifier: Modifier = Modifier) = Icon(Res.drawable.ic_more, modifier)

    @Composable fun Sort(modifier: Modifier = Modifier) = Icon(Res.drawable.ic_sort, modifier)

    @Composable fun Share(modifier: Modifier = Modifier) = Icon(Res.drawable.ic_share, modifier)

    @Composable fun Edit(modifier: Modifier = Modifier) = Icon(Res.drawable.ic_edit, modifier)

    @Composable
    private fun Icon(
        drawable: DrawableResource,
        modifier: Modifier,
        autoMirror: Boolean = false,
        size: Dp = Dp.Unspecified,
        tint: Color = Color.Unspecified,
    ) = CamoIcon(IconSource.Resource(drawable, autoMirror), contentDescription = null, modifier, size, tint)
}
