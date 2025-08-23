package com.wakeup.esmoglogger.ui.mapview

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wakeup.esmoglogger.R
import com.wakeup.esmoglogger.SharedViewModel
import com.wakeup.esmoglogger.data.ESmogAndLocation
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import androidx.core.graphics.toColorInt
import com.wakeup.esmoglogger.getLevelColor


class MapViewFragment : Fragment() {
    private val viewModel: SharedViewModel by activityViewModels()
    private lateinit var mapView: MapView
    private val values = arrayListOf<ESmogAndLocation>()
    private val pathSegments = mutableListOf<Polyline>()
    private lateinit var currentLocationMarker: Marker

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        // Initialize MapView
        mapView = view.findViewById(R.id.map)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(viewModel.mapViewZoom)

        // Initialize marker for current location
        currentLocationMarker = Marker(mapView)
        currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        currentLocationMarker.title = "Current Location"
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

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mapViewDataQueue.collect { value ->
                    values.add(value)
                    val geoPoint = GeoPoint(value.latitude, value.longitude)
                    val color = if (value.frequency == 0) {
                        Color.GRAY
                    } else {
                        getLevelColor(value.level)
                    }
                    if (pathSegments.isEmpty() || pathSegments.last().color != color) {
                        val newPath = Polyline()
                        newPath.color = color
                        newPath.width = 8f
                        if (!pathSegments.isEmpty() && pathSegments.last().points.isNotEmpty()) {
                            newPath.addPoint(pathSegments.last().points.lastOrNull())
                        }
                        pathSegments.add(newPath)
                        mapView.overlays.add(newPath)
                    }
                    pathSegments.last().addPoint(geoPoint) // Add new point to the path
                    mapView.controller.setCenter(geoPoint) // Center map on current location
                    currentLocationMarker.position = geoPoint
                    mapView.invalidate() // Refresh map to show updated path
                }
            }
        }

        Thread {
            viewModel.dataSeries.data.forEach { value ->
                viewModel.enqueueLocationAndESmog(value)
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.mapViewZoom = mapView.zoomLevelDouble
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}