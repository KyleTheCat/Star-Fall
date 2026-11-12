/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.online

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.metrolist.music.starfall.profile.ProfileIdentity
import com.metrolist.music.starfall.profile.ProfileHit
import com.metrolist.music.starfall.profile.ProfileLookup
import com.metrolist.music.starfall.profile.ProfilePage
import com.metrolist.music.starfall.profile.ProfileQuery
import com.metrolist.music.starfall.profile.ProfileStore
import com.metrolist.music.starfall.profile.RemoteNameState
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
import kotlinx.coroutines.flow.first
import timber.log.Timber
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/** Leaderboard weeks run Monday 00:00 UTC so every listener shares one window. */
object WeekClock {
    fun weekKey(now: Instant = Instant.now()): String =
        LocalDate.ofInstant(now, ZoneOffset.UTC).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
}

data class BoardEntry(val uid: String, val name: String, val avatar: String?, val hours: Double, val rank: Rank)
data class Board(val top: List<BoardEntry>, val me: BoardEntry?, val myPosition: Int?)

/** Why a sign-in attempt did not complete; mapped to friendly text in the UI. */
enum class SignInProblem { NotConfigured, Cancelled, NoAccount, Network, Other }

class SignInException(val problem: SignInProblem, cause: Throwable? = null) : Exception(problem.name, cause)

@Singleton
class StarFallOnline @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val profileStore: ProfileStore,
) {
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
        val name = effectiveName(settings.customName, user)
        val data = mapOf(
            "visible" to true,
            "displayName" to name,
            "nameLower" to ProfileQuery.normalize(name),
            "avatar" to user.photoUrl?.toString(),
            "totalSeconds" to totalSeconds,
            "bio" to settings.bio,
            "genres" to settings.genres,
            "playlists" to settings.playlists.map { mapOf("id" to it.id, "title" to it.title, "thumbnail" to it.thumbnail) },
        )
        profileDoc(user.uid).set(data).await()
    }

    private fun effectiveName(custom: String?, user: FirebaseUser) = custom ?: user.displayName ?: "Listener"

    /**
     * Stores the custom name in the private account document. Firestore rules reject it unless the
     * stored lastNameChangeAt is at least 7 days old, so the lock holds even for a modified client.
     */
    suspend fun saveCustomName(name: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not signed in")
        userDoc(user.uid).set(
            mapOf("customName" to name, "lastNameChangeAt" to FieldValue.serverTimestamp()),
            SetOptions.merge(),
        ).await()
        // The leaderboard entry may not exist yet this week; that is fine.
        runCatching { entries().document(user.uid).update("name", name).await() }
    }

    suspend fun fetchNameState(): RemoteNameState? {
        val uid = auth.currentUser?.uid ?: return null
        val snap = userDoc(uid).get().await()
        val changedAt = snap.getTimestamp("lastNameChangeAt")?.toDate()?.time ?: 0L
        return RemoteNameState(snap.getString("customName"), changedAt)
    }

    /** True when Firestore refused a write because of the rules (e.g. the name lock). */
    fun isDenied(t: Throwable) =
        (t as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED

    /** Published, visible profiles whose lowercase name starts with [prefix]; pass the previous cursor for the next page. */
    suspend fun searchProfiles(prefix: String, after: Any?, limit: Int): ProfilePage {
        var query = db.collection("profiles")
            .whereEqualTo("visible", true)
            .orderBy("nameLower")
            .startAt(prefix)
            .endAt(ProfileQuery.upperBound(prefix))
            .limit(limit.toLong())
        // startAfter replaces the startAt bound, so later pages continue from the last document.
        if (after is DocumentSnapshot) query = query.startAfter(after)
        val docs = query.get().await().documents
        val hits = docs.map {
            ProfileHit(
                uid = it.id,
                name = it.getString("displayName") ?: "Listener",
                avatar = it.getString("avatar"),
                totalSeconds = it.getLong("totalSeconds") ?: 0L,
            )
        }
        return ProfilePage(hits, docs.lastOrNull(), endReached = docs.size < limit)
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
        if (!available) throw SignInException(SignInProblem.NotConfigured)
        val token = requestIdToken(activityContext)
        val result = auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        result.user?.let { saveProfile(it) }
        Unit
    }.onFailure { Timber.e(it, "StarFall sign-in failed") }
        .recoverCatching { throw SignInException(classify(it), it) }

    private fun classify(t: Throwable): SignInProblem = when {
        t is SignInException -> t.problem
        t is GetCredentialCancellationException -> SignInProblem.Cancelled
        t is NoCredentialException -> SignInProblem.NoAccount
        t is FirebaseNetworkException || t is java.io.IOException -> SignInProblem.Network
        else -> SignInProblem.Other
    }

    private suspend fun requestIdToken(activityContext: Context): String {
        val id = ctx.resources.getIdentifier("default_web_client_id", "string", ctx.packageName)
        if (id == 0) throw SignInException(SignInProblem.NotConfigured)
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(ctx.getString(id))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    /** The email stays in the private users document; it is never copied to the public profile. */
    private suspend fun saveProfile(user: FirebaseUser) {
        val profile = mapOf("googleName" to user.displayName, "avatar" to user.photoUrl?.toString(), "email" to user.email)
        userDoc(user.uid).set(profile, SetOptions.merge()).await()
    }

    suspend fun signOut() {
        auth.signOut()
        optInCache = null
        pendingSeconds = 0
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

    private val listeningLock = Mutex()
    private var pendingSeconds = 0L
    private var optInCache: Boolean? = null

    /**
     * Accumulates listening time and writes it in one batch per [WRITE_EVERY_SECONDS] (or on [flushListening]),
     * instead of an increment, a read and two more writes for every 30 seconds of playback.
     */
    suspend fun addListening(seconds: Long, totalSeconds: Long) {
        if (!available) return
        listeningLock.withLock {
            pendingSeconds += seconds
            if (pendingSeconds >= WRITE_EVERY_SECONDS) writePending(totalSeconds)
        }
    }

    suspend fun flushListening(totalSeconds: Long) {
        if (!available) return
        listeningLock.withLock { if (pendingSeconds > 0) writePending(totalSeconds) }
    }

    private suspend fun writePending(totalSeconds: Long) {
        val user = auth.currentUser ?: run { pendingSeconds = 0; return }
        val seconds = pendingSeconds
        runCatching {
            userDoc(user.uid).set(mapOf("totalSeconds" to FieldValue.increment(seconds)), SetOptions.merge()).await()
            val optIn = optInCache ?: (userDoc(user.uid).get().await().getBoolean("optIn") == true).also { optInCache = it }
            if (optIn) writeWeekly(user, seconds, totalSeconds)
            // Keeps the published hours fresh; fails harmlessly when the profile is hidden/unpublished.
            runCatching { profileDoc(user.uid).update("totalSeconds", FieldValue.increment(seconds)).await() }
        }.onSuccess { pendingSeconds = 0 }.onFailure { Timber.w(it, "StarFall listening sync deferred") }
    }

    private suspend fun writeWeekly(user: FirebaseUser, seconds: Long, total: Long) {
        val name = effectiveName(profileStore.settings.first().customName, user)
        val data = mapOf(
            "seconds" to FieldValue.increment(seconds),
            "name" to name,
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
        optInCache = optIn
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
        rank = Rank.entries.getOrElse((d.getLong("rankTier") ?: 1L).toInt() - 1) { Rank.STARDUST },
    )

    val myUid: String? get() = if (available) auth.currentUser?.uid else null

    private companion object {
        const val WRITE_EVERY_SECONDS = 300L
    }
}
