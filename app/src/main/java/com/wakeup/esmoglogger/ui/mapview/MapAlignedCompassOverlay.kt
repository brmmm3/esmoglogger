import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.IOrientationProvider

class MapAlignedCompassOverlay(
    context: Context,
    orientationProvider: IOrientationProvider,
    mapView: MapView
) : CompassOverlay(context, orientationProvider, mapView) {

    // Expose the compass bounding rectangle for tap detection
    fun getCompassBounds(): RectF {
        val compassRadius = 20f
        val compassSize = 5f * compassRadius
        val compassX = mCompassRoseCenterX - compassRadius
        val compassY = mCompassRoseCenterY - compassRadius
        return RectF(compassX, compassY, compassX + compassSize, compassY + compassSize)
    }

    override fun drawCompass(canvas: Canvas, bearing: Float, screenRect: android.graphics.Rect) {
        // Adjust bearing by map's orientation
        val adjustedBearing = bearing - mMapView.mapOrientation
        super.drawCompass(canvas, adjustedBearing, screenRect)
    }
}
