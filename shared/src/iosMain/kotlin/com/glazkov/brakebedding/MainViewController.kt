package com.glazkov.brakebedding

import androidx.compose.ui.window.ComposeUIViewController
import com.glazkov.brakebedding.ui.BrakeBeddingApp
import platform.UIKit.UIViewController

/** The entry point for the Swift app. It shows the common Compose UI. */
fun MainViewController(): UIViewController = ComposeUIViewController {
    BrakeBeddingApp()
}
