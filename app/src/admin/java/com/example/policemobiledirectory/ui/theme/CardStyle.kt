package com.example.policemobiledirectory.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed class CardStyle(val name: String) {
    object Classic : CardStyle("Classic")
    object Modern : CardStyle("Modern")
    object Vibrant : CardStyle("Vibrant")
}
