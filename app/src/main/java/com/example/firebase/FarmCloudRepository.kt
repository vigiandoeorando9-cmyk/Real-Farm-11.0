package com.example.firebase

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class CloudSaveUploadPayload(
    val slotIndex: Int,
    val farmName: String,
    val mapId: String,
    val mapGridSize: Int,
    val startingMode: String,
    val day: Int,
    val month: Int,
    val hourOfDay: Double,
    val weather: String,
    val moneyBrl: Double,
    val bankLoanBrl: Double,
    val localRevision: Int,
    val deviceId: String,
    val deviceName: String,
    val checksumSha256: String,
    val requiredModIds: List<String>,
    val compactCellsRle: String,
    val playerMap: Map<String, Any?>,
    val plotsList: List<Map<String, Any?>>,
    val vehiclesList: List<Map<String, Any?>>,
    val implementsList: List<Map<String, Any?>>,
    val buildingsList: List<Map<String, Any?>>,
    val industriesList: List<Map<String, Any?>>,
    val herdsList: List<Map<String, Any?>>,
    val inventoryMap: Map<String, Any?>,
    val contractsList: List<Map<String, Any?>>,
    val activeVehicleUid: String?,
)

sealed interface CloudUploadOutcome {
    data class Success(val newRevision: Long) : CloudUploadOutcome
    data class Conflict(val cloudSave: CloudSaveDoc) : CloudUploadOutcome
}

