package com.gamopanel.mobile

import androidx.compose.ui.window.ComposeUIViewController

/**
 * Punto de entrada iOS/iPadOS: crea un UIViewController que aloja la UI de Compose.
 * Desde Swift se llama como `MainViewControllerKt.MainViewController()`.
 * Compose lee los safe areas de UIKit (Dynamic Island, esquinas del iPad, teclado).
 */
fun MainViewController() = ComposeUIViewController { App() }
