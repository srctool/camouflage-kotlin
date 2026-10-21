package com.srctool.camouflage.core.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/** Corner radii by role. The kind of corner (round or continuous) is the skin's decision. */
@Immutable
public data class CamoShapes(
    val none: CornerBasedShape = RoundedCornerShape(0.dp),
    val extraSmall: CornerBasedShape = RoundedCornerShape(4.dp),
    val small: CornerBasedShape = RoundedCornerShape(6.dp),
    val medium: CornerBasedShape = RoundedCornerShape(8.dp),
    val large: CornerBasedShape = RoundedCornerShape(12.dp),
    val extraLarge: CornerBasedShape = RoundedCornerShape(16.dp),
    val full: CornerBasedShape = RoundedCornerShape(percent = 50),
)

/** The shape for a [ShapeRole]. */
public fun CamoShapes.of(role: ShapeRole): CornerBasedShape = when (role) {
    ShapeRole.None -> none
    ShapeRole.ExtraSmall -> extraSmall
    ShapeRole.Small -> small
    ShapeRole.Medium -> medium
    ShapeRole.Large -> large
    ShapeRole.ExtraLarge -> extraLarge
    ShapeRole.Full -> full
}
