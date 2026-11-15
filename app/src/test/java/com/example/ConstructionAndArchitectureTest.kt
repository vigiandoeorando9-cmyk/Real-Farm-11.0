package com.example

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.domain.BuildingType
import com.example.domain.CONSTRUCTION_CATALOG
import com.example.domain.ConstructionCategory
import com.example.domain.HOUSE_CUSTOMIZATION_PRESETS
import com.example.domain.MAP_SIZE_META
import com.example.domain.MapSizeOption
import com.example.domain.PlacedBuilding
import com.example.domain.PropertyEvolutionTier
import com.example.domain.SoilState
import com.example.domain.StorageQuotaConfig
import com.example.domain.WorldPropType
import com.example.domain.getRecommendedStructuresForMap
import com.example.domain.getStructureDef
import com.example.domain.resolveBuildingVisualProfile
import com.example.domain.resolveRegionalArchStyle
import com.example.simulation.ControlInput
import com.example.simulation.confirmConstructionPurchase
import com.example.simulation.createInitialConstructionPreview
import com.example.simulation.createNewWorld
import com.example.simulation.customizeOwnedHouseBuilding
import com.example.simulation.evaluatePropertyEvolution
import com.example.simulation.getCellIndex
import com.example.simulation.getCellSize
import com.example.simulation.getTerrainHeightAt
import com.example.simulation.isPointBlockedBySolidWorld
import com.example.simulation.stepSimulation
import com.example.simulation.updateConstructionPreviewTransform
import com.example.simulation.validateBuildingPlacement
import com.example.state.FarmViewModel
import com.example.storage.loadWorldFromSlot
import com.example.storage.saveWorldToSlot
import com.example.ui.screens.GameplayHudScreen
import com.example.ui.theme.FarmBrasilTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.hypot

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ConstructionAndArchitectureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun regionalArchitecture_distinctStylesAcrossBrazilAndWorldwideAndPropertyTiers() {
        val mapIds = listOf(
            "map_pr",
            "map_rs",
            "map_sc",
            "map_mt",
            "map_mg",
            "map_ce",
            "map_pa",
            "mod_map_iowa_midwest",
            "mod_map_bavaria_eu",
        )

        val archCodes = mapIds.map { resolveRegionalArchStyle(it).code }.toSet()
        assertThat(archCodes.size).isEqualTo(mapIds.size)

        val signatures = mutableSetOf<String>()
        for (mapId in mapIds) {
            val world = createNewWorld(0, "Fazenda $mapId", mapId, 250000.0, "Padrão", 64)
            val mainHouse = world.buildings.firstOrNull {
                val p = resolveBuildingVisualProfile(it, mapId)
                p.isHouse && !it.uid.startsWith("bld_vila_")
            }
            assertThat(mainHouse).isNotNull()
            val profile = resolveBuildingVisualProfile(mainHouse!!, mapId)
            signatures.add("${profile.archStyle.code}|${profile.materialStyle}|${profile.wallColorHex}|${profile.roofColorHex}")
            assertThat(world.buildings.any { it.uid.startsWith("bld_vila_") }).isTrue()
        }
        assertThat(signatures.size).isAtLeast(7)

        // Property Tiers: Small ("Do Zero") vs Medium ("Padrão") vs Large ("Milionário")
        val smallWorld = createNewWorld(0, "Sítio Pequeno", "map_pr", 0.0, "Do Zero", 64)
        val mediumWorld = createNewWorld(1, "Fazenda Média", "map_pr", 250000.0, "Padrão", 64)
        val largeWorld = createNewWorld(2, "Fazenda Grande", "map_mt", 2500000.0, "Milionário", 64)

        val smallHouse = smallWorld.buildings.first { resolveBuildingVisualProfile(it, "map_pr").isHouse && !it.uid.startsWith("bld_vila_") }
        val medHouse = mediumWorld.buildings.first { resolveBuildingVisualProfile(it, "map_pr").isHouse && !it.uid.startsWith("bld_vila_") }
        val largeHouse = largeWorld.buildings.first { resolveBuildingVisualProfile(it, "map_mt").isHouse && !it.uid.startsWith("bld_vila_") }

        val smallProf = resolveBuildingVisualProfile(smallHouse, "map_pr")
        val medProf = resolveBuildingVisualProfile(medHouse, "map_pr")
        val largeProf = resolveBuildingVisualProfile(largeHouse, "map_mt")

        assertThat(smallProf.widthM).isLessThan(medProf.widthM)
        assertThat(medProf.widthM).isLessThan(largeProf.widthM)
        assertThat(largeProf.hasGarage).isTrue()
        assertThat(largeProf.hasVeranda).isTrue()
        assertThat(smallWorld.buildings.count { it.type == BuildingType.GALPAO }).isAtMost(1)
        assertThat(largeWorld.buildings.count { it.type == BuildingType.GALPAO || it.type == BuildingType.SILO }).isGreaterThan(
            smallWorld.buildings.count { it.type == BuildingType.GALPAO || it.type == BuildingType.SILO }
        )
    }

    @Test
    fun constructionCatalog_allNineCategoriesPopulatedAndOrderedByRegion() {
        assertThat(ConstructionCategory.entries).hasSize(9)
        for (cat in ConstructionCategory.entries) {
            val items = CONSTRUCTION_CATALOG.filter { it.category == cat }
            assertThat(items).isNotEmpty()
            for (def in items) {
                assertThat(def.widthM).isGreaterThan(0.0)
                assertThat(def.depthM).isGreaterThan(0.0)
                assertThat(def.heightM).isGreaterThan(0.0)
                assertThat(def.priceBrl).isGreaterThan(0.0)
            }
        }
        val neRec = getRecommendedStructuresForMap("map_ce")
        val firstHouseNe = neRec.first { it.category == ConstructionCategory.CASAS }
        assertThat(firstHouseNe.modelId).isEqualTo("casa_alpendre_nordeste")

        val paRec = getRecommendedStructuresForMap("map_pa")
        val firstHousePa = paRec.first { it.category == ConstructionCategory.CASAS }
        assertThat(firstHousePa.modelId).isEqualTo("casa_amazonica_palafita")
    }

    @Test
    fun terrainPlacementValidation_rejectsWaterRoadsAppUnownedSteepOverlapAndAllowsOwnedPlots() {
        val base = createNewWorld(0, "Fazenda Obra", "map_pr", 500000.0, "Padrão", 64)
        val shedDef = getStructureDef("galpao_madeira_sul")!!

        // 1. Valid spot inside owned Plot 1 (with clean terrain & no overlap)
        val flatCells = base.cells.map { c ->
            if (c.gx in 28..36 && c.gz in 28..36) c.copy(height = 2.0, soilState = SoilState.PASTAGEM, plotId = 1) else c
        }
        val flatWorld = base.copy(
            cells = flatCells,
            buildings = emptyList(),
            vehicles = emptyList(),
            implements = emptyList(),
            props = emptyList(),
        )
        val okRes = validateBuildingPlacement(flatWorld, shedDef, x = 0.0, z = 0.0, rotationDeg = 0.0)
        assertThat(okRes.isValid).isTrue()

        // 2. Rejects water cell
        val waterCells = flatCells.mapIndexed { idx, c ->
            if (c.gx == 32 && c.gz == 32) c.copy(soilState = SoilState.AGUA_RIO) else c
        }
        val waterRes = validateBuildingPlacement(flatWorld.copy(cells = waterCells), shedDef, x = 0.0, z = 0.0, rotationDeg = 0.0)
        assertThat(waterRes.isValid).isFalse()
        assertThat(waterRes.reason).contains("água")

        // 3. Rejects asphalt road
        val roadCells = flatCells.map { c ->
            if (c.gx == 32 && c.gz == 32) c.copy(soilState = SoilState.ESTRADA_ASFALTO) else c
        }
        val roadRes = validateBuildingPlacement(flatWorld.copy(cells = roadCells), shedDef, x = 0.0, z = 0.0, rotationDeg = 0.0)
        assertThat(roadRes.isValid).isFalse()
        assertThat(roadRes.reason).contains("rodovia")

        // 4. Rejects APP protected reserve
        val appCells = flatCells.map { c ->
            if (c.gx == 32 && c.gz == 32) c.copy(soilState = SoilState.AREA_PRESERVACAO) else c
        }
        val appRes = validateBuildingPlacement(flatWorld.copy(cells = appCells), shedDef, x = 0.0, z = 0.0, rotationDeg = 0.0)
        assertThat(appRes.isValid).isFalse()
        assertThat(appRes.reason).contains("Preservação")

        // 5. Rejects unowned plot
        val unownedCells = flatCells.map { c -> c.copy(plotId = 2) }
        val unownedRes = validateBuildingPlacement(flatWorld.copy(cells = unownedCells), shedDef, x = 0.0, z = 0.0, rotationDeg = 0.0)
        assertThat(unownedRes.isValid).isFalse()
        assertThat(unownedRes.reason).contains("outro dono")

        // 6. Rejects steep terrain
        val steepCells = flatCells.map { c ->
            if (c.gx <= 32) c.copy(height = 0.0) else c.copy(height = 8.5)
        }
        val steepRes = validateBuildingPlacement(flatWorld.copy(cells = steepCells), shedDef, x = 0.0, z = 0.0, rotationDeg = 0.0)
        assertThat(steepRes.isValid).isFalse()
        assertThat(steepRes.reason).contains("inclinado")

        // 7. Rejects overlap against existing building and parked tractor
        val existingHouse = PlacedBuilding("h_ex", BuildingType.CASA, x = 3.0, z = 0.0, rotationDeg = 0.0)
        val overlapRes = validateBuildingPlacement(
            flatWorld.copy(buildings = listOf(existingHouse)),
            shedDef,
            x = 0.0,
            z = 0.0,
            rotationDeg = 0.0,
        )
        assertThat(overlapRes.isValid).isFalse()
        assertThat(overlapRes.reason).contains("Colisão")

        val parkedTractor = base.vehicles.first().copy(x = 0.0, z = 0.0)
        val vehOverlapRes = validateBuildingPlacement(
            flatWorld.copy(vehicles = listOf(parkedTractor)),
            shedDef,
            x = 0.0,
            z = 0.0,
            rotationDeg = 0.0,
        )
        assertThat(vehOverlapRes.isValid).isFalse()
        assertThat(vehOverlapRes.reason).contains("Veículo")
    }

    @Test
    @Config(sdk = [35], qualifiers = "w800dp-h480dp-land")
    fun constructionPurchaseFlow_hudPreviewCancelConfirmFinancialAndLedgerIntegrity() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = FarmViewModel(app)
        vm.startNewCareer(0, "Fazenda Construtora", "map_pr", 500000.0, "Padrão", 64)

        composeRule.setContent {
            val ws by vm.worldState.collectAsStateWithLifecycle()
            val settings by vm.settings.collectAsStateWithLifecycle()
            val cameraMode by vm.cameraMode.collectAsStateWithLifecycle()
            val timeScale by vm.timeScale.collectAsStateWithLifecycle()
            val activeTab by vm.activeTab.collectAsStateWithLifecycle()
            val showFullMap by vm.showFullMap.collectAsStateWithLifecycle()
            val hudHidden by vm.hudHidden.collectAsStateWithLifecycle()
            FarmBrasilTheme {
                GameplayHudScreen(
                    viewModel = vm,
                    worldState = ws,
                    settings = settings,
                    cameraMode = cameraMode,
                    timeScale = timeScale,
                    activeTab = activeTab,
                    showFullMap = showFullMap,
                    hudHidden = hudHidden,
                    roomMembers = emptyList(),
                    localUid = "local_test",
                )
            }
        }
        composeRule.waitForIdle()

        val initialMoney = vm.worldState.value.moneyBrl
        val initialBuildingCount = vm.worldState.value.buildings.size

        // Open Menu -> Propriedades -> Garagens -> Garagem Simples
        composeRule.onNodeWithTag("hud_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_item_propriedades").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("construction_catalog_list").performScrollToNode(hasTestTag("cat_garagens"))
        composeRule.onNodeWithTag("cat_garagens").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("construction_catalog_list").performScrollToIndex(2)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("construct_item_garagem_madeira_simples").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Construction HUD must appear and ZERO money has been deducted yet!
        assertThat(vm.constructionPreview.value).isNotNull()
        assertThat(vm.activeTab.value).isNull()
        composeRule.onNodeWithTag("construction_placement_hud").assertIsDisplayed()
        composeRule.onNodeWithTag("construction_confirm_btn").assertExists()
        assertThat(vm.worldState.value.moneyBrl).isEqualTo(initialMoney)
        assertThat(vm.worldState.value.buildings.size).isEqualTo(initialBuildingCount)

        // Rotate and nudge preview, then CANCEL: still ZERO deduction & ZERO buildings added
        composeRule.onNodeWithTag("construction_rotate_fine_btn").performClick()
        composeRule.waitForIdle()
        assertThat(vm.constructionPreview.value?.rotationDeg).isWithin(0.1).of(15.0)
        composeRule.onNodeWithTag("construction_cancel_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertThat(vm.constructionPreview.value).isNull()
        assertThat(vm.worldState.value.moneyBrl).isEqualTo(initialMoney)
        assertThat(vm.worldState.value.buildings.size).isEqualTo(initialBuildingCount)

        // Start a structure in clean valid spot, confirm -> exact 1x deduction + ledger + history
        val garDef = getStructureDef("garagem_madeira_simples")!!
        vm.startConstructionPlacement(garDef)
        vm.setConstructionPreviewPosition(18.0, 26.0)
        composeRule.waitForIdle()
        if (vm.constructionPreview.value?.isValid != true) {
            // Find guaranteed clean yard point from spiral or clear area
            val clean = createInitialConstructionPreview(vm.worldState.value, garDef)
            vm.setConstructionPreviewPosition(clean.x, clean.z)
            composeRule.waitForIdle()
        }
        assertThat(vm.constructionPreview.value?.isValid).isTrue()
        composeRule.onNodeWithTag("construction_confirm_btn").performScrollTo().assertIsEnabled().performClick()
        composeRule.waitForIdle()

        assertThat(vm.constructionPreview.value).isNull()
        assertThat(vm.worldState.value.moneyBrl).isWithin(0.01).of(initialMoney - garDef.priceBrl)
        assertThat(vm.worldState.value.buildings.size).isEqualTo(initialBuildingCount + 1)
        assertThat(vm.worldState.value.ledger.any { it.description.contains(garDef.name) && it.amountBrl == -garDef.priceBrl }).isTrue()
        assertThat(vm.worldState.value.purchaseHistory.orEmpty().any { it.itemName == garDef.name && it.amountBrl == garDef.priceBrl }).isTrue()

        // Insufficient funds blocks confirm
        vm.startNewCareer(1, "Fazenda Sem Caixa", "map_pr", 0.0, "Do Zero", 64)
        val mansionDef = getStructureDef("casa_sede_grande_garagem")!!
        vm.startConstructionPlacement(mansionDef)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("construction_confirm_btn").assertIsNotEnabled()
    }

    @Test
    fun safeHouseReplacement_customizationPresetsAndFourTierEvolution() {
        val base = createNewWorld(0, "Fazenda Evolução", "map_mg", 1500000.0, "Do Zero", 64)
        val initialHouse = base.buildings.first { resolveBuildingVisualProfile(it, "map_mg").isHouse && !it.uid.startsWith("bld_vila_") }
        val houseCountBefore = base.buildings.size

        // 1. Safe replacement preview does NOT delete initialHouse if canceled
        val bigHouseDef = getStructureDef("casa_sede_grande_garagem")!!
        val repPreview = createInitialConstructionPreview(base, bigHouseDef, replaceHouseUid = initialHouse.uid)
        assertThat(repPreview.replaceHouseUid).isEqualTo(initialHouse.uid)
        assertThat(base.buildings.any { it.uid == initialHouse.uid }).isTrue()

        // Confirm replacement in valid position -> removes old house AND places new house cleanly
        val clearWorld = base.copy(
            vehicles = emptyList(),
            implements = emptyList(),
            props = emptyList(),
            buildings = listOf(initialHouse),
        )
        val validPreview = createInitialConstructionPreview(clearWorld, bigHouseDef, replaceHouseUid = initialHouse.uid)
        val (afterReplace, _) = confirmConstructionPurchase(clearWorld, validPreview)
        assertThat(afterReplace.buildings.any { it.uid == initialHouse.uid }).isFalse()
        assertThat(afterReplace.buildings.any { it.modelId == "casa_sede_grande_garagem" }).isTrue()
        assertThat(afterReplace.buildings.size).isEqualTo(1)
        assertThat(afterReplace.moneyBrl).isWithin(0.01).of(clearWorld.moneyBrl - bigHouseDef.priceBrl)

        // 2. Customize house (preset + add veranda + add garage) on a simple house
        val customBase = base.copy(moneyBrl = 200000.0)
        val simpleHouse = PlacedBuilding(
            uid = "custom_casa_1",
            type = BuildingType.CASA,
            x = -2.0,
            z = 32.0,
            rotationDeg = 0.0,
            modelId = "casa_madeira_simples",
            hasVeranda = false,
            hasGarage = false,
        )
        val worldWithSimple = customBase.copy(buildings = listOf(simpleHouse))
        val preset = HOUSE_CUSTOMIZATION_PRESETS.first { it.id == "verde_fazenda" }
        val (afterPreset, _) = customizeOwnedHouseBuilding(
            state = worldWithSimple,
            buildingUid = simpleHouse.uid,
            presetId = preset.id,
            addVeranda = true,
            addGarage = true,
        )
        // Cost = 3500 (paint/material) + 12000 (veranda) + 18500 (garage) = 34000
        assertThat(afterPreset.moneyBrl).isWithin(0.01).of(200000.0 - 34000.0)
        val modHouse = afterPreset.buildings.first { it.uid == simpleHouse.uid }
        assertThat(modHouse.wallColorHex).isEqualTo(preset.wallHex)
        assertThat(modHouse.hasVeranda).isTrue()
        assertThat(modHouse.hasGarage).isTrue()

        // 3. 4-Tier Evolution Progression (must NEVER mutate player buildings automatically)
        val t1World = base.copy(
            plots = base.plots.map { it.copy(owned = false) },
            buildings = listOf(simpleHouse),
        )
        val t1 = evaluatePropertyEvolution(t1World)
        assertThat(t1.currentTier).isEqualTo(PropertyEvolutionTier.PEQUENA_INICIAL)

        val masonryHouse = simpleHouse.copy(materialStyle = "reboco_fazenda", hasVeranda = true)
        val shed = PlacedBuilding("ev_galp", BuildingType.GALPAO, x = 20.0, z = 20.0, rotationDeg = 0.0)
        val t2World = t1World.copy(
            plots = base.plots.mapIndexed { idx, p -> p.copy(owned = idx == 0) },
            buildings = listOf(masonryHouse, shed),
        )
        val t2 = evaluatePropertyEvolution(t2World)
        assertThat(t2.currentTier).isEqualTo(PropertyEvolutionTier.RURAL_INTERMEDIARIA)

        val silo1 = PlacedBuilding("ev_silo1", BuildingType.SILO, x = 35.0, z = 20.0, rotationDeg = 0.0)
        val barn1 = PlacedBuilding("ev_cele1", BuildingType.CELEIRO, x = 45.0, z = 20.0, rotationDeg = 0.0)
        val gar1 = PlacedBuilding("ev_gar1", BuildingType.GARAGEM, x = -20.0, z = 20.0, rotationDeg = 0.0)
        val jardim = PlacedBuilding("ev_jardim", BuildingType.JARDIM, x = -10.0, z = 20.0, rotationDeg = 0.0)
        val cerca1 = PlacedBuilding("ev_cerca1", BuildingType.CERCA, x = -15.0, z = 20.0, rotationDeg = 0.0)
        val patio = PlacedBuilding("ev_patio", BuildingType.PATIO, x = 0.0, z = 20.0, rotationDeg = 0.0)

        val t3World = t2World.copy(
            plots = base.plots.mapIndexed { idx, p -> p.copy(owned = idx < 2) },
            buildings = listOf(masonryHouse, shed, silo1, jardim, patio),
        )
        val t3 = evaluatePropertyEvolution(t3World)
        assertThat(t3.currentTier).isEqualTo(PropertyEvolutionTier.FAZENDA_GRANDE)

        val t4World = t3World.copy(
            plots = base.plots.mapIndexed { idx, p -> p.copy(owned = idx < 4) },
            buildings = listOf(masonryHouse, shed, gar1, silo1, barn1, jardim, cerca1, patio),
        )
        val t4 = evaluatePropertyEvolution(t4World)
        assertThat(t4.currentTier).isEqualTo(PropertyEvolutionTier.FAZENDA_DESENVOLVIDA)
        // Verify original building references stayed untouched
        assertThat(t1World.buildings.size).isEqualTo(1)
        assertThat(houseCountBefore).isGreaterThan(1)
    }

    @Test
    fun collisionsAndFlora_openBayDriveInWallsPillarsTreesGatesAndNoCropFloraOverlap() {
        val base = createNewWorld(0, "Fazenda Colisão 2", "map_pr", 500000.0, "Padrão", 64)
        val flatCells = base.cells.map { it.copy(height = 2.0, soilState = SoilState.ESTRADA_TERRA) }
        val tractorBase = base.vehicles.first().copy(
            uid = "v_col",
            defId = "valmet_65id",
            x = 0.0,
            z = 0.0,
            headingDeg = 0.0,
            engineRunning = true,
            handbrakeOn = false,
            speedMs = 5.0,
        )

        // 1. Open-front GARAGEM allows driving INSIDE the bay, while rear wall stops tractor from exiting the back
        val openGarage = PlacedBuilding("test_gar_open", BuildingType.GARAGEM, x = 0.0, z = 18.0, rotationDeg = 0.0, modelId = "garagem_alvenaria_dupla")
        var garState = base.copy(
            cells = flatCells,
            buildings = listOf(openGarage),
            props = emptyList(),
            implements = emptyList(),
            roadVehicles = emptyList(),
            vehicles = listOf(tractorBase.copy(x = 0.0, z = 8.0, speedMs = 4.5)),
            activeVehicleUid = "v_col",
        )
        repeat(75) {
            garState = stepSimulation(garState, ControlInput(throttle = 1.0), dtSec = 0.05, timeScale = 1.0)
        }
        val insideZ = garState.vehicles.first().z
        // Depth is 6.8m centered at z=18 -> entrance z=14.6, center z=18.0, rear wall z=21.0
        assertThat(insideZ).isGreaterThan(15.2)
        assertThat(insideZ).isLessThan(20.6)

        // 2. Walkable JARDIM & CAMINHO_TERRA never block vehicle movement
        val garden = PlacedBuilding("test_jardim", BuildingType.JARDIM, x = 0.0, z = 14.0, rotationDeg = 0.0, modelId = "jardim_pergolado_casa")
        val path = PlacedBuilding("test_caminho", BuildingType.CAMINHO_TERRA, x = 0.0, z = 20.0, rotationDeg = 0.0, modelId = "caminho_terra_quintal")
        assertThat(isPointBlockedBySolidWorld(base.copy(buildings = listOf(garden, path), props = emptyList()), 0.0, 14.0, 0.8)).isFalse()
        assertThat(isPointBlockedBySolidWorld(base.copy(buildings = listOf(garden, path), props = emptyList()), 0.0, 20.0, 0.8)).isFalse()

        // 3. Solid CELEIRO & ARVORE_ORNAMENTAL block vehicle passage
        val barn = PlacedBuilding("test_celeiro", BuildingType.CELEIRO, x = 0.0, z = 18.0, rotationDeg = 0.0, modelId = "celeiro_madeira_tradicional")
        var barnState = garState.copy(buildings = listOf(barn), vehicles = listOf(tractorBase.copy(x = 0.0, z = 6.0, speedMs = 5.0)))
        repeat(60) {
            barnState = stepSimulation(barnState, ControlInput(throttle = 1.0), dtSec = 0.05, timeScale = 1.0)
        }
        assertThat(barnState.vehicles.first().z).isLessThan(13.6)

        val ornTree = PlacedBuilding("test_arvore_bld", BuildingType.ARVORE_ORNAMENTAL, x = 0.0, z = 12.0, rotationDeg = 0.0, modelId = "arvore_sombra_ipe")
        assertThat(isPointBlockedBySolidWorld(base.copy(buildings = listOf(ornTree), props = emptyList()), 0.0, 12.0, 0.8)).isTrue()

        // 4. Generated flora props across Brazil (MG/MT/CE/PR/PA) never sit inside crop plots or asphalt roads
        for (mId in listOf("map_pr", "map_mg", "map_mt", "map_ce", "map_pa")) {
            val w = createNewWorld(0, "Check Flora", mId, 250000.0, "Padrão", 64)
            for (p in w.props) {
                if (p.type == WorldPropType.FLOWER_BED_AZALEA || p.type == WorldPropType.ORNAMENTAL_HEDGE ||
                    p.type == WorldPropType.TREE_IPE_AMARELO || p.type == WorldPropType.TREE_IPE_ROXO ||
                    p.type == WorldPropType.TREE_SHADE_MANGUEIRA || p.type == WorldPropType.BANANA_CLUMP
                ) {
                    val cIdx = getCellIndex(p.x, p.z, 64)
                    if (cIdx != null) {
                        val c = w.cells[cIdx]
                        assertThat(c.soilState).isNotEqualTo(SoilState.ESTRADA_ASFALTO)
                        assertThat(c.soilState).isNotEqualTo(SoilState.PRONTO_COLHEITA)
                        assertThat(c.soilState).isNotEqualTo(SoilState.CRESCENDO)
                    }
                }
            }
            // Also verify buildings rest grounded cleanly on finite terrain heights
            for (b in w.buildings) {
                val hy = getTerrainHeightAt(w, b.x, b.z)
                assertThat(hy.isFinite()).isTrue()
            }
        }
    }

    @Test
    fun giantMapAnd8GbStorageQuota_192x192MapAndSaveLoadRoundTrip() {
        // 1. Storage Quota Config defaults to 8192 MB (8 GB)
        val defaultQuota = StorageQuotaConfig()
        assertThat(defaultQuota.mobileLimitMb).isEqualTo(8192.0)

        // 2. GIGANTE_CONTINENTAL map option exists with gridSide 192 & 8m cells (1536m x 1536m)
        assertThat(MAP_SIZE_META[MapSizeOption.GIGANTE_CONTINENTAL]?.gridSide).isEqualTo(192)
        assertThat(getCellSize(192)).isEqualTo(8)

        val giantWorld = createNewWorld(
            slotIndex = 3,
            farmName = "Fazenda 8GB Gigante",
            mapId = "map_mt",
            startingMoneyBrl = 500000.0,
            startingMode = "Padrão",
            requestedGridSide = 192,
        )
        assertThat(giantWorld.mapGridSize).isEqualTo(192)
        assertThat(giantWorld.cells.size).isEqualTo(192 * 192)
        assertThat(giantWorld.plots.size).isAtLeast(50)

        // Place a customized house on the giant map and verify Save/Load round-trip preserves everything
        val app = ApplicationProvider.getApplicationContext<Application>()
        val customBuilding = PlacedBuilding(
            uid = "giant_custom_casa",
            type = BuildingType.CASA,
            x = -24.0,
            z = 42.0,
            rotationDeg = 30.0,
            modelId = "casa_cerrado_moderna",
            archStyle = "cerrado_mt_fazenda",
            materialStyle = "tijolinho",
            wallColorHex = "#EAE1D0",
            roofColorHex = "#5A3D31",
            trimColorHex = "#A24C2D",
            hasVeranda = true,
            hasGarage = true,
            customWidthM = 13.0,
            customDepthM = 9.5,
            customHeightM = 4.9,
            customLabel = "Sede Centro-Oeste",
        )
        val stateToSave = giantWorld.copy(buildings = giantWorld.buildings + customBuilding)
        val (savedOk, _) = saveWorldToSlot(app, stateToSave)
        assertThat(savedOk).isTrue()

        val (loaded, isBackup) = loadWorldFromSlot(app, 3)
        assertThat(isBackup).isFalse()
        assertThat(loaded).isNotNull()
        assertThat(loaded!!.mapGridSize).isEqualTo(192)
        assertThat(loaded.cells.size).isEqualTo(192 * 192)
        val restoredHouse = loaded.buildings.firstOrNull { it.uid == "giant_custom_casa" }
        assertThat(restoredHouse).isNotNull()
        assertThat(restoredHouse!!.modelId).isEqualTo("casa_cerrado_moderna")
        assertThat(restoredHouse.hasGarage).isTrue()
        assertThat(restoredHouse.hasVeranda).isTrue()
        assertThat(restoredHouse.wallColorHex).isEqualTo("#EAE1D0")
    }
}
