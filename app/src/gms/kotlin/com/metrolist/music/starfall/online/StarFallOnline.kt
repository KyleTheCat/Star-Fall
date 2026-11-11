/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.online

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.metrolist.music.starfall.profile.ProfileIdentity
import com.metrolist.music.starfall.profile.ProfileLookup
import com.metrolist.music.starfall.profile.ProfileSettings
import com.metrolist.music.starfall.profile.PublicProfile
import com.metrolist.music.starfall.profile.ShowcaseItem
import com.metrolist.music.starfall.rank.Rank
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** Leaderboard weeks run Monday 00:00 UTC so every listener shares one window. */
object WeekClock {
    fun weekKey(now: Instant = Instant.now()): String =
        LocalDate.ofInstant(now, ZoneOffset.UTC).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
}

data class BoardEntry(val uid: String, val name: String, val avatar: String?, val hours: Double, val rankIcon: String)
data class Board(val top: List<BoardEntry>, val me: BoardEntry?, val myPosition: Int?)

@Singleton
class StarFallOnline @Inject constructor(@ApplicationContext private val ctx: Context) {
    val available: Boolean get() = runCatching { FirebaseApp.getApps(ctx).isNotEmpty() }.getOrDefault(false)
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun entries() = db.collection("weekly").document(WeekClock.weekKey()).collection("entries")

    val user: Flow<FirebaseUser?> = callbackFlow {
        if (!available) {
            trySend(null)
            awaitClose { }
        } else {
            val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }
    }

    val identity: Flow<ProfileIdentity?> = user.map { u ->
        u?.let { ProfileIdentity(it.uid, it.displayName ?: "Listener", it.photoUrl?.toString()) }
    }

    private fun profileDoc(uid: String) = db.collection("profiles").document(uid)

    /**
     * Publishes the public copy of the profile. It carries no rank and no email, so
     * rules can safely allow any signed-in user to read documents that exist.
     */
    suspend fun publishProfile(settings: ProfileSettings, totalSeconds: Long) {
        val user = auth.currentUser ?: return
        val data = mapOf(
            "visible" to true,
            "displayName" to (user.displayName ?: "Listener"),
            "avatar" to user.photoUrl?.toString(),
            "totalSeconds" to totalSeconds,
            "bio" to settings.bio,
            "genres" to settings.genres,
            "playlists" to settings.playlists.map { mapOf("id" to it.id, "title" to it.title, "thumbnail" to it.thumbnail) },
        )
        profileDoc(user.uid).set(data).await()
    }

    /** Hiding a profile deletes the published document, so there is nothing left to visit. */
    suspend fun unpublishProfile() {
        val user = auth.currentUser ?: return
        profileDoc(user.uid).delete().await()
    }

    suspend fun loadProfile(uid: String): ProfileLookup {
        if (!available) return ProfileLookup.Unavailable
        val snap = profileDoc(uid).get().await()
        if (!snap.exists() || snap.getBoolean("visible") != true) return ProfileLookup.Private
        @Suppress("UNCHECKED_CAST")
        val playlists = (snap.get("playlists") as? List<Map<String, Any?>>).orEmpty().mapNotNull {
            val id = it["id"] as? String ?: return@mapNotNull null
            ShowcaseItem(id, it["title"] as? String ?: "Playlist", it["thumbnail"] as? String)
        }
        @Suppress("UNCHECKED_CAST")
        val genres = (snap.get("genres") as? List<String>).orEmpty()
        return ProfileLookup.Found(
            PublicProfile(
                uid = uid,
                name = snap.getString("displayName") ?: "Listener",
                avatar = snap.getString("avatar"),
                totalSeconds = snap.getLong("totalSeconds") ?: 0L,
                bio = snap.getString("bio").orEmpty(),
                genres = genres,
                playlists = playlists,
            ),
        )
    }

