/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.R
import com.metrolist.music.starfall.online.StarFallOnline
import com.metrolist.music.starfall.rank.RankIcon
import com.metrolist.music.starfall.ui.StarFallSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class ProfileSearchState(
    val results: List<ProfileHit> = emptyList(),
    val loading: Boolean = false,
    val endReached: Boolean = true,
    val failed: Boolean = false,
    val searched: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileSearchViewModel @Inject constructor(online: StarFallOnline) : ViewModel() {
    /** False in builds without online features, where the Profiles chip is hidden. */
    val available: Boolean = online.available

    private val pager = ProfileSearchPager(online::searchProfiles)
    private val query = MutableStateFlow("")
    val state = MutableStateFlow(ProfileSearchState())

    init {
        if (available) {
            viewModelScope.launch {
                query.debounce(ProfileQuery.DEBOUNCE_MS).distinctUntilChanged().collectLatest { q ->
                    state.value = state.value.copy(loading = true, failed = false)
                    runCatching { pager.refresh(q) }
                        .onSuccess { publish(searched = ProfileQuery.normalize(q).isNotEmpty()) }
                        .onFailure { state.value = ProfileSearchState(failed = true, searched = true) }
                }
            }
        }
    }

    fun setQuery(value: String) { query.value = value }

    fun loadMore() {
        val current = state.value
        if (current.loading || current.endReached) return
        viewModelScope.launch {
            state.value = current.copy(loading = true)
            runCatching { pager.loadMore() }
                .onSuccess { publish(searched = true) }
                .onFailure { state.value = state.value.copy(loading = false, failed = true) }
        }
    }

    private fun publish(searched: Boolean) {
        state.value = ProfileSearchState(pager.results, loading = false, endReached = pager.endReached, searched = searched)
    }
}

/** Results of a profile-name prefix search: avatar, name and rank; tapping opens the read-only profile. */
@Composable
fun StarFallProfileResults(
    query: String,
    onOpen: (uid: String) -> Unit,
    modifier: Modifier = Modifier,
    vm: ProfileSearchViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    LaunchedEffect(query) { vm.setQuery(query) }
    LaunchedEffect(listState, state.results.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last -> if (last >= state.results.size - 4) vm.loadMore() }
    }
    LazyColumn(modifier.fillMaxSize(), state = listState) {
        items(state.results, key = { it.uid }) { hit -> ProfileHitRow(hit) { onOpen(hit.uid) } }
        if (state.searched && !state.loading && !state.failed && state.results.isEmpty()) {
            item(key = "empty") { Hint(R.string.starfall_search_profiles_empty) }
        }
        if (state.failed) item(key = "failed") { Hint(R.string.starfall_search_profiles_error) }
    }
}

@Composable
private fun Hint(res: Int) {
    Text(
        stringResource(res),
        Modifier.fillMaxWidth().padding(StarFallSpacing.xl),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ProfileHitRow(hit: ProfileHit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick).padding(horizontal = StarFallSpacing.lg, vertical = StarFallSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StarFallSpacing.md),
    ) {
        ProfileAvatar(hit.name, hit.avatar, 48.dp)
        Column(Modifier.weight(1f)) {
            Text(hit.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(StarFallSpacing.xs)) {
                RankIcon(hit.rank, size = 16.dp)
                Text(hit.rank.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
