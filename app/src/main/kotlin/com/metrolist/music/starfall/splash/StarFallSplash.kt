/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val Navy = Color(0xFF121013)
private val Deep = Color(0xFF0D0C0E)

private data class Twinkle(val x: Float, val y: Float, val r: Float, val phase: Float)

/** Falling star streaks across the sky, lands as the app icon, then fades into the app. */
@Composable
fun StarFallSplash(onFinished: () -> Unit) {
    val stars = remember {
        val rnd = Random(7)
        List(70) { Twinkle(rnd.nextFloat(), rnd.nextFloat(), 0.6f + rnd.nextFloat() * 1.6f, rnd.nextFloat() * 6.28f) }
    }
    val fall = remember { Animatable(0f) }
    val twinkle = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val exit = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { twinkle.animateTo(6.28f * 3, tween(3200, easing = LinearEasing)) }
            fall.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
            launch { logoAlpha.animateTo(1f, tween(250)) }
            launch { logoScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            launch { textAlpha.animateTo(1f, tween(500, delayMillis = 150)) }
            kotlinx.coroutines.delay(900)
            exit.animateTo(0f, tween(350))
        }
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .alpha(exit.value)
            .background(Brush.verticalGradient(listOf(Deep, Navy))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            stars.forEach {
                val a = 0.35f + 0.65f * ((sin(twinkle.value + it.phase) + 1f) / 2f)
                drawCircle(Color.White.copy(alpha = a), it.r.dp.toPx(), Offset(it.x * size.width, it.y * size.height))
            }
            // The streak: head travels from the top-left corner to the logo spot, trail fades behind it.
            val target = Offset(size.width / 2f, size.height / 2f - 40.dp.toPx())
            val start = Offset(-size.width * 0.1f, -size.height * 0.05f)
            val p = fall.value
            if (p in 0.001f..0.999f) {
                val head = Offset(start.x + (target.x - start.x) * p, start.y + (target.y - start.y) * p)
                val tailP = (p - 0.35f).coerceAtLeast(0f)
                val tail = Offset(start.x + (target.x - start.x) * tailP, start.y + (target.y - start.y) * tailP)
                drawLine(
                    brush = Brush.linearGradient(listOf(Color.Transparent, Color(0xFFBFE9FF)), tail, head),
                    start = tail,
                    end = head,
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(Color.White, 6.dp.toPx(), head)
                drawCircle(Color(0x66BFE9FF), 14.dp.toPx(), head)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.starfall_star_art),
                contentDescription = null,
                modifier = Modifier
                    .size(132.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .clip(RoundedCornerShape(32.dp)),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "StarFall",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(textAlpha.value),
            )
        }
    }
}
