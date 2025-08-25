package com.wakeup.esmoglogger.ui.chartview

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.wakeup.esmoglogger.data.Recording

class LineChartManager(lineChart: LineChart, customRenderer: Boolean) {
    private val chart: LineChart = lineChart
    private var lvlDataSet = LineDataSet(ArrayList<Entry>(), "Level")
    private var frqDataSet = LineDataSet(ArrayList<Entry>(), "Frequency")

    init {
        if (customRenderer) {
            chart.renderer = CustomLineChartRenderer(chart, chart.animator, chart.viewPortHandler)
        }
        chart.data = LineData()

        chart.isEnabled = true
        chart.visibility = View.VISIBLE

        // Basic chart setup
        //chart.description.text = "ESmog data"
        chart.description.isEnabled = false // Disable description
        chart.setTouchEnabled(true)
        chart.setDragEnabled(true) // Enable dragging
        chart.setScaleEnabled(true) // Enable scaling
        chart.setPinchZoom(false) // Enable pinch zoom
        //chart.setDrawGridBackground(false) // Disable grid background
        //chart.setHardwareAccelerationEnabled(false)
        //chart.animateX(1000)

        // X-axis configuration
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawLimitLinesBehindData(true)
            setDrawGridLines(false)
            enableGridDashedLine(2f, 7f, 0f)
            setAxisMinimum(0f) // Minimum Y value
            setAxisMaximum(10000f) // Maximum Y value
            setLabelCount(6, true)
            isGranularityEnabled = true
            granularity = 0.1f  // Smallest interval between labels
            isEnabled = true
            //textSize = 12f  // Text size for X-axis labels
            // Custom formatter to convert float time to string (e.g., "12:16")
            /*valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val hours = value.toInt()
                    val minutes = ((value - hours) * 60).toInt()
                    return String.format("%02d:%02d", hours, minutes)
                }
            }*/
        }

        // Y-axis configuration (left)
        chart.axisLeft.apply {
            enableGridDashedLine(10f, 10f, 0f)
            setDrawZeroLine(false)
            setDrawGridLines(true)
            setAxisMinimum(0f) // Minimum Y value
            setAxisMaximum(1f) // Maximum Y value
            setGranularity(0.01f) // Interval for Y-axis labels
            setDrawLimitLinesBehindData(true)
            textColor = Color.GREEN
            //setLabelCount(6, true)

            isEnabled = true
            textSize = 12f  // Text size for Y-axis labels
            // Optional: Custom formatter for Y-axis (e.g., add units)
            valueFormatter = object : ValueFormatter() {
                @SuppressLint("DefaultLocale")
                override fun getFormattedValue(value: Float): String {
                    return String.format("%.1f mW", value)
                }
            }
        }

        // Y-axis configuration (left)
        chart.axisRight.apply {
            enableGridDashedLine(10f, 10f, 0f)
            setDrawZeroLine(false)
            setDrawGridLines(false)
            setAxisMinimum(0f) // Minimum Y value
            setAxisMaximum(0.1f) // Maximum Y value
            setGranularity(0.01f) // Interval for Y-axis labels
            setDrawLimitLinesBehindData(true)
            textColor = Color.YELLOW
            //setLabelCount(6, true)

            isEnabled = true
            textSize = 12f  // Text size for Y-axis labels
            // Optional: Custom formatter for Y-axis (e.g., add units)
            valueFormatter = object : ValueFormatter() {
                @SuppressLint("DefaultLocale")
                override fun getFormattedValue(value: Float): String {
                    return String.format("%.1f MHz", value)
                }
            }
        }

        lvlDataSet.apply {
            color = Color.GREEN
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            axisDependency = YAxis.AxisDependency.LEFT
        }
        chart.data.addDataSet(lvlDataSet)

        frqDataSet.apply {
            color = Color.argb(100, 255, 255, 0)
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            axisDependency = YAxis.AxisDependency.RIGHT
        }
        chart.data.addDataSet(frqDataSet)

        chart.resetViewPortOffsets()
        chart.fitScreen() // Reset zoom
        chart.setVisibleXRangeMaximum(100f)
    }

    fun showLevelData(show: Boolean) {
        chart.axisLeft.isEnabled = show
        chart.resetViewPortOffsets()
        chart.fitScreen() // Reset zoom
        chart.setVisibleXRangeMaximum(100f)
    }

    fun showFrequencyData(show: Boolean) {
        chart.axisRight.isEnabled = show
        chart.resetViewPortOffsets()
        chart.fitScreen() // Reset zoom
        chart.setVisibleXRangeMaximum(100f)
    }

    private fun scaleAxisToLast100(values: ArrayList<Entry>): Float {
        var ymax = 0.1f
        val xmin = chart.getLowestVisibleX()
        values.forEach { it ->
            if (it.x >= xmin && it.y > ymax) {
                ymax = it.y
            }
        }
        return ymax
    }

    fun resetScale() {
        chart.resetViewPortOffsets()
        chart.fitScreen() // Reset zoom
        chart.setVisibleXRangeMaximum(100f)
        chart.axisLeft.axisMaximum = scaleAxisToLast100(lvlDataSet.values as ArrayList<Entry>)
        chart.axisRight.axisMaximum = scaleAxisToLast100(frqDataSet.values as ArrayList<Entry>)
    }

    fun isEmpty(): Boolean {
        return lvlDataSet.values.isEmpty() && frqDataSet.values.isEmpty()
    }

    fun clear() {
        lvlDataSet.clear()
        frqDataSet.clear()
    }

    fun set(data: Recording) {
        lvlDataSet.clear()
        frqDataSet.clear()
        // TODO
    }

    fun addChartPt(time: Float, level: Float, frequency: Int) {
        if (chart.axisLeft.isEnabled) {
            lvlDataSet.addEntry(Entry(time, level))
            if (level > chart.axisLeft.mAxisMaximum) {
                chart.axisLeft.mAxisMaximum = level
            }
        }
        if (chart.axisRight.isEnabled) {
            val frequency = frequency.toFloat()
            frqDataSet.addEntry(Entry(time, frequency))
            if (frequency > chart.axisRight.mAxisMaximum) {
                chart.axisRight.mAxisMaximum = frequency
            }
        }
        chart.data.notifyDataChanged()
        chart.notifyDataSetChanged()
        if (time > 100f) {
            //entries.removeAt(0)
            chart.moveViewToX(time - 100f)
        } else {
            chart.invalidate()
        }
    }
}