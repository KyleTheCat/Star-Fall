/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.rank

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.music.LocalDatabase
import com.metrolist.music.R
import com.metrolist.music.starfall.online.StarFallAccountSection
import com.metrolist.music.starfall.profile.MAX_BIO
import com.metrolist.music.starfall.profile.MAX_GENRES
import com.metrolist.music.starfall.profile.MAX_PLAYLISTS
import com.metrolist.music.starfall.profile.NameChangeResult
import com.metrolist.music.starfall.profile.NameLock
import com.metrolist.music.starfall.profile.ProfileAvatar
import com.metrolist.music.starfall.profile.ProfileLookup
import com.metrolist.music.starfall.profile.ProfileSettings
import com.metrolist.music.starfall.profile.ProfileStatsRow
import com.metrolist.music.starfall.profile.ProfileViewModel
import com.metrolist.music.starfall.profile.PublicProfile
import com.metrolist.music.starfall.profile.ShowcaseItem
import com.metrolist.music.starfall.profile.VisitedProfileViewModel
import com.metrolist.music.starfall.profile.nameLockText
import com.metrolist.music.starfall.ui.StarFallSpacing
import kotlinx.coroutines.launch

/** Your profile: avatar, name, bio, rank progress, leaderboard entry and account controls, in one place. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarFallProfileScreen(
    navController: NavController,
    vm: ProfileViewModel = hiltViewModel(),
    rankVm: RankViewModel = hiltViewModel(),
) {
    val settings by vm.settings.collectAsState()
    val total by vm.totalSeconds.collectAsState()
    val identity by vm.identity.collectAsState()
    val name by vm.displayName.collectAsState()
    val celebrate by rankVm.celebrate.collectAsState()
    var editing by remember { mutableStateOf(false) }
    val lockMs = remember(settings.lastNameChangeAt) { NameLock.remainingMs(settings.lastNameChangeAt, System.currentTimeMillis()) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.starfall_my_profile)) },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.cd_back))
                }
            },
        )
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = StarFallSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StarFallSpacing.lg),
        ) {
            ProfileAvatar(
                name = name,
                imageUrl = identity?.avatar,
                size = 88.dp,
                contentDescription = stringResource(R.string.starfall_avatar_signed_out),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(StarFallSpacing.xs)) {
                Text(name ?: stringResource(R.string.starfall_you), style = MaterialTheme.typography.headlineSmall)
                if (lockMs > 0) {
                    Text(
                        stringResource(R.string.starfall_name_locked, nameLockText(lockMs)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (settings.bio.isNotBlank()) {
                Text(settings.bio, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
            if (settings.genres.isNotEmpty()) GenreChips(settings.genres)
            FilledTonalButton(onClick = { editing = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.starfall_edit_profile))
            }

            ProfileStatsRow(hours = total / 3600.0)

            Panel { RankProgress(total) }

            if (vm.onlineAvailable && identity != null) {
                Panel(onClick = { navController.navigate("starfall/leaderboard") }) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.starfall_leaderboard), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Icon(painterResource(R.drawable.navigate_next), stringResource(R.string.starfall_leaderboard_cd))
                    }
                }
            }

            StarFallAccountSection(navController)

            Text(
                stringResource(
                    when {
                        !settings.visible -> R.string.starfall_profile_hidden_desc
                        !vm.onlineAvailable -> R.string.starfall_profile_local_only
                        identity == null -> R.string.starfall_profile_sign_in_to_share
                        else -> R.string.starfall_profile_visible_desc
                    },
                ),
                Modifier.fillMaxWidth().clickable { navController.navigate("settings/privacy") }.heightIn(min = 48.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (settings.playlists.isNotEmpty()) {
                PlaylistRow(
                    title = stringResource(R.string.starfall_your_playlists),
                    playlists = settings.playlists,
                    onPlaylist = { navController.navigate("online_playlist/${it.id}") },
                )
            }
            Spacer(Modifier.height(StarFallSpacing.xl))
        }
    }
    if (editing) EditProfileDialog(settings, lockMs, onDismiss = { editing = false }, vm = vm)
    celebrate?.let { RankUpCelebration(it, onDone = rankVm::dismissCelebration) }
}

/** Someone else's profile. Private/hidden profiles show a plain notice, never data. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarFallVisitedProfileScreen(navController: NavController, vm: VisitedProfileViewModel = hiltViewModel()) {
    val lookup by vm.lookup.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.starfall_profile_title)) },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.cd_back))
                }
            },
        )
        when (val l = lookup) {
            ProfileLookup.Loading -> Text(stringResource(R.string.starfall_profile_loading), Modifier.padding(StarFallSpacing.xl))
            ProfileLookup.Private -> Notice(R.string.starfall_profile_private)
            ProfileLookup.Unavailable -> Notice(R.string.starfall_online_unavailable)
            is ProfileLookup.Found -> VisitedBody(l.profile, navController)
        }
    }
}

@Composable
private fun Notice(res: Int) {
    Text(stringResource(res), Modifier.padding(StarFallSpacing.xl), style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun VisitedBody(profile: PublicProfile, navController: NavController) {
    val rank = Rank.forHours(profile.hours)
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = StarFallSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StarFallSpacing.lg),
    ) {
        ProfileAvatar(profile.name, profile.avatar, 88.dp, contentDescription = stringResource(R.string.starfall_avatar_of, profile.name))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(StarFallSpacing.xs)) {
            Text(profile.name, style = MaterialTheme.typography.headlineSmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(StarFallSpacing.sm)) {
                RankIcon(rank, size = 20.dp)
                Text(rank.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (profile.bio.isNotBlank()) Text(profile.bio, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (profile.genres.isNotEmpty()) GenreChips(profile.genres)
        if (profile.playlists.isNotEmpty()) {
            PlaylistRow(
                title = stringResource(R.string.starfall_playlists_section),
                playlists = profile.playlists,
                onPlaylist = { navController.navigate("online_playlist/${it.id}") },
            )
        }
        Spacer(Modifier.height(StarFallSpacing.xl))
    }
}

/** One flat surface level; never nested inside another panel. */
@Composable
private fun Panel(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val base = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerLow)
    Column((if (onClick != null) base.clickable(onClick = onClick) else base).padding(StarFallSpacing.lg)) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreChips(genres: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(StarFallSpacing.sm, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(StarFallSpacing.xs)) {
        genres.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
    }
}

