package com.niranjan.englisharticle.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Paper, not plastic. The old scale ran to 32dp pills on everything, which made
// every element read as the same floating card. These radii are small enough that
// a surface reads as an inset area of the page rather than an object on top of it.
// Only the bottom sheet keeps a large radius, because it genuinely is a thing that
// slides over the page.
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(22.dp)
)
