package com.example.domain

/**
 * Brand-faithful color mapping applied consistently across:
 * 1. 3D Vehicle & Harvester Rendering (`ViewportVehicleRenderer.kt`)
 * 2. Vehicle Tuning & Rim Defaults (`MachineCatalog.kt`)
 * 3. UI Dealership / Garage / Workshop / Gallery Previews (`FarmUiKit.kt`)
 */
data class BrandVisualPalette(
    val primaryRgb: RgbTriple,
    val hoodRgb: RgbTriple,
    val hoodStripeRgb: RgbTriple,
    val decalTextRgb: RgbTriple,
    val rimRgb: RgbTriple,
    val cabRoofRgb: RgbTriple,
    val fenderRgb: RgbTriple,
    val chassisRgb: RgbTriple,
    val seatRgb: RgbTriple,
    val exhaustRgb: RgbTriple,
    val headerFrameRgb: RgbTriple,
    val headerReelRgb: RgbTriple,
    val augerRgb: RgbTriple,
)

private val DARK_CHASSIS = rgb(0.13, 0.14, 0.16)
private val CHARCOAL_FENDER = rgb(0.11, 0.12, 0.13)
private val CHROME_SILVER = rgb(0.84, 0.85, 0.87)
private val MF_SILVER = rgb(0.86, 0.87, 0.89)
private val MF_CHARCOAL = rgb(0.22, 0.23, 0.25)
private val FENDT_TERRA_RED = rgb(0.84, 0.16, 0.14)
private val CLAAS_RED = rgb(0.84, 0.14, 0.14)
private val CLAAS_WHITE = rgb(0.93, 0.93, 0.90)
private val NH_COMBINE_YELLOW = rgb(0.95, 0.74, 0.08)
private val SEAT_DARK = rgb(0.16, 0.17, 0.19)
private val SEAT_CLASSIC_GRAY = rgb(0.28, 0.29, 0.31)
private val EXHAUST_DARK = rgb(0.14, 0.15, 0.16)

