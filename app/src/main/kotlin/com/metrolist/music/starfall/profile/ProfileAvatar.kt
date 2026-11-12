/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.R

/** Circular avatar: the photo when there is one, else the name's initial, else a neutral placeholder. */
@Composable
fun ProfileAvatar(
    name: String?,
    imageUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(
        modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        when {
            imageUrl != null -> AsyncImage(imageUrl, contentDescription, Modifier.size(size), contentScale = ContentScale.Crop)
            !name.isNullOrBlank() -> Text(
                name.first().uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = (size.value * 0.42f).sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            else -> Icon(
                painterResource(R.drawable.account),
                contentDescription,
                Modifier.size(size * 0.55f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun nameLockText(remainingMs: Long): String {
    val r = NameLock.split(remainingMs)
    return when {
        r.days > 0 -> androidx.compose.ui.res.stringResource(R.string.starfall_duration_dh, r.days.toInt(), r.hours.toInt())
        r.hours > 0 -> androidx.compose.ui.res.stringResource(R.string.starfall_duration_hm, r.hours.toInt(), r.minutes.toInt())
        else -> androidx.compose.ui.res.stringResource(R.string.starfall_duration_m, r.minutes.toInt())
    }
}

/** Light-weight source for the Home avatar, so the top bar does not pull in the full profile editor. */
@dagger.hilt.android.lifecycle.HiltViewModel
class ProfileEntryViewModel @javax.inject.Inject constructor(
    online: com.metrolist.music.starfall.online.StarFallOnline,
    store: ProfileStore,
) : androidx.lifecycle.ViewModel() {
    val state = kotlinx.coroutines.flow.combine(online.identity, store.settings) { id, s ->
        id?.let { ProfileIdentity(it.uid, s.customName ?: it.name, it.avatar) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Top-bar entry to the combined profile; a neutral placeholder while signed out. */
@Composable
fun ProfileEntryButton(onClick: () -> Unit, vm: ProfileEntryViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()) {
    val identity by vm.state.collectAsState()
    androidx.compose.material3.IconButton(onClick = onClick) {
        ProfileAvatar(
            name = identity?.name,
            imageUrl = identity?.avatar,
            size = 32.dp,
            contentDescription = identity?.let { stringResource(R.string.starfall_avatar_of, it.name) }
                ?: stringResource(R.string.starfall_avatar_signed_out),
        )
    }
}
