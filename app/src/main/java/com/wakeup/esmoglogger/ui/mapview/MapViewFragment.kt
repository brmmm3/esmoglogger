package com.wakeup.esmoglogger.ui.mapview

import MapAlignedCompassOverlay
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.charts.LineChart
import com.wakeup.esmoglogger.R
import com.wakeup.esmoglogger.SharedViewModel
import com.wakeup.esmoglogger.data.ESmogAndLocation
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import com.wakeup.esmoglogger.getLevelColor
import com.wakeup.esmoglogger.ui.chartview.LineChartManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import androidx.core.view.isGone
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import kotlin.math.abs
import androidx.core.graphics.withSave
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.core.view.isVisible
import kotlin.math.max

data class MapViewData(val value: ESmogAndLocation, val new: Boolean)

class MapViewFragment : Fragment() {
    private val TAG = "MapViewFragment"
    private val viewModel: SharedViewModel by activityViewModels()
    private lateinit var mapView: MapView
    private lateinit var sensorManager: SensorManager
    private var orientationSensor: Sensor? = null
    private var currentHeading = 0f
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private lateinit var compassOverlay: MapAlignedCompassOverlay
    private lateinit var currentLocationMarker: Marker
    private var tapPositionMarker: Marker? = null
    private var isCompassRotationEnabled = true
    private var lastLevel = 0f
    private var lastCenter: GeoPoint? = null
    private var lastOrientation: Float = 0f
    private var lastLocationTime: LocalDateTime = LocalDateTime.now()
    private var lastLocation: GeoPoint? = null
    private var distance = 0f // m
    private var speed = 0f // m/s
    private val pathSegments = mutableListOf<Polyline>()
    private val esmogAndLocation = mutableListOf<ESmogAndLocation>()
    private var delayedMapInvalidate: Job? = null
    private var delayedChartInvalidate: Job? = null

    private val _curLocation = MutableLiveData<GeoPoint>()
    private val curLocation: LiveData<GeoPoint> get() = _curLocation

    private lateinit var lineChart: LineChart
    private var chartManager: LineChartManager? = null

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                // Get rotation matrix and orientation angles
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                var heading = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                if (heading < 0) heading += 360f

                // Smooth heading to reduce jitter
                if (abs(currentHeading - heading) > 1f) {
                    currentHeading = smoothHeading(heading, currentHeading)
                    if (isCompassRotationEnabled) {
                        mapView.mapOrientation = -currentHeading // Negative to align with direction
                        mapView.invalidate()
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // Handle accuracy changes if needed
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        // Initialize MapView
        mapView = view.findViewById(R.id.map_view)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setUseDataConnection(true) // Allow downloading tiles when online
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(viewModel.mapViewZoom)
        mapView.controller.setCenter(GeoPoint(48.2035, 15.6233)) // Set initial location St. Pölten

        // Enable rotation gestures
        val rotationGestureOverlay = RotationGestureOverlay(mapView)
        rotationGestureOverlay.isEnabled = true
        mapView.overlays.add(rotationGestureOverlay)

        // Add CompassOverlay
        compassOverlay = MapAlignedCompassOverlay(requireContext(), InternalCompassOrientationProvider(requireContext()), mapView)
        compassOverlay.enableCompass()
        mapView.overlays.add(compassOverlay)

        // Add MapEventsOverlay for tap detection
        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p ?: return false
                if (viewModel.isRecording.value == true || pathSegments.isEmpty()) {
                    if (tapPositionMarker != null) {
                        mapView.overlays.remove(tapPositionMarker)
                        tapPositionMarker = null
                    }
                    return false
                }
                val combinedPoints = mutableListOf<GeoPoint>().apply {
                    for (pathSegment in pathSegments) {
                        addAll(pathSegment.points)
                    }
                }
                val closestPoint = combinedPoints.minByOrNull { it.distanceToAsDouble(p) }
                val distance = closestPoint?.distanceToAsDouble(p)
                if (tapPositionMarker == null && distance!! <= 50.0) {
                    tapPositionMarker = Marker(mapView)
                    tapPositionMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mapView.overlays.add(tapPositionMarker)
                } else distance?.let {
                    if (it > 50.0) {
                        mapView.overlays.remove(tapPositionMarker)
                        tapPositionMarker = null
                    }
                }
                if (tapPositionMarker != null) {
                    tapPositionMarker?.position = closestPoint
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false // Not used
            }
        })
        mapView.overlays.add(0, mapEventsOverlay)

