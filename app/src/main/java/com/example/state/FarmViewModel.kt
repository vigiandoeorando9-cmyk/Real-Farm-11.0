package com.example.state

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AmbientAudioEngine
import com.example.audio.VehicleAudioSnapshot
import com.example.auth.FarmAccountManager
import com.example.auth.FarmAccountResult
import com.example.auth.GoogleAuthManager
import com.example.domain.AccountSyncStatus
import com.example.domain.ACCESSORY_OPTIONS
import com.example.domain.ALL_CROPS
import com.example.domain.ANIMAL_META
import com.example.domain.AnimalSpecies
import com.example.domain.AppRoute
import com.example.domain.AppRoute.Gameplay
import com.example.domain.AppRoute.MainMenu
import com.example.domain.BuildableStructureDef
import com.example.domain.BuildingType
import com.example.domain.CAB_OPTIONS
import com.example.domain.CHARACTER_PRESETS
import com.example.domain.CameraMode
import com.example.domain.CloudSaveConflictState
import com.example.domain.CloudSaveSummary
import com.example.domain.CustomModParseResult
import com.example.domain.DEFAULT_PLAYER_APPEARANCE
import com.example.domain.ENGINE_MODELS
import com.example.domain.ENGINE_UPGRADES
import com.example.domain.EXHAUST_OPTIONS
import com.example.domain.EstablishmentDef
import com.example.domain.FarmAccountUser
import com.example.domain.GRAPHICS_PRESET_META
import com.example.domain.GameSettings
import com.example.domain.GraphicsConfig
import com.example.domain.GraphicsPreset
import com.example.domain.HEADLIGHT_OPTIONS
import com.example.domain.HudLayoutConfig
import com.example.domain.HubPackEntity
import com.example.domain.INDUSTRY_CHAINS
import com.example.domain.LandUnit
import com.example.domain.LedgerEntry
import com.example.domain.ManualTool
import com.example.domain.ModDownloadProgress
import com.example.domain.ModValidationStatus
import com.example.domain.PAINT_PALETTE
import com.example.domain.PlayerAppearanceConfig
import com.example.domain.RIM_COLORS
import com.example.domain.StorageQuotaConfig
import com.example.domain.TURBO_OPTIONS
import com.example.domain.WEATHER_META
import com.example.domain.WHEEL_OPTIONS
import com.example.domain.WorkshopComponent
import com.example.domain.WorldState
import com.example.domain.detectRecommendedGraphicsPreset
import com.example.domain.getVehicleDef
import com.example.domain.validateAndParseCustomModManifest
import com.example.firebase.CloudUploadOutcome
import com.example.firebase.FarmCloudRepository
import com.example.firebase.MultiplayerRepository
import com.example.firebase.MultiplayerRoomDoc
import com.example.firebase.RoomChatDoc
import com.example.firebase.RoomMemberDoc
import com.example.simulation.ControlInput
import com.example.simulation.EMPTY_CONTROL_INPUT
import com.example.simulation.HireOutsourcingParams
import com.example.simulation.WorkshopQuote
import com.example.simulation.acceptOrCompleteContractInWorld
import com.example.simulation.advanceDay
import com.example.simulation.applyContourProtectionInWorld
import com.example.simulation.applyLimeToPlotInWorld
import com.example.simulation.assignEmployeeInWorld
import com.example.simulation.buyImplementInWorld
import com.example.simulation.buyInputStockInWorld
import com.example.simulation.buyVehicleInWorld
import com.example.simulation.createNewWorld
import com.example.simulation.cycleWeather
import com.example.simulation.engageTowRescueInWorld
import com.example.simulation.fireEmployeeInWorld
import com.example.simulation.formatBrl
import com.example.simulation.hireEmployeeInWorld
import com.example.simulation.hireOutsourcingServiceInWorld
import com.example.simulation.inspectVehicle
import com.example.simulation.irrigatePlotInWorld
import com.example.simulation.leasePlotWithTermInWorld
import com.example.simulation.mergeAdjacentPlotsInWorld
import com.example.simulation.performManualAction
import com.example.simulation.performPreventiveMaintenance
import com.example.simulation.placeAuctionBidInWorld
import com.example.simulation.placeBuildingInWorld
import com.example.simulation.rateOutsourcingContractInWorld
import com.example.simulation.refillImplementReservoirInWorld
import com.example.simulation.refillSprayerInWorld
import com.example.simulation.refuelVehicleInWorld
import com.example.simulation.repairVehicleComponent
import com.example.simulation.repairVehicleFull
import com.example.simulation.repayBankLoan
import com.example.simulation.resolveOutsourcingIncidentInWorld
import com.example.simulation.sellAnimalHeadInWorld
import com.example.simulation.sellPlotInWorld
import com.example.simulation.sellProductAtWeighbridge
import com.example.simulation.sellVehicleInWorld
import com.example.simulation.setGpsPoint
import com.example.simulation.sprayPlotPestsInWorld
import com.example.simulation.stepSimulation
import com.example.simulation.takeBankLoan
import com.example.simulation.adjustCruiseSpeedInWorld
import com.example.simulation.toggleCruiseControlInWorld
import com.example.simulation.toggleEnterExitVehicle
import com.example.simulation.toggleFieldCreationMode
import com.example.simulation.toggleGpsAutoSteer
import com.example.simulation.toggleHarvesterHeader
import com.example.simulation.toggleHitchNearestImplement
import com.example.simulation.toggleImplementLowered
import com.example.simulation.toggleSprayerInWorld
import com.example.simulation.toggleVehicle4wd
import com.example.simulation.toggleVehicleEngineInWorld
import com.example.simulation.towAndRecoverVehicle
import com.example.simulation.unloadGrainInWorld
import com.example.simulation.washVehicle
import com.example.storage.CareerImportResult
import com.example.storage.CareerSlotMeta
import com.example.storage.FarmAccountSyncManager
import com.example.storage.ModDownloadManager
import com.example.storage.StorageBreakdownReport
import com.example.storage.SyncBundleRestoreResult
import com.example.storage.buildCloudSaveUploadPayload
import com.example.storage.clearBackupSlots
import com.example.storage.cloudSaveDocToSummary
import com.example.storage.deleteCareerSlot
import com.example.storage.detectMissingModsForCloudSave
import com.example.storage.exportCareerToJson
import com.example.storage.getOrCreateDeviceIdentity
import com.example.storage.getStorageBreakdown
import com.example.storage.installHubPackInList
import com.example.storage.listCareerSlots
import com.example.storage.loadHubFavorites
import com.example.storage.loadHubPacks
import com.example.storage.loadSettings
import com.example.storage.loadStorageQuotaConfig
import com.example.storage.loadWorldFromSlot
import com.example.storage.parseAndVerifyCareerImport
import com.example.storage.saveHubFavorites
import com.example.storage.saveHubPacks
import com.example.storage.saveSettings
import com.example.storage.saveStorageQuotaConfig
import com.example.storage.saveWorldToSlot
import com.example.storage.toggleHubPackEnabledInList
import com.example.storage.uninstallHubPackInList
import com.example.storage.updateHubPackInList
import com.example.storage.worldStateFromCloudSaveDoc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

import com.example.domain.BUILDING_META
import com.example.domain.ESTABLISHMENTS
import com.example.domain.ImplementCategory
import com.example.domain.SeasonType
import com.example.domain.WeatherType
import com.example.domain.getImplementDef
import com.example.domain.getVehiclePhysicsSpec
import com.example.simulation.ConstructionPlacementPreview
import com.example.simulation.confirmConstructionPurchase
import com.example.simulation.createInitialConstructionPreview
import com.example.simulation.customizeOwnedHouseBuilding
import com.example.simulation.depositToBank
import com.example.simulation.evaluatePropertyEvolution
import com.example.simulation.getGridSide
import com.example.simulation.resolveVehicleDrivetrainSpec
import com.example.simulation.sellCargoAtNearbyStation
import com.example.simulation.togglePtoInWorld
import com.example.simulation.toggleVehicleHandbrakeInWorld
import com.example.simulation.unloadAttachedTrailerInWorld
import com.example.simulation.updateConstructionPreviewTransform
import com.example.simulation.withdrawFromBank
import kotlinx.coroutines.CancellationException
import kotlin.math.hypot

enum class InGameTab {
    MENU,
    LOJA,
    ECONOMIA,
    FROTA,
    OFICINA,
    CONSTRUCAO,
    CONTRATOS,
    CONFIGURACOES,
    TERCEIRIZACAO,
    INDUSTRIAS,
    PECUARIA,
    EQUIPE,
    CALENDARIO,
}

val IN_GAME_TAB_TITLES: Map<InGameTab, String> = mapOf(
    InGameTab.MENU to "Menu da Fazenda",
    InGameTab.LOJA to "Loja & Concessionária",
    InGameTab.ECONOMIA to "Banco & Balança",
    InGameTab.FROTA to "Garagem & Frota",
    InGameTab.OFICINA to "Oficina",
    InGameTab.CONSTRUCAO to "Propriedades & Obras",
    InGameTab.CONTRATOS to "Missões, Contratos & DRE",
    InGameTab.CONFIGURACOES to "Configurações da Partida",
    InGameTab.TERCEIRIZACAO to "Terceirização",
    InGameTab.INDUSTRIAS to "Indústrias",
    InGameTab.PECUARIA to "Pecuária",
    InGameTab.EQUIPE to "Equipe & Solo",
    InGameTab.CALENDARIO to "${ALL_CROPS.size} Culturas",
)

enum class ContextualActionKind {
    ENTER_VEHICLE,
    EXIT_VEHICLE,
    HITCH_IMPLEMENT,
    UNLOAD_GRAIN,
    RESCUE_VEHICLE,
    MANUAL_INTERACT,
    SELL_AT_STATION,
    REFUEL_AT_STATION,
    OPEN_BANK,
    OPEN_DEALERSHIP,
    OPEN_AGRO_STORE,
    OPEN_WORKSHOP,
    OPEN_LIVESTOCK,
    OPEN_CONSTRUCTION,
}

data class ContextualInteraction(
    val label: String,
    val kind: ContextualActionKind,
)

