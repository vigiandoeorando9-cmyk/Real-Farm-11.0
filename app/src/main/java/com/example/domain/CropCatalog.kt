package com.example.domain

import kotlinx.serialization.Serializable

@Serializable
data class CropDef(
    val id: String,
    val name: String,
    val category: CropCategory,
    val plantingMonths: List<Int>,
    val growthDays: Int,
    val waterNeed: Double,
    val nutrientNeed: Double,
    val baseYieldTonsPerHa: Double,
    val unitName: String,
    val kgPerUnit: Double,
    val basePricePerUnitBrl: Double,
    val seedCostPerHaBrl: Double,
    val seedColorRgb: RgbTriple,
    val ripeColorRgb: RgbTriple,
    val maxHeightMeters: Double,
    val industrialOutputs: List<String>,
    val requiresPlanter: Boolean = true,
)

private fun c(
    id: String,
    name: String,
    category: CropCategory,
    plantingMonths: List<Int>,
    growthDays: Int,
    waterNeed: Double,
    nutrientNeed: Double,
    baseYieldTonsPerHa: Double,
    unitName: String,
    kgPerUnit: Double,
    basePricePerUnitBrl: Double,
    seedCostPerHaBrl: Double,
    seedColorRgb: RgbTriple,
    ripeColorRgb: RgbTriple,
    maxHeightMeters: Double,
    industrialOutputs: List<String>,
    requiresPlanter: Boolean = true,
): CropDef = CropDef(
    id = id,
    name = name,
    category = category,
    plantingMonths = plantingMonths,
    growthDays = growthDays,
    waterNeed = waterNeed,
    nutrientNeed = nutrientNeed,
    baseYieldTonsPerHa = baseYieldTonsPerHa,
    unitName = unitName,
    kgPerUnit = kgPerUnit,
    basePricePerUnitBrl = basePricePerUnitBrl,
    seedCostPerHaBrl = seedCostPerHaBrl,
    seedColorRgb = seedColorRgb,
    ripeColorRgb = ripeColorRgb,
    maxHeightMeters = maxHeightMeters,
    industrialOutputs = industrialOutputs,
    requiresPlanter = requiresPlanter,
)

