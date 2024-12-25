package com.teamname.canopy.utils

import android.R
import android.R.attr.data
import android.R.attr.resource
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.GeoPoint

// Define the Canopy class
class Canopy(
    val canopyName: String,
    val canopyCoords: GeoPoint,
    val canopyOwner: String,
    val canopyAddress: String,
    val canopyIndoorMapPoints: ArrayList<CanopyIndoorPoint>?
){

    override fun toString(): String {
        return canopyName
    }
    class CanopyIndoorPoint( val topMargin: Long, val marginStart: Long)
}

// Custom Adapter for the Canopy dropdown
class CanopyCustomAdapter(
    context: Context,
    private val resource: Int,
    private val objects: MutableList<Canopy>
) : ArrayAdapter<Canopy>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var row = convertView
        val holder: HeaderHolder
        if (row == null) {
            val inflater = LayoutInflater.from(context)
            row = inflater.inflate(resource, parent, false)

            holder = HeaderHolder(
                row.findViewById(R.id.text1),
                row.findViewById(R.id.text2),

            )

            row.tag = holder
        } else {
            holder = row.tag as HeaderHolder
        }

        val item = objects[position]
        holder.name.text = item.canopyName
        holder.address.text = item.canopyAddress

        return row!!
    }

    // ViewHolder to optimize performance
    private class HeaderHolder(val name: TextView, val address: TextView)
}
 class THLDataPoint(val temperature: Double, val humidity: Double, val lightIntensity: Double)



class CanopyRecyclerViewAdapter(
    private var items: List<Canopy>,
    private val onItemClick: (Canopy) -> Unit
) : RecyclerView.Adapter<CanopyRecyclerViewAdapter.CanopyViewHolder>() {

    // ViewHolder to bind the item views
    inner class CanopyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.text1)
        private val subtitle = view.findViewById<TextView>(R.id.text2)

        fun bind(item: Canopy) {
            title.text = item.canopyName
            subtitle.text = item.canopyAddress
            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CanopyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.simple_list_item_2, // Reuse your existing layout
            parent,
            false
        )
        return CanopyViewHolder(view)
    }

    override fun onBindViewHolder(holder: CanopyViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    // Update the data when search results change
    fun updateList(newItems: List<Canopy>) {
        items = newItems
        notifyDataSetChanged()
    }
}
