package com.hitbosss.presentation.feature.settings

/** Tablas de umbrales por ejercicio (extraídas del iOS ExerciseType+UI.swift; números, comunes es/en). */
enum class ThresholdLevel { Beginner, Noob, Intermediate, Advanced, Elite }
data class ThresholdRow(val level: ThresholdLevel, val weightKg: String, val weightLbs: String, val points: String)
data class ThresholdTable(val maleRows: List<ThresholdRow>, val femaleRows: List<ThresholdRow>)

val tutorialThresholds: Map<String, ThresholdTable> = mapOf(
    "officialPowerlifting" to ThresholdTable(
        maleRows = listOf(
            ThresholdRow(ThresholdLevel.Beginner, "0-229", "0-504", "0-139"),
            ThresholdRow(ThresholdLevel.Noob, "230-339", "507-747", "140-209"),
            ThresholdRow(ThresholdLevel.Intermediate, "340-409", "749-901", "210-269"),
            ThresholdRow(ThresholdLevel.Advanced, "410-489", "903-1078", "270-319"),
            ThresholdRow(ThresholdLevel.Elite, "490+", "1080+", "320+")
        ),
        femaleRows = listOf(
            ThresholdRow(ThresholdLevel.Beginner, "0-99", "0-218", "0-119"),
            ThresholdRow(ThresholdLevel.Noob, "100-169", "220-372", "120-189"),
            ThresholdRow(ThresholdLevel.Intermediate, "170-249", "374-548", "190-259"),
            ThresholdRow(ThresholdLevel.Advanced, "250-329", "551-725", "260-319"),
            ThresholdRow(ThresholdLevel.Elite, "330+", "727+", "320+")
        ),
    ),
    "squat" to ThresholdTable(
        maleRows = listOf(
            ThresholdRow(ThresholdLevel.Beginner, "0-59", "0-130", "0-55"),
            ThresholdRow(ThresholdLevel.Noob, "60-99", "132-218", "56-75"),
            ThresholdRow(ThresholdLevel.Intermediate, "100-139", "220-306", "76-95"),
            ThresholdRow(ThresholdLevel.Advanced, "140-169", "309-373", "96-110"),
            ThresholdRow(ThresholdLevel.Elite, "170+", "375+", "111+")
        ),
        femaleRows = listOf(
            ThresholdRow(ThresholdLevel.Beginner, "0-34", "0-75", "0-40"),
            ThresholdRow(ThresholdLevel.Noob, "35-59", "77-130", "41-65"),
            ThresholdRow(ThresholdLevel.Intermediate, "60-89", "132-196", "66-90"),
            ThresholdRow(ThresholdLevel.Advanced, "90-119", "198-262", "91-110"),
            ThresholdRow(ThresholdLevel.Elite, "120+", "265+", "111+")
        ),
    ),
    "benchpress" to ThresholdTable(
        maleRows = listOf(
            ThresholdRow(ThresholdLevel.Beginner, "0-49", "0-108", "0-45"),
            ThresholdRow(ThresholdLevel.Noob, "50-79", "110-174", "46-65"),
            ThresholdRow(ThresholdLevel.Intermediate, "80-109", "176-240", "66-85"),
            ThresholdRow(ThresholdLevel.Advanced, "110-129", "243-284", "86-100"),
            ThresholdRow(ThresholdLevel.Elite, "130+", "287+", "101+")
        ),
        femaleRows = listOf(
            ThresholdRow(ThresholdLevel.Beginner, "0-24", "0-53", "0-35"),
            ThresholdRow(ThresholdLevel.Noob, "25-39", "55-86", "36-55"),
            ThresholdRow(ThresholdLevel.Intermediate, "40-59", "88-130", "56-75"),
            ThresholdRow(ThresholdLevel.Advanced, "60-79", "132-174", "76-95"),
            ThresholdRow(ThresholdLevel.Elite, "80+", "176+", "96+")
        ),
    ),
    "deadlift" to ThresholdTable(
        maleRows = listOf(
            ThresholdRow(ThresholdLevel.Beginner, "0-69", "0-152", "0-60"),
            ThresholdRow(ThresholdLevel.Noob, "70-119", "154-262", "61-85"),
            ThresholdRow(ThresholdLevel.Intermediate, "120-159", "265-351", "86-105"),
            ThresholdRow(ThresholdLevel.Advanced, "160-189", "353-417", "106-120"),
            ThresholdRow(ThresholdLevel.Elite, "190+", "419+", "121+")
        ),
        femaleRows = listOf(
            ThresholdRow(ThresholdLevel.Beginner, "0-39", "0-86", "0-45"),
            ThresholdRow(ThresholdLevel.Noob, "40-69", "88-152", "46-70"),
            ThresholdRow(ThresholdLevel.Intermediate, "70-99", "154-218", "71-95"),
            ThresholdRow(ThresholdLevel.Advanced, "100-129", "220-284", "96-115"),
            ThresholdRow(ThresholdLevel.Elite, "130+", "287+", "116+")
        ),
    ),
    "sumoDeadlift" to ThresholdTable(
        maleRows = listOf(
            ThresholdRow(ThresholdLevel.Beginner, "0-79", "0-174", "0-65"),
            ThresholdRow(ThresholdLevel.Noob, "80-129", "176-284", "66-90"),
            ThresholdRow(ThresholdLevel.Intermediate, "130-169", "287-373", "91-110"),
            ThresholdRow(ThresholdLevel.Advanced, "170-199", "375-439", "111-125"),
            ThresholdRow(ThresholdLevel.Elite, "200+", "441+", "126+")
        ),
        femaleRows = listOf(
            ThresholdRow(ThresholdLevel.Beginner, "0-44", "0-97", "0-45"),
            ThresholdRow(ThresholdLevel.Noob, "45-74", "99-163", "46-72"),
            ThresholdRow(ThresholdLevel.Intermediate, "75-104", "165-229", "73-98"),
            ThresholdRow(ThresholdLevel.Advanced, "105-134", "231-295", "99-118"),
            ThresholdRow(ThresholdLevel.Elite, "135+", "298+", "119+")
        ),
    ),
    "officialCrossfit" to ThresholdTable(
        maleRows = listOf(
            ThresholdRow(ThresholdLevel.Beginner, "0-129", "0-284", "0-109"),
            ThresholdRow(ThresholdLevel.Noob, "130-209", "287-461", "110-159"),
            ThresholdRow(ThresholdLevel.Intermediate, "210-289", "463-637", "160-209"),
            ThresholdRow(ThresholdLevel.Advanced, "290-359", "639-791", "210-259"),
            ThresholdRow(ThresholdLevel.Elite, "360+", "794+", "260+")
        ),
        femaleRows = listOf(
            ThresholdRow(ThresholdLevel.Beginner, "0-79", "0-174", "0-99"),
            ThresholdRow(ThresholdLevel.Noob, "80-139", "176-306", "100-149"),
            ThresholdRow(ThresholdLevel.Intermediate, "140-199", "309-439", "150-199"),
            ThresholdRow(ThresholdLevel.Advanced, "200-249", "441-549", "200-239"),
            ThresholdRow(ThresholdLevel.Elite, "250+", "551+", "240+")
        ),
    ),
    "snatch" to ThresholdTable(
        maleRows = listOf(
            ThresholdRow(ThresholdLevel.Beginner, "0-39", "0-86", "0-30"),
            ThresholdRow(ThresholdLevel.Noob, "40-59", "88-130", "31-45"),
            ThresholdRow(ThresholdLevel.Intermediate, "60-79", "132-174", "46-60"),
            ThresholdRow(ThresholdLevel.Advanced, "80-99", "176-218", "61-71"),
            ThresholdRow(ThresholdLevel.Elite, "100+", "220+", "72+")
        ),
        femaleRows = listOf(
            ThresholdRow(ThresholdLevel.Beginner, "0-24", "0-53", "0-26"),
            ThresholdRow(ThresholdLevel.Noob, "25-39", "55-86", "27-40"),
            ThresholdRow(ThresholdLevel.Intermediate, "40-54", "88-119", "41-56"),
            ThresholdRow(ThresholdLevel.Advanced, "55-69", "121-152", "57-70"),
            ThresholdRow(ThresholdLevel.Elite, "70+", "154+", "71+")
        ),
    ),
    "clean" to ThresholdTable(
        maleRows = listOf(
            ThresholdRow(ThresholdLevel.Beginner, "0-44", "0-97", "0-35"),
            ThresholdRow(ThresholdLevel.Noob, "45-74", "99-163", "36-50"),
            ThresholdRow(ThresholdLevel.Intermediate, "75-104", "165-229", "51-70"),
            ThresholdRow(ThresholdLevel.Advanced, "105-129", "231-284", "71-85"),
            ThresholdRow(ThresholdLevel.Elite, "130+", "287+", "86+")
        ),
        femaleRows = listOf(
            ThresholdRow(ThresholdLevel.Beginner, "0-24", "0-52", "0-28"),
            ThresholdRow(ThresholdLevel.Noob, "25-44", "55-97", "29-49"),
            ThresholdRow(ThresholdLevel.Intermediate, "45-64", "99-141", "50-64"),
            ThresholdRow(ThresholdLevel.Advanced, "65-79", "143-174", "65-85"),
            ThresholdRow(ThresholdLevel.Elite, "80+", "176+", "86+")
        ),
    ),
    "cleanAndJerk" to ThresholdTable(
        maleRows = listOf(
            ThresholdRow(ThresholdLevel.Beginner, "0-49", "0-108", "0-40"),
            ThresholdRow(ThresholdLevel.Noob, "50-79", "110-174", "41-56"),
            ThresholdRow(ThresholdLevel.Intermediate, "80-109", "176-240", "57-75"),
            ThresholdRow(ThresholdLevel.Advanced, "110-139", "243-306", "76-92"),
            ThresholdRow(ThresholdLevel.Elite, "140+", "309+", "93+")
        ),
        femaleRows = listOf(
            ThresholdRow(ThresholdLevel.Beginner, "0-29", "0-64", "0-34"),
            ThresholdRow(ThresholdLevel.Noob, "30-49", "66-108", "35-54"),
            ThresholdRow(ThresholdLevel.Intermediate, "50-69", "110-152", "55-74"),
            ThresholdRow(ThresholdLevel.Advanced, "70-89", "154-196", "75-94"),
            ThresholdRow(ThresholdLevel.Elite, "90+", "198+", "95+")
        ),
    ),
)
