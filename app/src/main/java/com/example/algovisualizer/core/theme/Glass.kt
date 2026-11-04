package com.example.algovisualizer.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Glass design tokens. Two values for every token: light and dark, switched by the system theme.
 *
 * The "glass" is translucency + a bright specular edge over an ambient gradient, not a live
 * backdrop blur -- blurring a smooth gradient returns nearly the same gradient, so the cheap
 * version is visually identical here and works below API 31.
 */
@Immutable
data class GlassColors(
    // Ambient background
    val base: Color,
    val baseDeep: Color,
    val orbPrimary: Color,
    val orbSecondary: Color,
    val orbTertiary: Color,
    val orbAlpha: Float,

    // Glass surfaces
    val fill: Color,
    val fillStrong: Color,
    val fillPressed: Color,
    val edgeHighlight: Color,
    val edgeShadow: Color,
    val shadowTint: Color,

    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,

    // Accent
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,

    // Visualizer palette -- replaces the ~90 hardcoded colors across the feature screens
    val nodeIdle: Color,
    val nodeComparing: Color,
    val nodeFound: Color,
    val nodeVisited: Color,
    val nodeQueued: Color,
    val edge: Color,
    val onNode: Color,
    val codeHighlight: Color,
    val isLight: Boolean
)

val DarkGlass = GlassColors(
    base = Color(0xFF070B14),
    baseDeep = Color(0xFF03060D),
    orbPrimary = Color(0xFF34D399),
    orbSecondary = Color(0xFF22D3EE),
    orbTertiary = Color(0xFF8B5CF6),
    orbAlpha = 0.26f,

    fill = Color.White.copy(alpha = 0.07f),
    fillStrong = Color.White.copy(alpha = 0.13f),
    fillPressed = Color.White.copy(alpha = 0.17f),
    edgeHighlight = Color.White.copy(alpha = 0.34f),
    edgeShadow = Color.White.copy(alpha = 0.05f),
    shadowTint = Color(0xFF000000),

    textPrimary = Color.White,
    textSecondary = Color.White.copy(alpha = 0.72f),
    textTertiary = Color.White.copy(alpha = 0.48f),

    accent = Color(0xFF4ADE80),
    accentSoft = Color(0xFF4ADE80).copy(alpha = 0.16f),
    onAccent = Color(0xFF052E16),

    nodeIdle = Color(0xFF38BDF8),
    nodeComparing = Color(0xFFFBBF24),
    nodeFound = Color(0xFF4ADE80),
    nodeVisited = Color(0xFFA78BFA),
    nodeQueued = Color(0xFFF472B6),
    edge = Color.White.copy(alpha = 0.32f),
    onNode = Color(0xFF04121F),
    codeHighlight = Color(0xFF4ADE80).copy(alpha = 0.20f),
    isLight = false
)

val LightGlass = GlassColors(
    base = Color(0xFFF4F6FB),
    baseDeep = Color(0xFFE6EBF5),
    orbPrimary = Color(0xFF6EE7B7),
    orbSecondary = Color(0xFF7DD3FC),
    orbTertiary = Color(0xFFC4B5FD),
    orbAlpha = 0.55f,

    fill = Color.White.copy(alpha = 0.58f),
    fillStrong = Color.White.copy(alpha = 0.78f),
    fillPressed = Color.White.copy(alpha = 0.92f),
    edgeHighlight = Color.White.copy(alpha = 0.90f),
    edgeShadow = Color(0xFF0B1220).copy(alpha = 0.06f),
    shadowTint = Color(0xFF1E293B),

    textPrimary = Color(0xFF0B1220),
    textSecondary = Color(0xFF0B1220).copy(alpha = 0.68f),
    textTertiary = Color(0xFF0B1220).copy(alpha = 0.45f),

    accent = Color(0xFF059669),
    accentSoft = Color(0xFF059669).copy(alpha = 0.12f),
    onAccent = Color.White,

    nodeIdle = Color(0xFF0284C7),
    nodeComparing = Color(0xFFD97706),
    nodeFound = Color(0xFF059669),
    nodeVisited = Color(0xFF7C3AED),
    nodeQueued = Color(0xFFDB2777),
    edge = Color(0xFF0B1220).copy(alpha = 0.28f),
    onNode = Color.White,
    codeHighlight = Color(0xFF059669).copy(alpha = 0.16f),
    isLight = true
)

val LocalGlass = staticCompositionLocalOf { DarkGlass }
