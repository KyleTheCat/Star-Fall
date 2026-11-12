/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.online

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.starfall.rank.RankStore
import com.metrolist.music.starfall.ui.StarFallSpacing
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
    val problem = MutableStateFlow<SignInProblem?>(null)

    fun signIn(context: Context) {
        viewModelScope.launch {
            problem.value = null
            online.signIn(context)
                .onSuccess { store.raiseTo(online.mergeTotals(store.totalSeconds.first())) }
                .onFailure { problem.value = (it as? SignInException)?.problem ?: SignInProblem.Other }
        }
    }

    fun signOut() { viewModelScope.launch { online.signOut() } }
    fun setOptIn(value: Boolean) { viewModelScope.launch { online.setOptIn(value) } }
}

@Composable
fun StarFallAccountSection(navController: NavController, vm: StarFallAccountViewModel = hiltViewModel()) {
    if (!vm.available) {
        Text(
            stringResource(R.string.starfall_sign_in_not_configured),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val user by vm.user.collectAsState()
    val optIn by vm.optIn.collectAsState()
    val problem by vm.problem.collectAsState()
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(StarFallSpacing.md)) {
        if (user == null) {
            StarFallSignInCard(
                label = stringResource(R.string.starfall_sign_in),
                prompt = stringResource(R.string.starfall_sign_in_prompt),
                onClick = { vm.signIn(context) },
            )
            // A dismissed account picker is not an error worth showing.
            problemText(problem)?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StarFallSpacing.md),
            ) {
                Text(stringResource(R.string.starfall_join_leaderboard), Modifier.weight(1f))
                Switch(checked = optIn, onCheckedChange = vm::setOptIn)
            }
            TextButton(onClick = vm::signOut, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.starfall_sign_out)) }
        }
    }
}

@Composable
private fun problemText(problem: SignInProblem?): String? = when (problem) {
    null, SignInProblem.Cancelled -> null
    SignInProblem.NotConfigured -> stringResource(R.string.starfall_sign_in_not_configured)
    SignInProblem.Network -> stringResource(R.string.starfall_sign_in_error_network)
    SignInProblem.NoAccount -> stringResource(R.string.starfall_sign_in_error_no_account)
    SignInProblem.Other -> stringResource(R.string.starfall_sign_in_error_generic)
}
