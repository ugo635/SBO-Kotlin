package net.sbo.mod.guis.graphs;

import gg.essential.elementa.ElementaVersion;
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus
import gg.essential.elementa.dsl.toConstraint
import gg.essential.universal.UKeyboard
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.fabric.GuiLib
import net.sbo.mod.SBOKotlin
import net.sbo.mod.guis.partyfinder.GuiHandler
import java.awt.Color

abstract class Graph(
    val name: String,
    val description: String,
    val graphWidth: Int,
    val graphHeight: Int,
    //val minX: Int,
    //val maxX: Int,
    //val minY: Int,
    //val maxY: Int,
    //val xAxisLabel: String,
    //val yAxisLabel: String,
    //val xAxisGap: Int,
    //val yAxisGap: Int,
    //val xStart: Int,
    //val yStart: Int
) {
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
        drawXAxis()
        drawYAxis()
    }

    fun NodeBuilder.drawXAxis() {
        div(className = "line x-axis-line") {

        }
    }

    fun NodeBuilder.drawYAxis() {
        div(className = "line y-axis-line") {

        }
    }

    fun drawDataPoints() {
        // Implement the data points drawing logic here
    }

    fun drawLabels() {
        // Implement the labels drawing logic here
    }

    fun addXAxisLabels() {

    }

    fun addYAxisLabels() {

    }

    fun drawLegend() {
        // Implement the legend drawing logic here
    }

    fun addPoint(point: DataPoint) {
        // Implement the logic to add a data point to the graph here
    }

    fun drawPointLinkingLines() {
        // Implement the logic to add the lines between the points here
    }

}