        // Handle compass tap to reset orientation
        mapView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val x = event.x
                val y = event.y
                val compassBounds = compassOverlay.getCompassBounds()
                if (compassBounds.contains(x, y)) {
                    if (isCompassRotationEnabled) {
                        isCompassRotationEnabled = false
                        mapView.mapOrientation = 0f
                    } else {
                        isCompassRotationEnabled = true
                    }
                    mapView.invalidate()
                    true // Consume the touch event
                } else {
                    false // Let other listeners handle the event
                }
            } else {
                false
            }
        }

        addSplitViewbutton()
        addTextOverlay()

        // Add MapListener to detect panning and rotation
        mapView.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                val newCenter = mapView.mapCenter as GeoPoint
                if (lastCenter == null || newCenter.distanceToAsDouble(lastCenter!!) > 10.0) { // Threshold to avoid noise
                    lastCenter = GeoPoint(newCenter.latitude, newCenter.longitude)
                }
                if (viewModel.isRecording.value != true && lineChart.isVisible) {
                    delayedChartUpdate()
                }
                isCompassRotationEnabled = false
                return true
            }

            override fun onZoom(event: ZoomEvent): Boolean {
                val newOrientation = mapView.mapOrientation
                if (abs(newOrientation - lastOrientation) > 1f) { // Threshold to avoid noise
                    lastOrientation = newOrientation
                    if (viewModel.isRecording.value != true && lineChart.isVisible) {
                        delayedChartUpdate()
                    }
                    isCompassRotationEnabled = false
                }
                return true
            }
        })

        // Initialize SensorManager
        sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        // Initialize marker for current location
        currentLocationMarker = Marker(mapView)
        currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        // Optional: Set a custom marker icon (add drawable to res/drawable)
        // currentLocationMarker.setIcon(getDrawable(R.drawable.ic_marker))
        mapView.overlays.add(currentLocationMarker)

        viewModel.gps.observe(viewLifecycleOwner) { enabled ->
            if (enabled) {
                onResume()
            } else {
                onPause()
            }
        }

        // Initialize line chart
        lineChart = view.findViewById(R.id.map_line_chart)
        chartManager = LineChartManager(lineChart, false)
        chartManager?.showLevelData(true)
        chartManager?.showFrequencyData(false)
        lineChart.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.esmogQueue.collect { value ->
                    chartManager?.addChartPt(value.time, value.level, value.frequency, true)
                }
            }
        }

        return view
    }

    @SuppressLint("DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val R = 6371000.0
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mapViewDataQueue.buffer(10000).collect { data ->
                    if (tapPositionMarker != null) {
                        mapView.overlays.remove(tapPositionMarker)
                        tapPositionMarker = null
                    }
                    esmogAndLocation.add(ESmogAndLocation(data.value.time, data.value.level, data.value.frequency, data.value.latitude, data.value.longitude, data.value.altitude))
                    val geoPoint = GeoPoint(data.value.latitude, data.value.longitude)
                    if (lastLocation != null) {
                        val now = LocalDateTime.now()
                        val dt = Duration.between(lastLocationTime, now).toNanos().toFloat() / 1000000f
                        if (now > lastLocationTime) {
                            lastLocationTime = now
                        }
                        val dLatitde: Double = abs(lastLocation?.latitude?.minus(geoPoint.latitude)!!)
                        val dLongitude: Double = abs(lastLocation?.longitude?.minus(geoPoint.longitude)!!)
                        val a = sin(dLatitde / 360.0 * PI).pow(2.0) + cos(lastLocation?.latitude!! / 180.0 * PI) * cos(geoPoint.latitude / 180.0 * PI) * sin(dLongitude / 360.0 * PI).pow(2.0)
                        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                        val curDistance = (R * c).toFloat()
                        distance += curDistance
                        speed = if (curDistance == 0f || dt == 0f) {
                            0f
                        } else {
                            3.6f * curDistance / dt
                        }
                    }
                    lastLocation = geoPoint
                    val color = if (data.value.frequency == 0) {
                        Color.GRAY
                    } else {
                        getLevelColor(data.value.level)
                    }
                    if (data.new || pathSegments.isEmpty() || pathSegments.last().color != color) {
                        val newPath = Polyline()
                        newPath.color = color
                        newPath.width = 8f
                        if (!data.new && !pathSegments.isEmpty() && pathSegments.last().points.isNotEmpty()) {
                            newPath.addPoint(pathSegments.last().points.lastOrNull())
                        }
                        pathSegments.add(newPath)
                        mapView.overlays.add(newPath)
                    }
                    pathSegments.last().addPoint(geoPoint) // Add new point to the path
                    _curLocation.value = geoPoint
                    if (delayedMapInvalidate == null) {
                        delayedMapInvalidate = launch(Dispatchers.Main) {
                            delay(100)
                            curLocation.value.let {
                                mapView.controller.setCenter(it) // Center map on current location
                                currentLocationMarker.position = it
                            }
                            mapView.invalidate() // Refresh map to show updated path
                            delayedMapInvalidate = null
                        }
                    }
                }
            }
        }
        viewModel.esmog.observe(viewLifecycleOwner) { value ->
            lastLevel = value.level
            currentLocationMarker.title = String.format("%6.2f mW", lastLevel)
            if (currentLocationMarker.isInfoWindowShown) {
                currentLocationMarker.closeInfoWindow()
                currentLocationMarker.showInfoWindow()
            }
        }

        Thread {
            var new = true
            // Show loaded recordings
            for (recording in viewModel.recordings) {
                if (recording.isLoaded()) {
                    new = true
                    recording.data.forEach { value ->
                        viewModel.enqueueLocationAndESmog(value, new)
                        new = false
                    }
                }
            }
            // Show latest (not saved) recording
            new = true
            viewModel.recording.data.forEach { value ->
                viewModel.enqueueLocationAndESmog(value, new)
                new = false
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.mapViewZoom = mapView.zoomLevelDouble
    }

    override fun onResume() {
        super.onResume()
        Configuration.getInstance().load(requireContext(), requireContext().getSharedPreferences("osmdroid", MODE_PRIVATE))
        mapView.onResume()
        orientationSensor?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        } ?: run {
            Toast.makeText(requireContext(), "Orientation sensor not available", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        Configuration.getInstance().save(requireContext(), requireContext().getSharedPreferences("osmdroid", MODE_PRIVATE))
        mapView.onPause()
        sensorManager.unregisterListener(sensorEventListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach()
    }

    private fun smoothHeading(newHeading: Float, currentHeading: Float): Float {
        val alpha = 0.1f // Smoothing factor (0 to 1, lower = smoother)
        var delta = newHeading - currentHeading
        if (delta > 180) delta -= 360
        else if (delta < -180) delta += 360
        return currentHeading + alpha * delta
    }

    private fun delayedChartUpdate() {
        if (delayedChartInvalidate == null) {
            delayedChartInvalidate = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                delay(100)
                val width = lineChart.viewPortHandler.contentWidth()
                val values = getVisibleESmogAndLocation()
                chartManager?.clear()
                if (!values.isEmpty()) {
                    val startTime = values.first().time
                    val endTime = values.last().time
                    val dt = endTime - startTime
                    val xZoom = 100f / max(dt, 1.0f)
                    println("$width ${values.size} $startTime $endTime $dt $xZoom")
                    for (value in values) {
                        chartManager?.addChartPt(
                            (value.time - startTime) * xZoom,
                            value.level,
                            value.frequency,
                            false
                        )
                    }
                }
                chartManager?.notifyDataChanged()
                chartManager?.resetScale()
                delayedChartInvalidate = null
            }
        }
    }

    private fun addSplitViewbutton() {
        val iconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.split_vertical_32p)
        val iconWidth = 100 // Adjust size as needed
        val iconHeight = 100 // Adjust size as needed
        val margin = 20 // Margin from top-right corner

        val overlay = object : Overlay() {
            override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
                if (shadow) return // Skip shadow layer
                iconDrawable?.let {
                    canvas.withSave {
                        // Get map rotation and apply inverse rotation to keep icon orientation fixed
                        val rotation = mapView.mapOrientation
                        rotate(-rotation, mapView.width / 2f, mapView.height / 2f)
                        it.setBounds(
                            mapView.width - iconWidth - margin,
                            margin,
                            mapView.width - margin,
                            margin + iconHeight
                        )
                        if (viewModel.location.isInitialized) {
                            if (!isCompassRotationEnabled) {
                                val colorMatrix = ColorMatrix(floatArrayOf(
                                    0f, 1f, 0f, 0f, 0f, // Red: keep red
                                    0f, 1f, 0f, 0f, 0f, // Green: map green to red
                                    0f, 0f, 1f, 0f, 0f, // Blue: keep blue
                                    0f, 0f, 0f, 1f, 0f  // Alpha: keep alpha
                                ))
                                it.colorFilter = ColorMatrixColorFilter(colorMatrix)
                            } else {
                                it.colorFilter = null
                            }
                        } else {
                            // Apply grayscale filter to the drawable
                            val colorMatrix = ColorMatrix()
                            colorMatrix.setSaturation(0f) // Set saturation to 0 for grayscale
                            it.colorFilter = ColorMatrixColorFilter(colorMatrix)
                        }
                        it.draw(this)
                    }
                }
            }

            override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                iconDrawable?.let {
                    // Adjust click coordinates for map rotation
                    val rotation = mapView.mapOrientation
                    val matrix = Matrix()
                    matrix.postRotate(-rotation, mapView.width / 2f, mapView.height / 2f)
                    // Invert the matrix to map screen coordinates to the unrotated space
                    val inverseMatrix = Matrix()
                    matrix.invert(inverseMatrix)

                    // Transform the touch point
                    val point = floatArrayOf(e.x, e.y)
                    inverseMatrix.mapPoints(point)

                    val bounds = it.bounds
                    if (point[0] >= bounds.left && point[0] <= bounds.right &&
                        point[1] >= bounds.top && point[1] <= bounds.bottom) {
                        if (lineChart.isGone) {
                            lineChart.visibility = View.VISIBLE
                            delayedChartUpdate()
                        } else {
                            lineChart.visibility = View.GONE
                        }
                        return true
                    }
                }
                return false
            }
        }
        mapView.overlays.add(overlay)
        mapView.invalidate()
    }

    private fun addTextOverlay() {
        val paint = Paint().apply {
            color = Color.BLACK // Text color
            textSize = 60f // Text size in pixels
            isAntiAlias = true // Smooth edges
            textAlign = Paint.Align.RIGHT // Align text to the left
            typeface = Typeface.DEFAULT_BOLD
        }
        paint.setShadowLayer(4f, 2f, 2f, Color.BLACK)
        val speedPaint = Paint().apply {
            color = Color.BLACK // Text color
            textSize = 60f // Text size in pixels
            isAntiAlias = true // Smooth edges
            textAlign = Paint.Align.LEFT // Align text to the left
            typeface = Typeface.DEFAULT_BOLD
        }
        speedPaint.setShadowLayer(4f, 2f, 2f, Color.BLACK)
        val levelPaint = Paint().apply {
            color = Color.BLACK // Text color
            textSize = 80f // Text size in pixels
            isAntiAlias = true // Smooth edges
            textAlign = Paint.Align.RIGHT // Align text to the left
            typeface = Typeface.DEFAULT_BOLD
        }
        levelPaint.setShadowLayer(4f, 2f, 2f, Color.BLACK)

        val xMargin = 20f
        val yMargin = 200f

        val overlay = object : Overlay() {
            @SuppressLint("DefaultLocale")
            override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
                if (shadow) return // Skip if shadow
                if (viewModel.isRecording.value != true) return
                canvas.withSave {
                    // Apply inverse rotation to keep text orientation fixed
                    val rotation = mapView.mapOrientation
                    rotate(-rotation, mapView.width / 2f, mapView.height / 2f)
                    val x = mapView.width - xMargin
                    val y = mapView.height - yMargin
                    if (lastLevel == 0f) {
                        levelPaint.color = Color.GRAY
                    } else {
                        levelPaint.color = getLevelColor(lastLevel)
                    }
                    drawText(String.format("%6.2f mW", lastLevel), x, y, levelPaint)
                    drawText(String.format("%6.2f km/h", speed), xMargin, y, speedPaint)
                    drawText(String.format("%6.2f m", distance), xMargin, y - 100, speedPaint)
                    val dt = Duration.between(viewModel.startTime, LocalDateTime.now())
                    drawText("${dt.toMillis() / 1000} s", x, y - 100, paint)
                }
            }
        }
        mapView.overlays.add(overlay)
        mapView.invalidate()
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun preloadTiles(mapView: MapView, boundingBox: BoundingBox, minZoom: Int, maxZoom: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            for (zoom in minZoom..maxZoom) {
                mapView.controller.setZoom(zoom.toDouble())
                // Calculate step sizes
                val latStep = (boundingBox.latNorth - boundingBox.latSouth) / 10
                val lonStep = (boundingBox.lonEast - boundingBox.lonWest) / 10

                // Iterate over latitude
                var lat = boundingBox.latSouth
                while (lat <= boundingBox.latNorth) {
                    // Iterate over longitude
                    var lon = boundingBox.lonWest
                    while (lon <= boundingBox.lonEast) {
                        mapView.controller.setCenter(GeoPoint(lat, lon))
                        mapView.invalidate() // Force redraw to load tiles
                        kotlinx.coroutines.delay(100) // Small delay to allow tile loading
                        lon += lonStep
                    }
                    lat += latStep
                }
            }
        }
    }

    fun getVisibleESmogAndLocation(): List<ESmogAndLocation> {
        val boundingBox = mapView.boundingBox
        val visibleESmog = mutableListOf<ESmogAndLocation>()
        for (value in esmogAndLocation) {
            if (boundingBox.contains(GeoPoint(value.latitude, value.longitude))) {
                visibleESmog.add(value)
            }
        }
        return visibleESmog
    }
}
