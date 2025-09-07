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
import com.google.android.material.button.MaterialButton
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
import com.wakeup.esmoglogger.data.ESmog
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import kotlin.math.abs
import androidx.core.graphics.withSave

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
    private var isGpsLocationEnabled = true
    private var isCompassRotationEnabled = true
    private var lastCenter: GeoPoint? = null
    private var lastOrientation: Float = 0f
    private val pathSegments = mutableListOf<Polyline>()
    private val esmog = mutableListOf<ESmog>()
    private var delayedInvalidate: Job? = null

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
                    if (isGpsLocationEnabled && isCompassRotationEnabled) {
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
                if (!pathSegments.isEmpty()) {
                    val closestPoint = pathSegments.first().points.minByOrNull { it.distanceToAsDouble(p) }
                    return true
                }
                return false
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

        addCenterToCurrentLocationButton()
        addTextOverlay()

        // Add MapListener to detect panning and rotation
        mapView.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                val newCenter = mapView.mapCenter as GeoPoint
                if (lastCenter == null || newCenter.distanceToAsDouble(lastCenter!!) > 10.0) { // Threshold to avoid noise
                    lastCenter = GeoPoint(newCenter.latitude, newCenter.longitude)
                }
                isGpsLocationEnabled = false
                return true
            }

            override fun onZoom(event: ZoomEvent): Boolean {
                val newOrientation = mapView.mapOrientation
                if (abs(newOrientation - lastOrientation) > 1f) { // Threshold to avoid noise
                    lastOrientation = newOrientation
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

        // Split view Button
        val buttonSplitView = view.findViewById<MaterialButton>(R.id.button_map_switch_view)

        buttonSplitView.setOnClickListener {
            if (lineChart.isGone) {
                lineChart.visibility = View.VISIBLE
            } else {
                lineChart.visibility = View.GONE
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
                    chartManager?.addChartPt(value.time, value.level, value.frequency)
                }
            }
        }

        return view
    }

    @SuppressLint("DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mapViewDataQueue.buffer(10000).collect { data ->
                    esmog.add(ESmog(data.value.time, data.value.level, data.value.frequency))
                    val geoPoint = GeoPoint(data.value.latitude, data.value.longitude)
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
                    if (delayedInvalidate == null) {
                        delayedInvalidate = launch(Dispatchers.Main) {
                            delay(100)
                            curLocation.value.let {
                                mapView.controller.setCenter(it) // Center map on current location
                                currentLocationMarker.position = it
                            }
                            mapView.invalidate() // Refresh map to show updated path
                            delayedInvalidate = null
                        }
                    }
                }
            }
        }
        viewModel.esmog.observe(viewLifecycleOwner) { value ->
            currentLocationMarker.title = String.format("%6.2f mW", value.level)
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

    private fun addCenterToCurrentLocationButton() {
        val iconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.location_green_32p)
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
                            if (!isGpsLocationEnabled) {
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
                        if (viewModel.location.isInitialized) {
                            isGpsLocationEnabled = true
                            val position = viewModel.location.value
                            mapView.controller.setCenter(GeoPoint(position?.latitude!!,
                                position.longitude
                            ))
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
            textSize = 80f // Text size in pixels
            isAntiAlias = true // Smooth edges
            textAlign = Paint.Align.RIGHT // Align text to the left
            typeface = Typeface.DEFAULT_BOLD
        }
        paint.setShadowLayer(4f, 2f, 2f, Color.BLACK)
        val margin = 20f

        val overlay = object : Overlay() {
            override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
                if (shadow) return // Skip if shadow
                canvas.withSave {
                    // Apply inverse rotation to keep text orientation fixed
                    val rotation = mapView.mapOrientation
                    rotate(-rotation, mapView.width / 2f, mapView.height / 2f)
                    drawText("HELLOHELLOHELLO", mapView.width - margin, mapView.height - margin, paint)
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

    fun getVisibleESmog(): List<ESmog> {
        val boundingBox = mapView.boundingBox
        val visibleESmog = mutableListOf<ESmog>()
        var index = 0
        for (pathSegment in pathSegments) {
            for (point in pathSegment.points)  {
                if (boundingBox.contains(point)) {
                    visibleESmog.add(esmog[index])
                }
                index++
            }
        }
        return visibleESmog
    }
}
