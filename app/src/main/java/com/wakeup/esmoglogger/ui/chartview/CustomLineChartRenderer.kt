package com.wakeup.esmoglogger.ui.chartview

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.renderer.LineChartRenderer
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.ViewPortHandler

class CustomLineChartRenderer(
    chart: LineChart,
    animator: com.github.mikephil.charting.animation.ChartAnimator,
    viewPortHandler: ViewPortHandler
) : LineChartRenderer(chart, animator, viewPortHandler) {

    private val greenPaint = Paint().apply {
        color = Color.argb(50, 0, 255, 0)
        style = Paint.Style.FILL
    }

    private val yellowPaint = Paint().apply {
        color = Color.argb(50, 255, 255, 0)
        style = Paint.Style.FILL
    }

    private val redPaint = Paint().apply {
        color = Color.argb(50, 255, 0, 0)
        style = Paint.Style.FILL
    }

    override fun drawExtras(c: Canvas) {
        super.drawExtras(c)

        // Y-Bereiche in Pixel umrechnen
        val transformer: Transformer = mChart.getTransformer(com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT)

        // Pixel-Koordinaten für die Y-Bereiche berechnen
        val zeroLine = transformer.getPixelForValues(0f, 0f).y.toFloat()
        val greenArea = transformer.getPixelForValues(0f, 0.18f).y.toFloat()
        val yellowArea = transformer.getPixelForValues(0f, 5.8f).y.toFloat()
        val redArea = transformer.getPixelForValues(0f, 180f).y.toFloat()

        // Rechtecke für die Hintergrundbereiche zeichnen
        val viewPort = mViewPortHandler.contentRect
        // Grüner Bereich (Y: 0–0.18)
        c.drawRect(viewPort.left, greenArea, viewPort.right, zeroLine, greenPaint)
        // Gelber Bereich (Y: 0.18–5.8)
        c.drawRect(viewPort.left, yellowArea, viewPort.right, greenArea, yellowPaint
        )
        // Roter Bereich (Y: 5.8-200)
        c.drawRect(viewPort.left, redArea,viewPort.right, yellowArea, redPaint
        )
    }
}
