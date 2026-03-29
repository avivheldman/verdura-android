package com.verdura.app.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.squareup.picasso.Picasso
import com.verdura.app.R
import com.verdura.app.model.PlantInfo

class PlantAdapter(
    private val onItemClick: (PlantInfo) -> Unit = {}
) : ListAdapter<PlantInfo, PlantAdapter.PlantViewHolder>(PlantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant, parent, false)
        val holder = PlantViewHolder(view)
        view.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onItemClick(getItem(pos))
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val plantImage: ImageView = itemView.findViewById(R.id.plantImage)
        private val plantName: TextView = itemView.findViewById(R.id.plantName)
        private val plantScientificName: TextView = itemView.findViewById(R.id.plantScientificName)
        private val plantMeta: TextView = itemView.findViewById(R.id.plantMeta)

        fun bind(plant: PlantInfo) {
            plantName.text = plant.commonName ?: "Unknown"
            plantScientificName.text = plant.scientificName ?: ""

            val metaParts = listOfNotNull(
                plant.cycle?.takeIf { it.isNotBlank() }?.let { "🔄 $it" },
                plant.watering?.takeIf { it.isNotBlank() }?.let { "💧 $it" },
                plant.sunlight?.takeIf { it.isNotBlank() }?.let { "☀️ $it" }
            )
            plantMeta.text = metaParts.joinToString("  ")
            plantMeta.isVisible = metaParts.isNotEmpty()

            if (!plant.imageUrl.isNullOrEmpty()) {
                ImageViewCompat.setImageTintList(plantImage, null)
                Picasso.get()
                    .load(plant.imageUrl)
                    .placeholder(R.drawable.ic_plant)
                    .error(R.drawable.ic_plant)
                    .into(plantImage)
            } else {
                plantImage.setImageResource(R.drawable.ic_plant)
                val primaryColor = MaterialColors.getColor(plantImage, com.google.android.material.R.attr.colorPrimary)
                ImageViewCompat.setImageTintList(plantImage, ColorStateList.valueOf(primaryColor))
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