    suspend fun signIn(activityContext: Context): Result<Unit> = runCatching {
        check(available) { "Firebase is not configured" }
        val token = requestIdToken(activityContext)
        val result = auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        result.user?.let { saveProfile(it) }
    }

    private suspend fun requestIdToken(activityContext: Context): String {
        val id = ctx.resources.getIdentifier("default_web_client_id", "string", ctx.packageName)
        check(id != 0) { "default_web_client_id is missing (google-services.json not applied)" }
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(ctx.getString(id))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    private suspend fun saveProfile(user: FirebaseUser) {
        val profile = mapOf("displayName" to user.displayName, "avatar" to user.photoUrl?.toString(), "email" to user.email)
        userDoc(user.uid).set(profile, SetOptions.merge()).await()
    }

    suspend fun signOut() {
        auth.signOut()
        CredentialManager.create(ctx).clearCredentialState(ClearCredentialStateRequest())
    }

    /** Cloud and device keep whichever total is larger, so rank follows the account. */
    suspend fun mergeTotals(localSeconds: Long): Long {
        val uid = auth.currentUser?.uid ?: return localSeconds
        val ref = userDoc(uid)
        val cloud = ref.get().await().getLong("totalSeconds") ?: 0L
        val merged = maxOf(cloud, localSeconds)
        if (merged != cloud) ref.set(mapOf("totalSeconds" to merged), SetOptions.merge()).await()
        return merged
    }

    suspend fun addListening(seconds: Long) {
        if (!available) return
        val user = auth.currentUser ?: return
        val ref = userDoc(user.uid)
        ref.set(mapOf("totalSeconds" to FieldValue.increment(seconds)), SetOptions.merge()).await()
        val snap = ref.get().await()
        if (snap.getBoolean("optIn") == true) writeWeekly(user, seconds, snap.getLong("totalSeconds") ?: 0L)
        // Keeps the published hours fresh; fails harmlessly when the profile is hidden/unpublished.
        runCatching { profileDoc(user.uid).update("totalSeconds", FieldValue.increment(seconds)).await() }
    }

    private suspend fun writeWeekly(user: FirebaseUser, seconds: Long, total: Long) {
        val data = mapOf(
            "seconds" to FieldValue.increment(seconds),
            "name" to (user.displayName ?: "Listener"),
            "avatar" to user.photoUrl?.toString(),
            "rankTier" to Rank.forSeconds(total).tier,
        )
        entries().document(user.uid).set(data, SetOptions.merge()).await()
    }

    fun observeOptIn(uid: String): Flow<Boolean> = callbackFlow {
        val reg = userDoc(uid).addSnapshotListener { snap, _ -> trySend(snap?.getBoolean("optIn") == true) }
        awaitClose { reg.remove() }
    }

    suspend fun setOptIn(optIn: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        userDoc(uid).set(mapOf("optIn" to optIn), SetOptions.merge()).await()
    }

    suspend fun loadBoard(limit: Long = 50): Board {
        val top = entries().orderBy("seconds", Query.Direction.DESCENDING).limit(limit).get().await()
            .documents.map(::toEntry)
        val uid = auth.currentUser?.uid ?: return Board(top, null, null)
        val mine = entries().document(uid).get().await().takeIf { it.exists() } ?: return Board(top, null, null)
        val ahead = entries().whereGreaterThan("seconds", mine.getLong("seconds") ?: 0L)
            .count().get(AggregateSource.SERVER).await().count
        return Board(top, toEntry(mine), ahead.toInt() + 1)
    }

    private fun toEntry(d: DocumentSnapshot) = BoardEntry(
        uid = d.id,
        name = d.getString("name") ?: "Listener",
        avatar = d.getString("avatar"),
        hours = (d.getLong("seconds") ?: 0L) / 3600.0,
        rankIcon = Rank.entries.getOrElse((d.getLong("rankTier") ?: 1L).toInt() - 1) { Rank.STARDUST }.icon,
    )

    val myUid: String? get() = if (available) auth.currentUser?.uid else null
}