fun resolveContextualInteraction(state: WorldState): ContextualInteraction? {
    val gridSide = getGridSide(state)
    val activeVeh = state.activeVehicleUid?.let { uid -> state.vehicles.find { it.uid == uid } }
    if (activeVeh != null) {
        if (activeVeh.isBogged == true || activeVeh.conservationPct < 5.0) {
            return ContextualInteraction("Resgatar", ContextualActionKind.RESCUE_VEHICLE)
        }
        val nearEst = ESTABLISHMENTS.any {
            hypot(it.scaledWorldX(gridSide) - activeVeh.x, it.scaledWorldZ(gridSide) - activeVeh.z) <= 24.0
        } || state.buildings.any {
            (it.type == BuildingType.ESTACAO_GRAOS || it.type == BuildingType.COOPERATIVA || it.type == BuildingType.MERCADO_RURAL) &&
                hypot(it.x - activeVeh.x, it.z - activeVeh.z) <= 18.0
        }
        val hasTrailerCargo = activeVeh.attachedImplementUid?.let { iUid ->
            state.implements.find { it.uid == iUid }?.takeIf { imp ->
                getImplementDef(imp.defId).category == ImplementCategory.CARRETA && imp.fillL > 50.0
            }
        }
        val hasHarvesterCargo = activeVeh.grainTankFillL > 50.0
        if (nearEst && (hasTrailerCargo != null || hasHarvesterCargo)) {
            return ContextualInteraction("Vender Carga", ContextualActionKind.SELL_AT_STATION)
        }
        val nearUnload = hasHarvesterCargo && (
            nearEst ||
                state.buildings.any { it.type == BuildingType.SILO && hypot(it.x - activeVeh.x, it.z - activeVeh.z) <= 18.0 } ||
                state.implements.any { getImplementDef(it.defId).category == ImplementCategory.CARRETA && hypot(it.x - activeVeh.x, it.z - activeVeh.z) <= 14.0 }
            )
        if (nearUnload) {
            return ContextualInteraction("Descarregar", ContextualActionKind.UNLOAD_GRAIN)
        }
        val attachedTrailerUnload = hasTrailerCargo?.let { imp ->
            nearEst || state.buildings.any {
                (it.type == BuildingType.SILO || it.type == BuildingType.SECADOR || it.type == BuildingType.DEPOSITO || it.type == BuildingType.ARMAZEM) &&
                    hypot(it.x - imp.x, it.z - imp.z) <= 18.0
            }
        } == true
        if (attachedTrailerUnload) {
            return ContextualInteraction("Descarregar", ContextualActionKind.UNLOAD_GRAIN)
        }
        if (abs(activeVeh.speedMs) < 1.2) {
            val vDef = getVehicleDef(activeVeh.defId)
            val nearFuel = state.buildings.any {
                (it.type == BuildingType.POSTO_COMBUSTIVEL || it.type == BuildingType.TANQUE_COMBUSTIVEL) &&
                    hypot(it.x - activeVeh.x, it.z - activeVeh.z) <= 16.0
            }
            if (nearFuel && activeVeh.fuelL < vDef.fuelCapacityL * 0.96) {
                return ContextualInteraction("Abastecer", ContextualActionKind.REFUEL_AT_STATION)
            }
            val nearWorkshop = state.buildings.any {
                it.type == BuildingType.OFICINA && hypot(it.x - activeVeh.x, it.z - activeVeh.z) <= 14.0
            }
            if (nearWorkshop) {
                return ContextualInteraction("Oficina", ContextualActionKind.OPEN_WORKSHOP)
            }
        }
        return ContextualInteraction("Sair", ContextualActionKind.EXIT_VEHICLE)
    }

    val p = state.player
    if (p.carryingPropUid != null) {
        return ContextualInteraction("Entregar", ContextualActionKind.MANUAL_INTERACT)
    }
    val nearGate = state.buildings.any { it.type == BuildingType.PORTEIRA && hypot(it.x - p.x, it.z - p.z) < 5.2 }
    if (nearGate) {
        return ContextualInteraction("Porteira", ContextualActionKind.MANUAL_INTERACT)
    }
    val nearNpc = (state.npcAgents ?: emptyList()).any { hypot(it.x - p.x, it.z - p.z) <= 6.5 }
    if (nearNpc) {
        return ContextualInteraction("Conversar", ContextualActionKind.MANUAL_INTERACT)
    }
    val nearVeh = state.vehicles.any { hypot(it.x - p.x, it.z - p.z) <= 7.5 }
    if (nearVeh) {
        return ContextualInteraction("Entrar", ContextualActionKind.ENTER_VEHICLE)
    }

    val nearBuilding = state.buildings.minByOrNull { hypot(it.x - p.x, it.z - p.z) }
        ?.takeIf { hypot(it.x - p.x, it.z - p.z) <= 14.0 }
    when (nearBuilding?.type) {
        BuildingType.BANCO -> return ContextualInteraction("Banco Rural", ContextualActionKind.OPEN_BANK)
        BuildingType.CONCESSIONARIA -> return ContextualInteraction("Concessionária", ContextualActionKind.OPEN_DEALERSHIP)
        BuildingType.LOJA_AGROPECUARIA -> return ContextualInteraction("Loja Agro", ContextualActionKind.OPEN_AGRO_STORE)
        BuildingType.OFICINA -> return ContextualInteraction("Oficina", ContextualActionKind.OPEN_WORKSHOP)
        BuildingType.PONTO_ANIMAIS -> return ContextualInteraction("Feira Animais", ContextualActionKind.OPEN_LIVESTOCK)
        BuildingType.MADEIREIRA, BuildingType.LOJA_MATERIAIS ->
            return ContextualInteraction("Materiais & Obras", ContextualActionKind.OPEN_CONSTRUCTION)
        BuildingType.ESTACAO_GRAOS, BuildingType.COOPERATIVA, BuildingType.MERCADO_RURAL, BuildingType.ARMAZEM ->
            return ContextualInteraction("Vender Grãos", ContextualActionKind.SELL_AT_STATION)
        else -> Unit
    }
    if (ESTABLISHMENTS.any { hypot(it.scaledWorldX(gridSide) - p.x, it.scaledWorldZ(gridSide) - p.z) <= 22.0 }) {
        return ContextualInteraction("Vender Grãos", ContextualActionKind.SELL_AT_STATION)
    }

    val nearProp = state.props.any { hypot(it.x - p.x, it.z - p.z) < 4.5 }
    if (nearProp || p.tool != ManualTool.MAOS) {
        return ContextualInteraction("Interagir", ContextualActionKind.MANUAL_INTERACT)
    }
    return null
}

data class UniversalWorkshopPatch(
    val wheelOptionId: String? = null,
    val engineModelId: String? = null,
    val engineUpgradeId: String? = null,
    val turboId: String? = null,
    val rimColorId: String? = null,
    val cabStyleId: String? = null,
    val customPaintRgb: List<Double>? = null,
    val clearPaint: Boolean = false,
    val headlightStyleId: String? = null,
    val exhaustStyleId: String? = null,
    val accessoryId: String? = null,
)

data class PerfSample(
    val fps: Int = 60,
    val frameTimeMs: Double = 16.6,
    val activeCells: Int = 0,
    val vehiclesCount: Int = 0,
    val implementsCount: Int = 0,
)

enum class MpConnectionStatus(val label: String) {
    DISCONNECTED("Desconectado"),
    CONNECTING("Conectando..."),
    CONNECTED("Conectado (Tempo Real)"),
    PAUSED_BY_DISCONNECT("Pausado por Desconexão"),
}

class FarmViewModel(application: Application) : AndroidViewModel(application) {
    private val appCtx = application.applicationContext
    val farmAccountManager = FarmAccountManager(appCtx)
    val authManager = GoogleAuthManager(appCtx)
    val isFirebaseConfigured: Boolean = authManager.isFirebaseConfigured
    private val cloudRepo = FarmCloudRepository(appCtx)
    private val mpRepo = MultiplayerRepository(appCtx)
    private val downloadManager = ModDownloadManager(appCtx, viewModelScope)
    val audioEngine = AmbientAudioEngine(viewModelScope)

    private val _accountUser = MutableStateFlow(farmAccountManager.currentUser())
    val accountUser: StateFlow<FarmAccountUser?> = _accountUser.asStateFlow()

