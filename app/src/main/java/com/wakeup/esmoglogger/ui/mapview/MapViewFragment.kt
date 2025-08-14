package com.wakeup.esmoglogger.ui.mapview

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import com.wakeup.esmoglogger.R
import com.wakeup.esmoglogger.location.SharedLocationData

class MapViewFragment : Fragment() {
    private lateinit var mapView: MapView
    private val path = Polyline()

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

        // Set up the polyline for the path
        path.color = Color.RED // Set path color to red
        path.width = 8f // Set path width
        mapView.overlays.add(path)

        // Initialize location manager
        SharedLocationData.location.observe(viewLifecycleOwner) { location ->
            val geoPoint = GeoPoint(location.latitude, location.longitude)
            path.addPoint(geoPoint) // Add new point to the path
            mapView.controller.setCenter(geoPoint) // Center map on current location
            mapView.invalidate() // Refresh map to show updated path
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