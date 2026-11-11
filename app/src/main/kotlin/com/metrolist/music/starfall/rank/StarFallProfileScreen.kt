/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.rank

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.music.LocalDatabase
import com.metrolist.music.starfall.profile.ProfileStatsRow
import com.metrolist.music.R
import com.metrolist.music.starfall.profile.MAX_BIO
import com.metrolist.music.starfall.profile.MAX_GENRES
import com.metrolist.music.starfall.profile.MAX_PLAYLISTS
import com.metrolist.music.starfall.profile.ProfileIdentity
import com.metrolist.music.starfall.profile.ProfileLookup
import com.metrolist.music.starfall.profile.ProfileSettings
import com.metrolist.music.starfall.profile.ProfileViewModel
import com.metrolist.music.starfall.profile.PublicProfile
import com.metrolist.music.starfall.profile.ShowcaseItem
import com.metrolist.music.starfall.profile.VisitedProfileViewModel
import com.metrolist.music.starfall.ui.StarFallCard
import com.metrolist.music.starfall.ui.StarFallSectionTitle

/** Your own profile, with editing. The rank shown is derived from listening hours. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarFallProfileScreen(navController: NavController, vm: ProfileViewModel = hiltViewModel()) {
    val settings by vm.settings.collectAsState()
    val total by vm.totalSeconds.collectAsState()
    val identity by vm.identity.collectAsState()
    var editing by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.starfall_my_profile)) },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.cd_back))
                }
            },
            actions = {
                IconButton(onClick = { editing = true }) {
                    Icon(painterResource(R.drawable.edit), stringResource(R.string.starfall_edit_profile))
                }
            },
        )
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VisibilityBanner(settings.visible, online = vm.onlineAvailable, signedIn = identity != null) {
                navController.navigate("settings/privacy")
            }
            ProfileStatsRow(hours = total / 3600.0)
            ProfileBody(
                name = identity?.name ?: stringResource(R.string.starfall_you),
                avatar = identity?.avatar,
                hours = total / 3600.0,
                bio = settings.bio,
                genres = settings.genres,
                playlists = settings.playlists,
                onPlaylist = { navController.navigate("online_playlist/${it.id}") },
            )
            OutlinedButton(onClick = { navController.navigate("starfall/rank") }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.starfall_rank_title))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (editing) EditProfileDialog(settings, onDismiss = { editing = false }, vm = vm)
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
            ProfileLookup.Loading -> Text(stringResource(R.string.starfall_profile_loading), Modifier.padding(24.dp))
            ProfileLookup.Private -> Notice(R.string.starfall_profile_private)
            ProfileLookup.Unavailable -> Notice(R.string.starfall_online_unavailable)
            is ProfileLookup.Found -> VisitedBody(l.profile, navController)
        }
    }
}

@Composable
private fun Notice(res: Int) {
    StarFallCard(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(stringResource(res), Modifier.padding(24.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun VisitedBody(profile: PublicProfile, navController: NavController) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProfileBody(
            name = profile.name,
            avatar = profile.avatar,
            hours = profile.hours,
            bio = profile.bio,
            genres = profile.genres,
            playlists = profile.playlists,
            onPlaylist = { navController.navigate("online_playlist/${it.id}") },
        )
    }
}

@Composable
private fun VisibilityBanner(visible: Boolean, online: Boolean, signedIn: Boolean, onOpenPrivacy: () -> Unit) {
    StarFallCard(Modifier.fillMaxWidth(), onClick = onOpenPrivacy) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(if (visible) R.string.starfall_profile_visible else R.string.starfall_profile_hidden),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(
                    when {
                        !visible -> R.string.starfall_profile_hidden_desc
                        !online -> R.string.starfall_profile_local_only
                        !signedIn -> R.string.starfall_profile_sign_in_to_share
                        else -> R.string.starfall_profile_visible_desc
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ProfileBody(
    name: String,
    avatar: String?,
    hours: Double,
    bio: String,
    genres: List<String>,
    playlists: List<ShowcaseItem>,
    onPlaylist: (ShowcaseItem) -> Unit,
) {
    StarFallCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (avatar != null) {
                AsyncImage(avatar, null, Modifier.size(96.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                Icon(painterResource(R.drawable.account), null, Modifier.size(96.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            ProfileRankLabel(hours)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.starfall_hours_listened, "%.1f".format(hours)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    StarFallSectionTitle(stringResource(R.string.starfall_bio))
    StarFallCard(Modifier.fillMaxWidth()) {
        Text(
            bio.ifBlank { stringResource(R.string.starfall_bio_empty) },
            Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = if (bio.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
    if (genres.isNotEmpty()) {
        StarFallSectionTitle(stringResource(R.string.starfall_favorite_genres))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            genres.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
        }
    }
    StarFallSectionTitle(stringResource(R.string.starfall_favorite_playlists))
    if (playlists.isEmpty()) {
        Text(stringResource(R.string.starfall_playlists_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(playlists, key = { it.id }) { item ->
                StarFallCard(Modifier.size(width = 140.dp, height = 176.dp), onClick = { onPlaylist(item) }, corner = 20.dp) {
                    Column(Modifier.padding(10.dp)) {
                        AsyncImage(
                            item.thumbnail,
                            null,
                            Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun EditProfileDialog(settings: ProfileSettings, onDismiss: () -> Unit, vm: ProfileViewModel) {
    var bio by remember { mutableStateOf(settings.bio) }
    var genres by remember { mutableStateOf(settings.genres.joinToString(", ")) }
    val picked = remember { mutableStateListOf<ShowcaseItem>().apply { addAll(settings.playlists) } }
    val database = LocalDatabase.current
    val allPlaylists by database.playlistsByCreateDateAsc().collectAsState(initial = emptyList())
    // Only YouTube Music playlists can be opened by other listeners.
    val playlists = allPlaylists.filter { it.playlist.browseId != null }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.starfall_edit_profile)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                vm.setBio(bio)
                vm.setGenres(genres.split(','))
                vm.setPlaylists(picked.toList())
                onDismiss()
            }) { Text(stringResource(R.string.starfall_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@Composable
private fun ProfileRankLabel(hours: Double) {
    val rank = Rank.forHours(hours)
    Text(
        "${rank.icon} ${rank.label}",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