fun resolveBrandVisualPalette(
    def: VehicleDef,
    customPaintRgb: RgbTriple? = null,
    rimColorId: String? = null,
): BrandVisualPalette {
    val brandLower = def.brand.lowercase()
    val idLower = def.id.lowercase()
    val modelLower = def.model.lowercase()
    val customRim = RIM_COLORS.find { it.id == (rimColorId ?: "stock") }?.colorRgb
    val baseBody = customPaintRgb ?: def.primaryColorRgb

    val palette = when {
        // JOHN DEERE (Agricultural & Forestry):
        // Green body, GREEN hood (never yellow!), GREEN cab roof,
        // YELLOW wheel rims, thin YELLOW hood lettering/pinstripe, YELLOW seat!
        brandLower.contains("john deere") -> {
            val isCrawler = def.category == VehicleCategory.TRATOR_ESTEIRA
            val isClassic2wd = def.bodyStyle == VehicleBodyStyle.CLASSIC_2WD || def.era == "Clássico"
            BrandVisualPalette(
                primaryRgb = customPaintRgb ?: BrandColors.JD_GREEN,
                hoodRgb = customPaintRgb ?: BrandColors.JD_GREEN,
                hoodStripeRgb = BrandColors.JD_YELLOW,
                decalTextRgb = BrandColors.JD_YELLOW,
                rimRgb = if (isCrawler) DARK_CHASSIS else BrandColors.JD_YELLOW,
                cabRoofRgb = customPaintRgb ?: BrandColors.JD_GREEN,
                fenderRgb = if (isClassic2wd) (customPaintRgb ?: BrandColors.JD_GREEN) else CHARCOAL_FENDER,
                chassisRgb = DARK_CHASSIS,
                seatRgb = BrandColors.JD_YELLOW,
                exhaustRgb = EXHAUST_DARK,
                headerFrameRgb = customPaintRgb ?: BrandColors.JD_GREEN,
                headerReelRgb = BrandColors.JD_YELLOW,
                augerRgb = BrandColors.JD_YELLOW,
            )
        }

        // SLC (Brazil pioneer):
        // 2200 classic is terracotta-orange + cream; 6200/7100/7200/7500/7700 use JD/SLC green + yellow!
        brandLower == "slc" -> {
            val is2200 = idLower.contains("2200")
            if (is2200) {
                BrandVisualPalette(
                    primaryRgb = customPaintRgb ?: rgb(0.84, 0.34, 0.10),
                    hoodRgb = customPaintRgb ?: rgb(0.84, 0.34, 0.10),
                    hoodStripeRgb = rgb(0.92, 0.89, 0.78),
                    decalTextRgb = rgb(0.92, 0.89, 0.78),
                    rimRgb = rgb(0.92, 0.89, 0.78),
                    cabRoofRgb = rgb(0.92, 0.89, 0.78),
                    fenderRgb = DARK_CHASSIS,
                    chassisRgb = DARK_CHASSIS,
                    seatRgb = SEAT_CLASSIC_GRAY,
                    exhaustRgb = EXHAUST_DARK,
                    headerFrameRgb = rgb(0.84, 0.34, 0.10),
                    headerReelRgb = rgb(0.92, 0.89, 0.78),
                    augerRgb = rgb(0.92, 0.89, 0.78),
                )
            } else {
                BrandVisualPalette(
                    primaryRgb = customPaintRgb ?: BrandColors.JD_GREEN,
                    hoodRgb = customPaintRgb ?: BrandColors.JD_GREEN,
                    hoodStripeRgb = BrandColors.JD_YELLOW,
                    decalTextRgb = BrandColors.JD_YELLOW,
                    rimRgb = BrandColors.JD_YELLOW,
                    cabRoofRgb = customPaintRgb ?: BrandColors.JD_GREEN,
                    fenderRgb = CHARCOAL_FENDER,
                    chassisRgb = DARK_CHASSIS,
                    seatRgb = BrandColors.JD_YELLOW,
                    exhaustRgb = EXHAUST_DARK,
                    headerFrameRgb = customPaintRgb ?: BrandColors.JD_GREEN,
                    headerReelRgb = BrandColors.JD_YELLOW,
                    augerRgb = BrandColors.JD_YELLOW,
                )
            }
        }

        // MASSEY FERGUSON:
        // Red body & hood, silver-gray rims & grille/stripes, white roof on classics/combines or MF red roof on modern,
        // IDEAL 9T wears Fendt/AGCO graphite + red accents.
        brandLower.contains("massey") -> {
            val isIdeal = idLower.contains("ideal")
            val isClassic = def.era == "Clássico" || def.category == VehicleCategory.COLHEITADEIRA
            if (isIdeal) {
                BrandVisualPalette(
                    primaryRgb = customPaintRgb ?: rgb(0.20, 0.21, 0.23),
                    hoodRgb = customPaintRgb ?: rgb(0.20, 0.21, 0.23),
                    hoodStripeRgb = BrandColors.MF_RED,
                    decalTextRgb = MF_SILVER,
                    rimRgb = BrandColors.MF_RED,
                    cabRoofRgb = rgb(0.20, 0.21, 0.23),
                    fenderRgb = DARK_CHASSIS,
                    chassisRgb = DARK_CHASSIS,
                    seatRgb = SEAT_DARK,
                    exhaustRgb = EXHAUST_DARK,
                    headerFrameRgb = DARK_CHASSIS,
                    headerReelRgb = BrandColors.MF_RED,
                    augerRgb = rgb(0.24, 0.25, 0.27),
                )
            } else {
                BrandVisualPalette(
                    primaryRgb = customPaintRgb ?: BrandColors.MF_RED,
                    hoodRgb = customPaintRgb ?: BrandColors.MF_RED,
                    hoodStripeRgb = MF_SILVER,
                    decalTextRgb = MF_SILVER,
                    rimRgb = MF_SILVER,
                    cabRoofRgb = if (isClassic) CLAAS_WHITE else (customPaintRgb ?: BrandColors.MF_RED),
                    fenderRgb = if (def.era == "Clássico") (customPaintRgb ?: BrandColors.MF_RED) else MF_CHARCOAL,
                    chassisRgb = MF_CHARCOAL,
                    seatRgb = SEAT_DARK,
                    exhaustRgb = if (def.era == "Clássico") CHROME_SILVER else EXHAUST_DARK,
                    headerFrameRgb = if (def.category == VehicleCategory.COLHEITADEIRA) MF_CHARCOAL else (customPaintRgb ?: BrandColors.MF_RED),
                    headerReelRgb = MF_SILVER,
                    augerRgb = BrandColors.MF_RED,
                )
            }
        }

        // NEW HOLLAND:
        // Tractors = NH Blue body/hood, NH White roof + white/silver rims, white lettering.
        // Combines = NH Harvest Yellow body, NH Blue panels/stripes, NH White cab roof + white rims, blue/yellow header.
        brandLower.contains("new holland") -> {
            val isCombineOrTele = def.category == VehicleCategory.COLHEITADEIRA || def.category == VehicleCategory.MANIPULADOR
            if (isCombineOrTele) {
                BrandVisualPalette(
                    primaryRgb = customPaintRgb ?: NH_COMBINE_YELLOW,
                    hoodRgb = customPaintRgb ?: NH_COMBINE_YELLOW,
                    hoodStripeRgb = BrandColors.NH_BLUE,
                    decalTextRgb = BrandColors.NH_BLUE,
                    rimRgb = BrandColors.NH_WHITE,
                    cabRoofRgb = BrandColors.NH_WHITE,
                    fenderRgb = DARK_CHASSIS,
                    chassisRgb = BrandColors.NH_BLUE,
                    seatRgb = SEAT_DARK,
                    exhaustRgb = EXHAUST_DARK,
                    headerFrameRgb = BrandColors.NH_BLUE,
                    headerReelRgb = NH_COMBINE_YELLOW,
                    augerRgb = NH_COMBINE_YELLOW,
                )
            } else {
                BrandVisualPalette(
                    primaryRgb = customPaintRgb ?: BrandColors.NH_BLUE,
                    hoodRgb = customPaintRgb ?: BrandColors.NH_BLUE,
                    hoodStripeRgb = BrandColors.NH_WHITE,
                    decalTextRgb = BrandColors.NH_WHITE,
                    rimRgb = BrandColors.NH_WHITE,
                    cabRoofRgb = BrandColors.NH_WHITE,
                    fenderRgb = CHARCOAL_FENDER,
                    chassisRgb = DARK_CHASSIS,
                    seatRgb = SEAT_DARK,
                    exhaustRgb = EXHAUST_DARK,
                    headerFrameRgb = BrandColors.NH_BLUE,
                    headerReelRgb = BrandColors.NH_WHITE,
                    augerRgb = BrandColors.NH_BLUE,
                )
            }
        }

        // CASE IH:
        // Case Red body & hood, Black roof/fenders/grille/chassis, silver-white rims (combines black rims or silver), white decal.
        // Note: retro/skid/grader construction equipment keeps construction yellow if originally yellow.
        brandLower.contains("case") -> {
            val isConstruction = def.category == VehicleCategory.RETROESCAVADEIRA ||
                def.category == VehicleCategory.MINICARREGADEIRA ||
                def.category == VehicleCategory.MOTONIVELADORA ||
                def.category == VehicleCategory.CARREGADEIRA
            if (isConstruction) {
                BrandVisualPalette(
                    primaryRgb = baseBody,
                    hoodRgb = baseBody,
                    hoodStripeRgb = BrandColors.CASE_BLACK,
                    decalTextRgb = BrandColors.CASE_BLACK,
                    rimRgb = baseBody,
                    cabRoofRgb = BrandColors.CASE_BLACK,
                    fenderRgb = BrandColors.CASE_BLACK,
                    chassisRgb = BrandColors.CASE_BLACK,
                    seatRgb = SEAT_DARK,
                    exhaustRgb = EXHAUST_DARK,
                    headerFrameRgb = BrandColors.CASE_BLACK,
                    headerReelRgb = baseBody,
                    augerRgb = BrandColors.CASE_BLACK,
                )
            } else {
                BrandVisualPalette(
                    primaryRgb = customPaintRgb ?: BrandColors.CASE_RED,
                    hoodRgb = customPaintRgb ?: BrandColors.CASE_RED,
                    hoodStripeRgb = CLAAS_WHITE,
                    decalTextRgb = CLAAS_WHITE,
                    rimRgb = if (idLower.contains("quadtrac") || idLower.contains("9250")) BrandColors.CASE_BLACK else CHROME_SILVER,
                    cabRoofRgb = BrandColors.CASE_BLACK,
                    fenderRgb = BrandColors.CASE_BLACK,
                    chassisRgb = BrandColors.CASE_BLACK,
                    seatRgb = SEAT_DARK,
                    exhaustRgb = EXHAUST_DARK,
                    headerFrameRgb = BrandColors.CASE_RED,
                    headerReelRgb = CHROME_SILVER,
                    augerRgb = BrandColors.CASE_RED,
                )
            }
        }

        // FENDT:
        // Nature Green body/hood/roof, iconic Terra Red rims, dark Fendt gray chassis, white stripe.
        brandLower.contains("fendt") -> {
            BrandVisualPalette(
                primaryRgb = customPaintRgb ?: BrandColors.FENDT_GREEN,
                hoodRgb = customPaintRgb ?: BrandColors.FENDT_GREEN,
                hoodStripeRgb = CLAAS_WHITE,
                decalTextRgb = CLAAS_WHITE,
                rimRgb = FENDT_TERRA_RED,
                cabRoofRgb = customPaintRgb ?: BrandColors.FENDT_GREEN,
                fenderRgb = BrandColors.FENDT_GRAY,
                chassisRgb = BrandColors.FENDT_GRAY,
                seatRgb = SEAT_DARK,
                exhaustRgb = EXHAUST_DARK,
                headerFrameRgb = BrandColors.FENDT_GREEN,
                headerReelRgb = FENDT_TERRA_RED,
                augerRgb = BrandColors.FENDT_GREEN,
            )
        }

        // CLAAS:
        // Seed Green body/hood, Claas Red rims & stripe, white cab roof & lettering, dark charcoal chassis.
        brandLower.contains("claas") -> {
            BrandVisualPalette(
                primaryRgb = customPaintRgb ?: BrandColors.CLAAS_GREEN,
                hoodRgb = customPaintRgb ?: BrandColors.CLAAS_GREEN,
                hoodStripeRgb = CLAAS_RED,
                decalTextRgb = CLAAS_WHITE,
                rimRgb = CLAAS_RED,
                cabRoofRgb = CLAAS_WHITE,
                fenderRgb = DARK_CHASSIS,
                chassisRgb = DARK_CHASSIS,
                seatRgb = SEAT_DARK,
                exhaustRgb = EXHAUST_DARK,
                headerFrameRgb = BrandColors.CLAAS_GREEN,
                headerReelRgb = CLAAS_RED,
                augerRgb = CLAAS_WHITE,
            )
        }

        // VALTRA:
        // Respects model signature paint (Gold, Red, Blue, Black Edition, White),
        // charcoal chassis/fenders, silver or black rims, crisp contrasting lettering.
        brandLower.contains("valtra") -> {
            val isBlack = !modelLower.contains("ouro") && (idLower.contains("t210") || idLower.contains("s374"))
            val textCol = if (isBlack) BrandColors.VALTRA_GOLD else CLAAS_WHITE
            val rimCol = if (isBlack) BrandColors.VALTRA_BLACK else CHROME_SILVER
            BrandVisualPalette(
                primaryRgb = baseBody,
                hoodRgb = baseBody,
                hoodStripeRgb = textCol,
                decalTextRgb = textCol,
                rimRgb = rimCol,
                cabRoofRgb = if (isBlack) BrandColors.VALTRA_BLACK else baseBody,
                fenderRgb = CHARCOAL_FENDER,
                chassisRgb = BrandColors.VALTRA_BLACK,
                seatRgb = SEAT_DARK,
                exhaustRgb = EXHAUST_DARK,
                headerFrameRgb = baseBody,
                headerReelRgb = CHROME_SILVER,
                augerRgb = baseBody,
            )
        }

        // VALMET (Brazil classic):
        // Signature Valmet Yellow or Orange body/hood, dark chassis, warm silver-cream rims.
        brandLower.contains("valmet") -> {
            BrandVisualPalette(
                primaryRgb = baseBody,
                hoodRgb = baseBody,
                hoodStripeRgb = DARK_CHASSIS,
                decalTextRgb = DARK_CHASSIS,
                rimRgb = rgb(0.88, 0.86, 0.78),
                cabRoofRgb = if (def.hasCab) CLAAS_WHITE else baseBody,
                fenderRgb = baseBody,
                chassisRgb = DARK_CHASSIS,
                seatRgb = SEAT_CLASSIC_GRAY,
                exhaustRgb = EXHAUST_DARK,
                headerFrameRgb = baseBody,
                headerReelRgb = CHROME_SILVER,
                augerRgb = baseBody,
            )
        }

        // CBT: Yellow body/hood, dark charcoal chassis & classic cream-silver or yellow rims.
        brandLower.contains("cbt") -> {
            BrandVisualPalette(
                primaryRgb = customPaintRgb ?: BrandColors.CBT_YELLOW,
                hoodRgb = customPaintRgb ?: BrandColors.CBT_YELLOW,
                hoodStripeRgb = BrandColors.CBT_CHASSIS,
                decalTextRgb = BrandColors.CBT_CHASSIS,
                rimRgb = rgb(0.84, 0.82, 0.74),
                cabRoofRgb = if (def.hasCab) CLAAS_WHITE else BrandColors.CBT_YELLOW,
                fenderRgb = customPaintRgb ?: BrandColors.CBT_YELLOW,
                chassisRgb = BrandColors.CBT_CHASSIS,
                seatRgb = SEAT_CLASSIC_GRAY,
                exhaustRgb = EXHAUST_DARK,
                headerFrameRgb = BrandColors.CBT_YELLOW,
                headerReelRgb = CHROME_SILVER,
                augerRgb = BrandColors.CBT_YELLOW,
            )
        }

        // AGRALE: Classic Green or BX Red with cream wheels/roof.
        brandLower.contains("agrale") -> {
            BrandVisualPalette(
                primaryRgb = baseBody,
                hoodRgb = baseBody,
                hoodStripeRgb = BrandColors.AGRALE_CREAM,
                decalTextRgb = BrandColors.AGRALE_CREAM,
                rimRgb = BrandColors.AGRALE_CREAM,
                cabRoofRgb = if (def.hasCab) BrandColors.AGRALE_CREAM else baseBody,
                fenderRgb = baseBody,
                chassisRgb = DARK_CHASSIS,
                seatRgb = SEAT_DARK,
                exhaustRgb = EXHAUST_DARK,
                headerFrameRgb = baseBody,
                headerReelRgb = BrandColors.AGRALE_CREAM,
                augerRgb = baseBody,
            )
        }

        // FORD (tractors & trucks)
        brandLower.contains("ford") && def.category == VehicleCategory.TRATOR -> {
            BrandVisualPalette(
                primaryRgb = baseBody,
                hoodRgb = baseBody,
                hoodStripeRgb = BrandColors.FORD_GRAY,
                decalTextRgb = BrandColors.FORD_GRAY,
                rimRgb = BrandColors.FORD_GRAY,
                cabRoofRgb = BrandColors.FORD_GRAY,
                fenderRgb = baseBody,
                chassisRgb = if (idLower == "ford_8n") BrandColors.MF_RED else BrandColors.FORD_BLUE,
                seatRgb = SEAT_CLASSIC_GRAY,
                exhaustRgb = EXHAUST_DARK,
                headerFrameRgb = baseBody,
                headerReelRgb = BrandColors.FORD_GRAY,
                augerRgb = baseBody,
            )
        }

        // FIAT / FIATAGRI
        brandLower.contains("fiat") -> {
            BrandVisualPalette(
                primaryRgb = customPaintRgb ?: BrandColors.FIAT_TERRACOTTA,
                hoodRgb = customPaintRgb ?: BrandColors.FIAT_TERRACOTTA,
                hoodStripeRgb = BrandColors.FIAT_WHITE,
                decalTextRgb = BrandColors.FIAT_WHITE,
                rimRgb = BrandColors.FIAT_WHITE,
                cabRoofRgb = BrandColors.FIAT_WHITE,
                fenderRgb = customPaintRgb ?: BrandColors.FIAT_TERRACOTTA,
                chassisRgb = rgb(0.24, 0.20, 0.18),
                seatRgb = SEAT_DARK,
                exhaustRgb = EXHAUST_DARK,
                headerFrameRgb = BrandColors.FIAT_TERRACOTTA,
                headerReelRgb = BrandColors.FIAT_WHITE,
                augerRgb = BrandColors.FIAT_TERRACOTTA,
            )
        }

        // KUBOTA
        brandLower.contains("kubota") -> {
            BrandVisualPalette(
                primaryRgb = customPaintRgb ?: BrandColors.KUBOTA_ORANGE,
                hoodRgb = customPaintRgb ?: BrandColors.KUBOTA_ORANGE,
                hoodStripeRgb = CLAAS_WHITE,
                decalTextRgb = CLAAS_WHITE,
                rimRgb = BrandColors.NH_WHITE,
                cabRoofRgb = customPaintRgb ?: BrandColors.KUBOTA_ORANGE,
                fenderRgb = CHARCOAL_FENDER,
                chassisRgb = DARK_CHASSIS,
                seatRgb = SEAT_DARK,
                exhaustRgb = EXHAUST_DARK,
                headerFrameRgb = BrandColors.KUBOTA_ORANGE,
                headerReelRgb = BrandColors.NH_WHITE,
                augerRgb = BrandColors.KUBOTA_ORANGE,
            )
        }

        // DEUTZ-FAHR
        brandLower.contains("deutz") -> {
            BrandVisualPalette(
                primaryRgb = customPaintRgb ?: BrandColors.DEUTZ_GREEN,
                hoodRgb = customPaintRgb ?: BrandColors.DEUTZ_GREEN,
                hoodStripeRgb = CHROME_SILVER,
                decalTextRgb = CLAAS_WHITE,
                rimRgb = CHROME_SILVER,
                cabRoofRgb = customPaintRgb ?: BrandColors.DEUTZ_GREEN,
                fenderRgb = DARK_CHASSIS,
                chassisRgb = DARK_CHASSIS,
                seatRgb = SEAT_DARK,
                exhaustRgb = EXHAUST_DARK,
                headerFrameRgb = BrandColors.DEUTZ_GREEN,
                headerReelRgb = CHROME_SILVER,
                augerRgb = BrandColors.DEUTZ_GREEN,
            )
        }

        // STEYR / SAME / YANMAR / INTERNATIONAL / URSUS / LANDINI / LS / JCB / STARA / JACTO / TRUCKS
        brandLower.contains("steyr") -> BrandVisualPalette(
            primaryRgb = customPaintRgb ?: BrandColors.STEYR_RED,
            hoodRgb = customPaintRgb ?: BrandColors.STEYR_RED,
            hoodStripeRgb = CLAAS_WHITE,
            decalTextRgb = CLAAS_WHITE,
            rimRgb = CLAAS_WHITE,
            cabRoofRgb = CLAAS_WHITE,
            fenderRgb = DARK_CHASSIS,
            chassisRgb = DARK_CHASSIS,
            seatRgb = SEAT_DARK,
            exhaustRgb = EXHAUST_DARK,
            headerFrameRgb = BrandColors.STEYR_RED,
            headerReelRgb = CLAAS_WHITE,
            augerRgb = BrandColors.STEYR_RED,
        )

        brandLower.contains("international") -> BrandVisualPalette(
            primaryRgb = customPaintRgb ?: BrandColors.IH_RED,
            hoodRgb = customPaintRgb ?: BrandColors.IH_RED,
            hoodStripeRgb = BrandColors.IH_WHITE,
            decalTextRgb = BrandColors.IH_WHITE,
            rimRgb = CHROME_SILVER,
            cabRoofRgb = BrandColors.IH_WHITE,
            fenderRgb = customPaintRgb ?: BrandColors.IH_RED,
            chassisRgb = DARK_CHASSIS,
            seatRgb = SEAT_CLASSIC_GRAY,
            exhaustRgb = CHROME_SILVER,
            headerFrameRgb = BrandColors.IH_RED,
            headerReelRgb = BrandColors.IH_WHITE,
            augerRgb = BrandColors.IH_RED,
        )

        brandLower.contains("jcb") -> BrandVisualPalette(
            primaryRgb = customPaintRgb ?: BrandColors.JCB_YELLOW,
            hoodRgb = customPaintRgb ?: BrandColors.JCB_YELLOW,
            hoodStripeRgb = DARK_CHASSIS,
            decalTextRgb = DARK_CHASSIS,
            rimRgb = customPaintRgb ?: BrandColors.JCB_YELLOW,
            cabRoofRgb = DARK_CHASSIS,
            fenderRgb = DARK_CHASSIS,
            chassisRgb = DARK_CHASSIS,
            seatRgb = SEAT_DARK,
            exhaustRgb = EXHAUST_DARK,
            headerFrameRgb = DARK_CHASSIS,
            headerReelRgb = BrandColors.JCB_YELLOW,
            augerRgb = BrandColors.JCB_YELLOW,
        )

        brandLower.contains("stara") -> BrandVisualPalette(
            primaryRgb = baseBody,
            hoodRgb = baseBody,
            hoodStripeRgb = BrandColors.JD_GREEN,
            decalTextRgb = CLAAS_WHITE,
            rimRgb = CHROME_SILVER,
            cabRoofRgb = baseBody,
            fenderRgb = DARK_CHASSIS,
            chassisRgb = DARK_CHASSIS,
            seatRgb = SEAT_DARK,
            exhaustRgb = EXHAUST_DARK,
            headerFrameRgb = baseBody,
            headerReelRgb = BrandColors.JD_GREEN,
            augerRgb = baseBody,
        )

        brandLower.contains("jacto") -> BrandVisualPalette(
            primaryRgb = baseBody,
            hoodRgb = baseBody,
            hoodStripeRgb = rgb(0.95, 0.44, 0.08),
            decalTextRgb = CLAAS_WHITE,
            rimRgb = CHROME_SILVER,
            cabRoofRgb = rgb(0.95, 0.44, 0.08),
            fenderRgb = DARK_CHASSIS,
            chassisRgb = DARK_CHASSIS,
            seatRgb = SEAT_DARK,
            exhaustRgb = EXHAUST_DARK,
            headerFrameRgb = baseBody,
            headerReelRgb = rgb(0.95, 0.44, 0.08),
            augerRgb = baseBody,
        )

        def.category == VehicleCategory.CAMINHAO -> BrandVisualPalette(
            primaryRgb = baseBody,
            hoodRgb = baseBody,
            hoodStripeRgb = def.accentColorRgb,
            decalTextRgb = CLAAS_WHITE,
            rimRgb = CHROME_SILVER,
            cabRoofRgb = baseBody,
            fenderRgb = DARK_CHASSIS,
            chassisRgb = DARK_CHASSIS,
            seatRgb = SEAT_DARK,
            exhaustRgb = CHROME_SILVER,
            headerFrameRgb = DARK_CHASSIS,
            headerReelRgb = CHROME_SILVER,
            augerRgb = baseBody,
        )

        else -> BrandVisualPalette(
            primaryRgb = baseBody,
            hoodRgb = baseBody,
            hoodStripeRgb = def.accentColorRgb,
            decalTextRgb = if (baseBody.sum() < 1.3) CLAAS_WHITE else DARK_CHASSIS,
            rimRgb = if (def.accentColorRgb.sum() > 2.2) def.accentColorRgb else CHROME_SILVER,
            cabRoofRgb = baseBody,
            fenderRgb = shadeRgb(baseBody, 0.72),
            chassisRgb = DARK_CHASSIS,
            seatRgb = SEAT_DARK,
            exhaustRgb = EXHAUST_DARK,
            headerFrameRgb = baseBody,
            headerReelRgb = def.accentColorRgb,
            augerRgb = baseBody,
        )
    }

    return if (customRim != null) palette.copy(rimRgb = customRim) else palette
}