    private val _hasRegisteredAccounts = MutableStateFlow(farmAccountManager.hasAnyRegisteredAccount())
    val hasRegisteredAccounts: StateFlow<Boolean> = _hasRegisteredAccounts.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _backStack = MutableStateFlow<List<AppRoute>>(listOf(MainMenu))
    val route: StateFlow<AppRoute> = _backStack
        .map { it.lastOrNull() ?: MainMenu }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainMenu)

    private val _worldState = MutableStateFlow(
        createNewWorld(0, "Fazenda Pioneira", "map_pr", 250000.0, "Padrão", 128)
    )
    val worldState: StateFlow<WorldState> = _worldState.asStateFlow()

    private val _hasActiveCareer = MutableStateFlow(false)
    val hasActiveCareer: StateFlow<Boolean> = _hasActiveCareer.asStateFlow()

    private val _cameraMode = MutableStateFlow(CameraMode.TERCEIRA_PESSOA)
    val cameraMode: StateFlow<CameraMode> = _cameraMode.asStateFlow()

    private val _timeScale = MutableStateFlow(1.0)
    val timeScale: StateFlow<Double> = _timeScale.asStateFlow()

    private val _activeTab = MutableStateFlow<InGameTab?>(null)
    val activeTab: StateFlow<InGameTab?> = _activeTab.asStateFlow()

    private val _showFullMap = MutableStateFlow(false)
    val showFullMap: StateFlow<Boolean> = _showFullMap.asStateFlow()

    private val _hudHidden = MutableStateFlow(false)
    val hudHidden: StateFlow<Boolean> = _hudHidden.asStateFlow()

    private val _selectedMapForNewCareer = MutableStateFlow("map_pr")
    val selectedMapForNewCareer: StateFlow<String> = _selectedMapForNewCareer.asStateFlow()

    private val _workshopFocusUid = MutableStateFlow<String?>(null)
    val workshopFocusUid: StateFlow<String?> = _workshopFocusUid.asStateFlow()

    private val _constructionPreview = MutableStateFlow<ConstructionPlacementPreview?>(null)
    val constructionPreview: StateFlow<ConstructionPlacementPreview?> = _constructionPreview.asStateFlow()

    private val _careerSlots = MutableStateFlow<List<CareerSlotMeta?>>(emptyList())
    val careerSlots: StateFlow<List<CareerSlotMeta?>> = _careerSlots.asStateFlow()

    private val _hubPacks = MutableStateFlow(loadHubPacks(appCtx))
    val hubPacks: StateFlow<List<HubPackEntity>> = _hubPacks.asStateFlow()

    val installedPackIds: StateFlow<Set<String>> = _hubPacks
        .map { list -> list.filter { it.installed && it.enabled }.map { it.packId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _hubFavorites = MutableStateFlow(loadHubFavorites(appCtx))
    val hubFavorites: StateFlow<List<String>> = _hubFavorites.asStateFlow()

    val downloadStates: StateFlow<Map<String, ModDownloadProgress>> = downloadManager.downloads

    private val _quotaConfig = MutableStateFlow(loadStorageQuotaConfig(appCtx))
    val quotaConfig: StateFlow<StorageQuotaConfig> = _quotaConfig.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings(appCtx))
    val settings: StateFlow<GameSettings> = _settings.asStateFlow()

    private val _perfStats = MutableStateFlow(PerfSample())
    val perfStats: StateFlow<PerfSample> = _perfStats.asStateFlow()

    // Cloud Saves
    private val _cloudSaves = MutableStateFlow<List<CloudSaveSummary>>(emptyList())
    val cloudSaves: StateFlow<List<CloudSaveSummary>> = _cloudSaves.asStateFlow()

    private val _cloudConflict = MutableStateFlow<CloudSaveConflictState?>(null)
    val cloudConflict: StateFlow<CloudSaveConflictState?> = _cloudConflict.asStateFlow()

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

    // Multiplayer
    private val _openRooms = MutableStateFlow<List<MultiplayerRoomDoc>>(emptyList())
    val openRooms: StateFlow<List<MultiplayerRoomDoc>> = _openRooms.asStateFlow()

    private val _activeRoomCode = MutableStateFlow<String?>(null)
    val activeRoomCode: StateFlow<String?> = _activeRoomCode.asStateFlow()

    private val _roomMembers = MutableStateFlow<List<RoomMemberDoc>>(emptyList())
    val roomMembers: StateFlow<List<RoomMemberDoc>> = _roomMembers.asStateFlow()

    private val _roomMessages = MutableStateFlow<List<RoomChatDoc>>(emptyList())
    val roomMessages: StateFlow<List<RoomChatDoc>> = _roomMessages.asStateFlow()

    private val _mpStatus = MutableStateFlow(MpConnectionStatus.DISCONNECTED)
    val mpStatus: StateFlow<MpConnectionStatus> = _mpStatus.asStateFlow()

    private val _mpIsHost = MutableStateFlow(false)
    val mpIsHost: StateFlow<Boolean> = _mpIsHost.asStateFlow()

    private val worldLock = Any()

    private inline fun mutateWorld(crossinline block: (WorldState) -> WorldState): WorldState {
        synchronized(worldLock) {
            val next = block(_worldState.value)
            _worldState.value = next
            return next
        }
    }

    @Volatile
    private var controlInput: ControlInput = EMPTY_CONTROL_INPUT

    @Volatile
    private var touchInput: ControlInput = EMPTY_CONTROL_INPUT

    @Volatile
    private var keyboardInput: ControlInput = EMPTY_CONTROL_INPUT

    @Volatile
    private var gamepadInput: ControlInput = EMPTY_CONTROL_INPUT

    private var keyLeftPressed = false
    private var keyRightPressed = false
    private var keyUpPressed = false
    private var keyDownPressed = false
    private var keyBrakePressed = false
    private var keyRunPressed = false

    private fun recomputeControlInput() {
        val mx = (touchInput.moveX + keyboardInput.moveX + gamepadInput.moveX).coerceIn(-1.0, 1.0)
        val my = (touchInput.moveY + keyboardInput.moveY + gamepadInput.moveY).coerceIn(-1.0, 1.0)
        val thr = (touchInput.throttle + keyboardInput.throttle + gamepadInput.throttle).coerceIn(0.0, 1.0)
        val brk = (touchInput.brake + keyboardInput.brake + gamepadInput.brake).coerceIn(0.0, 1.0)
        val hb = touchInput.handbrake || keyboardInput.handbrake || gamepadInput.handbrake
        val run = touchInput.running || keyboardInput.running || gamepadInput.running
        controlInput = ControlInput(mx, my, thr, brk, hb, run)
    }

    private var simJob: Job? = null
    private var cloudObserveJob: Job? = null
    private var roomsObserveJob: Job? = null
    private var roomSessionJob: Job? = null
    private var telemetryPushJob: Job? = null

    init {
        refreshCareerSlots()
        audioEngine.start()
        audioEngine.setEnabled(_settings.value.soundEnabled)

        viewModelScope.launch {
            farmAccountManager.currentUserFlow.collect { user ->
                _accountUser.value = user
                _hasRegisteredAccounts.value = farmAccountManager.hasAnyRegisteredAccount()
                if (user != null) {
                    onUserSignedIn(user)
                } else {
                    onUserSignedOut()
                }
            }
        }

        startSimulationLoop()
    }

    private fun onUserSignedIn(user: FarmAccountUser) {
        val hasFirebaseUser = runCatching {
            com.google.firebase.FirebaseApp.getApps(appCtx).isNotEmpty() &&
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
        }.getOrDefault(false)
        if (!isFirebaseConfigured || !hasFirebaseUser) return
        val (devId, devName) = getOrCreateDeviceIdentity(appCtx)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                cloudRepo.ensureProfilePrivateAndDevice(
                    displayName = user.displayName,
                    avatarStyle = user.avatarStyle,
                    activePlatform = "MOBILE",
                    email = user.email,
                    mobileStorageLimitMb = _quotaConfig.value.mobileLimitMb.roundToInt().toLong(),
                    deviceId = devId,
                    deviceName = devName,
                    graphicsPreset = _settings.value.graphicsPreset.name,
                )
            }
        }
        cloudObserveJob?.cancel()
        cloudObserveJob = viewModelScope.launch {
            cloudRepo.observeCloudSaves(user.uid)
                .catch { /* logged by FirestoreErrorHandler */ }
                .collect { docs ->
                    _cloudSaves.value = docs.map { cloudSaveDocToSummary(it) }
                }
        }
        roomsObserveJob?.cancel()
        roomsObserveJob = viewModelScope.launch {
            mpRepo.observeOpenRooms()
                .catch { /* logged */ }
                .collect { rooms ->
                    _openRooms.value = rooms
                }
        }
    }

    private fun onUserSignedOut() {
        cloudObserveJob?.cancel()
        roomsObserveJob?.cancel()
        roomSessionJob?.cancel()
        telemetryPushJob?.cancel()
        _cloudSaves.value = emptyList()
        _openRooms.value = emptyList()
        _roomMembers.value = emptyList()
        _roomMessages.value = emptyList()
        _activeRoomCode.value = null
        _mpStatus.value = MpConnectionStatus.DISCONNECTED
    }

    fun generatePersonalCode(): String = farmAccountManager.generateUniquePersonalCode()

    fun createFarmAccount(username: String, personalCode: String) {
        _authLoading.value = true
        _authError.value = null
        when (val res = farmAccountManager.createAccount(username, personalCode)) {
            is FarmAccountResult.Success -> {
                _accountUser.value = res.user
                _hasRegisteredAccounts.value = true
            }
            is FarmAccountResult.Error -> _authError.value = res.message
        }
        _authLoading.value = false
    }

    fun signInOrRecoverFarmAccount(username: String, personalCode: String) {
        _authLoading.value = true
        _authError.value = null
        when (val res = farmAccountManager.signInOrRecover(username, personalCode)) {
            is FarmAccountResult.Success -> {
                _accountUser.value = res.user
                _hasRegisteredAccounts.value = true
            }
            is FarmAccountResult.Error -> _authError.value = res.message
        }
        _authLoading.value = false
    }

    fun restoreAccountSyncBundle(username: String, personalCode: String, rawJson: String): Pair<Boolean, String> {
        _authLoading.value = true
        _authError.value = null
        return when (val res = FarmAccountSyncManager.verifyAndRestoreSyncBundle(appCtx, rawJson, username, personalCode)) {
            is SyncBundleRestoreResult.Ok -> {
                val accRes = farmAccountManager.registerRecoveredExternalAccount(
                    username = res.payload.username,
                    personalCode = personalCode,
                    uidOverride = res.payload.accountUid,
                    createdAtIso = res.payload.exportedAtIso,
                )
                when (accRes) {
                    is FarmAccountResult.Success -> {
                        _settings.value = loadSettings(appCtx)
                        _hubPacks.value = loadHubPacks(appCtx)
                        _hubFavorites.value = loadHubFavorites(appCtx)
                        audioEngine.setEnabled(_settings.value.soundEnabled)
                        refreshCareerSlots()
                        _accountUser.value = accRes.user
                        _hasRegisteredAccounts.value = true
                        val msg = "Conta '${accRes.user.displayName}' restaurada com ${res.restoredSlotsCount} fazenda(s)!"
                        _worldState.value = _worldState.value.copy(statusAlert = msg)
                        _authLoading.value = false
                        true to msg
                    }
                    is FarmAccountResult.Error -> {
                        _authError.value = accRes.message
                        _authLoading.value = false
                        false to accRes.message
                    }
                }
            }
            is SyncBundleRestoreResult.Err -> {
                _authError.value = res.message
                _authLoading.value = false
                false to res.message
            }
        }
    }

    fun acknowledgeAccountWarning() {
        farmAccountManager.acknowledgeCodeWarning()
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun exportAccountSyncBundleJson(): String {
        val user = _accountUser.value ?: return ""
        return FarmAccountSyncManager.exportSyncBundleJson(
            context = appCtx,
            user = user,
            settings = _settings.value,
            hubPacks = _hubPacks.value,
            hubFavorites = _hubFavorites.value,
        )
    }

    fun getAccountSyncStatus(): AccountSyncStatus =
        FarmAccountSyncManager.buildHonestSyncStatus(appCtx)

    fun signOut() {
        viewModelScope.launch {
            leaveMultiplayerRoom()
            farmAccountManager.signOut()
        }
    }

    private fun startSimulationLoop() {
        simJob?.cancel()
        simJob = viewModelScope.launch(Dispatchers.Default) {
            var lastNs = System.nanoTime()
            var perfCounter = 0
            var prevActiveVehUid: String? = null
            var prevGear = 0
            var prevBraking = false
            while (isActive) {
                try {
                    val curRoute = route.value
                    val menuPaused = _activeTab.value != null || _showFullMap.value
                    if (curRoute == Gameplay && !menuPaused) {
                        val nowNs = System.nanoTime()
                        val dt = ((nowNs - lastNs) / 1_000_000_000.0).coerceIn(0.01, 0.05)
                        lastNs = nowNs

                        val stepped = mutateWorld { cur ->
                            stepSimulation(
                                state = cur,
                                input = controlInput,
                                dtSec = dt,
                                timeScale = _timeScale.value,
                                steerSpeedMultiplier = _settings.value.steerSpeedMultiplier,
                            )
                        }

                        // Audio sync
                        val activeVeh = stepped.activeVehicleUid?.let { uid ->
                            stepped.vehicles.find { it.uid == uid }
                        }
                        val vDef = activeVeh?.let { getVehicleDef(it.defId) }
                        val drivetrain = if (vDef != null) {
                            resolveVehicleDrivetrainSpec(vDef, getVehiclePhysicsSpec(vDef))
                        } else null

                        if (activeVeh != null && activeVeh.uid == prevActiveVehUid && activeVeh.engineRunning == true) {
                            if (activeVeh.gear != prevGear) {
                                audioEngine.playGearShift()
                            }
                            if (drivetrain?.hasAirBrakes == true && prevBraking && activeVeh.isBraking != true) {
                                audioEngine.playAirBrakeHiss()
                            }
                        }
                        prevActiveVehUid = activeVeh?.uid
                        prevGear = activeVeh?.gear ?: 0
                        prevBraking = activeVeh?.isBraking == true

                        val rain = (WEATHER_META[stepped.weather] ?: WEATHER_META[WeatherType.ENSOLARADO])?.rainIntensity ?: 0.0
                        audioEngine.updateParameters(rain, if (rain > 0.4) 0.5 else 0.15, stepped.hourOfDay, _settings.value.audioChannels)
                        audioEngine.updateVehicleAudio(
                            VehicleAudioSnapshot(
                                inVehicle = activeVeh != null,
                                speedMs = activeVeh?.speedMs ?: 0.0,
                                rpm = activeVeh?.rpm ?: 0.0,
                                gear = activeVeh?.gear ?: 0,
                                engineRunning = activeVeh?.engineRunning == true,
                                surfaceType = activeVeh?.surfaceType ?: "terra",
                                soundProfile = vDef?.soundProfile ?: "",
                                horsepower = vDef?.horsepower ?: 90.0,
                                implementActive = activeVeh?.attachedImplementUid?.let { iUid ->
                                    stepped.implements.find { it.uid == iUid }?.isActive
                                } == true || (activeVeh?.isPtoEngaged == true),
                                isSprayerActive = activeVeh?.isSprayerActive == true,
                                isBraking = activeVeh?.isBraking == true,
                                engineLoadPct = activeVeh?.engineLoadPct ?: 0.0,
                                hasAirBrake = drivetrain?.hasAirBrakes == true,
                            )
                        )

                        perfCounter++
                        if (perfCounter % 15 == 0) {
                            val ms = max(8.0, dt * 1000.0)
                            _perfStats.value = PerfSample(
                                fps = (1000.0 / ms).roundToInt().coerceIn(15, 60),
                                frameTimeMs = (ms * 10.0).roundToInt() / 10.0,
                                activeCells = stepped.cells.size,
                                vehiclesCount = stepped.vehicles.size,
                                implementsCount = stepped.implements.size,
                            )
                        }
                    } else {
                        lastNs = System.nanoTime()
                        val cur = _worldState.value
                        val rain = (WEATHER_META[cur.weather] ?: WEATHER_META[WeatherType.ENSOLARADO])?.rainIntensity ?: 0.0
                        audioEngine.updateParameters(rain, 0.08, cur.hourOfDay, _settings.value.audioChannels)
                        audioEngine.updateVehicleAudio(
                            VehicleAudioSnapshot(
                                inVehicle = false,
                                speedMs = 0.0,
                                rpm = 0.0,
                                gear = 0,
                                engineRunning = false,
                                surfaceType = "terra",
                                soundProfile = "",
                                horsepower = 90.0,
                                implementActive = false,
                                isSprayerActive = false,
                                isBraking = false,
                            )
                        )
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (t: Throwable) {
                    mutateWorld { cur ->
                        cur.copy(statusAlert = "Recuperação automática da simulação: ${t.message ?: "estado normalizado"}")
                    }
                }
                delay(33L)
            }
        }
    }

    // Navigation
    fun navigateTo(target: AppRoute) {
        audioEngine.playUiClick()
        val stack = _backStack.value
        if (stack.lastOrNull() == target) return
        _backStack.value = stack + target
    }

    fun goBack(): Boolean {
        audioEngine.playUiClick()
        val stack = _backStack.value
        if (stack.size <= 1) return false
        _backStack.value = stack.dropLast(1)
        return true
    }

    fun setSelectedMapForNewCareer(mapId: String) {
        _selectedMapForNewCareer.value = mapId
    }

    // Career Lifecycle
    fun refreshCareerSlots() {
        _careerSlots.value = listCareerSlots(appCtx)
    }

    fun startNewCareer(
        slotIndex: Int,
        farmName: String,
        mapId: String,
        startingMoneyBrl: Double,
        startingMode: String,
        gridSide: Int = 128,
        initialAppearance: PlayerAppearanceConfig = DEFAULT_PLAYER_APPEARANCE,
    ) {
        val world = createNewWorld(
            slotIndex = slotIndex,
            farmName = farmName.ifBlank { "Fazenda Brasil" },
            mapId = mapId,
            startingMoneyBrl = startingMoneyBrl,
            startingMode = startingMode,
            requestedGridSide = gridSide,
        ).let { w ->
            w.copy(player = w.player.copy(appearance = initialAppearance))
        }
        _worldState.value = world
        _hasActiveCareer.value = true
        _constructionPreview.value = null
        saveWorldToSlot(appCtx, world)
        refreshCareerSlots()
        navigateTo(Gameplay)
    }

    fun loadCareer(slotIndex: Int) {
        val (loaded, recovered) = loadWorldFromSlot(appCtx, slotIndex)
        if (loaded != null) {
            _worldState.value = loaded.copy(
                statusAlert = if (recovered) "Carreira recuperada via backup SHA-256!" else "Carreira carregada com sucesso."
            )
            _hasActiveCareer.value = true
            _constructionPreview.value = null
            navigateTo(Gameplay)
        } else {
            _worldState.value = _worldState.value.copy(statusAlert = "Espaço ${slotIndex + 1} vazio ou corrompido.")
        }
    }

    fun continueMostRecentCareer() {
        val slots = listCareerSlots(appCtx).filterNotNull()
        val latest = slots.maxByOrNull { it.savedAtEpochMs }
        if (latest != null) {
            loadCareer(latest.slotIndex)
        } else {
            navigateTo(AppRoute.NewCareerSetup)
        }
    }

    fun saveCurrentCareer() {
        val cur = _worldState.value
        val (ok, _) = saveWorldToSlot(appCtx, cur)
        refreshCareerSlots()
        _worldState.value = cur.copy(
            statusAlert = if (ok) "Jogo salvo no Espaço ${cur.slotIndex + 1} (SHA-256 verificado)." else "Erro ao salvar localmente."
        )
    }

    fun deleteSlot(slotIndex: Int) {
        deleteCareerSlot(appCtx, slotIndex)
        refreshCareerSlots()
    }

    fun exportCurrentCareerJson(): String = exportCareerToJson(_worldState.value)

    fun importCareerJson(rawJson: String, targetSlot: Int): Pair<Boolean, String> {
        return when (val res = parseAndVerifyCareerImport(rawJson, targetSlot)) {
            is CareerImportResult.Ok -> {
                saveWorldToSlot(appCtx, res.state)
                refreshCareerSlots()
                true to "Backup '${res.state.farmName}' importado para o Espaço ${targetSlot + 1}!"
            }
            is CareerImportResult.Err -> false to res.error
        }
    }

    // Cloud Save Actions
    fun uploadCurrentToCloud(forceOverwrite: Boolean = false) {
        val user = _accountUser.value ?: return
        val cur = _worldState.value
        val (devId, devName) = getOrCreateDeviceIdentity(appCtx)
        viewModelScope.launch {
            _isSyncingCloud.value = true
            runCatching {
                val payload = buildCloudSaveUploadPayload(cur, devId, devName)
                when (val outcome = cloudRepo.uploadCloudSave(payload, forceOverwrite)) {
                    is CloudUploadOutcome.Success -> {
                        val updated = cur.copy(
                            cloudRevision = outcome.newRevision.toInt(),
                            statusAlert = "Nuvem sincronizada! Revisão #${outcome.newRevision} salva para ${user.displayName}.",
                        )
                        _worldState.value = updated
                        saveWorldToSlot(appCtx, updated)
                        refreshCareerSlots()
                        _cloudConflict.value = null
                    }
                    is CloudUploadOutcome.Conflict -> {
                        _cloudConflict.value = CloudSaveConflictState(
                            slotIndex = cur.slotIndex,
                            localFarmName = cur.farmName,
                            localDay = cur.day,
                            localRevision = cur.cloudRevision ?: 0,
                            localMoneyBrl = cur.moneyBrl,
                            cloud = cloudSaveDocToSummary(outcome.cloudSave),
                        )
                    }
                }
            }.onFailure { err ->
                _worldState.value = cur.copy(statusAlert = "Erro ao enviar para nuvem: ${err.localizedMessage}")
            }
            _isSyncingCloud.value = false
        }
    }

    fun resolveCloudConflict(choice: String) {
        val conflict = _cloudConflict.value ?: return
        when (choice) {
            "KEEP_LOCAL" -> uploadCurrentToCloud(forceOverwrite = true)
            "USE_CLOUD" -> downloadCloudSlot(conflict.slotIndex)
            else -> _cloudConflict.value = null
        }
    }

    fun downloadCloudSlot(slotIndex: Int) {
        viewModelScope.launch {
            _isSyncingCloud.value = true
            runCatching {
                val doc = cloudRepo.getCloudSave(slotIndex)
                if (doc == null) {
                    _worldState.value = _worldState.value.copy(statusAlert = "Save na nuvem não encontrado neste espaço.")
                } else {
                    val missing = detectMissingModsForCloudSave(doc.requiredModIds, _hubPacks.value)
                    val restored = worldStateFromCloudSaveDoc(doc)
                    _worldState.value = restored.copy(
                        statusAlert = if (missing.isNotEmpty()) {
                            "Fazenda baixada! Instale ${missing.size} mod(s) no ModHub para todos os veículos."
                        } else {
                            "Fazenda '${restored.farmName}' restaurada da nuvem (Rev #${doc.revision})!"
                        }
                    )
                    _hasActiveCareer.value = true
                    _cloudConflict.value = null
                    saveWorldToSlot(appCtx, _worldState.value)
                    refreshCareerSlots()
                }
            }.onFailure { err ->
                _worldState.value = _worldState.value.copy(statusAlert = "Erro ao baixar da nuvem: ${err.localizedMessage}")
            }
            _isSyncingCloud.value = false
        }
    }

    fun deleteCloudSlot(slotIndex: Int) {
        viewModelScope.launch {
            runCatching { cloudRepo.deleteCloudSave(slotIndex) }
        }
    }

    // Gameplay Controls & Actions
    fun updateTouchInput(patch: ControlInput) {
        touchInput = patch
        recomputeControlInput()
    }

    fun onHardwareKeyEvent(keyCode: Int, isDown: Boolean): Boolean {
        val activeGameplay = route.value == Gameplay && _activeTab.value == null && !_showFullMap.value && _constructionPreview.value == null
        if (!activeGameplay) {
            if (keyLeftPressed || keyRightPressed || keyUpPressed || keyDownPressed || keyBrakePressed) {
                keyLeftPressed = false
                keyRightPressed = false
                keyUpPressed = false
                keyDownPressed = false
                keyBrakePressed = false
                keyRunPressed = false
                keyboardInput = EMPTY_CONTROL_INPUT
                recomputeControlInput()
            }
            return false
        }
        val handled = when (keyCode) {
            android.view.KeyEvent.KEYCODE_A, android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                keyLeftPressed = isDown
                true
            }
            android.view.KeyEvent.KEYCODE_D, android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                keyRightPressed = isDown
                true
            }
            android.view.KeyEvent.KEYCODE_W, android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                keyUpPressed = isDown
                true
            }
            android.view.KeyEvent.KEYCODE_S, android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                keyDownPressed = isDown
                true
            }
            android.view.KeyEvent.KEYCODE_SPACE -> {
                keyBrakePressed = isDown
                true
            }
            android.view.KeyEvent.KEYCODE_SHIFT_LEFT, android.view.KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                keyRunPressed = isDown
                true
            }
            android.view.KeyEvent.KEYCODE_E, android.view.KeyEvent.KEYCODE_ENTER -> {
                if (isDown) performContextualInteraction()
                true
            }
            android.view.KeyEvent.KEYCODE_C -> {
                if (isDown) cycleCameraMode()
                true
            }
            android.view.KeyEvent.KEYCODE_L -> {
                if (isDown) toggleHeadlights()
                true
            }
            android.view.KeyEvent.KEYCODE_H -> {
                if (isDown) playHorn()
                true
            }
            else -> false
        }
        if (handled) {
            val kMoveX = (if (keyRightPressed) 1.0 else 0.0) - (if (keyLeftPressed) 1.0 else 0.0)
            val kMoveY = (if (keyUpPressed) 1.0 else 0.0) - (if (keyDownPressed) 1.0 else 0.0)
            val kThrottle = if (keyUpPressed) 1.0 else 0.0
            val kBrake = if (keyBrakePressed || keyDownPressed) 1.0 else 0.0
            keyboardInput = ControlInput(
                moveX = kMoveX,
                moveY = kMoveY,
                throttle = kThrottle,
                brake = kBrake,
                handbrake = keyBrakePressed,
                running = keyRunPressed,
            )
            recomputeControlInput()
        }
        return handled
    }

    fun onGamepadMotion(axisX: Double, axisY: Double, throttle: Double, brake: Double): Boolean {
        val activeGameplay = route.value == Gameplay && _activeTab.value == null && !_showFullMap.value
        if (!activeGameplay) {
            if (gamepadInput != EMPTY_CONTROL_INPUT) {
                gamepadInput = EMPTY_CONTROL_INPUT
                recomputeControlInput()
            }
            return false
        }
        val dzX = if (abs(axisX) < 0.12) 0.0 else axisX.coerceIn(-1.0, 1.0)
        val dzY = if (abs(axisY) < 0.12) 0.0 else axisY.coerceIn(-1.0, 1.0)
        val thr = if (throttle < 0.08) 0.0 else throttle.coerceIn(0.0, 1.0)
        val brk = if (brake < 0.08) 0.0 else brake.coerceIn(0.0, 1.0)
        gamepadInput = ControlInput(
            moveX = dzX,
            moveY = if (abs(dzY) > 0.12) dzY else (thr - brk).coerceIn(-1.0, 1.0),
            throttle = thr,
            brake = brk,
        )
        recomputeControlInput()
        return true
    }

    fun currentEffectiveControlInput(): ControlInput = controlInput

    fun cycleCameraMode() {
        val all = CameraMode.entries
        _cameraMode.value = all[(all.indexOf(_cameraMode.value) + 1) % all.size]
    }

    fun cycleTimeScale() {
        _timeScale.value = when {
            _timeScale.value < 4.0 -> 5.0
            _timeScale.value < 14.0 -> 15.0
            _timeScale.value < 50.0 -> 60.0
            else -> 1.0
        }
    }

    fun toggleHudHidden() {
        _hudHidden.value = !_hudHidden.value
    }

    fun openInGameTab(tab: InGameTab?) {
        audioEngine.playUiClick()
        _activeTab.value = tab
    }

    fun toggleFullMap(open: Boolean) {
        audioEngine.playUiClick()
        _showFullMap.value = open
    }

    fun teleportToLocation(wx: Double, wz: Double, label: stringCompat) {
        mutateWorld { cur ->
            val activeUid = cur.activeVehicleUid
            if (activeUid != null) {
                cur.copy(
                    vehicles = cur.vehicles.map { if (it.uid == activeUid) it.copy(x = wx, z = wz, speedMs = 0.0) else it },
                    player = cur.player.copy(x = wx, z = wz),
                    statusAlert = "Deslocado para $label.",
                )
            } else {
                cur.copy(
                    player = cur.player.copy(x = wx, z = wz),
                    statusAlert = "Movido para $label.",
                )
            }
        }
        _showFullMap.value = false
    }

    fun clearStatusAlert() {
        mutateWorld { it.copy(statusAlert = null) }
    }

    fun cycleWorldWeather() { mutateWorld { cycleWeather(it) } }
    fun toggleEnterExit() { mutateWorld { toggleEnterExitVehicle(it) } }
    fun toggleEngine() {
        val next = mutateWorld { toggleVehicleEngineInWorld(it) }
        val running = next.activeVehicleUid?.let { uid -> next.vehicles.find { it.uid == uid }?.engineRunning } == true
        if (running) audioEngine.playEngineStart() else audioEngine.playEngineStop()
    }
    fun toggleHandbrake() {
        audioEngine.playUiClick()
        mutateWorld { toggleVehicleHandbrakeInWorld(it) }
    }
    fun toggleCruiseControl() {
        audioEngine.playUiClick()
        mutateWorld { toggleCruiseControlInWorld(it) }
    }
    fun adjustCruiseSpeed(deltaKmh: Double) {
        audioEngine.playUiClick()
        mutateWorld { adjustCruiseSpeedInWorld(it, deltaKmh) }
    }
    fun setPlayerAppearancePreset(presetId: String) {
        val preset = CHARACTER_PRESETS.firstOrNull { it.id == presetId } ?: return
        audioEngine.playUiClick()
        mutateWorld { cur ->
            cur.copy(
                player = cur.player.copy(appearance = preset.config),
                statusAlert = "Personagem alterado para ${preset.title}.",
            )
        }
    }
    fun performContextualInteraction() {
        val action = resolveContextualInteraction(_worldState.value) ?: return
        when (action.kind) {
            ContextualActionKind.ENTER_VEHICLE,
            ContextualActionKind.EXIT_VEHICLE -> toggleEnterExit()
            ContextualActionKind.HITCH_IMPLEMENT -> toggleHitch()
            ContextualActionKind.UNLOAD_GRAIN -> unloadGrain()
            ContextualActionKind.RESCUE_VEHICLE -> engageTowRescue()
            ContextualActionKind.MANUAL_INTERACT -> actionManual()
            ContextualActionKind.SELL_AT_STATION -> sellCargoAtStation()
            ContextualActionKind.REFUEL_AT_STATION -> refuelActiveVehicle()
            ContextualActionKind.OPEN_BANK -> openInGameTab(InGameTab.ECONOMIA)
            ContextualActionKind.OPEN_DEALERSHIP -> navigateTo(AppRoute.Dealership)
            ContextualActionKind.OPEN_AGRO_STORE -> openInGameTab(InGameTab.EQUIPE)
            ContextualActionKind.OPEN_WORKSHOP -> openWorkshopFromWorld()
            ContextualActionKind.OPEN_LIVESTOCK -> openInGameTab(InGameTab.PECUARIA)
            ContextualActionKind.OPEN_CONSTRUCTION -> openInGameTab(InGameTab.CONSTRUCAO)
        }
    }
    fun toggleHitch() { mutateWorld { toggleHitchNearestImplement(it) } }
    fun toggleLowerImp() { mutateWorld { toggleImplementLowered(it) } }
    fun togglePto() { mutateWorld { togglePtoInWorld(it) } }
    fun toggleFieldOpenMode() { mutateWorld { toggleFieldCreationMode(it) } }
    fun toggle4wd() { mutateWorld { toggleVehicle4wd(it) } }
    fun toggleHarvesterHeader() { mutateWorld { toggleHarvesterHeader(it) } }
    fun toggleSprayer() { mutateWorld { toggleSprayerInWorld(it) } }
    fun refillSprayer() { mutateWorld { refillSprayerInWorld(it) } }
    fun engageTowRescue() { mutateWorld { engageTowRescueInWorld(it) } }
    fun refuelActiveVehicle() { mutateWorld { refuelVehicleInWorld(it, true) } }
    fun refillAttachedImplement() { mutateWorld { refillImplementReservoirInWorld(it) } }
    fun unloadGrain() {
        val cur = _worldState.value
        val activeVeh = cur.activeVehicleUid?.let { uid -> cur.vehicles.find { it.uid == uid } }
        audioEngine.playGrainUnload()
        if (activeVeh != null && activeVeh.grainTankFillL <= 5.0 && activeVeh.attachedImplementUid != null) {
            mutateWorld { unloadAttachedTrailerInWorld(it) }
        } else {
            mutateWorld { unloadGrainInWorld(it) }
        }
    }
    fun unloadAttachedTrailer() {
        audioEngine.playGrainUnload()
        mutateWorld { unloadAttachedTrailerInWorld(it) }
    }
    fun actionManual() { mutateWorld { performManualAction(it) } }
    fun selectManualTool(tool: ManualTool) {
        mutateWorld { it.copy(player = it.player.copy(tool = tool)) }
    }
    fun toggleHeadlights() {
        mutateWorld { cur ->
            val activeUid = cur.activeVehicleUid ?: return@mutateWorld cur
            cur.copy(
                vehicles = cur.vehicles.map { if (it.uid == activeUid) it.copy(headlightsOn = !it.headlightsOn) else it }
            )
        }
    }
    fun markGpsA() { mutateWorld { setGpsPoint(it, "A") } }
    fun markGpsB() { mutateWorld { setGpsPoint(it, "B") } }
    fun toggleGpsAuto() { mutateWorld { toggleGpsAutoSteer(it) } }
    fun playHorn() { audioEngine.playHorn() }

    fun nextDay() {
        val next = mutateWorld { advanceDay(it) }
        saveWorldToSlot(appCtx, next)
        refreshCareerSlots()
    }

    // Finance & Fleet & Workshop
    fun depositBank(amountBrl: Double) {
        audioEngine.playPurchaseSuccess()
        mutateWorld { depositToBank(it, amountBrl) }
    }
    fun withdrawBank(amountBrl: Double) {
        audioEngine.playPurchaseSuccess()
        mutateWorld { withdrawFromBank(it, amountBrl) }
    }
    fun sellCargoAtStation() {
        audioEngine.playGrainUnload()
        mutateWorld { sellCargoAtNearbyStation(it) }
    }
    fun openWorkshopFromWorld() {
        val cur = _worldState.value
        val targetUid = cur.activeVehicleUid ?: cur.vehicles.firstOrNull()?.uid
        if (targetUid != null) {
            openWorkshopForVehicle(targetUid)
        } else {
            openInGameTab(InGameTab.OFICINA)
        }
    }
    fun takeLoan(amountBrl: Double) { mutateWorld { takeBankLoan(it, amountBrl) } }
    fun repayLoan(amountBrl: Double) { mutateWorld { repayBankLoan(it, amountBrl) } }

    fun switchToVehicle(vehicleUid: stringCompat) {
        mutateWorld { cur ->
            val v = cur.vehicles.find { it.uid == vehicleUid } ?: return@mutateWorld cur
            cur.copy(
                activeVehicleUid = vehicleUid,
                player = cur.player.copy(x = v.x, z = v.z, headingDeg = v.headingDeg),
                vehicles = cur.vehicles.map { if (it.uid == vehicleUid) it.copy(engineRunning = true) else it },
            )
        }
        _activeTab.value = null
    }

    fun selectVehicleSeedCrop(vehicleUid: String, cropId: String) {
        mutateWorld { cur ->
            cur.copy(
                vehicles = cur.vehicles.map { if (it.uid == vehicleUid) it.copy(selectedCropId = cropId) else it }
            )
        }
    }

    fun openWorkshopForVehicle(vehicleUid: String) {
        _workshopFocusUid.value = vehicleUid
        navigateTo(AppRoute.Workshop)
    }

    fun customizeVehicleUniversal(vehicleUid: String, patch: UniversalWorkshopPatch) {
        mutateWorld { cur ->
            val veh = cur.vehicles.find { it.uid == vehicleUid } ?: return@mutateWorld cur
            var cost = 0.0
            patch.wheelOptionId?.let { id -> WHEEL_OPTIONS.find { it.id == id }?.let { if (veh.wheelOptionId != id) cost += it.priceBrl } }
            patch.engineModelId?.let { id -> ENGINE_MODELS.find { it.id == id }?.let { if (veh.engineModelId != id) cost += it.priceBrl } }
            patch.engineUpgradeId?.let { id -> ENGINE_UPGRADES.find { it.id == id }?.let { if (veh.engineUpgradeId != id) cost += it.priceBrl } }
            patch.turboId?.let { id -> TURBO_OPTIONS.find { it.id == id }?.let { if (veh.turboId != id) cost += it.priceBrl } }
            patch.rimColorId?.let { id -> RIM_COLORS.find { it.id == id }?.let { if (veh.rimColorId != id) cost += it.priceBrl } }
            patch.cabStyleId?.let { id -> CAB_OPTIONS.find { it.id == id }?.let { if (veh.cabStyleId != id) cost += it.priceBrl } }
            patch.headlightStyleId?.let { id -> HEADLIGHT_OPTIONS.find { it.id == id }?.let { if (veh.headlightStyleId != id) cost += it.priceBrl } }
            patch.exhaustStyleId?.let { id -> EXHAUST_OPTIONS.find { it.id == id }?.let { if (veh.exhaustStyleId != id) cost += it.priceBrl } }
            patch.accessoryId?.let { id -> ACCESSORY_OPTIONS.find { it.id == id }?.let { if (veh.accessoryId != id) cost += it.priceBrl } }
            if (patch.customPaintRgb != null || patch.clearPaint) {
                cost += 4800.0
            }
            if (cur.moneyBrl < cost) {
                return@mutateWorld cur.copy(statusAlert = "Saldo insuficiente na oficina (${formatBrl(cost)}).")
            }
            val nextVeh = veh.copy(
                wheelOptionId = patch.wheelOptionId ?: veh.wheelOptionId,
                engineModelId = patch.engineModelId ?: veh.engineModelId,
                engineUpgradeId = patch.engineUpgradeId ?: veh.engineUpgradeId,
                turboId = patch.turboId ?: veh.turboId,
                rimColorId = patch.rimColorId ?: veh.rimColorId,
                cabStyleId = patch.cabStyleId ?: veh.cabStyleId,
                customPaintRgb = if (patch.clearPaint) null else (patch.customPaintRgb ?: veh.customPaintRgb),
                headlightStyleId = patch.headlightStyleId ?: veh.headlightStyleId,
                exhaustStyleId = patch.exhaustStyleId ?: veh.exhaustStyleId,
                accessoryId = patch.accessoryId ?: veh.accessoryId,
            )
            audioEngine.playPurchaseSuccess()
            cur.copy(
                moneyBrl = cur.moneyBrl - cost,
                vehicles = cur.vehicles.map { if (it.uid == vehicleUid) nextVeh else it },
                ledger = if (cost > 0) cur.ledger + LedgerEntry(cur.day, "Customização Oficina", -cost) else cur.ledger,
                statusAlert = "Customização aplicada na oficina (${formatBrl(cost)})!",
            )
        }
    }

    fun inspectVehicleQuote(vehicleUid: String): WorkshopQuote? =
        _worldState.value.vehicles.find { it.uid == vehicleUid }?.let { inspectVehicle(it) }

    fun repairVehicleFullAction(vehicleUid: String, includeRefuel: Boolean = false) {
        mutateWorld { repairVehicleFull(it, vehicleUid, includeRefuel) }
    }

    fun repairVehicleCompAction(vehicleUid: String, comp: WorkshopComponent) {
        mutateWorld { repairVehicleComponent(it, vehicleUid, comp) }
    }

    fun preventiveMaintenanceAction(vehicleUid: String) {
        mutateWorld { performPreventiveMaintenance(it, vehicleUid) }
    }

    fun washVehicleAction(vehicleUid: String) {
        mutateWorld { washVehicle(it, vehicleUid) }
    }

    fun towVehicleAction(vehicleUid: String) {
        mutateWorld { towAndRecoverVehicle(it, vehicleUid) }
    }

    // Store & Market & Lands & Livestock & Industries & Outsourcing
    fun buyVehicle(defId: String) {
        audioEngine.playPurchaseSuccess()
        mutateWorld { buyVehicleInWorld(it, defId) }
    }

    fun sellVehicle(vehicleUid: String) {
        audioEngine.playPurchaseSuccess()
        mutateWorld { sellVehicleInWorld(it, vehicleUid) }
    }

    fun buyImplement(defId: String) {
        audioEngine.playPurchaseSuccess()
        mutateWorld { buyImplementInWorld(it, defId) }
    }

    fun sellAtWeighbridge(est: EstablishmentDef, prodId: String, amountKg: Double) {
        audioEngine.playPurchaseSuccess()
        mutateWorld { sellProductAtWeighbridge(it, est.id, prodId, amountKg) }
    }

    fun buyPlot(plotId: Int) {
        mutateWorld { cur ->
            val p = cur.plots.find { it.id == plotId } ?: return@mutateWorld cur
            if (p.owned || p.isPreservation) return@mutateWorld cur
            if (cur.moneyBrl < p.buyPriceBrl) {
                return@mutateWorld cur.copy(statusAlert = "Saldo insuficiente para comprar ${p.name} (${formatBrl(p.buyPriceBrl)}).")
            }
            audioEngine.playPurchaseSuccess()
            cur.copy(
                moneyBrl = cur.moneyBrl - p.buyPriceBrl,
                plots = cur.plots.map { if (it.id == plotId) it.copy(owned = true, leased = false) else it },
                ledger = cur.ledger + LedgerEntry(cur.day, "Compra de Terra: ${p.name}", -p.buyPriceBrl),
                statusAlert = "${p.name} agora é seu!",
            )
        }
    }

    fun sellPlot(plotId: Int) { mutateWorld { sellPlotInWorld(it, plotId) } }
    fun leasePlotWithTerm(plotId: Int, term: String) { mutateWorld { leasePlotWithTermInWorld(it, plotId, term) } }
    fun mergeAdjacentPlots(primaryId: Int, secondaryId: Int) { mutateWorld { mergeAdjacentPlotsInWorld(it, primaryId, secondaryId) } }

    fun placeBuildingNearPlayer(type: BuildingType) {
        mutateWorld { cur ->
            val rad = Math.toRadians(cur.player.headingDeg)
            val bx = cur.player.x + sin(rad) * 14.0
            val bz = cur.player.z + cos(rad) * 14.0
            audioEngine.playPurchaseSuccess()
            placeBuildingInWorld(cur, type, bx, bz)
        }
    }

    // ===== Property Construction Mode, Safe House Replacement & Customization =====
    fun startConstructionPlacement(def: BuildableStructureDef, replaceHouseUid: String? = null) {
        audioEngine.playUiClick()
        val cur = _worldState.value
        _constructionPreview.value = createInitialConstructionPreview(cur, def, replaceHouseUid)
        _activeTab.value = null
    }

    fun moveConstructionPreview(dx: Double, dz: Double) {
        val curPreview = _constructionPreview.value ?: return
        _constructionPreview.value = updateConstructionPreviewTransform(
            state = _worldState.value,
            preview = curPreview,
            newX = curPreview.x + dx,
            newZ = curPreview.z + dz,
        )
    }

    fun setConstructionPreviewPosition(wx: Double, wz: Double) {
        val curPreview = _constructionPreview.value ?: return
        _constructionPreview.value = updateConstructionPreviewTransform(
            state = _worldState.value,
            preview = curPreview,
            newX = wx,
            newZ = wz,
        )
    }

    fun rotateConstructionPreview(deltaDeg: Double) {
        audioEngine.playUiClick()
        val curPreview = _constructionPreview.value ?: return
        _constructionPreview.value = updateConstructionPreviewTransform(
            state = _worldState.value,
            preview = curPreview,
            newRotDeg = curPreview.rotationDeg + deltaDeg,
        )
    }

    fun confirmConstructionPlacement(): Boolean {
        val curPreview = _constructionPreview.value ?: return false
        var succeeded = false
        val after = mutateWorld { cur ->
            val (next, msg) = confirmConstructionPurchase(cur, curPreview)
            if (next !== cur) {
                succeeded = true
                next
            } else {
                cur.copy(statusAlert = msg)
            }
        }
        if (succeeded) {
            audioEngine.playPurchaseSuccess()
            _constructionPreview.value = null
            saveWorldToSlot(appCtx, after)
            refreshCareerSlots()
        }
        return succeeded
    }

    fun cancelConstructionPlacement() {
        audioEngine.playUiClick()
        _constructionPreview.value = null
    }

    fun customizeOwnedHouse(
        buildingUid: String,
        presetId: String? = null,
        wallColorHex: String? = null,
        roofColorHex: String? = null,
        trimColorHex: String? = null,
        materialStyle: String? = null,
        addVeranda: Boolean? = null,
        addGarage: Boolean? = null,
    ): Boolean {
        var changed = false
        val after = mutateWorld { cur ->
            val (next, msg) = customizeOwnedHouseBuilding(
                state = cur,
                buildingUid = buildingUid,
                presetId = presetId,
                wallColorHex = wallColorHex,
                roofColorHex = roofColorHex,
                trimColorHex = trimColorHex,
                materialStyle = materialStyle,
                addVeranda = addVeranda,
                addGarage = addGarage,
            )
            if (next !== cur) {
                changed = true
                next
            } else {
                cur.copy(statusAlert = msg)
            }
        }
        if (changed) {
            audioEngine.playPurchaseSuccess()
            saveWorldToSlot(appCtx, after)
            refreshCareerSlots()
        }
        return changed
    }

    fun checkPropertyEvolutionStatus() = evaluatePropertyEvolution(_worldState.value)

    fun buyAnimals(species: AnimalSpecies, qty: Int) {
        mutateWorld { cur ->
            val meta = ANIMAL_META[species] ?: return@mutateWorld cur
            val total = meta.unitPriceBrl * qty
            if (cur.moneyBrl < total) {
                return@mutateWorld cur.copy(statusAlert = "Saldo insuficiente para comprar $qty ${meta.displayName} (${formatBrl(total)}).")
            }
            audioEngine.playPurchaseSuccess()
            cur.copy(
                moneyBrl = cur.moneyBrl - total,
                herds = cur.herds.map {
                    if (it.species == species) it.copy(count = it.count + qty, feedReserveKg = it.feedReserveKg + qty * 20.0) else it
                },
                ledger = cur.ledger + LedgerEntry(cur.day, "Compra Pecuária: ${qty}x ${meta.displayName}", -total),
                statusAlert = "+$qty ${meta.displayName} adicionados ao rebanho!",
            )
        }
    }

    fun sellAnimalHeads(species: AnimalSpecies, qty: Int) { mutateWorld { sellAnimalHeadInWorld(it, species, qty) } }

    fun feedAndWaterHerd(species: AnimalSpecies) {
        mutateWorld { cur ->
            val meta = ANIMAL_META[species] ?: return@mutateWorld cur
            val herd = cur.herds.find { it.species == species } ?: return@mutateWorld cur
            val cost = max(120.0, herd.count * 24.0)
            if (cur.moneyBrl < cost) {
                return@mutateWorld cur.copy(statusAlert = "Saldo insuficiente para ração e água (${formatBrl(cost)}).")
            }
            cur.copy(
                moneyBrl = cur.moneyBrl - cost,
                herds = cur.herds.map {
                    if (it.species == species) {
                        it.copy(
                            feedReserveKg = it.feedReserveKg + max(250.0, herd.count * 35.0),
                            waterReserveL = it.waterReserveL + max(450.0, herd.count * 80.0),
                            healthPct = min(100.0, it.healthPct + 5.0),
                        )
                    } else it
                },
                ledger = cur.ledger + LedgerEntry(cur.day, "Trato Pecuária: ${meta.displayName}", -cost),
                statusAlert = "Ração e aguada abastecidas para ${meta.displayName}!",
            )
        }
    }

    fun buyIndustry(recipeId: String) {
        mutateWorld { cur ->
            val recipe = INDUSTRY_CHAINS.find { it.id == recipeId } ?: return@mutateWorld cur
            if (cur.moneyBrl < recipe.plantCostBrl) {
                return@mutateWorld cur.copy(statusAlert = "Saldo insuficiente para adquirir ${recipe.industryName} (${formatBrl(recipe.plantCostBrl)}).")
            }
            audioEngine.playPurchaseSuccess()
            cur.copy(
                moneyBrl = cur.moneyBrl - recipe.plantCostBrl,
                industries = cur.industries.map { if (it.recipeId == recipeId) it.copy(purchased = true, stage = 1) else it },
                ledger = cur.ledger + LedgerEntry(cur.day, "Compra Agroindústria: ${recipe.industryName}", -recipe.plantCostBrl),
                statusAlert = "${recipe.industryName} instalada e pronta para operar!",
            )
        }
    }

    fun supplyIndustry(recipeId: String) {
        mutateWorld { cur ->
            val recipe = INDUSTRY_CHAINS.find { it.id == recipeId } ?: return@mutateWorld cur
            val stock = cur.inventoryKg[recipe.inputProductId] ?: 0.0
            if (stock < recipe.inputAmountKg) {
                return@mutateWorld cur.copy(statusAlert = "Precisa de ${recipe.inputAmountKg.roundToInt()} kg de ${recipe.inputProductId} no estoque.")
            }
            val nextInv = cur.inventoryKg.toMutableMap()
            nextInv[recipe.inputProductId] = stock - recipe.inputAmountKg
            cur.copy(
                inventoryKg = nextInv,
                industries = cur.industries.map {
                    if (it.recipeId == recipeId) {
                        it.copy(rawStockKg = it.rawStockKg + recipe.inputAmountKg, isProcessing = true)
                    } else it
                },
                statusAlert = "Lote enviado para processamento em ${recipe.industryName}!",
            )
        }
    }

    fun acceptOrCompleteContract(contractUid: String) { mutateWorld { acceptOrCompleteContractInWorld(it, contractUid) } }
    fun irrigatePlot(plotId: Int) { mutateWorld { irrigatePlotInWorld(it, plotId) } }
    fun applyLimeToPlot(plotId: Int) { mutateWorld { applyLimeToPlotInWorld(it, plotId) } }
    fun sprayPlotPests(plotId: Int) { mutateWorld { sprayPlotPestsInWorld(it, plotId) } }
    fun applyContourProtection(plotId: Int) { mutateWorld { applyContourProtectionInWorld(it, plotId) } }
    fun buyInputStock(inputId: String, kg: Double, totalCostBrl: Double) { mutateWorld { buyInputStockInWorld(it, inputId, kg, totalCostBrl) } }
    fun hireEmployee(name: String, role: String, dailyWageBrl: Double) { mutateWorld { hireEmployeeInWorld(it, name, role, dailyWageBrl) } }
    fun assignEmployee(empUid: String, vehUid: String?, plotId: Int?, taskType: String) { mutateWorld { assignEmployeeInWorld(it, empUid, vehUid, plotId, taskType) } }
    fun fireEmployee(empUid: String) { mutateWorld { fireEmployeeInWorld(it, empUid) } }
    fun placeAuctionBid(lotUid: String, bidBrl: Double) { mutateWorld { placeAuctionBidInWorld(it, lotUid, bidBrl) } }
    fun hireOutsourcingService(params: HireOutsourcingParams) { mutateWorld { hireOutsourcingServiceInWorld(it, params) } }
    fun resolveOutsourcingIncident(contractUid: String, action: String) { mutateWorld { resolveOutsourcingIncidentInWorld(it, contractUid, action) } }
    fun rateOutsourcingContract(contractUid: String, stars: Int) { mutateWorld { rateOutsourcingContractInWorld(it, contractUid, stars) } }
    fun setLandUnit(unit: LandUnit) { mutateWorld { it.copy(landUnit = unit) } }

    // Hub & Downloads & Storage Quota
    fun startHubPackDownload(packId: String) {
        val pack = _hubPacks.value.find { it.packId == packId } ?: return
        downloadManager.startDownload(
            pack = pack,
            allPacks = _hubPacks.value,
            quotaConfig = _quotaConfig.value,
            onComplete = { completed ->
                val next = installHubPackInList(_hubPacks.value, completed.packId)
                _hubPacks.value = next
                saveHubPacks(appCtx, next)
                _worldState.value = _worldState.value.copy(statusAlert = "ModHub: '${completed.name}' instalado e ativado!")
            },
            onError = { err ->
                _worldState.value = _worldState.value.copy(statusAlert = err)
            }
        )
    }

    fun pauseHubDownload(packId: String) = downloadManager.pauseDownload(packId)

    fun resumeHubDownload(packId: String) {
        val pack = _hubPacks.value.find { it.packId == packId } ?: return
        downloadManager.resumeDownload(pack) { completed ->
            val next = installHubPackInList(_hubPacks.value, completed.packId)
            _hubPacks.value = next
            saveHubPacks(appCtx, next)
        }
    }

    fun cancelHubDownload(packId: String) = downloadManager.cancelDownload(packId)

    fun updateHubPack(packId: String) {
        val next = updateHubPackInList(_hubPacks.value, packId)
        _hubPacks.value = next
        saveHubPacks(appCtx, next)
    }

    fun uninstallHubPack(packId: String) {
        val next = uninstallHubPackInList(_hubPacks.value, packId)
        _hubPacks.value = next
        saveHubPacks(appCtx, next)
        downloadManager.markUninstalled(packId)
    }

    fun toggleHubPackEnabled(packId: String) {
        val next = toggleHubPackEnabledInList(_hubPacks.value, packId)
        _hubPacks.value = next
        saveHubPacks(appCtx, next)
        val toggled = next.find { it.packId == packId }
        if (toggled != null) {
            _worldState.value = _worldState.value.copy(
                statusAlert = if (toggled.enabled) "Mod '${toggled.name}' ativado!" else "Mod '${toggled.name}' desativado."
            )
        }
    }

    fun getAvailableDeviceFreeSpaceMb(): Double = downloadManager.getAvailableDeviceSpaceMb()

    fun toggleHubFavorite(packId: String) {
        val cur = _hubFavorites.value
        val next = if (cur.contains(packId)) cur - packId else cur + packId
        _hubFavorites.value = next
        saveHubFavorites(appCtx, next)
    }

    fun importCustomHubMod(rawJson: String): Pair<Boolean, String> {
        return when (val res = validateAndParseCustomModManifest(rawJson, _hubPacks.value)) {
            is CustomModParseResult.Ok -> {
                val next = _hubPacks.value + res.pack
                _hubPacks.value = next
                saveHubPacks(appCtx, next)
                true to "Mod '${res.pack.name}' importado com sucesso!"
            }
            is CustomModParseResult.Err -> false to res.error
        }
    }

    fun updateStorageQuotaConfig(config: StorageQuotaConfig) {
        _quotaConfig.value = config
        saveStorageQuotaConfig(appCtx, config)
    }

    fun storageBreakdown(): StorageBreakdownReport =
        getStorageBreakdown(appCtx, _hubPacks.value, downloadStates.value)

    fun clearLocalBackups(): Int = clearBackupSlots(appCtx)

    // Settings
    fun updateSettings(updater: (GameSettings) -> GameSettings) {
        val next = updater(_settings.value)
        _settings.value = next
        saveSettings(appCtx, next)
        audioEngine.setEnabled(next.soundEnabled)
    }

    fun applyGraphicsPreset(preset: GraphicsPreset) {
        val cfg = (GRAPHICS_PRESET_META[preset] ?: GRAPHICS_PRESET_META[GraphicsPreset.EQUILIBRADO])?.config ?: return
        updateSettings { it.copy(graphicsPreset = preset, customGraphics = cfg) }
    }

    fun detectRecommendedGraphics() {
        val rec = detectRecommendedGraphicsPreset()
        applyGraphicsPreset(rec)
        _worldState.value = _worldState.value.copy(
            statusAlert = "Perfil recomendado detectado: ${GRAPHICS_PRESET_META[rec]?.displayName ?: rec.name}"
        )
    }

    fun updateCustomGraphics(updater: (GraphicsConfig) -> GraphicsConfig) {
        updateSettings { it.copy(graphicsPreset = GraphicsPreset.PERSONALIZADO, customGraphics = updater(it.customGraphics)) }
    }

    fun updateHudLayout(patch: HudLayoutConfig) {
        updateSettings { it.copy(hudLayout = patch) }
    }

    // Multiplayer Online Cooperativo
    fun hostMultiplayerRoom(roomCode: String, sharedFarm: Boolean = true, pauseOnDisconnect: Boolean = true) {
        val user = _accountUser.value ?: return
        val cur = _worldState.value
        viewModelScope.launch {
            _mpStatus.value = MpConnectionStatus.CONNECTING
            runCatching {
                mpRepo.createOrHostRoom(
                    roomCode = roomCode,
                    hostName = user.displayName,
                    mapId = cur.mapId,
                    sharedFarmMode = sharedFarm,
                    pauseOnDisconnect = pauseOnDisconnect,
                )
                val cleanRoom = roomCode.trim().uppercase().replace(Regex("[^A-Z0-9_\\-]"), "-").take(32)
                _mpIsHost.value = true
                attachToRoom(cleanRoom)
            }.onFailure { err ->
                _mpStatus.value = MpConnectionStatus.DISCONNECTED
                _worldState.value = cur.copy(statusAlert = "Erro ao criar sala: ${err.localizedMessage}")
            }
        }
    }

    fun joinMultiplayerRoom(roomCode: String) {
        val user = _accountUser.value ?: return
        val cur = _worldState.value
        viewModelScope.launch {
            _mpStatus.value = MpConnectionStatus.CONNECTING
            runCatching {
                val room = mpRepo.joinRoomAsGuest(
                    roomCode = roomCode,
                    playerName = user.displayName,
                    x = cur.player.x,
                    z = cur.player.z,
                )
                _mpIsHost.value = room.hostUid == user.uid
                attachToRoom(room.roomCode)
            }.onFailure { err ->
                _mpStatus.value = MpConnectionStatus.DISCONNECTED
                _worldState.value = cur.copy(statusAlert = "Erro ao entrar na sala: ${err.localizedMessage}")
            }
        }
    }

    private fun attachToRoom(roomCode: String) {
        _activeRoomCode.value = roomCode
        _mpStatus.value = MpConnectionStatus.CONNECTED

        roomSessionJob?.cancel()
        roomSessionJob = viewModelScope.launch {
            launch {
                mpRepo.observeRoomMembers(roomCode)
                    .catch { /* logged */ }
                    .collect { members -> _roomMembers.value = members }
            }
            launch {
                mpRepo.observeRoomMessages(roomCode)
                    .catch { /* logged */ }
                    .collect { msgs -> _roomMessages.value = msgs }
            }
        }

        telemetryPushJob?.cancel()
        telemetryPushJob = viewModelScope.launch {
            while (isActive && _activeRoomCode.value == roomCode) {
                val user = _accountUser.value
                val cur = _worldState.value
                val v = cur.activeVehicleUid?.let { uid -> cur.vehicles.find { it.uid == uid } }
                if (user != null) {
                    runCatching {
                        mpRepo.updateLocalTelemetry(
                            roomCode = roomCode,
                            playerName = user.displayName,
                            x = v?.x ?: cur.player.x,
                            z = v?.z ?: cur.player.z,
                            vehicleDefId = v?.defId,
                            headingDeg = v?.headingDeg ?: cur.player.headingDeg,
                            pitchDeg = v?.pitchDeg ?: 0.0,
                            rollDeg = v?.rollDeg ?: 0.0,
                            steerAngleDeg = v?.steerAngleDeg ?: 0.0,
                            wheelSpinDeg = v?.wheelSpinDeg ?: 0.0,
                            suspensionOffsetY = v?.suspensionOffsetY ?: 0.0,
                        )
                    }
                }
                delay(180L)
            }
        }
    }

    fun approveRoomMember(memberUid: String, approve: Boolean) {
        val code = _activeRoomCode.value ?: return
        viewModelScope.launch {
            runCatching { mpRepo.approveMember(code, memberUid, approve) }
        }
    }

    fun sendRoomChat(text: String) {
        val code = _activeRoomCode.value ?: return
        val user = _accountUser.value ?: return
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            runCatching { mpRepo.sendChatMessage(code, user.displayName, clean) }
        }
    }

    fun leaveMultiplayerRoom() {
        val code = _activeRoomCode.value
        val wasHost = _mpIsHost.value
        roomSessionJob?.cancel()
        telemetryPushJob?.cancel()
        _activeRoomCode.value = null
        _roomMembers.value = emptyList()
        _roomMessages.value = emptyList()
        _mpIsHost.value = false
        _mpStatus.value = MpConnectionStatus.DISCONNECTED
        if (wasHost && code != null) {
            viewModelScope.launch {
                runCatching { mpRepo.closeRoom(code) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        simJob?.cancel()
        audioEngine.release()
    }
}

private typealias stringCompat = String
