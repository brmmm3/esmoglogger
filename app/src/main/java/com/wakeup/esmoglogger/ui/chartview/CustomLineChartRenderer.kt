package com.wakeup.esmoglogger.ui.chartview

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.renderer.LineChartRenderer
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.ViewPortHandler
import com.wakeup.esmoglogger.rfPowerColors

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

        fun getColorWithAlpha(color: Int, alpha: Int): Int {
            return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        }

        val transformer: Transformer = mChart.getTransformer(com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT)
        val viewPort = mViewPortHandler.contentRect
        var oldLevelColor = rfPowerColors.firstOrNull()
        for (levelColor in rfPowerColors.drop(1)) {
            val oldArea = transformer.getPixelForValues(0f, oldLevelColor!!.first.toFloat()).y.toFloat()
            val area = transformer.getPixelForValues(0f, levelColor.first.toFloat()).y.toFloat()
            val paint = Paint().apply {
                color = getColorWithAlpha(levelColor.second, 30)
                style = Paint.Style.FILL
            }
            c.drawRect(viewPort.left, area, viewPort.right, oldArea, paint)
            oldLevelColor = levelColor
        }
    }
}