val ALL_CROPS: List<CropDef> = listOf(
    // 1. Grãos e Leguminosas (16)
    c("soja", "Soja", CropCategory.GRAOS_LEGUMINOSAS, listOf(9, 10, 11, 12), 4, 0.65, 0.70, 3.8, "saca 60kg", 60.0, 134.0, 480.0, rgb(0.24, 0.60, 0.20), rgb(0.76, 0.66, 0.25), 0.9, listOf("Óleo de Soja", "Farelo de Soja")),
    c("milho", "Milho", CropCategory.GRAOS_LEGUMINOSAS, listOf(1, 2, 3, 9, 10, 11), 4, 0.75, 0.85, 8.5, "saca 60kg", 60.0, 68.0, 620.0, rgb(0.20, 0.62, 0.18), rgb(0.84, 0.72, 0.22), 2.1, listOf("Fubá", "Farinha", "Amido", "Ração", "Etanol")),
    c("feijao_carioca", "Feijão Carioca", CropCategory.GRAOS_LEGUMINOSAS, listOf(2, 3, 10, 11), 3, 0.60, 0.65, 2.6, "saca 60kg", 60.0, 245.0, 510.0, rgb(0.26, 0.58, 0.22), rgb(0.74, 0.62, 0.34), 0.6, listOf("Feijão Selecionado")),
    c("feijao_preto", "Feijão-Preto", CropCategory.GRAOS_LEGUMINOSAS, listOf(2, 3, 9, 10), 3, 0.60, 0.65, 2.5, "saca 60kg", 60.0, 260.0, 490.0, rgb(0.24, 0.55, 0.22), rgb(0.55, 0.48, 0.32), 0.6, listOf("Feijão-Preto Embalado")),
    c("feijao_caupi", "Feijão-Caupi", CropCategory.GRAOS_LEGUMINOSAS, listOf(1, 2, 3, 4), 3, 0.45, 0.50, 1.9, "saca 60kg", 60.0, 220.0, 380.0, rgb(0.28, 0.59, 0.23), rgb(0.75, 0.68, 0.36), 0.55, listOf("Feijão-Caupi Beneficiado")),
    c("arroz", "Arroz", CropCategory.GRAOS_LEGUMINOSAS, listOf(9, 10, 11), 4, 0.95, 0.75, 7.8, "saca 50kg", 50.0, 108.0, 560.0, rgb(0.28, 0.68, 0.24), rgb(0.85, 0.78, 0.32), 1.0, listOf("Arroz Beneficiado", "Farelo de Arroz"), false),
    c("trigo", "Trigo", CropCategory.GRAOS_LEGUMINOSAS, listOf(4, 5, 6), 4, 0.55, 0.70, 3.4, "saca 60kg", 60.0, 84.0, 440.0, rgb(0.25, 0.62, 0.24), rgb(0.88, 0.76, 0.34), 0.95, listOf("Farinha de Trigo", "Massas e Pães"), false),
    c("aveia", "Aveia", CropCategory.GRAOS_LEGUMINOSAS, listOf(4, 5, 6), 3, 0.50, 0.55, 2.8, "saca 60kg", 60.0, 72.0, 320.0, rgb(0.26, 0.64, 0.25), rgb(0.84, 0.77, 0.40), 0.9, listOf("Aveia em Flocos", "Palhada Direta"), false),
    c("cevada", "Cevada", CropCategory.GRAOS_LEGUMINOSAS, listOf(5, 6), 4, 0.55, 0.65, 3.5, "saca 60kg", 60.0, 92.0, 410.0, rgb(0.24, 0.60, 0.22), rgb(0.86, 0.74, 0.36), 0.9, listOf("Malte Cervejeiro"), false),
    c("centeio", "Centeio", CropCategory.GRAOS_LEGUMINOSAS, listOf(4, 5, 6), 3, 0.45, 0.50, 2.6, "saca 60kg", 60.0, 78.0, 310.0, rgb(0.25, 0.58, 0.25), rgb(0.80, 0.72, 0.38), 1.1, listOf("Farinha de Centeio"), false),
    c("triticale", "Triticale", CropCategory.GRAOS_LEGUMINOSAS, listOf(4, 5, 6), 4, 0.50, 0.60, 3.6, "saca 60kg", 60.0, 74.0, 350.0, rgb(0.24, 0.61, 0.23), rgb(0.82, 0.73, 0.35), 1.05, listOf("Ração Animal", "Farinha"), false),
    c("sorgo", "Sorgo", CropCategory.GRAOS_LEGUMINOSAS, listOf(1, 2, 3, 10, 11), 3, 0.40, 0.55, 4.8, "saca 60kg", 60.0, 56.0, 340.0, rgb(0.23, 0.57, 0.19), rgb(0.75, 0.44, 0.20), 1.6, listOf("Silagem de Sorgo", "Ração")),
    c("milheto", "Milheto", CropCategory.GRAOS_LEGUMINOSAS, listOf(2, 3, 9, 10), 2, 0.35, 0.40, 2.4, "saca 60kg", 60.0, 64.0, 220.0, rgb(0.28, 0.62, 0.21), rgb(0.76, 0.70, 0.36), 1.7, listOf("Palhada Plantio Direto", "Ração"), false),
    c("ervilha", "Ervilha", CropCategory.GRAOS_LEGUMINOSAS, listOf(4, 5, 6), 3, 0.55, 0.55, 2.7, "saca 60kg", 60.0, 195.0, 420.0, rgb(0.28, 0.66, 0.24), rgb(0.66, 0.72, 0.30), 0.7, listOf("Ervilha Seca Embalada")),
    c("lentilha", "Lentilha", CropCategory.GRAOS_LEGUMINOSAS, listOf(4, 5, 6), 3, 0.50, 0.50, 1.8, "saca 60kg", 60.0, 290.0, 460.0, rgb(0.27, 0.60, 0.24), rgb(0.72, 0.64, 0.32), 0.5, listOf("Lentilha Beneficiada")),
    c("grao_de_bico", "Grão-de-Bico", CropCategory.GRAOS_LEGUMINOSAS, listOf(4, 5, 6), 4, 0.45, 0.55, 2.2, "saca 60kg", 60.0, 310.0, 520.0, rgb(0.25, 0.59, 0.24), rgb(0.78, 0.70, 0.38), 0.6, listOf("Grão-de-Bico Selecionado")),

    // 2. Oleaginosas (6)
    c("canola", "Canola", CropCategory.OLEAGINOSAS, listOf(4, 5, 6), 4, 0.55, 0.70, 2.1, "saca 60kg", 60.0, 155.0, 410.0, rgb(0.24, 0.58, 0.21), rgb(0.92, 0.82, 0.14), 1.1, listOf("Óleo de Canola", "Farelo"), false),
    c("girassol", "Girassol", CropCategory.OLEAGINOSAS, listOf(2, 3, 9, 10), 3, 0.45, 0.60, 2.2, "saca 60kg", 60.0, 148.0, 390.0, rgb(0.24, 0.58, 0.19), rgb(0.96, 0.78, 0.12), 1.8, listOf("Óleo de Girassol", "Farelo")),
    c("amendoim", "Amendoim", CropCategory.OLEAGINOSAS, listOf(9, 10, 11), 4, 0.55, 0.55, 4.2, "saca 25kg", 25.0, 95.0, 590.0, rgb(0.27, 0.61, 0.20), rgb(0.68, 0.60, 0.26), 0.5, listOf("Óleo de Amendoim", "Farelo")),
    c("gergelim", "Gergelim", CropCategory.OLEAGINOSAS, listOf(1, 2, 3), 3, 0.35, 0.45, 1.3, "saca 60kg", 60.0, 340.0, 290.0, rgb(0.28, 0.60, 0.22), rgb(0.80, 0.74, 0.42), 1.3, listOf("Óleo de Gergelim", "Tahine"), false),
    c("mamona", "Mamona", CropCategory.OLEAGINOSAS, listOf(10, 11, 12, 1), 4, 0.40, 0.50, 1.9, "saca 60kg", 60.0, 195.0, 310.0, rgb(0.22, 0.52, 0.20), rgb(0.62, 0.56, 0.26), 1.9, listOf("Óleo de Mamona", "Biodiesel")),
    c("algodao", "Algodão", CropCategory.OLEAGINOSAS, listOf(12, 1, 2), 5, 0.65, 0.85, 4.4, "arroba 15kg", 15.0, 138.0, 920.0, rgb(0.22, 0.58, 0.20), rgb(0.94, 0.94, 0.90), 1.3, listOf("Pluma", "Óleo de Caroço", "Farelo")),

    // 3. Culturas Industriais (3)
    c("cana", "Cana-de-Açúcar", CropCategory.INDUSTRIAIS, listOf(2, 3, 4, 9, 10), 6, 0.75, 0.80, 82.0, "tonelada", 1000.0, 165.0, 1250.0, rgb(0.25, 0.64, 0.20), rgb(0.42, 0.68, 0.25), 2.8, listOf("Açúcar", "Etanol", "Bagaço Energético")),
    c("mandioca", "Mandioca", CropCategory.INDUSTRIAIS, listOf(8, 9, 10, 11), 5, 0.45, 0.45, 26.0, "tonelada", 1000.0, 520.0, 650.0, rgb(0.23, 0.55, 0.19), rgb(0.32, 0.58, 0.22), 1.7, listOf("Farinha de Mandioca", "Fécula", "Polvilho")),
    c("fumo", "Fumo", CropCategory.INDUSTRIAIS, listOf(7, 8, 9), 4, 0.65, 0.75, 2.4, "arroba 15kg", 15.0, 245.0, 780.0, rgb(0.26, 0.64, 0.22), rgb(0.55, 0.62, 0.24), 1.4, listOf("Fumo Curado")),

    // 4. Frutas e Culturas Permanentes (16)
    c("cafe", "Café", CropCategory.FRUTAS_PERMANENTES, listOf(10, 11, 12), 6, 0.70, 0.80, 2.4, "saca 60kg", 60.0, 1450.0, 1800.0, rgb(0.16, 0.44, 0.16), rgb(0.72, 0.18, 0.14), 1.9, listOf("Café Beneficiado", "Café Torrado e Moído")),
    c("laranja", "Laranja", CropCategory.FRUTAS_PERMANENTES, listOf(9, 10, 11), 6, 0.70, 0.75, 34.0, "caixa 40.8kg", 40.8, 58.0, 1400.0, rgb(0.18, 0.48, 0.16), rgb(0.94, 0.55, 0.12), 2.5, listOf("Suco de Laranja Concentrado")),
    c("limao", "Limão", CropCategory.FRUTAS_PERMANENTES, listOf(9, 10, 11), 6, 0.65, 0.70, 30.0, "caixa 27kg", 27.0, 64.0, 1200.0, rgb(0.19, 0.50, 0.17), rgb(0.58, 0.80, 0.20), 2.3, listOf("Suco Cítrico", "Óleo Essencial")),
    c("banana", "Banana", CropCategory.FRUTAS_PERMANENTES, listOf(1, 2, 3, 9, 10, 11, 12), 5, 0.80, 0.75, 28.0, "caixa 20kg", 20.0, 48.0, 950.0, rgb(0.24, 0.62, 0.18), rgb(0.92, 0.82, 0.20), 2.6, listOf("Polpa de Banana", "Banana-Passa")),
    c("uva", "Uva", CropCategory.FRUTAS_PERMANENTES, listOf(7, 8, 9), 5, 0.65, 0.70, 22.0, "caixa 20kg", 20.0, 110.0, 1650.0, rgb(0.24, 0.56, 0.18), rgb(0.45, 0.18, 0.46), 1.8, listOf("Suco de Uva", "Vinho Artesanal")),
    c("maca", "Maçã", CropCategory.FRUTAS_PERMANENTES, listOf(7, 8), 6, 0.70, 0.70, 32.0, "caixa 18kg", 18.0, 82.0, 1500.0, rgb(0.22, 0.54, 0.19), rgb(0.84, 0.20, 0.18), 2.4, listOf("Suco de Maçã", "Polpa")),
    c("cacau", "Cacau", CropCategory.FRUTAS_PERMANENTES, listOf(10, 11, 12), 6, 0.80, 0.65, 1.4, "arroba 15kg", 15.0, 420.0, 1300.0, rgb(0.16, 0.42, 0.15), rgb(0.84, 0.58, 0.14), 2.5, listOf("Amêndoa de Cacau", "Manteiga e Chocolate")),
    c("acai", "Açaí", CropCategory.FRUTAS_PERMANENTES, listOf(1, 2, 3, 12), 6, 0.90, 0.65, 11.0, "lata 14kg", 14.0, 68.0, 1100.0, rgb(0.15, 0.44, 0.15), rgb(0.26, 0.12, 0.28), 3.0, listOf("Polpa de Açaí")),
    c("manga", "Manga", CropCategory.FRUTAS_PERMANENTES, listOf(1, 2, 10, 11), 5, 0.65, 0.65, 26.0, "caixa 20kg", 20.0, 62.0, 1150.0, rgb(0.19, 0.48, 0.16), rgb(0.92, 0.45, 0.18), 2.7, listOf("Polpa de Manga", "Suco")),
    c("mamao", "Mamão", CropCategory.FRUTAS_PERMANENTES, listOf(1, 2, 9, 10, 11), 4, 0.75, 0.70, 45.0, "caixa 20kg", 20.0, 52.0, 980.0, rgb(0.24, 0.58, 0.20), rgb(0.92, 0.62, 0.18), 2.2, listOf("Polpa de Mamão")),
    c("abacaxi", "Abacaxi", CropCategory.FRUTAS_PERMANENTES, listOf(3, 4, 9, 10), 5, 0.55, 0.65, 35.0, "unidade", 1.5, 4.8, 880.0, rgb(0.24, 0.56, 0.22), rgb(0.86, 0.66, 0.18), 0.8, listOf("Polpa e Suco de Abacaxi")),
    c("coco", "Coco", CropCategory.FRUTAS_PERMANENTES, listOf(1, 2, 3, 10, 11), 6, 0.75, 0.65, 24.0, "unidade", 1.2, 3.2, 1050.0, rgb(0.20, 0.52, 0.18), rgb(0.32, 0.58, 0.22), 3.2, listOf("Água de Coco", "Coco Ralado")),
    c("castanha_caju", "Castanha-de-Caju", CropCategory.FRUTAS_PERMANENTES, listOf(1, 2, 3), 6, 0.40, 0.45, 1.2, "saca 50kg", 50.0, 320.0, 820.0, rgb(0.20, 0.46, 0.17), rgb(0.88, 0.36, 0.18), 2.4, listOf("Castanha Torrada", "Polpa de Caju")),
    c("maracuja", "Maracujá", CropCategory.FRUTAS_PERMANENTES, listOf(9, 10, 11), 4, 0.70, 0.70, 20.0, "caixa 15kg", 15.0, 76.0, 860.0, rgb(0.22, 0.58, 0.18), rgb(0.95, 0.80, 0.16), 1.6, listOf("Polpa e Suco de Maracujá")),
    c("goiaba", "Goiaba", CropCategory.FRUTAS_PERMANENTES, listOf(9, 10, 11), 5, 0.65, 0.65, 28.0, "caixa 20kg", 20.0, 54.0, 920.0, rgb(0.20, 0.50, 0.18), rgb(0.78, 0.80, 0.24), 2.2, listOf("Goiabada", "Polpa")),
    c("pessego", "Pêssego", CropCategory.FRUTAS_PERMANENTES, listOf(6, 7, 8), 5, 0.65, 0.65, 18.0, "caixa 18kg", 18.0, 88.0, 1100.0, rgb(0.22, 0.54, 0.20), rgb(0.92, 0.52, 0.26), 2.2, listOf("Pêssego em Calda", "Polpa")),

    // 5. Hortaliças e Tubérculos (10)
    c("batata", "Batata", CropCategory.HORTALICAS_TUBERCULOS, listOf(2, 3, 8, 9), 3, 0.75, 0.80, 32.0, "saca 25kg", 25.0, 78.0, 1350.0, rgb(0.26, 0.62, 0.20), rgb(0.55, 0.52, 0.22), 0.65, listOf("Batata Lavada")),
    c("batata_doce", "Batata-Doce", CropCategory.HORTALICAS_TUBERCULOS, listOf(9, 10, 11, 1), 4, 0.55, 0.55, 24.0, "caixa 22kg", 22.0, 46.0, 620.0, rgb(0.25, 0.58, 0.22), rgb(0.42, 0.52, 0.20), 0.45, listOf("Batata-Doce Classificada")),
    c("tomate", "Tomate", CropCategory.HORTALICAS_TUBERCULOS, listOf(3, 4, 8, 9), 3, 0.80, 0.85, 65.0, "caixa 20kg", 20.0, 68.0, 1600.0, rgb(0.24, 0.62, 0.20), rgb(0.90, 0.20, 0.16), 1.2, listOf("Extrato e Molho de Tomate")),
    c("cebola", "Cebola", CropCategory.HORTALICAS_TUBERCULOS, listOf(4, 5, 6, 7), 4, 0.70, 0.75, 34.0, "saca 20kg", 20.0, 52.0, 940.0, rgb(0.24, 0.65, 0.26), rgb(0.70, 0.58, 0.28), 0.55, listOf("Cebola Curada")),
    c("alho", "Alho", CropCategory.HORTALICAS_TUBERCULOS, listOf(4, 5, 6), 4, 0.65, 0.75, 12.0, "caixa 10kg", 10.0, 145.0, 1550.0, rgb(0.23, 0.64, 0.24), rgb(0.75, 0.68, 0.36), 0.5, listOf("Alho Nobre Beneficiado")),
    c("cenoura", "Cenoura", CropCategory.HORTALICAS_TUBERCULOS, listOf(2, 3, 4, 8, 9), 3, 0.70, 0.70, 42.0, "caixa 20kg", 20.0, 48.0, 880.0, rgb(0.22, 0.66, 0.22), rgb(0.32, 0.60, 0.20), 0.45, listOf("Cenoura Lavada")),
    c("alface", "Alface", CropCategory.HORTALICAS_TUBERCULOS, listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), 2, 0.85, 0.65, 22.0, "caixa 10kg", 10.0, 44.0, 650.0, rgb(0.32, 0.76, 0.26), rgb(0.36, 0.82, 0.28), 0.3, listOf("Hortaliça Higienizada")),
    c("melancia", "Melancia", CropCategory.HORTALICAS_TUBERCULOS, listOf(8, 9, 10, 11), 3, 0.75, 0.70, 38.0, "unidade", 9.0, 18.0, 720.0, rgb(0.24, 0.58, 0.19), rgb(0.20, 0.48, 0.16), 0.4, listOf("Melancia In Natura")),
    c("melao", "Melão", CropCategory.HORTALICAS_TUBERCULOS, listOf(5, 6, 7, 8, 9), 3, 0.70, 0.70, 28.0, "caixa 13kg", 13.0, 48.0, 810.0, rgb(0.26, 0.60, 0.21), rgb(0.90, 0.82, 0.28), 0.4, listOf("Melão Exportação")),
    c("abobora", "Abóbora", CropCategory.HORTALICAS_TUBERCULOS, listOf(9, 10, 11, 12), 3, 0.60, 0.60, 24.0, "tonelada", 1000.0, 1450.0, 480.0, rgb(0.24, 0.58, 0.20), rgb(0.86, 0.48, 0.14), 0.45, listOf("Abóbora Cabotiá")),

    // 6. Horticultura & Flores da Agricultura Familiar (2)
    c("rosas_familiares", "Rosas (Agricultura Familiar)", CropCategory.HORTALICAS_TUBERCULOS, listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), 3, 0.72, 0.75, 16.0, "maço 2kg", 2.0, 32.0, 1650.0, rgb(0.19, 0.56, 0.21), rgb(0.91, 0.19, 0.38), 0.85, listOf("Buquê de Rosas", "Óleo Essencial de Rosas"), false),
    c("morango_familiar", "Morango Familiar", CropCategory.HORTALICAS_TUBERCULOS, listOf(3, 4, 5, 6, 7, 8, 9), 3, 0.76, 0.72, 22.0, "caixa 4kg", 4.0, 42.0, 1480.0, rgb(0.22, 0.60, 0.20), rgb(0.88, 0.14, 0.20), 0.42, listOf("Polpa de Morango", "Geleia Artesanal"), false),

    // 7. Culturas Florestais (2)
    c("eucalipto", "Eucalipto", CropCategory.FLORESTAIS, listOf(10, 11, 12, 1, 2), 8, 0.55, 0.50, 140.0, "m³ madeira", 650.0, 118.0, 1400.0, rgb(0.18, 0.46, 0.22), rgb(0.16, 0.40, 0.20), 4.2, listOf("Celulose", "Carvão Vegetal", "Madeira Serrada")),
    c("pinus", "Pinus", CropCategory.FLORESTAIS, listOf(5, 6, 7, 8), 8, 0.55, 0.45, 130.0, "m³ madeira", 600.0, 128.0, 1350.0, rgb(0.14, 0.36, 0.16), rgb(0.13, 0.32, 0.15), 4.4, listOf("Madeira Beneficiada", "Resina", "Celulose")),
)

fun getCropById(id: String): CropDef = ALL_CROPS.firstOrNull { it.id == id } ?: ALL_CROPS.first()
