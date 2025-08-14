package com.wakeup.esmoglogger.ui.chartview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.wakeup.esmoglogger.R
import com.wakeup.esmoglogger.data.SharedESmogData


class ChartViewFragment : Fragment() {
    private lateinit var lineChart: LineChart
    private var chartManager: LineChartManager? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chart, container, false)

        // Find the LineChart in the inflated view
        lineChart = view.findViewById(R.id.lineChart)

        // Create the chart manager
        chartManager = LineChartManager(lineChart)

        SharedChartData.view.observe(viewLifecycleOwner) { view ->
            updateView(view)
            if (chartManager?.isEmpty() == true) {
                SharedESmogData.dataSeries.lvlFrqData.forEach { it -> chartManager?.add(it.first, it.second) }
            }
        }

        SharedChartData.command.observe(viewLifecycleOwner) { command ->
            if (command == "resetScale") {
                chartManager?.resetScale()
            }
        }

        SharedChartData.data.observe(viewLifecycleOwner) { value ->
            // value = Pair(Time, Y-Value)
            chartManager?.add(value.first, value.second)
        }

        val resetScaleButton = view.findViewById<Button>(R.id.button_reset_scale)

        resetScaleButton?.setOnClickListener { v: View? ->
            SharedChartData.sendCommand("resetScale")
        }

        val chartViewSpinner: Spinner = view.findViewById(R.id.chart_view_spinner)

        chartViewSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                updateView(parent.getItemAtPosition(position).toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                updateView("Lvl")
            }
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chartManager = null
    }

    fun updateView(view: String) {
        when (view) {
            "Lvl" -> {
                chartManager?.showLevelData(true)
                chartManager?.showFrequencyData(false)
            }
            "Frq" -> {
                chartManager?.showLevelData(false)
                chartManager?.showFrequencyData(true)
            }
            "Lvl + Frq" -> {
                chartManager?.showLevelData(true)
                chartManager?.showFrequencyData(true)
            }
            "Lvl | Frq" -> {
                chartManager?.showLevelData(true)
                chartManager?.showFrequencyData(true)
            }
        }

    }
}