fun resolveImplementBrandPalette(
    def: ImplementDef,
    customPaintRgb: RgbTriple? = null,
    rimColorId: String? = null,
): BrandVisualPalette {
    val brandLower = def.brand.lowercase()
    val base = customPaintRgb ?: def.colorRgb
    val customRim = RIM_COLORS.find { it.id == (rimColorId ?: "stock") }?.colorRgb
    val basePalette = when {
        brandLower.contains("john deere") -> BrandVisualPalette(
            primaryRgb = customPaintRgb ?: BrandColors.JD_GREEN,
            hoodRgb = customPaintRgb ?: BrandColors.JD_GREEN,
            hoodStripeRgb = BrandColors.JD_YELLOW,
            decalTextRgb = BrandColors.JD_YELLOW,
            rimRgb = BrandColors.JD_YELLOW,
            cabRoofRgb = BrandColors.JD_GREEN,
            fenderRgb = CHARCOAL_FENDER,
            chassisRgb = DARK_CHASSIS,
            seatRgb = BrandColors.JD_YELLOW,
            exhaustRgb = EXHAUST_DARK,
            headerFrameRgb = BrandColors.JD_GREEN,
            headerReelRgb = BrandColors.JD_YELLOW,
            augerRgb = BrandColors.JD_YELLOW,
        )
        brandLower.contains("case") -> BrandVisualPalette(
            primaryRgb = customPaintRgb ?: BrandColors.CASE_RED,
            hoodRgb = customPaintRgb ?: BrandColors.CASE_RED,
            hoodStripeRgb = CLAAS_WHITE,
            decalTextRgb = CLAAS_WHITE,
            rimRgb = CHROME_SILVER,
            cabRoofRgb = BrandColors.CASE_BLACK,
            fenderRgb = BrandColors.CASE_BLACK,
            chassisRgb = BrandColors.CASE_BLACK,
            seatRgb = SEAT_DARK,
            exhaustRgb = EXHAUST_DARK,
            headerFrameRgb = BrandColors.CASE_RED,
            headerReelRgb = CHROME_SILVER,
            augerRgb = BrandColors.CASE_RED,
        )
        brandLower.contains("new holland") -> BrandVisualPalette(
            primaryRgb = base,
            hoodRgb = base,
            hoodStripeRgb = BrandColors.NH_BLUE,
            decalTextRgb = BrandColors.NH_WHITE,
            rimRgb = BrandColors.NH_WHITE,
            cabRoofRgb = BrandColors.NH_BLUE,
            fenderRgb = DARK_CHASSIS,
            chassisRgb = DARK_CHASSIS,
            seatRgb = SEAT_DARK,
            exhaustRgb = EXHAUST_DARK,
            headerFrameRgb = base,
            headerReelRgb = BrandColors.NH_BLUE,
            augerRgb = base,
        )
        brandLower.contains("massey") -> BrandVisualPalette(
            primaryRgb = customPaintRgb ?: BrandColors.MF_RED,
            hoodRgb = customPaintRgb ?: BrandColors.MF_RED,
            hoodStripeRgb = MF_SILVER,
            decalTextRgb = MF_SILVER,
            rimRgb = MF_SILVER,
            cabRoofRgb = MF_CHARCOAL,
            fenderRgb = MF_CHARCOAL,
            chassisRgb = MF_CHARCOAL,
            seatRgb = SEAT_DARK,
            exhaustRgb = EXHAUST_DARK,
            headerFrameRgb = BrandColors.MF_RED,
            headerReelRgb = MF_SILVER,
            augerRgb = BrandColors.MF_RED,
        )
        brandLower.contains("fendt") -> BrandVisualPalette(
            primaryRgb = customPaintRgb ?: BrandColors.FENDT_GREEN,
            hoodRgb = customPaintRgb ?: BrandColors.FENDT_GREEN,
            hoodStripeRgb = FENDT_TERRA_RED,
            decalTextRgb = CLAAS_WHITE,
            rimRgb = FENDT_TERRA_RED,
            cabRoofRgb = BrandColors.FENDT_GREEN,
            fenderRgb = BrandColors.FENDT_GRAY,
            chassisRgb = BrandColors.FENDT_GRAY,
            seatRgb = SEAT_DARK,
            exhaustRgb = EXHAUST_DARK,
            headerFrameRgb = BrandColors.FENDT_GREEN,
            headerReelRgb = FENDT_TERRA_RED,
            augerRgb = BrandColors.FENDT_GREEN,
        )
        brandLower.contains("claas") -> BrandVisualPalette(
            primaryRgb = customPaintRgb ?: BrandColors.CLAAS_GREEN,
            hoodRgb = customPaintRgb ?: BrandColors.CLAAS_GREEN,
            hoodStripeRgb = CLAAS_RED,
            decalTextRgb = CLAAS_WHITE,
            rimRgb = CLAAS_RED,
            cabRoofRgb = CLAAS_WHITE,
            fenderRgb = DARK_CHASSIS,
            chassisRgb = DARK_CHASSIS,
            seatRgb = SEAT_DARK,
            exhaustRgb = EXHAUST_DARK,
            headerFrameRgb = BrandColors.CLAAS_GREEN,
            headerReelRgb = CLAAS_RED,
            augerRgb = CLAAS_WHITE,
        )
        else -> BrandVisualPalette(
            primaryRgb = base,
            hoodRgb = base,
            hoodStripeRgb = CHROME_SILVER,
            decalTextRgb = CLAAS_WHITE,
            rimRgb = CHROME_SILVER,
            cabRoofRgb = base,
            fenderRgb = DARK_CHASSIS,
            chassisRgb = DARK_CHASSIS,
            seatRgb = SEAT_DARK,
            exhaustRgb = EXHAUST_DARK,
            headerFrameRgb = base,
            headerReelRgb = CHROME_SILVER,
            augerRgb = base,
        )
    }
    return if (customRim != null) basePalette.copy(rimRgb = customRim) else basePalette
}

private fun RgbTriple.sum(): Double = r + g + b

private fun shadeRgb(c: RgbTriple, factor: Double): RgbTriple = RgbTriple(
    (c.r * factor).coerceIn(0.0, 1.0),
    (c.g * factor).coerceIn(0.0, 1.0),
    (c.b * factor).coerceIn(0.0, 1.0),
)
