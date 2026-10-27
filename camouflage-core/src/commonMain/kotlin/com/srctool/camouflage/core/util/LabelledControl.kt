package com.srctool.camouflage.core.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.skin.LabelPosition

/**
 * The row a checkbox, switch or radio renderer draws: [control] and [label] side by side in reading order
 * ([labelPosition] `Start` puts the label first and pushes the control to the end), and [below] (an error
 * message) under them. With [centerControl] the control is centered on the whole label block (a switch);
 * otherwise it's centered on the label's first line, [firstLineHeight] tall (a checkbox, a radio).
 */
@Composable
public fun LabelledControl(
    modifier: Modifier,
    labelPosition: LabelPosition,
    gap: Dp,
    centerControl: Boolean,
    firstLineHeight: Dp,
    control: @Composable () -> Unit,
    label: (@Composable () -> Unit)?,
    below: (@Composable () -> Unit)? = null,
) {
    Column(modifier) {
        if (label == null) {
            control()
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = if (centerControl) Alignment.CenterVertically else Alignment.Top,
            ) {
                val placed: @Composable () -> Unit = {
                    if (centerControl) {
                        control()
                    } else {
                        Box(Modifier.heightIn(min = firstLineHeight), contentAlignment = Alignment.Center) { control() }
                    }
                }
                if (labelPosition == LabelPosition.Start) {
                    Box(Modifier.weight(1f)) { label() }
                    placed()
                } else {
                    placed()
                    Box(Modifier.weight(1f, fill = false)) { label() }
                }
            }
        }
        below?.invoke()
    }
}
