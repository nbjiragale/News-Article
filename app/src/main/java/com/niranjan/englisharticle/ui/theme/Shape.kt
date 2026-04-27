package com.niranjan.englisharticle.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Bigger / more playful radii drive the M3 Expressive feel. Cards use `large`
// (24dp), pills/chips use `extraLarge` (32dp), bottom sheets and hero headers
// use `extraLarge` for a soft top edge.
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
