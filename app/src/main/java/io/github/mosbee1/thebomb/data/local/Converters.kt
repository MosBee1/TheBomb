package io.github.mosbee1.thebomb.data.local

import androidx.room.TypeConverter
import io.github.mosbee1.thebomb.data.model.DurationUnit

class Converters {

    @TypeConverter
    fun durationUnitToString(unit: DurationUnit): String = unit.name

    @TypeConverter
    fun stringToDurationUnit(value: String): DurationUnit =
        DurationUnit.fromNameOrDefault(value)
}
