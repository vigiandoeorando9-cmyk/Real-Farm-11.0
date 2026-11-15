package com.example.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfileDoc(
    val ownerId: String = "",
    val displayName: String = "",
    val avatarStyle: String = "palha",
    val activePlatform: String = "MOBILE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class UserPrivateAccountDoc(
    val ownerId: String = "",
    val email: String = "",
    val preferredRegion: String? = null,
    val mobileStorageLimitMb: Long = 8192L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class UserDeviceDoc(
    val ownerId: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val platform: String = "EQUILIBRADO",
    val graphicsPreset: String? = "EQUILIBRADO",
    val appVersion: String = "3.0",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CloudSaveDoc(
    val ownerId: String = "",
    val slotIndex: Long = 0L,
    val farmName: String = "",
    val mapId: String = "",
    val mapGridSize: Long = 64L,
    val startingMode: String = "",
    val day: Long = 1L,
    val month: Long = 1L,
    val hourOfDay: Double = 8.0,
    val weather: String = "ENSOLARADO",
    val moneyBrl: Double = 0.0,
    val bankLoanBrl: Double = 0.0,
    val revision: Long = 1L,
    val deviceId: String = "",
    val deviceName: String = "",
    val checksumSha256: String = "",
    val requiredModIds: List<String> = emptyList(),
    val payloadSizeBytes: Long = 0L,
    val compactCellsRle: String = "",
    val playerMap: Map<String, Any?> = emptyMap(),
    val plotsList: List<Map<String, Any?>> = emptyList(),
    val vehiclesList: List<Map<String, Any?>> = emptyList(),
    val implementsList: List<Map<String, Any?>> = emptyList(),
    val buildingsList: List<Map<String, Any?>> = emptyList(),
    val industriesList: List<Map<String, Any?>> = emptyList(),
    val herdsList: List<Map<String, Any?>> = emptyList(),
    val inventoryMap: Map<String, Any?> = emptyMap(),
    val contractsList: List<Map<String, Any?>> = emptyList(),
    val activeVehicleUid: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class MultiplayerRoomDoc(
    val roomCode: String = "",
    val hostUid: String = "",
    val hostName: String = "",
    val mapId: String = "",
    val gameVersion: String = "3.0",
    val sharedFarmMode: Boolean = true,
    val pauseOnDisconnect: Boolean = true,
    val status: String = "WAITING",
    val requiredModIds: List<String> = emptyList(),
    val memberCount: Long = 1L,
    val maxPlayers: Long = 8L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class RoomMemberDoc(
    val memberUid: String = "",
    val roomCode: String = "",
    val playerName: String = "",
    val x: Double = 0.0,
    val z: Double = 0.0,
    val vehicleDefId: String? = null,
    val headingDeg: Double = 0.0,
    val pitchDeg: Double? = null,
    val rollDeg: Double? = null,
    val steerAngleDeg: Double? = null,
    val wheelSpinDeg: Double? = null,
    val suspensionOffsetY: Double? = null,
    val isApproved: Boolean = false,
    val role: String = "GUEST",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class RoomChatDoc(
    @DocumentId val uid: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val roomCode: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)
