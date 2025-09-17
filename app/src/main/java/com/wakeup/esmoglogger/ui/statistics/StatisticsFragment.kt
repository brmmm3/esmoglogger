package com.wakeup.esmoglogger.ui.statistics

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.wakeup.esmoglogger.R
import com.wakeup.esmoglogger.SharedViewModel
import com.wakeup.esmoglogger.databinding.FragmentStatisticsBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.getValue
import java.time.temporal.ChronoUnit

class StatisticsFragment : Fragment() {
    private val viewModel: SharedViewModel by activityViewModels()
    private var binding: FragmentStatisticsBinding? = null

    @SuppressLint("ObsoleteSdkInt", "DefaultLocale")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        val root: View = binding!!.getRoot()

        var startTimeValue = LocalDateTime.now()
        var endTimeValue = LocalDateTime.now()
        var totalRecordingTimeValue = 0 // Seconds
        var totalDistanceValue = 0 // m
        var maximumSpeed = 0f // m/s
        for (recording in viewModel.recordings) {
            if (recording.startTime < startTimeValue) {
                startTimeValue = recording.startTime
            }
            if (recording.endTime < endTimeValue) {
                endTimeValue = recording.endTime
            }
            totalRecordingTimeValue += ChronoUnit.SECONDS.between(recording.startTime, recording.endTime).toInt()
            if (!recording.data.isEmpty()) {
                var lastValue = recording.data.first()
                for (value in recording.data) {

                }
            }
        }

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        val startTime = root.findViewById<TextView>(R.id.startTime)
        startTime.text = startTimeValue.format(formatter)

        val endTime = root.findViewById<TextView>(R.id.endTime)
        endTime.text = endTimeValue.format(formatter)

        val totalRecordingTime = root.findViewById<TextView>(R.id.totalRecordingTime)
        val hours = totalRecordingTimeValue / 3600
        val minutes = (totalRecordingTimeValue % 3600) / 60
        val seconds = totalRecordingTimeValue % 60
        totalRecordingTime.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        return root
    }
}