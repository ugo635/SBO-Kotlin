package net.sbo.mod.guis.graphs

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.fabric.GuiLib
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.hypot

data class DataPoint(val x: Double, val y: Double, val tooltip: String? = null)

abstract class Graph(
    val name: String,
    val description: String,
    val xAxisLabel: String,
    val yAxisLabel: String,
    var minX: Int = 0,
    var maxX: Int = 0,
    var minY: Int = 0,
    var maxY: Int = 0,
    val graphWidth: Double = 60.0,   // in vmin
    val graphHeight: Double = 36.0   // in vmin
) {
    protected var points: List<DataPoint> = listOf()
        set(value) {
            field = value.sortedBy { it.x }   // stable: equal y keep insertion order
        }

    // Plot area, as fractions of the graph box
    protected val padLeft = 0.10
    protected val padBottom = 0.14
    protected val padRight = 0.04
    protected val padTop = 0.08
    protected val plotW = 1.0 - padLeft - padRight
    protected val plotH = 1.0 - padBottom - padTop

    // Sizes in em (the graph's font-size scales with the graph)
    protected val dotSize = 0.9
    protected val lineThickness = 0.3
    protected val tickLength = 0.5
    protected val labelHeight = 1.2
    protected val labelWidth = 4.0
    protected val axisThickness = "max(1px, 0.15em)"
    protected val tickCount = 5

    protected val aspect = graphWidth / graphHeight
    protected val fontSize get() = minOf(graphWidth, graphHeight) * 0.045   // in vmin
    protected val rangeX get() = (maxX - minX).toDouble().takeIf { it != 0.0 } ?: 1.0
    protected val rangeY get() = (maxY - minY).toDouble().takeIf { it != 0.0 } ?: 1.0

    protected val app = component<String>("Default Graph") { initialSection ->
        div(className = "window") {
            div(className = "titlebar") {
                span(className = "title") { +name }
                button(className = "close", title = "Close", onClick = { GuiLib.close() }) { +"✕" }
            }

            div(className = "body") {
                div(
                    className = "graph",
                    style = "width: ${vmin(graphWidth)}; height: ${vmin(graphHeight)}; font-size: ${vmin(fontSize)}"
                ) {
                    renderGraph()
                }
            }
        }
    }

    open fun open(section: String = "Default Graph") =
        GuiLib.open(component("Default Graph") { app(section) },
            stylesheets = listOf("sbo:ui/graphs/graph.css"), title = name)

    protected open fun NodeBuilder.renderGraph() {
        drawAxis()
        drawGraduations()
        drawAxisLabels()
        drawPointLinkingLines() // lines first, so dots are drawn on top
        drawDataPoints()
        drawLegend()
    }

    protected open fun NodeBuilder.drawAxis() {
        div(
            className = "line x-axis-line",
            style = "left: ${pct(padLeft)}; bottom: ${pct(padBottom)}; width: ${pct(plotW)}; height: $axisThickness"
        ) {}
        div(
            className = "line y-axis-line",
            style = "left: ${pct(padLeft)}; bottom: ${pct(padBottom)}; width: $axisThickness; height: ${pct(plotH)}"
        ) {}
    }

    protected open fun NodeBuilder.drawDataPoints() {
        for (point in points.filter { it.inRange() }) {
            val x = fx(point.x)
            val y = fy(point.y)

            val tooltip = point.tooltip
                ?.replace("{x}", fmt(point.x))
                ?.replace("{y}", fmt(point.y))

            div(
                className = "dot",
                title = tooltip,
                style = "left: calc(${pct(x)} - ${em(dotSize / 2)}); " +
                        "bottom: calc(${pct(y)} - ${em(dotSize / 2)}); " +
                        "width: ${em(dotSize)}; height: ${em(dotSize)}"
            ) {}
        }
    }

    protected open fun NodeBuilder.drawAxisLabels() {
        div(
            className = "axis-label x-axis-label",
            style = "bottom: calc(${pct(padBottom)} + 0.4em); right: calc(${pct(padRight)} + 0.4em)"
        ) {
            span(className = "axis-label-content") { +xAxisLabel }
        }

        div(
            className = "axis-label y-axis-label",
            style = "top: 0.3em; left: calc(${pct(padLeft)} + 0.4em)"
        ) {
            span(className = "axis-label-content") { +yAxisLabel }
        }
    }

    protected open fun NodeBuilder.drawGraduations() {
        for (i in 0..tickCount) {
            val t = i.toDouble() / tickCount
            val xPos = padLeft + t * plotW
            val yPos = padBottom + t * plotH
            val vx = minX + t * (maxX - minX)
            val vy = minY + t * (maxY - minY)

            // X tick + label
            div(
                className = "line tick",
                style = "left: ${pct(xPos)}; bottom: calc(${pct(padBottom)} - ${em(tickLength)}); " +
                        "width: $axisThickness; height: ${em(tickLength)}"
            ) {}
            div(
                className = "tick-label",
                style = "left: calc(${pct(xPos)} - ${em(labelWidth / 2)}); " +
                        "bottom: calc(${pct(padBottom)} - ${em(tickLength + labelHeight)}); " +
                        "width: ${em(labelWidth)}; text-align: center"
            ) {
                span(className = "axis-label-content") { +fmt(vx) }
            }

            // Y tick + label
            div(
                className = "line tick",
                style = "left: calc(${pct(padLeft)} - ${em(tickLength)}); bottom: ${pct(yPos)}; " +
                        "width: ${em(tickLength)}; height: $axisThickness"
            ) {}
            div(
                className = "tick-label",
                style = "left: 0px; bottom: calc(${pct(yPos)} - ${em(labelHeight / 2)}); " +
                        "width: calc(${pct(padLeft)} - ${em(tickLength + 0.3)}); text-align: right"
            ) {
                span(className = "axis-label-content") { +fmt(vy) }
            }
        }
    }

    protected open fun NodeBuilder.drawLegend() {
        div(className = "legend") {
            div(className = "legend-dot") {}
            span(className = "axis-label-content") { +description }
        }
    }

    protected open fun addPoint(point: DataPoint) {
        points = points + point
    }

    protected open fun NodeBuilder.drawPointLinkingLines() {
        // Lines follow insertion order. Use points.sortedBy { it.x } if you want left-to-right.
        for ((a, b) in points.filter { it.inRange() }.zipWithNext()) {
            val x1 = fx(a.x)
            val y1 = fy(a.y)
            val dx = fx(b.x) - x1                  // fraction of the graph width
            val dy = (fy(b.y) - y1) / aspect       // fraction of the graph height, converted to width units
            val length = hypot(dx, dy)             // fraction of the graph width
            // CSS rotates clockwise on screen, but our y axis goes up, hence the minus
            val angle = -Math.toDegrees(atan2(dy, dx))

            div(
                className = "line data-line",
                style = "left: ${pct(x1)}; bottom: calc(${pct(y1)} - ${em(lineThickness / 2)}); " +
                        "width: ${pct(length)}; height: max(1px, ${em(lineThickness)}); " +
                        "transform-origin: 0% 50%; transform: rotate(${num(angle)}deg)"
            ) {}
        }
    }

    // --- helpers ---
    protected fun DataPoint.inRange() = x in minX.toDouble()..maxX.toDouble() && y in minY.toDouble()..maxY.toDouble()

    // Position of a data value, as a fraction of the graph width / height
    protected open fun fx(x: Double) = padLeft + (x - minX) / rangeX * plotW
    protected open fun fy(y: Double) = padBottom + (y - minY) / rangeY * plotH

    // Locale.ROOT matters: with a French locale "%.2f" would give "1,50" and break the CSS
    protected fun num(v: Double) = String.format(Locale.ROOT, "%.2f", v)
    protected fun px(v: Double) = "${num(v)}px"
    protected fun fmt(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale.ROOT, "%.1f", v)
    protected fun percentage(v: Double) = "${num(v)}%"
    protected fun pct(fraction: Double) = "${num(fraction * 100)}%"
    protected fun em(v: Double) = "${num(v)}em"
    protected fun vmin(v: Double) = "${num(v)}vmin"
}