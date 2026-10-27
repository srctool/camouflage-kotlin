package com.srctool.camouflage.core.util

import androidx.compose.runtime.Composable

/** Whether the device writes times in 24 hours: the user's setting where the platform has one, else the locale's. */
@Composable
internal expect fun rememberSystem24Hour(): Boolean

/** Whether the platform has a hardware keyboard and a pointer by default (desktop and web), so typing beats picking. */
internal expect val prefersTyping: Boolean
