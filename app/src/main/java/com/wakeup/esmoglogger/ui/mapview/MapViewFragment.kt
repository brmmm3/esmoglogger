package com.wakeup.esmoglogger.ui.mapview

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.wakeup.esmoglogger.R
import com.wakeup.esmoglogger.SharedViewModel
import com.wakeup.esmoglogger.buttonSetEnabled
import com.wakeup.esmoglogger.data.ESmogAndLocation
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import com.wakeup.esmoglogger.getLevelColor
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import org.osmdroid.api.IGeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider

data class MapViewData(val value: ESmogAndLocation, val new: Boolean)

class MapViewFragment : Fragment() {
    private val viewModel: SharedViewModel by activityViewModels()
    private lateinit var mapView: MapView
    private lateinit var compassOverlay: CompassOverlay
    private lateinit var currentLocationMarker: Marker
    private val pathSegments = mutableListOf<Polyline>()
    private var delayedInvalidate: Job? = null
    private val _curLocation = MutableLiveData<GeoPoint>()
    private val curLocation: LiveData<GeoPoint> get() = _curLocation

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        // Initialize MapView
        mapView = view.findViewById(R.id.map)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setUseDataConnection(true) // Allow downloading tiles when online
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(viewModel.mapViewZoom)
        mapView.controller.setCenter(GeoPoint(48.2035, 15.6233)) // Set initial location St. Pölten

        // Add CompassOverlay
        compassOverlay = CompassOverlay(requireContext(), InternalCompassOrientationProvider(requireContext()), mapView)
        compassOverlay.enableCompass()
        mapView.overlays.add(compassOverlay)

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

        val buttonRecenter = view.findViewById<MaterialButton>(R.id.button_map_recenter)
        buttonSetEnabled(buttonRecenter, false)

        viewModel.locationValid.observe(viewLifecycleOwner) {
            buttonSetEnabled(buttonRecenter, it)
        }

        buttonRecenter.setOnClickListener {
            if (viewModel.location.isInitialized) {
                val position = viewModel.location.value
                mapView.controller.setCenter(GeoPoint(position?.latitude!!,
                    position.longitude
                ))
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
    }

    override fun onPause() {
        super.onPause()
        Configuration.getInstance().save(requireContext(), requireContext().getSharedPreferences("osmdroid", MODE_PRIVATE))
        mapView.onPause()
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
}
