package com.wakeup.esmoglogger.ui.chartview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.charts.LineChart
import com.wakeup.esmoglogger.R
import com.wakeup.esmoglogger.SharedViewModel
import com.wakeup.esmoglogger.data.ESmog
import kotlinx.coroutines.launch
import kotlin.getValue


class ChartViewFragment : Fragment() {
    private val viewModel: SharedViewModel by activityViewModels()
    private lateinit var lineChart: LineChart
    private lateinit var lineChart2: LineChart
    private var chartManager: LineChartManager? = null
    private var chartManager2: LineChartManager? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chart, container, false)

        if (::lineChart.isInitialized) {
            return view
        }

        // Initialize first line chart
        lineChart = view.findViewById(R.id.line_chart)
        chartManager = LineChartManager(lineChart, true)

        // Initialize second line chart
        lineChart2 = view.findViewById(R.id.line_chart2)
        chartManager2 = LineChartManager(lineChart2, false)
        chartManager2?.showLevelData(false)
        chartManager2?.showFrequencyData(true)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.esmogQueue.collect { value ->
                    chartManager?.addChartPt(value.time, value.level, value.frequency, true)
                    chartManager2?.addChartPt(value.time, value.level, value.frequency, true)
                }
            }
        }

        val resetScaleButton = view.findViewById<Button>(R.id.button_reset_scale)

        resetScaleButton?.setOnClickListener { v: View? ->
            chartManager?.resetScale()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Thread {
            viewModel.recording.data.forEach { value ->
                viewModel.enqueueESmog(ESmog(value.time, value.level, value.frequency))
            }
        }.start()
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
                lineChart2.visibility = View.GONE
            }
            "Frq" -> {
                chartManager?.showLevelData(false)
                chartManager?.showFrequencyData(true)
                lineChart2.visibility = View.GONE
            }
            "Lvl + Frq" -> {
                chartManager?.showLevelData(true)
                chartManager?.showFrequencyData(true)
                lineChart2.visibility = View.GONE
            }
            "Lvl | Frq" -> {
                chartManager?.showLevelData(true)
                chartManager?.showFrequencyData(false)
                lineChart2.visibility = View.VISIBLE
            }
        }
    }
}