@Composable
private fun PlaylistRow(title: String, playlists: List<ShowcaseItem>, onPlaylist: (ShowcaseItem) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(StarFallSpacing.md)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(StarFallSpacing.md)) {
            items(playlists, key = { it.id }) { item ->
                Column(Modifier.size(width = 108.dp, height = 140.dp).clickable { onPlaylist(item) }, verticalArrangement = Arrangement.spacedBy(StarFallSpacing.sm)) {
                    AsyncImage(
                        item.thumbnail,
                        null,
                        Modifier.size(108.dp).clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop,
                    )
                    Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun EditProfileDialog(settings: ProfileSettings, lockMs: Long, onDismiss: () -> Unit, vm: ProfileViewModel) {
    val initialName = remember { settings.customName ?: vm.displayName.value.orEmpty() }
    var name by remember { mutableStateOf(initialName) }
    var bio by remember { mutableStateOf(settings.bio) }
    var genres by remember { mutableStateOf(settings.genres.joinToString(", ")) }
    var nameError by remember { mutableStateOf<String?>(null) }
    val picked = remember { mutableStateListOf<ShowcaseItem>().apply { addAll(settings.playlists) } }
    val scope = rememberCoroutineScope()
    val database = LocalDatabase.current
    val allPlaylists by database.playlistsByCreateDateAsc().collectAsState(initial = emptyList())
    // Only YouTube Music playlists can be opened by other listeners.
    val playlists = allPlaylists.filter { it.playlist.browseId != null }
    val locked = lockMs > 0
    val invalidText = stringResource(R.string.starfall_name_invalid, NameLock.MAX_NAME)
    val lockedText = stringResource(R.string.starfall_name_locked_dialog, nameLockText(lockMs))
    val failedText = stringResource(R.string.starfall_name_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.starfall_edit_profile)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(StarFallSpacing.md)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(NameLock.MAX_NAME); nameError = null },
                    label = { Text(stringResource(R.string.starfall_display_name)) },
                    supportingText = {
                        Text(nameError ?: if (locked) stringResource(R.string.starfall_name_locked, nameLockText(lockMs)) else stringResource(R.string.starfall_name_hint, NameLock.MAX_NAME))
                    },
                    isError = nameError != null,
                    enabled = !locked,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it.take(MAX_BIO) },
                    label = { Text(stringResource(R.string.starfall_bio)) },
                    supportingText = { Text("${bio.length}/$MAX_BIO") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = genres,
                    onValueChange = { genres = it },
                    label = { Text(stringResource(R.string.starfall_favorite_genres)) },
                    supportingText = { Text(stringResource(R.string.starfall_genres_hint, MAX_GENRES)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.starfall_pick_playlists, MAX_PLAYLISTS), style = MaterialTheme.typography.labelLarge)
                playlists.forEach { p ->
                    val id = p.playlist.browseId ?: return@forEach
                    val checked = picked.any { it.id == id }
                    Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { on ->
                                if (on && picked.size < MAX_PLAYLISTS) {
                                    picked.add(ShowcaseItem(id, p.title, p.thumbnails.firstOrNull()))
                                } else if (!on) {
                                    picked.removeAll { it.id == id }
                                }
                            },
                        )
                        Text(p.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    val result = if (locked || name.trim() == initialName.trim()) NameChangeResult.Unchanged else runCatching { vm.changeName(name) }.getOrNull()
                    when (result) {
                        null -> { nameError = failedText; return@launch }
                        NameChangeResult.Invalid -> { nameError = invalidText; return@launch }
                        is NameChangeResult.Locked -> { nameError = lockedText; return@launch }
                        else -> Unit
                    }
                    vm.setBio(bio)
                    vm.setGenres(genres.split(','))
                    vm.setPlaylists(picked.toList())
                    onDismiss()
                }
            }) { Text(stringResource(R.string.starfall_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}
