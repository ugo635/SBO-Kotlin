package net.sbo.mod.utils.game

import net.sbo.mod.overlays.DianaLoot
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.events.Register

/** Cumulative event-tracker totals at the END of an event day. */
data class DaySnapshot(val day: Int, val totalTimeMs: Long, val totalProfit: Double)

/** Per-day numbers ready for display. [finished] is false for the day in progress. */
data class DayStats(
    val day: Int,
    val position: Double,
    val playtimeMs: Long,
    val profitAtEnd: Double,
    val finished: Boolean
)

object DianaEventDays {
    private const val DAY_MS = 86_400_000L
    private const val EVENT_LENGTH_DAYS = 5.4

    // termStart (ms) -> snapshots of that event.
    private val events: MutableMap<Long, MutableList<DaySnapshot>>
        get() = DataManager.dianaEventDaysData.events

    // Cumulative totals of the EVENT tracker
    // TIME only advances while the mayor timer runs, and profit only changes with new drops,
    // so neither can change while the player is offline.
    private fun lootTimeMs(): Long = DataManager.dianaTrackerMayorData.items.TIME
    private fun lootProfit(): Double = DianaLoot.totalProfit(DataManager.dianaTrackerMayorData).toDouble()

    fun init() {
        Register.onTick(20) { if (Mayor.isDiana) update() }
    }

    /** Records end-of-day snapshots for every day that finished since the last call. */
    private fun update(now: Long = System.currentTimeMillis()) {
        val list = events.getOrPut(Mayor.currentTermStartMillis(now)) { mutableListOf() }
        val time = lootTimeMs()

        val last = list.lastOrNull()
        if (last != null && time < last.totalTimeMs) {
            list.clear()
            save()
        }

        val today = Mayor.eventDay(now)
        var next = (list.lastOrNull()?.day ?: -1) + 1
        if (next >= today) return

        // Days that ended while the player was offline get the current totals: nothing could have changed.
        val profit = lootProfit() // reflection over the tracker, so only computed when a day ended
        while (next < today) list += DaySnapshot(next++, time, profit)
        save()
    }

    /** One entry per day from day 0 to today (today uses the live tracker values). */
    fun currentEventStats(now: Long = System.currentTimeMillis()): List<DayStats> {
        val termStart = Mayor.currentTermStartMillis(now)
        val snaps = events[termStart] ?: emptyList()
        val elapsedDays = ((now - termStart).toDouble() / DAY_MS).coerceIn(0.0, EVENT_LENGTH_DAYS)
        val today = elapsedDays.toInt().coerceAtMost(5)
        var previousTime = 0L

        return (0..today).map { d ->
            val s = snaps.find { it.day == d }
            val time = s?.totalTimeMs ?: lootTimeMs()
            val profit = s?.totalProfit ?: lootProfit()
            val position = s?.let { d + 1.0 } ?: elapsedDays
            DayStats(d, position, (time - previousTime).coerceAtLeast(0), profit, s != null)
                .also { previousTime = time }
        }
    }

    private fun save() {
        DataManager.save(DataManager::dianaEventDaysData)
    }
}