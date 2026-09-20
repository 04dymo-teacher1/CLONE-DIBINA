package com.example.dibina.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dibina.ui.components.DibinaLogotypeView
import com.example.dibina.ui.components.KratzLogoView
import kotlinx.coroutines.delay

enum class SplashPhase {
    Kratz,
    Dibina
}

/**
 * Splash Screen showing IMG 3 (KratzLogoView) then IMG 2 (DibinaLogotypeView)
 * with subtle animations.
 *
 * Rules:
 * - Clean white/blue modern theme.
 * - Do not duplicate the DIBINA word if it is already contained in the logo.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPhase by remember { mutableStateOf(SplashPhase.Kratz) }
    var startAnim by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnim = true
        // Show Kratz Developer Logo (IMG 3)
        delay(1200)
        // Transition to DIBINA Logotype (IMG 2)
        currentPhase = SplashPhase.Dibina
        delay(1500)
        // Finish splash
        onSplashFinished()
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "SplashAlpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.94f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "SplashScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentPhase,
            transitionSpec = {
                (fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)))
                    .togetherWith(fadeOut(animationSpec = tween(400, easing = FastOutSlowInEasing)))
            },
            label = "SplashPhaseTransition"
        ) { phase ->
            Box(
                modifier = Modifier
                    .scale(scaleAnim)
                    .alpha(alphaAnim),
                contentAlignment = Alignment.Center
            ) {
                when (phase) {
                    SplashPhase.Kratz -> {
                        // IMG 3: Kratz Developer Branding
                        KratzLogoView(scale = 1.15f)
                    }
                    SplashPhase.Dibina -> {
                        // IMG 2: DIBINA Logotype (Custom Canvas drawing containing DIBINA)
                        DibinaLogotypeView(scale = 1.35f, showSubtitle = true)
                    }
                }
            }
        }
    }
}

