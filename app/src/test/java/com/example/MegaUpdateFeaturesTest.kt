package com.example

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.auth.ACCOUNT_CODE_WARNING_TEXT
import com.example.auth.ACCOUNT_INVALID_CREDENTIALS_ERROR
import com.example.auth.FarmAccountManager
import com.example.auth.FarmAccountResult
import com.example.domain.ALL_IMPLEMENTS
import com.example.domain.CameraMode
import com.example.domain.DEFAULT_HUB_PACKS
import com.example.domain.GRAPHICS_PRESET_META
import com.example.domain.GameSettings
import com.example.domain.GraphicsPreset
import com.example.domain.HUB_CATEGORIES
import com.example.domain.ImplementCategory
import com.example.domain.MOD_MISSING_FILE_ERROR
import com.example.domain.ModDownloadStatus
import com.example.domain.NEW_AGRITECH_IMPLEMENTS_24
import com.example.domain.OwnedImplement
import com.example.domain.SoilState
import com.example.domain.StorageQuotaConfig
import com.example.domain.TEXTURE_RESOLUTION_OPTIONS
import com.example.domain.WeatherType
import com.example.domain.clampGraphicsConfigForDevice
import com.example.domain.getExternalGraphicsPacks
import com.example.domain.getImplementDef
import com.example.domain.getVehicleDef
import com.example.domain.matchesHubCategory
import com.example.domain.validateModPack
import com.example.simulation.ControlInput
import com.example.simulation.checkMachineImplementCompatibility
import com.example.simulation.createNewWorld
import com.example.simulation.stepSimulation
import com.example.simulation.toggleHitchNearestImplement
import com.example.simulation.toggleImplementLowered
import com.example.state.FarmViewModel
import com.example.storage.FarmAccountSyncManager
import com.example.storage.ModDownloadManager
import com.example.storage.SyncBundleRestoreResult
import com.example.storage.saveWorldToSlot
import com.example.storage.toggleHubPackEnabledInList
import com.example.ui.components.GameplayViewport
import com.example.ui.components.SignInScreen
import com.example.ui.theme.FarmBrasilTheme
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MegaUpdateFeaturesTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun farmAccountManager_createsVerifiesRecoversAndRejectsWrongCredentials() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        File(app.filesDir, "farm_brasil_accounts.json").delete()

        val manager = FarmAccountManager(app)
        val generated = (1..20).map { manager.generateUniquePersonalCode() }
        assertThat(generated.toSet().size).isEqualTo(20)
        assertThat(generated.first()).startsWith("FB-")

        val created = manager.createAccount("Joao Agro", generated.first())
        assertThat(created).isInstanceOf(FarmAccountResult.Success::class.java)
        val ok = created as FarmAccountResult.Success
        assertThat(ok.user.displayName).isEqualTo("Joao Agro")
        assertThat(ok.user.warningAcknowledged).isFalse()

        manager.acknowledgeCodeWarning()
        assertThat(manager.currentUser()?.warningAcknowledged).isTrue()

        manager.signOut()
        assertThat(manager.currentUser()).isNull()

        val badLogin = manager.signInOrRecover("Joao Agro", "FB-ERR0-0000")
        assertThat(badLogin).isInstanceOf(FarmAccountResult.Error::class.java)
        assertThat((badLogin as FarmAccountResult.Error).message).isEqualTo(ACCOUNT_INVALID_CREDENTIALS_ERROR)

        val goodLogin = manager.signInOrRecover("  joao agro ", generated.first().lowercase())
        assertThat(goodLogin).isInstanceOf(FarmAccountResult.Success::class.java)
        assertThat((goodLogin as FarmAccountResult.Success).user.uid).isEqualTo(ok.user.uid)
    }

    @Test
    fun farmAccountSync_honestLocalStatusAndVerifiedCrossDeviceRestore() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        File(app.filesDir, "farm_brasil_accounts.json").delete()
        File(app.filesDir, "farm_brasil_sync_config.json").delete()

        val status = FarmAccountSyncManager.buildHonestSyncStatus(app)
        assertThat(status.isServerConfigured).isFalse()
        assertThat(status.statusMessage).contains("localmente")

        val mgr = FarmAccountManager(app)
        val code = mgr.generateUniquePersonalCode()
        val user = (mgr.createAccount("Maria Campo", code) as FarmAccountResult.Success).user
        val world = createNewWorld(0, "Fazenda Sul", "map_pr", 480000.0, "Padrão", 64)
        saveWorldToSlot(app, world)

        val megaSettings = GameSettings(
            graphicsPreset = GraphicsPreset.MEGA_REALISTA,
            customGraphics = GRAPHICS_PRESET_META.getValue(GraphicsPreset.MEGA_REALISTA).config,
        )
        val bundleJson = FarmAccountSyncManager.exportSyncBundleJson(
            context = app,
            user = user,
            settings = megaSettings,
            hubPacks = DEFAULT_HUB_PACKS,
            hubFavorites = listOf("pack_cbt_classico"),
        )

        val tampered = FarmAccountSyncManager.verifyAndRestoreSyncBundle(app, bundleJson, "Maria Campo", "FB-FAIL-9999")
        assertThat(tampered).isInstanceOf(SyncBundleRestoreResult.Err::class.java)
        assertThat((tampered as SyncBundleRestoreResult.Err).message).isEqualTo(ACCOUNT_INVALID_CREDENTIALS_ERROR)

        val restored = FarmAccountSyncManager.verifyAndRestoreSyncBundle(app, bundleJson, "Maria Campo", code)
        assertThat(restored).isInstanceOf(SyncBundleRestoreResult.Ok::class.java)
        val okRes = restored as SyncBundleRestoreResult.Ok
        assertThat(okRes.restoredSlotsCount).isGreaterThan(0)
        assertThat(okRes.payload.settings.graphicsPreset).isEqualTo(GraphicsPreset.MEGA_REALISTA)
    }

    @Test
    fun signInScreen_rendersWelcomeWarningAndRecoveryError() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        File(app.filesDir, "farm_brasil_accounts.json").delete()
        val vm = FarmViewModel(app)

        composeRule.setContent {
            val acc by vm.accountUser.collectAsStateWithLifecycle()
            val err by vm.authError.collectAsStateWithLifecycle()
            val loading by vm.authLoading.collectAsStateWithLifecycle()
            val hasAcc by vm.hasRegisteredAccounts.collectAsStateWithLifecycle()
            FarmBrasilTheme {
                SignInScreen(
                    authLoading = loading,
                    authError = err,
                    pendingWarningUser = acc?.takeIf { !it.warningAcknowledged },
                    hasRegisteredAccounts = hasAcc,
                    onGenerateCode = { vm.generatePersonalCode() },
                    onCreateAccount = { u, c -> vm.createFarmAccount(u, c) },
                    onSignInOrRecover = { u, c -> vm.signInOrRecoverFarmAccount(u, c) },
                    onRestoreSyncBundle = { u, c, j -> vm.restoreAccountSyncBundle(u, c, j) },
                    onAcknowledgeWarning = { vm.acknowledgeAccountWarning() },
                    onClearError = { vm.clearAuthError() },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("BEM-VINDO AO FARM BRASIL").assertIsDisplayed()
        composeRule.onNodeWithTag("account_username_input").performTextInput("Carlos Rural")
        composeRule.onNodeWithTag("generate_code_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("create_account_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(ACCOUNT_CODE_WARNING_TEXT).assertIsDisplayed()
        composeRule.onNodeWithTag("acknowledge_code_warning_button").performClick()
        composeRule.waitForIdle()

        vm.farmAccountManager.signOut()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("already_have_account_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("account_code_input").performTextInput("FB-0000-0000")
        composeRule.onNodeWithTag("login_account_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(ACCOUNT_INVALID_CREDENTIALS_ERROR).assertIsDisplayed()
    }

    @Test
    fun graphicsMegaRealista_configClampAnd3DRenderPass() {
        val megaMeta = GRAPHICS_PRESET_META[GraphicsPreset.MEGA_REALISTA]
        assertThat(megaMeta).isNotNull()
        assertThat(megaMeta!!.displayName).isEqualTo("Mega-realista")
        assertThat(megaMeta.config.textureResolutionPx).isEqualTo(4096)
        assertThat(megaMeta.config.shadowQuality).isEqualTo(3)
        assertThat(megaMeta.config.reflectionQuality).isEqualTo(3)
        assertThat(TEXTURE_RESOLUTION_OPTIONS.map { it.first }).containsExactly(512, 1024, 2048, 4096)

        val clampedNonAdaptive = clampGraphicsConfigForDevice(megaMeta.config.copy(adaptiveMobile = false))
        assertThat(clampedNonAdaptive.textureResolutionPx).isEqualTo(4096)

        val base = createNewWorld(0, "Fazenda Ultra", "map_pr", 300000.0, "Padrão", 64)
        val rainWorld = base.copy(weather = WeatherType.CHUVA_INTENSA, hourOfDay = 17.2)
        composeRule.setContent {
            FarmBrasilTheme {
                GameplayViewport(
                    worldState = rainWorld,
                    cameraMode = CameraMode.TERCEIRA_PESSOA,
                    remoteMembers = emptyList(),
                    localUid = "local",
                    settings = GameSettings(
                        graphicsPreset = GraphicsPreset.MEGA_REALISTA,
                        customGraphics = megaMeta.config,
                    ),
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun twentyFourNewImplements_coverAllCategoriesWithCultivadorAndCornHeaderMechanics() {
        assertThat(NEW_AGRITECH_IMPLEMENTS_24.size).isEqualTo(24)
        val allIds = ALL_IMPLEMENTS.map { it.id }
        assertThat(allIds.distinct().size).isEqualTo(allIds.size)

        val cats = ALL_IMPLEMENTS.map { it.category }.toSet()
        assertThat(cats).containsAtLeast(
            ImplementCategory.CULTIVADOR,
            ImplementCategory.PLATAFORMA_MILHO,
            ImplementCategory.ARADO,
            ImplementCategory.GRADE,
            ImplementCategory.SUBSOLADOR,
            ImplementCategory.ESCARIFICADOR,
            ImplementCategory.PLANTADEIRA,
            ImplementCategory.DISTRIBUIDOR,
            ImplementCategory.PULVERIZADOR,
            ImplementCategory.CARRETA,
            ImplementCategory.ENFARDADORA,
            ImplementCategory.SEGADORA,
            ImplementCategory.ENLEIRADOR,
        )

        // Cultivador removes weeds without destroying planted crop
        val base = createNewWorld(0, "Fazenda Implementos", "map_pr", 500000.0, "Padrão", 64)
        val tractor = base.vehicles.first().copy(
            uid = "v_cult",
            defId = "agrale_bx6150",
            x = 0.0,
            z = 0.0,
            headingDeg = 0.0,
            engineRunning = true,
            handbrakeOn = false,
            attachedImplementUid = "imp_cult",
        )
        val cultivador = OwnedImplement(
            uid = "imp_cult",
            defId = "cultivador_baldan_9_enxadas",
            x = 0.0,
            z = -1.6,
            headingDeg = 0.0,
            isLowered = false,
        )
        val weedyCropCells = base.cells.map {
            it.copy(
                height = 2.0,
                soilState = SoilState.CRESCENDO,
                cropId = "milho",
                growthProgress = 0.4,
                weedLevel = 0.75,
                compaction = 0.50,
            )
        }
        var world = toggleImplementLowered(
            base.copy(
                cells = weedyCropCells,
                vehicles = listOf(tractor),
                implements = listOf(cultivador),
                activeVehicleUid = "v_cult",
            )
        )
        repeat(35) {
            world = stepSimulation(world, ControlInput(throttle = 0.8), dtSec = 0.05, timeScale = 1.0)
        }
        assertThat(world.cells.any { it.cropId == "milho" && it.weedLevel < 0.2 }).isTrue()

        // Corn header attaches only to harvesters, not tractors
        val cornHeaderDef = getImplementDef("plataforma_milho_gts_12l")
        val tractorVehDef = getVehicleDef(tractor.defId)
        val combineDef = getVehicleDef("mf_5650")
        assertThat(checkMachineImplementCompatibility(tractorVehDef, cornHeaderDef).hitchCompatible).isFalse()
        assertThat(checkMachineImplementCompatibility(combineDef, cornHeaderDef).hitchCompatible).isTrue()

        val combine = tractor.copy(
            uid = "v_comb",
            defId = "jd_s770",
            x = 0.0,
            z = 0.0,
            attachedImplementUid = null,
            headerLowered = true,
            grainTankFillL = 0.0,
        )
        val cornHeader = OwnedImplement(
            uid = "imp_corn",
            defId = "plataforma_milho_gts_12l",
            x = 0.0,
            z = 2.8,
            headingDeg = 0.0,
        )
        val hitchedWorld = toggleHitchNearestImplement(
            base.copy(vehicles = listOf(combine), implements = listOf(cornHeader), activeVehicleUid = "v_comb")
        )
        assertThat(hitchedWorld.vehicles.first().attachedImplementUid).isEqualTo("imp_corn")
    }

    @Test
    fun modHubCategoriesExternalGraphicsPacksToggleAndMissingFileRefusal() {
        assertThat(HUB_CATEGORIES).containsAtLeast("Plantadeiras", "Pulverizadores", "Carretas", "Gráficos", "Texturas")
        val externalGfx = getExternalGraphicsPacks(DEFAULT_HUB_PACKS)
        assertThat(externalGfx.size).isEqualTo(7)
        assertThat(externalGfx.map { it.packId }).containsExactly(
            "gfx_terra_brasileira",
            "gfx_vegetacao_rural",
            "gfx_estradas_rurais",
            "gfx_tratores_hd",
            "gfx_implementos_hd",
            "gfx_iluminacao_clima",
            "gfx_ambiente_rural",
        )

        val firstGfx = externalGfx.first()
        val valRes = validateModPack(firstGfx, DEFAULT_HUB_PACKS, "android", 4096.0)
        assertThat(valRes.ok).isFalse()
        assertThat(valRes.notes).contains(MOD_MISSING_FILE_ERROR)

        val app = ApplicationProvider.getApplicationContext<Application>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val dm = ModDownloadManager(app, scope)
        var capturedError: String? = null
        dm.startDownload(
            pack = firstGfx,
            allPacks = DEFAULT_HUB_PACKS,
            quotaConfig = StorageQuotaConfig(),
            onComplete = {},
            onError = { capturedError = it },
        )
        assertThat(capturedError).isEqualTo(MOD_MISSING_FILE_ERROR)
        assertThat(dm.downloads.value[firstGfx.packId]?.status).isEqualTo(ModDownloadStatus.ERROR)

        // Category matching & Enable/Disable toggle
        val carretaPacks = DEFAULT_HUB_PACKS.filter { matchesHubCategory(it, "Carretas") }
        assertThat(carretaPacks).isNotEmpty()
        val installedPack = DEFAULT_HUB_PACKS.first { it.installed }
        val toggledOff = toggleHubPackEnabledInList(DEFAULT_HUB_PACKS, installedPack.packId)
        assertThat(toggledOff.first { it.packId == installedPack.packId }.enabled).isFalse()
    }
}
