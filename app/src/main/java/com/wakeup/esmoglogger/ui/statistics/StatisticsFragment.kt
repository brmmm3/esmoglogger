package com.wakeup.esmoglogger.ui.statistics

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.wakeup.esmoglogger.FixedSizeArray
import com.wakeup.esmoglogger.R
import com.wakeup.esmoglogger.SharedViewModel
import com.wakeup.esmoglogger.calcDistance
import com.wakeup.esmoglogger.databinding.FragmentStatisticsBinding
import com.wakeup.esmoglogger.levelLimits
import org.osmdroid.util.GeoPoint
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
        var endTimeValueInit = false
        var totalRecordingTimeValue = 0 // Seconds
        var totalDistanceValue = 0.0 // m
        var maximumSpeedValue = 0.0 // m/s
        var maximumLevelValue = 0f
        var minimumLevelValue = 0f
        var minimumLevelValueInit = false
        var totalEnergyValue = 0f
        var greenCnt = 0
        var yellowCnt = 0
        var redCnt = 0
        for (recording in viewModel.recordings) {
            if (recording.startTime < startTimeValue) {
                startTimeValue = recording.startTime
            }
            if (!endTimeValueInit || recording.endTime > endTimeValue) {
                endTimeValue = recording.endTime
                endTimeValueInit = true
            }
            totalRecordingTimeValue += ChronoUnit.SECONDS.between(recording.startTime, recording.endTime).toInt()
            if (!recording.data.isEmpty()) {
                val value = recording.data.first()
                var lastLocation = GeoPoint(value.latitude, value.longitude, value.altitude)
                var distanceArray = FixedSizeArray<Pair<Float, Double>>(3)
                var dt = 0.0
                for (value in recording.data) {
                    val location = GeoPoint(value.latitude, value.longitude, value.altitude)
                    dt += 0.5
                    if (location != lastLocation) {
                        val distance = calcDistance(location, lastLocation).toFloat()
                        totalDistanceValue += distance
                        distanceArray.push(Pair(distance, dt))
                        val speed = distanceArray.getDistanceSum().toDouble() / distanceArray.getTimeSum()
                        if (speed > maximumSpeedValue) {
                            maximumSpeedValue = speed
                        }
                        dt = 0.0
                    }
                    lastLocation = location
                    if (value.level > maximumLevelValue) {
                        maximumLevelValue = value.level
                    }
                    if (!minimumLevelValueInit || (value.level < minimumLevelValue)) {
                        minimumLevelValue = value.level
                        minimumLevelValueInit = true
                    }
                    totalEnergyValue += value.level * 0.5f
                    if (value.level > levelLimits[1]) {
                        redCnt += 1
                    } else if (value.level > levelLimits[0]) {
                        yellowCnt += 1
                    } else {
                        greenCnt += 1
                    }
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

        val totalDistance = root.findViewById<TextView>(R.id.totalDistance)
        if (totalDistanceValue > 1000.0) {
            totalDistance.text = "${"%.2f".format(totalDistanceValue / 1000.0)} km"
        } else {
            totalDistance.text = "${totalDistanceValue.toInt()} m"
        }

        // Workarounds
        val averageSpeedValue = totalDistanceValue / totalRecordingTimeValue
        if (maximumSpeedValue > 2.0 * averageSpeedValue) {
            maximumSpeedValue = 2.0 * averageSpeedValue
        }

        val maximumSpeed = root.findViewById<TextView>(R.id.maximumSpeed)
        maximumSpeed.text = "${"%.2f".format(maximumSpeedValue * 3.6)} km/h"

        val averageSpeed = root.findViewById<TextView>(R.id.averageSpeed)
        averageSpeed.text = "${"%.2f".format(averageSpeedValue * 3.6)} km/h"

        val maximumLevel = root.findViewById<TextView>(R.id.maximumLevel)
        maximumLevel.text = "${"%.2f".format(maximumLevelValue)} mW"

        val minimumLevel = root.findViewById<TextView>(R.id.minimumLevel)
        minimumLevel.text = "${"%.2f".format(minimumLevelValue)} mW"

        val averageLevel = root.findViewById<TextView>(R.id.averageLevel)
        averageLevel.text = "${"%.2f".format(totalEnergyValue / totalRecordingTimeValue)} mW"

        val totalEnergy = root.findViewById<TextView>(R.id.totalEnergy)
        if (totalEnergyValue > 0.0) {
            totalEnergy.text = "${"%.2f".format(totalEnergyValue / 3600.0f)} mWh"
        } else {
            totalEnergy.text = "${"%.2f".format(totalEnergyValue)} mWs"
        }

        val totalCnt = (greenCnt + yellowCnt + redCnt).toFloat()

        val percentGreen = root.findViewById<TextView>(R.id.percentGreen)
        percentGreen.text = "${"%.2f".format(100.0 * greenCnt.toFloat() / totalCnt)} %"

        val percentYellow = root.findViewById<TextView>(R.id.percentYellow)
        percentYellow.text = "${"%.2f".format(100.0 * yellowCnt.toFloat() / totalCnt)} %"

        val percentRed = root.findViewById<TextView>(R.id.percentRed)
        percentRed.text = "${"%.2f".format(100.0 * redCnt.toFloat() / totalCnt)} %"

        return root
    }
}