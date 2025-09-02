import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import org.osmdroid.views.MapView

class CustomMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : MapView(context, attrs) {

    // Store the touch listener to delegate touch events
    private var customTouchListener: ((MotionEvent) -> Boolean)? = null

    override fun setOnTouchListener(l: OnTouchListener?) {
        customTouchListener = l?.let { { event: MotionEvent -> l.onTouch(this, event) } }
        super.setOnTouchListener(l)
    }

    override fun performClick(): Boolean {
        // Call super to handle default accessibility behavior
        super.performClick()
        // Return true to indicate the click was handled
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Delegate to the custom touch listener if set
        if (event.action == MotionEvent.ACTION_UP && customTouchListener?.invoke(event) == true) {
            // Trigger performClick for accessibility when a tap is handled
            performClick()
            return true
        }
        // Fall back to default MapView touch handling
        return super.onTouchEvent(event)
    }
}
