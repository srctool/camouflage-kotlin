package com.srctool.camouflage.sample

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Called from Swift as `MainViewControllerKt.MainViewController()`, so it keeps the UIKit-style name. */
@Suppress("FunctionNaming")
fun MainViewController(): UIViewController = ComposeUIViewController { SampleApp() }
