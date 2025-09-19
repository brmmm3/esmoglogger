package com.wakeup.esmoglogger

// RecyclerView Adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

// Data class for navigation items
data class NavItem(val icon: Int, val title: String, val fragmentClass: Class<out Fragment>)

class BottomNavAdapter(
    private val items: List<NavItem>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<BottomNavAdapter.ViewHolder>() {
    private var selectedPosition = 0

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.nav_icon)
        val title: TextView = itemView.findViewById(R.id.nav_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bottom_nav, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.icon)
        holder.title.text = item.title

        // Highlight selected item
        if (position == selectedPosition) {
            holder.icon.setColorFilter(
                ContextCompat.getColor(holder.itemView.context, android.R.color.holo_blue_dark)
            )
            holder.title.setTextColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.holo_blue_dark)
            )
        } else {
            holder.icon.clearColorFilter()
            holder.title.setTextColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.black)
            )
        }

        holder.itemView.setOnClickListener { onItemClick(position) }
    }

    override fun getItemCount(): Int = items.size

    fun setSelectedPosition(position: Int) {
        val previousPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousPosition)
        notifyItemChanged(selectedPosition)
    }

    fun getItem(position: Int): NavItem = items[position]
}
