package net.sbo.mod.guis.graphs

import net.sbo.mod.utils.game.DayStats
import net.sbo.mod.utils.game.DianaEventDays
import net.sbo.mod.utils.game.Mayor
import kotlin.math.roundToInt

class ProfitGraph : Graph(
   "Profit Graph",
   "Profit",
   "Time (In days)",
   "Profit (In millions)"
) {
   init {
      getDataPoints().forEach { point -> addPoint(point) }

      this.maxY = getMaxProfit()
      this.maxX = 6        // a Diana term lasts 5 days 4 h: days 1..6, day 6 is only partial
      this.tickCount = 6
   }

   private fun getMaxProfit(): Int = (points.maxOfOrNull { it.y }?.roundToInt() ?: 0) + 1

   // One point per event day: x = day number, y = cumulative profit (in millions) at the end of that day
   private fun getDataPoints(): List<DataPoint> {
      // Outside a Diana term the tracker totals belong to an old event, so show nothing
      if (!Mayor.isDiana) return emptyList()
      return DianaEventDays.currentEventStats().map { it.toPoint() }
   }

   private fun DayStats.toPoint(): DataPoint {
      val tooltip = "Profit: {y}m, ${formatDuration(playtimeMs)} played" +
              if (finished) "" else " (so far)"
      return DataPoint(position, profitAtEnd / 1_000_000.0, tooltip)
   }

   private fun formatDuration(ms: Long): String {
      val minutes = ms / 60_000
      return "${minutes / 60}h${(minutes % 60).toString().padStart(2, '0')}"
   }
}