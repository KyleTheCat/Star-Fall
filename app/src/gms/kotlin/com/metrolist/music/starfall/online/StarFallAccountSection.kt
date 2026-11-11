/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.online

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.starfall.rank.RankStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StarFallAccountViewModel @Inject constructor(
    private val online: StarFallOnline,
    private val store: RankStore,
) : ViewModel() {
    val available = online.available
    val user = online.user.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val optIn = online.user
        .flatMapLatest { u -> if (u == null) flowOf(false) else online.observeOptIn(u.uid) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val error = MutableStateFlow<String?>(null)

    fun signIn(context: Context) {
        viewModelScope.launch {
            error.value = null
            online.signIn(context)
                .onSuccess { store.raiseTo(online.mergeTotals(store.totalSeconds.first())) }
                .onFailure { error.value = it.message ?: it.javaClass.simpleName }
        }
    }

    fun signOut() { viewModelScope.launch { online.signOut() } }
    fun setOptIn(value: Boolean) { viewModelScope.launch { online.setOptIn(value) } }
}

@Composable
fun StarFallAccountSection(navController: NavController, vm: StarFallAccountViewModel = hiltViewModel()) {
    if (!vm.available) {
        Text(stringResource(R.string.starfall_online_unavailable))
        return
    }
    val user by vm.user.collectAsState()
    val optIn by vm.optIn.collectAsState()
    val error by vm.error.collectAsState()
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val current = user
        if (current == null) {
            StarFallSignInCard(
                label = stringResource(R.string.starfall_sign_in),
                prompt = stringResource(R.string.starfall_sign_in_prompt),
                onClick = { vm.signIn(context) },
            )
        } else {
            Text(current.displayName ?: "Listener", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.starfall_join_leaderboard))
                Switch(checked = optIn, onCheckedChange = vm::setOptIn)
            }
            Button(onClick = { navController.navigate("starfall/leaderboard") }) {
                Text(stringResource(R.string.starfall_view_leaderboard))
            }
            OutlinedButton(onClick = vm::signOut) { Text(stringResource(R.string.starfall_sign_out)) }
        }
        error?.let {
            Text("${stringResource(R.string.starfall_sign_in_failed)}: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
