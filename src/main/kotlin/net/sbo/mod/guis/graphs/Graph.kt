package net.sbo.mod.guis.graphs;

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.fabric.GuiLib

abstract class Graph(
    val name: String,
    val description: String,
    //val graphWidth: Int,
    //val graphHeight: Int,
    val xAxisLabel: String,
    val yAxisLabel: String,
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int,
    val xStart: Int = 0,
    val yStart: Int = 0
) {
    protected var points: List<DataPoint> = listOf()

    private val app = component<String>("Default Graph") { initialSection ->
        div(className = "window") {
            div(className = "titlebar") {
                span(className = "title") { +"Default Graph" }
                button(className = "close", title = "Close", onClick = { GuiLib.close() }) { +"✕" }
            }

            div(className = "body") {
                div(className = "graph") {
                    renderGraph()
                }
            }
        }

    }

    fun open(section: String = "Default Graph") =
        GuiLib.open(component("Default Graph") { app(section) },
            stylesheets = listOf("sbo:ui/graphs/graph.css"), title = "Default Graph")

    fun NodeBuilder.renderGraph() {
        drawAxis()
        drawAxisLabels()
    }

    fun NodeBuilder.drawAxis() {
        div(className = "line x-axis-line") {}
        div(className = "line y-axis-line") {}
    }

    fun drawDataPoints() {
        // Implement the data points drawing logic here
    }

    fun NodeBuilder.drawAxisLabels() {
        div(className = "axis-label x-axis-label") {
            span(className = "axis-label-content") { + xAxisLabel }
        }

        div(className = "axis-label y-axis-label") {
            span(className = "axis-label-content") { + yAxisLabel }
        }
    }

    fun NodeBuilder.addGraduations() {

    }

    fun NodeBuilder.drawLegend() {
        // Implement the legend drawing logic here
    }

    fun addPoint(point: DataPoint) {
        // Implement the logic to add a data point to the graph here
    }

    fun NodeBuilder.drawPointLinkingLines() {
        // Implement the logic to add the lines between the points here
    }

}