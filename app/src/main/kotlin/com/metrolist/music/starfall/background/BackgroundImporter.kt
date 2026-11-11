/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.background

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/** Center-crops to the screen aspect, caps the long side, and stores a compressed JPEG copy. */
object BackgroundImporter {
    private const val MAX_SIDE = 1440
    private const val QUALITY = 85

    fun import(ctx: Context, uri: Uri, target: File): Boolean {
        val source = decode(ctx, uri) ?: return false
        val out = capSize(cropToScreen(source))
        target.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        return true
    }

    private fun decode(ctx: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sample = (maxOf(bounds.outWidth, bounds.outHeight) / (MAX_SIDE * 2)).coerceAtLeast(1)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private fun cropToScreen(src: Bitmap): Bitmap {
        val m = Resources.getSystem().displayMetrics
        val aspect = m.widthPixels.toFloat() / m.heightPixels
        val width = minOf(src.width.toFloat(), src.height * aspect).toInt()
        val height = minOf(src.height.toFloat(), src.width / aspect).toInt()
        return Bitmap.createBitmap(src, (src.width - width) / 2, (src.height - height) / 2, width, height)
    }

    private fun capSize(src: Bitmap): Bitmap {
        val scale = MAX_SIDE.toFloat() / maxOf(src.width, src.height)
        if (scale >= 1f) return src
        return Bitmap.createScaledBitmap(src, (src.width * scale).toInt(), (src.height * scale).toInt(), true)
    }
}
