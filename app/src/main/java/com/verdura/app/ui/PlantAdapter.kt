package com.verdura.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.verdura.app.R
import com.verdura.app.model.PlantInfo

class PlantAdapter : ListAdapter<PlantInfo, PlantAdapter.PlantViewHolder>(PlantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val plantImage: ImageView = itemView.findViewById(R.id.plantImage)
        private val plantName: TextView = itemView.findViewById(R.id.plantName)
        private val plantScientificName: TextView = itemView.findViewById(R.id.plantScientificName)
        private val plantCycle: TextView = itemView.findViewById(R.id.plantCycle)
        private val plantWatering: TextView = itemView.findViewById(R.id.plantWatering)
        private val plantSunlight: TextView = itemView.findViewById(R.id.plantSunlight)

        fun bind(plant: PlantInfo) {
            plantName.text = plant.commonName ?: "Unknown"
            plantScientificName.text = plant.scientificName ?: ""
            plantCycle.text = plant.cycle ?: ""
            plantWatering.text = plant.watering ?: ""
            plantSunlight.text = plant.sunlight ?: ""

            if (!plant.imageUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(plant.imageUrl)
                    .placeholder(R.drawable.ic_plant)
                    .error(R.drawable.ic_plant)
                    .into(plantImage)
            } else {
                plantImage.setImageResource(R.drawable.ic_plant)
            }
        }
    }

    class PlantDiffCallback : DiffUtil.ItemCallback<PlantInfo>() {
        override fun areItemsTheSame(oldItem: PlantInfo, newItem: PlantInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PlantInfo, newItem: PlantInfo): Boolean {
            return oldItem == newItem
        }
    }
}