class FarmCloudRepository(
    private val db: FirebaseFirestore?,
    private val auth: FirebaseAuth?,
) {
    constructor(context: Context) : this(
        db = tryCreateFirestore(context),
        auth = tryGetFirebaseAuth(context),
    )

    companion object {
        private fun tryCreateFirestore(context: Context): FirebaseFirestore? = runCatching {
            val appCtx = context.applicationContext
            if (FirebaseApp.getApps(appCtx).isNotEmpty()) {
                FirebaseFirestore.getInstance(appCtx.getString(R.string.firestore_database_id))
            } else null
        }.getOrNull()

        private fun tryGetFirebaseAuth(context: Context): FirebaseAuth? = runCatching {
            if (FirebaseApp.getApps(context.applicationContext).isNotEmpty()) Firebase.auth else null
        }.getOrNull()
    }

    private fun requireDb(): FirebaseFirestore =
        db ?: throw IllegalStateException("Sincronização em nuvem indisponível no modo offline.")

    private fun requireAuth(): FirebaseAuth =
        auth ?: throw IllegalStateException("Autenticação Firebase indisponível no modo offline.")

    private fun requireUserId(): String {
        return requireAuth().currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun ensureProfilePrivateAndDevice(
        displayName: String,
        avatarStyle: String = "palha",
        activePlatform: String = "MOBILE",
        email: String,
        preferredRegion: String = "BR",
        mobileStorageLimitMb: Long = 8192L,
        deviceId: String,
        deviceName: String,
        devicePlatform: String = "EQUILIBRADO",
        graphicsPreset: String = "EQUILIBRADO",
        appVersion: String = "3.0",
    ) {
        val uid = requireUserId()
        val firestore = requireDb()
        val safeName = displayName.trim().take(48).let { if (it.length >= 2) it else "Produtor BR" }
        val safeEmail = email.trim().ifBlank { "$uid@farmbrasil.local" }.take(160)

        val profileRef = firestore.collection("users").document(uid)
        try {
            val snap = profileRef.get().await()
            if (!snap.exists()) {
                profileRef.set(
                    mapOf(
                        "ownerId" to uid,
                        "displayName" to safeName,
                        "avatarStyle" to avatarStyle,
                        "activePlatform" to activePlatform,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                profileRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "avatarStyle" to avatarStyle,
                        "activePlatform" to activePlatform,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, profileRef.path, auth)
            throw e
        }

        val privateRef = profileRef.collection("private").document("account")
        try {
            val snap = privateRef.get().await()
            if (!snap.exists()) {
                privateRef.set(
                    mapOf(
                        "ownerId" to uid,
                        "email" to safeEmail,
                        "preferredRegion" to preferredRegion.take(48).ifBlank { "BR" },
                        "mobileStorageLimitMb" to mobileStorageLimitMb.coerceIn(512L, 102400L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                privateRef.update(
                    mapOf(
                        "email" to safeEmail,
                        "preferredRegion" to preferredRegion.take(48).ifBlank { "BR" },
                        "mobileStorageLimitMb" to mobileStorageLimitMb.coerceIn(512L, 102400L),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, privateRef.path, auth)
            throw e
        }

        val devRef = profileRef.collection("devices").document(deviceId)
        try {
            val snap = devRef.get().await()
            if (!snap.exists()) {
                devRef.set(
                    mapOf(
                        "ownerId" to uid,
                        "deviceId" to deviceId,
                        "deviceName" to deviceName.trim().take(64).let { if (it.length >= 2) it else "Android" },
                        "platform" to devicePlatform,
                        "graphicsPreset" to graphicsPreset.take(32),
                        "appVersion" to appVersion.take(24),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                devRef.update(
                    mapOf(
                        "deviceName" to deviceName.trim().take(64).let { if (it.length >= 2) it else "Android" },
                        "platform" to devicePlatform,
                        "graphicsPreset" to graphicsPreset.take(32),
                        "appVersion" to appVersion.take(24),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, devRef.path, auth)
            throw e
        }
    }

    suspend fun getUserProfile(targetUid: String): UserProfileDoc? {
        val docRef = requireDb().collection("users").document(targetUid)
        return try {
            docRef.get().await().toObject(
                UserProfileDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            throw e
        }
    }

    suspend fun getPrivateAccount(): UserPrivateAccountDoc? {
        val uid = requireUserId()
        val docRef = requireDb().collection("users").document(uid).collection("private").document("account")
        return try {
            docRef.get().await().toObject(
                UserPrivateAccountDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            throw e
        }
    }

    suspend fun listRegisteredDevices(): List<UserDeviceDoc> {
        val uid = requireUserId()
        val colRef = requireDb().collection("users").document(uid).collection("devices")
        return try {
            colRef.get().await().toObjects(
                UserDeviceDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path, auth)
            throw e
        }
    }

    suspend fun deleteDevice(deviceId: String) {
        val uid = requireUserId()
        val docRef = requireDb().collection("users").document(uid).collection("devices").document(deviceId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    fun observeCloudSaves(uid: String): Flow<List<CloudSaveDoc>> {
        val firestore = db ?: return flowOf(emptyList())
        return flow {
            val colRef = firestore.collection("users").document(uid).collection("cloudSaves")
            emitAll(
                colRef.snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            CloudSaveDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val toLog = firestoreError ?: (error as? Exception)
                        if (toLog != null && auth?.currentUser?.uid == uid) {
                            handleFirestoreError(toLog, OperationType.LIST, colRef.path, auth)
                        }
                        throw error
                    }
            )
        }
    }

    suspend fun listCloudSaves(): List<CloudSaveDoc> {
        val uid = requireUserId()
        val colRef = requireDb().collection("users").document(uid).collection("cloudSaves")
        return try {
            colRef.get().await().toObjects(
                CloudSaveDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path, auth)
            throw e
        }
    }

    suspend fun getCloudSave(slotIndex: Int): CloudSaveDoc? {
        val uid = requireUserId()
        val docRef = requireDb().collection("users").document(uid).collection("cloudSaves").document("slot_$slotIndex")
        return try {
            docRef.get().await().toObject(
                CloudSaveDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            throw e
        }
    }

    suspend fun uploadCloudSave(
        payload: CloudSaveUploadPayload,
        forceOverwrite: Boolean = false,
    ): CloudUploadOutcome {
        val uid = requireUserId()
        val slotId = "slot_${payload.slotIndex.coerceIn(0, 19)}"
        val docRef = requireDb().collection("users").document(uid).collection("cloudSaves").document(slotId)

        try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists() && !forceOverwrite) {
                val existingDoc = existingSnap.toObject(
                    CloudSaveDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existingDoc != null &&
                    existingDoc.revision > payload.localRevision &&
                    existingDoc.deviceId != payload.deviceId
                ) {
                    return CloudUploadOutcome.Conflict(existingDoc)
                }
            }

            val existingRev = existingSnap.getLong("revision") ?: 0L
            val nextRevision = maxOf(payload.localRevision + 1L, existingRev + 1L)
            val cleanRle = payload.compactCellsRle.ifBlank { "P64:0" }.take(449000)

            val commonFields = mutableMapOf<String, Any>(
                "ownerId" to uid,
                "slotIndex" to payload.slotIndex.coerceIn(0, 19).toLong(),
                "farmName" to payload.farmName.trim().take(64).ifBlank { "Fazenda Brasil" },
                "mapId" to payload.mapId.trim().take(64).ifBlank { "map_pr" },
                "mapGridSize" to payload.mapGridSize.coerceIn(32, 256).toLong(),
                "startingMode" to payload.startingMode.trim().take(48).ifBlank { "Novo Fazendeiro" },
                "day" to payload.day.coerceIn(1, 100000).toLong(),
                "month" to payload.month.coerceIn(1, 12).toLong(),
                "hourOfDay" to payload.hourOfDay.coerceIn(0.0, 24.0),
                "weather" to payload.weather.trim().take(32).ifBlank { "ENSOLARADO" },
                "moneyBrl" to payload.moneyBrl,
                "bankLoanBrl" to payload.bankLoanBrl,
                "revision" to nextRevision,
                "deviceId" to payload.deviceId,
                "deviceName" to payload.deviceName.trim().take(64).let { if (it.length >= 2) it else "Android" },
                "checksumSha256" to payload.checksumSha256.take(128).let { if (it.length >= 8) it else "fb000000" },
                "requiredModIds" to payload.requiredModIds.take(250),
                "payloadSizeBytes" to (cleanRle.length + 2048).coerceIn(0, 950000).toLong(),
                "compactCellsRle" to cleanRle,
                "playerMap" to payload.playerMap,
                "plotsList" to payload.plotsList.take(200),
                "vehiclesList" to payload.vehiclesList.take(150),
                "implementsList" to payload.implementsList.take(200),
                "buildingsList" to payload.buildingsList.take(300),
                "industriesList" to payload.industriesList.take(60),
                "herdsList" to payload.herdsList.take(20),
                "inventoryMap" to payload.inventoryMap,
                "contractsList" to payload.contractsList.take(80),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (!payload.activeVehicleUid.isNullOrBlank()) {
                commonFields["activeVehicleUid"] = payload.activeVehicleUid.take(128)
            }

            if (!existingSnap.exists()) {
                commonFields["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(commonFields).await()
            } else {
                val updateFields = commonFields.toMutableMap().apply {
                    remove("ownerId")
                    remove("slotIndex")
                }
                docRef.update(updateFields).await()
            }

            return CloudUploadOutcome.Success(nextRevision)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteCloudSave(slotIndex: Int) {
        val uid = requireUserId()
        val docRef = requireDb().collection("users").document(uid).collection("cloudSaves").document("slot_$slotIndex")
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }
}
