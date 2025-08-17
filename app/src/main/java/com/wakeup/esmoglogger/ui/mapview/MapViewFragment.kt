package com.wakeup.esmoglogger.ui.mapview

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.wakeup.esmoglogger.R
import com.wakeup.esmoglogger.data.SharedDataSeries
import com.wakeup.esmoglogger.location.SharedLocationData
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline


class MapViewFragment : Fragment() {
    private lateinit var mapView: MapView
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
        mapView.controller.setZoom(15.0)

        // Initialize marker for current location
        currentLocationMarker = Marker(mapView)
        currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        currentLocationMarker.title = "Current Location"
        // Optional: Set a custom marker icon (add drawable to res/drawable)
        // currentLocationMarker.setIcon(getDrawable(R.drawable.ic_marker))
        mapView.overlays.add(currentLocationMarker)

        // Initialize location manager
        SharedLocationData.location.observe(viewLifecycleOwner) { location ->
            val geoPoint = GeoPoint(location.latitude, location.longitude)
            val level = if (SharedDataSeries.dataSeries.esmogData.isEmpty()) {
                0f
            } else {
                SharedDataSeries.dataSeries.esmogData.last().level
            }
            val color = if (level < 0.18) {
                Color.GREEN
            } else if (level < 5.8) {
                Color.YELLOW
            } else {
                Color.RED
            }
            if (pathSegments.isEmpty() || pathSegments.last().color != color) {
                val newPath = Polyline()
                newPath.color = color
                newPath.width = 8f
                pathSegments.add(newPath)
                mapView.overlays.add(newPath)
            }
            pathSegments.last().addPoint(geoPoint) // Add new point to the path
            mapView.controller.setCenter(geoPoint) // Center map on current location
            currentLocationMarker.position = geoPoint
            mapView.invalidate() // Refresh map to show updated path
        }

        SharedLocationData.command.observe(viewLifecycleOwner) { command ->
            if (command == "start") {
                onResume()
            } else if (command == "stop") {
                onPause()
            }
        }

        return view
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