package com.wakeup.esmoglogger.ui.statistics

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.wakeup.esmoglogger.FixedSizeArray
import com.wakeup.esmoglogger.R
import com.wakeup.esmoglogger.SharedViewModel
import com.wakeup.esmoglogger.calcDistance
import com.wakeup.esmoglogger.databinding.FragmentStatisticsBinding
import com.wakeup.esmoglogger.rfPowerLimits
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
        val colorCnt: HashMap<String, Float> = hashMapOf()
        val rfPowerLimitKeys = rfPowerLimits.keys.sorted().reversed()
        val rfPowerLimitMin = rfPowerLimitKeys.min()
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
                val distanceArray = FixedSizeArray<Pair<Float, Double>>(3)
                var lastTime = 0L
                for (value in recording.data) {
                    val location = GeoPoint(value.latitude, value.longitude, value.altitude)
                    if (location != lastLocation) {
                        val distance = calcDistance(location, lastLocation).toFloat()
                        totalDistanceValue += distance
                        val dt = (value.time - lastTime).toDouble() / 1000.0
                        distanceArray.push(Pair(distance, dt))
                        val speed = distanceArray.getDistanceSum().toDouble() / distanceArray.getTimeSum()
                        if (speed > maximumSpeedValue) {
                            maximumSpeedValue = speed
                        }
                    }
                    lastLocation = location
                    lastTime = value.time
                    val level = value.level
                    if (level > maximumLevelValue) {
                        maximumLevelValue = level
                    }
                    if (!minimumLevelValueInit || (level < minimumLevelValue)) {
                        minimumLevelValue = level
                        minimumLevelValueInit = true
                    }
                    totalEnergyValue += level * 0.5f
                    for (limit in rfPowerLimitKeys) {
                        if (level > limit) {
                            val key = rfPowerLimits[limit]
                            colorCnt[key!!] = colorCnt.getOrDefault(key, 0f) + 1f
                            break
                        }
                    }
                    if (level <= rfPowerLimitMin) {
                        colorCnt["GREEN1"] = colorCnt.getOrDefault("GREEN1", 0f) + 1f
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
            totalDistance.text = "%.2f km".format(totalDistanceValue / 1000.0)
        } else {
            totalDistance.text = "${totalDistanceValue.toInt()} m"
        }

        // Workarounds
        val averageSpeedValue = totalDistanceValue / totalRecordingTimeValue

        val maximumSpeed = root.findViewById<TextView>(R.id.maximumSpeed)
        maximumSpeed.text = "%.2f km/h".format(maximumSpeedValue * 3.6)

        val averageSpeed = root.findViewById<TextView>(R.id.averageSpeed)
        averageSpeed.text = "%.2f km/h".format(averageSpeedValue * 3.6)

        val maximumLevel = root.findViewById<TextView>(R.id.maximumLevel)
        maximumLevel.text = "%.2f mW".format(maximumLevelValue)

        val minimumLevel = root.findViewById<TextView>(R.id.minimumLevel)
        minimumLevel.text = "%.2f mW".format(minimumLevelValue)

        val averageLevel = root.findViewById<TextView>(R.id.averageLevel)
        averageLevel.text = "%.2f mW".format(totalEnergyValue / totalRecordingTimeValue)

        val totalEnergy = root.findViewById<TextView>(R.id.totalEnergy)
        if (totalEnergyValue > 0.0) {
            totalEnergy.text = "%.2f mWh".format(totalEnergyValue / 3600.0f)
        } else {
            totalEnergy.text = "%.2f mWs".format(totalEnergyValue)
        }

        val totalCnt = colorCnt.values.sum() / 100f

        for (key in rfPowerLimits.values) {
            colorCnt[key] = colorCnt.getOrDefault(key, 0f) / totalCnt
        }

        setPercentTextBar(root, R.id.percentGreen1, R.id.percentGreen1Bar, colorCnt.getOrDefault("GREEN1", 0f), "#007F00".toColorInt())
        setPercentTextBar(root, R.id.percentGreen2, R.id.percentGreen2Bar, colorCnt.getOrDefault("GREEN2", 0f), "#00BF00".toColorInt())
        setPercentTextBar(root, R.id.percentGreen3, R.id.percentGreen3Bar, colorCnt.getOrDefault("GREEN3", 0f), Color.GREEN)

        setPercentTextBar(root, R.id.percentYellow1, R.id.percentYellow1Bar, colorCnt.getOrDefault("YELLOW1", 0f), "#7F7F00".toColorInt())
        setPercentTextBar(root, R.id.percentYellow2, R.id.percentYellow2Bar, colorCnt.getOrDefault("YELLOW2", 0f), "#BFBF00".toColorInt())
        setPercentTextBar(root, R.id.percentYellow3, R.id.percentYellow3Bar, colorCnt.getOrDefault("YELLOW3", 0f), Color.YELLOW)

        setPercentTextBar(root, R.id.percentRed1, R.id.percentRed1Bar, colorCnt.getOrDefault("RED1", 0f), "#7F0000".toColorInt())
        setPercentTextBar(root, R.id.percentRed2, R.id.percentRed2Bar, colorCnt.getOrDefault("RED2", 0f), "#BF0000".toColorInt())
        setPercentTextBar(root, R.id.percentRed3, R.id.percentRed3Bar, colorCnt.getOrDefault("RED3", 0f), Color.RED)

        return root
    }

    fun setPercentTextBar(root: View, textViewId: Int, barId: Int, value: Float, color: Int) {
        val text = root.findViewById<TextView>(textViewId)
        text.text = getString(R.string._1f).format(value)
        text.setTextColor(color)
        val bar = root.findViewById<View>(barId)
        bar.layoutParams.width = (value * 3.0).toInt()
    }
}