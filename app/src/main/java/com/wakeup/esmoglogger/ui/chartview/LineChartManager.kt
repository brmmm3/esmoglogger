package com.wakeup.esmoglogger.ui.chartview

import android.graphics.Color
import android.view.View
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

class LineChartManager(lineChart: LineChart) {
    private val chart: LineChart = lineChart
    private val entries = ArrayList<Entry>()
    private val dataSet = LineDataSet(entries, "Live Daten")

    init {
        dataSet.color = Color.BLUE
        dataSet.setDrawCircles(false)
        //dataSet.setCircleColor(Color.BLUE)
        dataSet.lineWidth = 2f
        dataSet.setDrawValues(true)

        chart.data = LineData(dataSet)
        chart.fitScreen() // Reset zoom
        chart.isEnabled = true
        chart.visibility = View.VISIBLE

        // Basic chart setup
        chart.description.text = "Live Daten"
        //chart.getDescription().setEnabled(false) // Disable description
        chart.setTouchEnabled(true)
        chart.setDragEnabled(true) // Enable dragging
        chart.setScaleEnabled(true) // Enable scaling
        chart.setPinchZoom(true) // Enable pinch zoom
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
            setGranularity(0.1f) // Interval for Y-axis labels
            setDrawLimitLinesBehindData(true)
            textColor = Color.GREEN
            //setLabelCount(6, true)

            isEnabled = true
            textSize = 12f  // Text size for Y-axis labels
            // Optional: Custom formatter for Y-axis (e.g., add units)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%.1f mW", value)
                }
            }
        }

        chart.axisRight.isEnabled = false // Disable right Y-axis

        chart.resetViewPortOffsets()
        chart.fitScreen() // Reset zoom
        chart.setVisibleXRangeMaximum(100f)
    }

    class FrequencyYAxisValueFormatter : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return value.toString() + "mW"
        }
    }

    fun addEntry(time: Float, value: Float) {
        entries.add(Entry(time, value))
        if (value > chart.axisLeft.mAxisMaximum) {
            chart.axisLeft.mAxisMaximum = value + 0.1f
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