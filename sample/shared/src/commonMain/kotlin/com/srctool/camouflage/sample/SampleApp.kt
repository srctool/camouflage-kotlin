package com.srctool.camouflage.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.srctool.camouflage.skin.minimal.minimalTheme

/** The sample app. Until milestone M3 it shows a placeholder; then it grows a screen per milestone. */
@Composable
fun SampleApp() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText("Camouflage sample (M0), theme ${minimalTheme.placeholder}")
    }
}
