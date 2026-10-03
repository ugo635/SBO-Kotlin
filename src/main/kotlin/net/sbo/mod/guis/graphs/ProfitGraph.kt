package net.sbo.mod.guis.graphs

import kotlin.math.roundToInt

class ProfitGraph : Graph(
    "Profit Graph",
   "Profit",
   "Time (In days)",
   "Profit (In millions)"
) {
   init {
      getDataPoints().forEach {point -> addPoint(point)}

      this.maxY = getMaxProfit()
      this.maxX = 5
   }

   private fun getMaxProfit(): Int = (points.maxOfOrNull { it.y }?.roundToInt() ?: 0) + 1

   private fun getDataPoints(): List<DataPoint> =
      mutableListOf(
         DataPoint(1.0, 72.9, "Profit: {y}m"),
         DataPoint(2.0, 321.5, "Profit: {y}m"),
         DataPoint(3.0, 224.7, "Profit: {y}m"),
         DataPoint(4.0, 74.0, "Profit: {y}m"),
         DataPoint(5.0, 728.4, "Profit: {y}m"),
         DataPoint(3.5, 50.0)
      )


}