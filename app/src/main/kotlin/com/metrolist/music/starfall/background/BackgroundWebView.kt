/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.background

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@SuppressLint("SetJavaScriptEnabled")
private fun newBackgroundWebView(ctx: Context) = WebView(ctx).apply {
    setBackgroundColor(Color.TRANSPARENT)
    setLayerType(View.LAYER_TYPE_HARDWARE, null)
    settings.javaScriptEnabled = true
    isFocusable = false
    isClickable = false
    setOnTouchListener { _, _ -> false }
}

@Composable
fun BackgroundWebView(presetId: String, modifier: Modifier = Modifier) {
    val view = remember { mutableStateOf<WebView?>(null) }
    PauseWithLifecycle(view.value)
    AndroidView(
        modifier = modifier,
        factory = { ctx -> newBackgroundWebView(ctx).also { view.value = it } },
        update = {
            if (it.tag != presetId) {
                it.tag = presetId
                it.loadUrl("file:///android_asset/starfall/background.html?preset=$presetId")
            }
        },
        onRelease = { it.destroy() },
    )
}

@Composable
private fun PauseWithLifecycle(webView: WebView?) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, webView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webView?.onPause()
                Lifecycle.Event.ON_RESUME -> webView?.onResume()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}
