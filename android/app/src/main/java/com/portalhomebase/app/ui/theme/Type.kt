package com.portalhomebase.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Kitchen viewing distance (50-100cm): body >= 18sp on Portal+.
val Typography = Typography(
    displayLarge = TextStyle(fontSize = 62.sp, fontWeight = FontWeight.Bold, lineHeight = 64.sp),
    headlineSmall = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 25.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp),
    titleMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 19.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 27.sp),
    labelLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
    bodySmall = TextStyle(fontSize = 15.sp),
    labelSmall = TextStyle(fontSize = 14.sp),
)
