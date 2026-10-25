package com.srctool.camouflage.core.provider

import androidx.compose.runtime.Composable

// Desktop OSes don't expose a common reduced-motion setting to the JVM.
@Composable
internal actual fun rememberReducedMotion(): Boolean = false
