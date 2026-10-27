package com.srctool.camouflage.core.util

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// The "Use 24-hour format" setting, read once per composition of the picker.
@Composable
internal actual fun rememberSystem24Hour(): Boolean = DateFormat.is24HourFormat(LocalContext.current)

internal actual val prefersTyping: Boolean = false
