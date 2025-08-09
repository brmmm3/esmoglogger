package com.wakeup.esmoglogger.ui.chartview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.wakeup.esmoglogger.R


class ChartViewFragment : Fragment() {
    private lateinit var lineChart: LineChart
    private var chartManager: LineChartManager? = null

    //private var time = 0f
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Find the LineChart in the inflated view
        lineChart = view.findViewById(R.id.lineChart)

        // Create the chart manager
        chartManager = LineChartManager(lineChart)

        SharedChartData.data.observe(viewLifecycleOwner) { value ->
            // value = Pair(Time, Y-Value)
            chartManager?.addEntry(value.first, value.second)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chartManager = null
    }
}