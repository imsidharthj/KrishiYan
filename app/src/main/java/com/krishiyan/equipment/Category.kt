package com.krishiyan.equipment

import androidx.annotation.StringRes
import com.krishiyan.R

enum class Category(val dbValue: String, @StringRes val label: Int) {
    Tractor("tractor", R.string.category_tractor),
    Harvester("harvester", R.string.category_harvester),
    Rotavator("rotavator", R.string.category_rotavator),
    Cultivator("cultivator", R.string.category_cultivator),
    SeedDrill("seed_drill", R.string.category_seed_drill),
    Sprayer("sprayer", R.string.category_sprayer),
    Thresher("thresher", R.string.category_thresher),
    Other("other", R.string.category_other);

    companion object {
        fun fromDb(value: String): Category = entries.firstOrNull { it.dbValue == value } ?: Other
    }
}

enum class PriceUnit(val dbValue: String, @StringRes val label: Int) {
    Hour("hour", R.string.price_per_hour),
    Day("day", R.string.price_per_day),
}
