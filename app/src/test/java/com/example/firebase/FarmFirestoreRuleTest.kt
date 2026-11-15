package com.example.firebase

import com.example.base.FirestoreEmulatorTestBase
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FarmFirestoreRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureProfilePrivateAndDevice_authenticatedOwner_writesAndReadsDocs() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FarmCloudRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureProfilePrivateAndDevice(
                displayName = "Alice Fazendeira",
                avatarStyle = "palha",
                activePlatform = "MOBILE",
                email = ALICE_EMAIL,
                preferredRegion = "sorriso_mt",
                mobileStorageLimitMb = 4096L,
                deviceId = "dev_pixel_01",
                deviceName = "Pixel 9 Agro",
                devicePlatform = "CELULAR",
                graphicsPreset = "MEDIO",
                appVersion = "3.0"
            )
        }

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getUserProfile(uid) }
        assertThat(profile).isNotNull()
        assertThat(profile?.displayName).isEqualTo("Alice Fazendeira")

        val priv = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getPrivateAccount() }
        assertThat(priv?.email).isEqualTo(ALICE_EMAIL)

        val devices = withTimeout(DEFAULT_TIMEOUT_MS) { repo.listRegisteredDevices() }
        assertThat(devices.map { it.deviceId }).contains("dev_pixel_01")
    }

    @Test
    fun uploadAndObserveCloudSave_authenticatedOwner_emitsRealtimeUpdate() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FarmCloudRepository(firestore, auth)

        val outcome = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.uploadCloudSave(
                CloudSaveUploadPayload(
                    slotIndex = 0,
                    farmName = "Fazenda Cerrado",
                    mapId = "sorriso_mt",
                    mapGridSize = 64,
                    startingMode = "Novo Fazendeiro",
                    day = 5,
                    month = 2,
                    hourOfDay = 10.5,
                    weather = "ENSOLARADO",
                    moneyBrl = 450000.0,
                    bankLoanBrl = 0.0,
                    localRevision = 0,
                    deviceId = "dev_pixel_01",
                    deviceName = "Pixel 9 Agro",
                    checksumSha256 = "abcd1234efgh5678",
                    requiredModIds = listOf("mod_valtra"),
                    compactCellsRle = "P64:0",
                    playerMap = mapOf("x" to 12.0, "z" to -6.0),
                    plotsList = listOf(mapOf("id" to 1, "owned" to true)),
                    vehiclesList = listOf(mapOf("uid" to "veh_1", "defId" to "valtra_bh180")),
                    implementsList = emptyList(),
                    buildingsList = emptyList(),
                    industriesList = emptyList(),
                    herdsList = emptyList(),
                    inventoryMap = mapOf("soja" to 1500.0),
                    contractsList = emptyList(),
                    activeVehicleUid = null
                ),
                forceOverwrite = true
            )
        }
        assertThat(outcome).isInstanceOf(CloudUploadOutcome.Success::class.java)

        val saves = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCloudSaves(uid).first { list -> list.any { it.farmName == "Fazenda Cerrado" } }
        }
        assertThat(saves).isNotEmpty()
        assertThat(saves.first().day).isEqualTo(5L)
    }

    @Test
    fun cloudSave_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FarmCloudRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureProfilePrivateAndDevice(
                displayName = "Alice Agro",
                email = ALICE_EMAIL,
                deviceId = "dev_alice_01",
                deviceName = "Alice Phone"
            )
            aliceRepo.uploadCloudSave(
                CloudSaveUploadPayload(
                    slotIndex = 2,
                    farmName = "Privada Alice",
                    mapId = "map_pr",
                    mapGridSize = 64,
                    startingMode = "Novo Fazendeiro",
                    day = 2,
                    month = 1,
                    hourOfDay = 8.0,
                    weather = "ENSOLARADO",
                    moneyBrl = 100000.0,
                    bankLoanBrl = 0.0,
                    localRevision = 0,
                    deviceId = "dev_alice_01",
                    deviceName = "Alice Phone",
                    checksumSha256 = "1234567890abcdef",
                    requiredModIds = emptyList(),
                    compactCellsRle = "P64:0",
                    playerMap = mapOf("x" to 0.0, "z" to 0.0),
                    plotsList = emptyList(),
                    vehiclesList = emptyList(),
                    implementsList = emptyList(),
                    buildingsList = emptyList(),
                    industriesList = emptyList(),
                    herdsList = emptyList(),
                    inventoryMap = emptyMap(),
                    contractsList = emptyList(),
                    activeVehicleUid = null
                ),
                forceOverwrite = true
            )
        }

        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("cloudSaves").document("slot_2").get().await()
            }
            fail("Expected PERMISSION_DENIED reading Alice's cloudSave as Bob")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) { "Expected FirebaseFirestoreException, got $e" }
            assertThat(fsEx.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
        }
    }

    @Test
    fun observeCloudSaves_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = FarmCloudRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeCloudSaves("alice_fake_uid").first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) { "Expected FirebaseFirestoreException, got $e" }
            assertThat(fsEx.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
        }
    }

    @Test
    fun multiplayerRoom_lifecycle_hostApprovesGuestAndStreamsChat() = runBlocking {
        val roomCode = "ROOM_${UUID.randomUUID().toString().take(8).uppercase()}"
        signInTestUser(ALICE_EMAIL)
        val aliceMp = MultiplayerRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceMp.createOrHostRoom(
                roomCode = roomCode,
                hostName = "Alice Host",
                mapId = "sorriso_mt"
            )
        }

        val bobUid = signInTestUser(BOB_EMAIL)
        val bobMp = MultiplayerRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobMp.joinRoomAsGuest(roomCode = roomCode, playerName = "Bob Guest")
        }

        // Bob queries open rooms using status filter aligned with security rules
        val openRooms = withTimeout(FLOW_TIMEOUT_MS) {
            bobMp.observeOpenRooms().first { rooms -> rooms.any { it.roomCode == roomCode } }
        }
        assertThat(openRooms.map { it.roomCode }).contains(roomCode)

        // Alice approves Bob
        signInTestUser(ALICE_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceMp.approveMember(roomCode = roomCode, memberUid = bobUid, approve = true)
        }

        // Bob updates telemetry and sends chat
        signInTestUser(BOB_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobMp.updateLocalTelemetry(
                roomCode = roomCode,
                playerName = "Bob Operator",
                x = 18.5,
                z = -9.2,
                vehicleDefId = "valtra_bh180",
                headingDeg = 45.0
            )
            bobMp.sendChatMessage(
                roomCode = roomCode,
                senderName = "Bob Operator",
                text = "Plantei o talhão 1!"
            )
        }

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            bobMp.observeRoomMessages(roomCode).first { list -> list.any { it.text.contains("Plantei") } }
        }
        assertThat(messages).isNotEmpty()

        // Host closes room
        signInTestUser(ALICE_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceMp.closeRoom(roomCode)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_farm@test.com"
        const val BOB_EMAIL = "bob_farm@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
