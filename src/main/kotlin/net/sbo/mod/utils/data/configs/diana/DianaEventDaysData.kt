package net.sbo.mod.utils.data.configs.diana

import net.sbo.mod.utils.game.DaySnapshot

data class DianaEventDaysData(
    /** The Long represents the start time of the Diana event in milliseconds. */
    var events: MutableMap<Long, MutableList<DaySnapshot>> = mutableMapOf(),
)
