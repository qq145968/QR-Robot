package com.scanrobot.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val BluePrimary = Color(0xFF1A73E8)
val TealAccent = Color(0xFF00B8A4)
val TealBright = Color(0xFF00E5CC)
val BgLight = Color(0xFFF5F6F8)
val BgWhite = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1A1A1A)
val TextSecondary = Color(0xFF999999)
val BorderLight = Color(0xFFECEEF1)
val DangerRed = Color(0xFFFF4D4F)
val WarningAmber = Color(0xFFD48806)
val WarningBg = Color(0xFFFFF7E6)
val TealBg = Color(0xFFE6FFFB)
val GoldYellow = Color(0xFFFFD700)
val RedPrimary = Color(0xFFFF3B30)

// 首页渐变背景色（方案一：浅色渐变）
val GradientTopBlue = Color(0xFFE8F0FE)       // 顶部淡蓝
val GradientTopPurple = Color(0xFFF0E8FF)     // 顶部淡紫点缀
val GradientMid = Color(0xFFF5F6F8)            // 中间过渡（与 BgLight 一致）
val GradientBottom = Color(0xFFF0F2F5)         // 底部稍深

/**
 * 首页垂直渐变 Brush：顶部淡蓝紫 → 中部浅灰 → 底部过渡
 * 营造层次感但不抢眼，配 Material3 卡片更显高级
 */
val HomeBackgroundBrush: Brush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFEAF2FF),   // 0%   顶部淡蓝
        Color(0xFFF0ECFF),   // 15%  淡紫点缀
        Color(0xFFF5F6F8),   // 35%  过渡到浅灰
        Color(0xFFF5F6F8),   // 70%  主体浅灰
        Color(0xFFF0F2F5),   // 100% 底部稍深
    )
)

/**
 * 顶部栏渐变 Brush（更浓郁一点，用于 TopAppBar 区域）
 */
val TopBarGradientBrush: Brush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFE3EEFF),
        Color(0xFFEBF1FF),
        Color(0xFFF0F2FF),
    )
)
