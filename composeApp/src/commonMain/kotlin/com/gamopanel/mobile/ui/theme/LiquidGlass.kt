package com.gamopanel.mobile.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/** Paleta del tema Liquid Glass. */
object GlassColors {
    val Base = Color(0xFF070B1A)    // Fondo muy oscuro (azul noche).
    val Violet = Color(0xFF7C4DFF)  // Mancha de luz 1.
    val Cyan = Color(0xFF00E5FF)    // Mancha de luz 2 y color de acento.
    val Magenta = Color(0xFFFF4081) // Mancha de luz 3.
    val Accent = Cyan               // Acento de botones/foco.
    val Ok = Color(0xFF4ADE80)      // Estado correcto.
    val Bad = Color(0xFFFF6B6B)     // Estado de error.
}

/** Formas estándar del tema. */
object GlassShapes {
    val Panel = RoundedCornerShape(28.dp)  // Paneles grandes.
    val Control = RoundedCornerShape(18.dp) // Botones y campos.
}

/**
 * Fondo de la pantalla + capa "fuente" del desenfoque.
 *
 * Por qué Haze y no Modifier.blur: Modifier.blur desenfoca el contenido PROPIO
 * del composable, no lo que hay detrás. Haze captura el fondo (hazeSource) y lo
 * desenfoca debajo de cada panel (hazeEffect). Internamente usa RenderEffect en
 * Android 12+ (Honor 90) y filtros de imagen de Skia en iOS (iPhone/iPad).
 */
@Composable
fun LiquidBackground(
    hazeState: HazeState,                 // Estado compartido entre fuente y paneles.
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit, // Contenido que va ENCIMA del fondo.
) {
    Box(modifier.fillMaxSize().background(GlassColors.Base)) {
        // Capa fuente: lo que se verá desenfocado detrás de los paneles de vidrio.
        Box(Modifier.fillMaxSize().hazeSource(hazeState)) { AuroraBlobs() }
        // El contenido va como hermano (no hijo) de la fuente, requisito de Haze.
        content()
    }
}

/** Manchas de color animadas que dan "vida" al cristal. */
@Composable
private fun AuroraBlobs() {
    // Animación infinita lenta (18 s) que va y viene entre 0 y 1: barata para la GPU.
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "phase",
    )
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val r = maxOf(w, h) * 0.55f // Radio relativo: se adapta a móvil y tablet.
        drawBlob(Offset(w * (0.15f + 0.5f * phase), h * (0.18f + 0.10f * phase)), r, GlassColors.Violet)
        drawBlob(Offset(w * (0.90f - 0.5f * phase), h * (0.55f + 0.10f * phase)), r * 0.9f, GlassColors.Cyan)
        drawBlob(Offset(w * (0.25f + 0.3f * phase), h * (0.95f - 0.15f * phase)), r * 0.8f, GlassColors.Magenta)
    }
}

/** Dibuja un círculo con degradado radial (centro opaco -> borde transparente). */
private fun DrawScope.drawBlob(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.75f), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/**
 * Convierte cualquier composable en un panel de cristal líquido:
 * 1) recorta a la forma, 2) desenfoca el fondo + tinte + grano,
 * 3) añade brillo superior (reflejo) y 4) borde degradado tipo canto de vidrio.
 */
fun Modifier.liquidGlass(
    hazeState: HazeState,                              // Mismo estado que LiquidBackground.
    shape: Shape = GlassShapes.Panel,                  // Forma del panel.
    tint: Color = Color.White.copy(alpha = 0.10f),     // Color del cristal.
): Modifier = this
    .clip(shape) // El desenfoque solo debe verse dentro de la forma.
    .hazeEffect(state = hazeState) {
        blurRadius = 32.dp                 // Intensidad del desenfoque del fondo.
        tints = listOf(HazeTint(tint))     // Capa de color sobre el desenfoque.
        noiseFactor = 0.05f                // Grano sutil: evita bandas y da textura de vidrio.
    }
    // Reflejo: más claro arriba, casi transparente abajo.
    .background(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.02f))),
        shape,
    )
    // Borde: luz fuerte en una esquina, tenue en el centro, media en la opuesta.
    .border(
        1.dp,
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.35f)),
        ),
        shape,
    )
