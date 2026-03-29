package com.verdura.app.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.squareup.picasso.Picasso
import com.verdura.app.R
import com.verdura.app.api.models.PlantDetailResponse
import com.verdura.app.api.models.TrefleSpeciesDetail
import com.verdura.app.data.AppDatabase
import com.verdura.app.model.PlantInfo
import com.verdura.app.repository.PlantRepository
import kotlinx.coroutines.launch

class PlantDetailBottomSheet : BottomSheetDialogFragment() {

    private var listCycle: String? = null
    private var listWatering: String? = null
    private var listSunlight: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_plant_detail_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments()
        val plantId = args.getInt(ARG_ID)
        val imageUrl = args.getString(ARG_IMAGE_URL)
        val name = args.getString(ARG_NAME) ?: "Unknown"
        val scientificName = args.getString(ARG_SCIENTIFIC_NAME)

        listCycle = args.getString(ARG_CYCLE)
        listWatering = args.getString(ARG_WATERING)
        listSunlight = args.getString(ARG_SUNLIGHT)

        val image = view.findViewById<ImageView>(R.id.detailImage)
        val loading = view.findViewById<CircularProgressIndicator>(R.id.detailLoading)
        val content = view.findViewById<View>(R.id.detailContent)

        if (!imageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.ic_plant)
                .error(R.drawable.ic_plant)
                .into(image)
        } else {
            image.setImageResource(R.drawable.ic_plant)
        }

        view.findViewById<TextView>(R.id.detailName).text = name
        view.findViewById<TextView>(R.id.detailScientificName).text = scientificName ?: ""

        showQuickFacts(view, listCycle, listWatering, listSunlight, null, null, null, null, null)

        content.isVisible = true
        loading.isVisible = true

        val dao = AppDatabase.getInstance(requireContext()).plantInfoDao()
        val repository = PlantRepository(dao)

        lifecycleScope.launch {
            val detailResult = repository.fetchPlantDetails(plantId)
            detailResult.fold(
                onSuccess = { detail -> bindPerenualDetail(view, detail) },
                onFailure = { }
            )

            val trefleResult = repository.fetchTrefleDetails(scientificName, name)
            loading.isVisible = false
            trefleResult.fold(
                onSuccess = { detail -> bindTrefleData(view, detail) },
                onFailure = { }
            )
        }
    }

    private fun bindPerenualDetail(view: View, detail: PlantDetailResponse) {
        val descView = view.findViewById<TextView>(R.id.detailDescription)
        if (!detail.description.isNullOrBlank()) {
            descView.text = detail.description
            descView.isVisible = true
        }

        showQuickFacts(
            view,
            detail.cycle ?: listCycle,
            detail.watering ?: listWatering,
            detail.sunlight?.joinToString(", ") ?: listSunlight,
            detail.careLevel,
            detail.type,
            detail.growthRate,
            detail.indoor,
            detail.flowers
        )

        if (!detail.floweringSeason.isNullOrBlank()) {
            addStyledChip(
                view.findViewById(R.id.factsChipGroup),
                "\uD83C\uDF38 Blooms: ${detail.floweringSeason}",
                ChipCategory.FLOWER
            )
        }

        val originCard = view.findViewById<View>(R.id.originCard)
        val originText = view.findViewById<TextView>(R.id.originText)
        if (!detail.origin.isNullOrEmpty()) {
            originText.text = detail.origin.joinToString(", ")
            originCard.isVisible = true
        }

        val propCard = view.findViewById<View>(R.id.propagationCard)
        val propText = view.findViewById<TextView>(R.id.propagationText)
        if (!detail.propagation.isNullOrEmpty()) {
            propText.text = detail.propagation.joinToString(", ")
            propCard.isVisible = true
        }

        if (!detail.defaultImage?.originalUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(detail.defaultImage?.originalUrl)
                .placeholder(R.drawable.ic_plant)
                .error(R.drawable.ic_plant)
                .into(view.findViewById<ImageView>(R.id.detailImage))
        }
    }

    private fun showQuickFacts(
        view: View,
        cycle: String?,
        watering: String?,
        sunlight: String?,
        careLevel: String?,
        type: String?,
        growthRate: String?,
        indoor: Boolean?,
        flowers: Boolean?
    ) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.factsChipGroup)

        val chips = mutableListOf<Pair<String, ChipCategory>>()

        cycle?.takeIf { it.isNotBlank() }?.let {
            chips.add("\uD83D\uDD04 $it" to ChipCategory.LIFECYCLE)
        }
        watering?.takeIf { it.isNotBlank() }?.let {
            chips.add("\uD83D\uDCA7 $it watering" to ChipCategory.WATER)
        }
        sunlight?.takeIf { it.isNotBlank() }?.let {
            chips.add("☀\uFE0F $it" to ChipCategory.SUN)
        }
        careLevel?.takeIf { it.isNotBlank() }?.let {
            chips.add("\uD83C\uDF31 $it care" to ChipCategory.CARE)
        }
        type?.takeIf { it.isNotBlank() }?.let {
            chips.add("\uD83C\uDF3F $it" to ChipCategory.TYPE)
        }
        growthRate?.takeIf { it.isNotBlank() }?.let {
            chips.add("\uD83D\uDCC8 $it growth" to ChipCategory.GROWTH)
        }
        indoor?.let {
            chips.add(
                (if (it) "\uD83C\uDFE0 Indoor friendly" else "\uD83C\uDF33 Outdoor")
                    to ChipCategory.LOCATION
            )
        }
        flowers?.let {
            if (it) chips.add("\uD83C\uDF3A Flowers" to ChipCategory.FLOWER)
        }

        if (chips.isEmpty() && chipGroup.childCount > 0) return

        chipGroup.removeAllViews()

        if (chips.isNotEmpty()) {
            view.findViewById<View>(R.id.factsHeading).isVisible = true
            chipGroup.isVisible = true
            for ((label, category) in chips) {
                addStyledChip(chipGroup, label, category)
            }
        }
    }

    private fun addStyledChip(chipGroup: ChipGroup, text: String, category: ChipCategory) {
        val chip = Chip(chipGroup.context).apply {
            this.text = text
            isClickable = false
            isCheckable = false
            setEnsureMinTouchTargetSize(false)

            val (bgColor, textColor) = category.colors()
            chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(bgColor))
            setTextColor(Color.parseColor(textColor))
            chipStrokeWidth = 0f
            textSize = 13f

            chipMinHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 34f, resources.displayMetrics
            )
        }
        chipGroup.addView(chip)
    }

    private fun bindTrefleData(view: View, detail: TrefleSpeciesDetail) {
        val descView = view.findViewById<TextView>(R.id.detailDescription)
        if (!descView.isVisible) {
            val desc = detail.growth?.description ?: detail.growth?.sowing
            if (!desc.isNullOrBlank()) {
                descView.text = desc
                descView.isVisible = true
            }
        }

        val container = view.findViewById<LinearLayout>(R.id.careContainer)
        val items = mutableListOf<Triple<String, String, String>>()

        detail.growth?.light?.let {
            items.add(Triple("\uD83D\uDD06", "Light Level", lightToLabel(it)))
        }

        val wateringValue = detail.growth?.soilHumidity ?: detail.growth?.atmosphericHumidity
        wateringValue?.let {
            items.add(Triple("\uD83D\uDCA7", "Humidity", humidityToLabel(it)))
        }

        detail.growth?.minimumTemperature?.degC?.let {
            items.add(Triple("\uD83C\uDF21\uFE0F", "Min Temperature", "${it.toInt()}°C"))
        }

        detail.specifications?.toxicity?.let { tox ->
            val label = if (tox == "none") "Non-toxic" else tox.replaceFirstChar { it.uppercase() }
            items.add(Triple("⚠\uFE0F", "Toxicity", label))
        }

        val phMin = detail.growth?.phMinimum
        val phMax = detail.growth?.phMaximum
        if (phMin != null && phMax != null) {
            items.add(Triple("\uD83E\uDDEA", "Soil pH", "$phMin – $phMax"))
        }

        detail.flower?.color?.takeIf { it.isNotEmpty() }?.let { colors ->
            items.add(Triple("\uD83C\uDF3B", "Flower Color",
                colors.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } }))
        }

        detail.foliage?.color?.takeIf { it.isNotEmpty() }?.let { colors ->
            items.add(Triple("\uD83C\uDF43", "Foliage",
                colors.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } }))
        }

        if (items.isNotEmpty()) {
            view.findViewById<View>(R.id.careInfoHeading).isVisible = true
            container.isVisible = true
            container.removeAllViews()
            for ((icon, label, value) in items) {
                addCareCard(container, icon, label, value)
            }
        }

        if (!detail.imageUrl.isNullOrEmpty() && !view.findViewById<TextView>(R.id.detailDescription).isVisible) {
            Picasso.get()
                .load(detail.imageUrl)
                .placeholder(R.drawable.ic_plant)
                .error(R.drawable.ic_plant)
                .into(view.findViewById<ImageView>(R.id.detailImage))
        }
    }

    private fun addCareCard(container: LinearLayout, icon: String, label: String, value: String) {
        val dp = { v: Float ->
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()
        }

        val row = LinearLayout(container.context).apply {
            orientation = LinearLayout.HORIZONTAL
            background = ContextCompat.getDrawable(container.context, R.drawable.bg_care_item)
            setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = dp(8f)
            layoutParams = params
        }

        val iconView = TextView(container.context).apply {
            text = icon
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            val params = LinearLayout.LayoutParams(dp(32f), LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams = params
        }

        val textContainer = LinearLayout(container.context).apply {
            orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = params
        }

        val labelView = TextView(container.context).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(ContextCompat.getColor(container.context, R.color.md_theme_onSurfaceVariant))
        }

        val valueView = TextView(container.context).apply {
            text = value
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(container.context, R.color.md_theme_onBackground))
        }

        textContainer.addView(labelView)
        textContainer.addView(valueView)
        row.addView(iconView)
        row.addView(textContainer)
        container.addView(row)
    }

    private enum class ChipCategory {
        LIFECYCLE, WATER, SUN, CARE, TYPE, GROWTH, LOCATION, FLOWER;

        fun colors(): Pair<String, String> = when (this) {
            LIFECYCLE -> "#E8F5E9" to "#2E7D32"
            WATER -> "#E3F2FD" to "#1565C0"
            SUN -> "#FFF8E1" to "#F57F17"
            CARE -> "#F3E5F5" to "#7B1FA2"
            TYPE -> "#E0F2F1" to "#00695C"
            GROWTH -> "#FFF3E0" to "#E65100"
            LOCATION -> "#E8EAF6" to "#283593"
            FLOWER -> "#FCE4EC" to "#C62828"
        }
    }

    companion object {
        const val TAG = "PlantDetailBottomSheet"
        private const val ARG_ID = "id"
        private const val ARG_IMAGE_URL = "image_url"
        private const val ARG_NAME = "name"
        private const val ARG_SCIENTIFIC_NAME = "scientific_name"
        private const val ARG_CYCLE = "cycle"
        private const val ARG_WATERING = "watering"
        private const val ARG_SUNLIGHT = "sunlight"

        fun newInstance(plant: PlantInfo): PlantDetailBottomSheet {
            return PlantDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID, plant.id)
                    putString(ARG_IMAGE_URL, plant.imageUrl)
                    putString(ARG_NAME, plant.commonName)
                    putString(ARG_SCIENTIFIC_NAME, plant.scientificName)
                    putString(ARG_CYCLE, plant.cycle)
                    putString(ARG_WATERING, plant.watering)
                    putString(ARG_SUNLIGHT, plant.sunlight)
                }
            }
        }

        private fun lightToLabel(light: Int): String = when {
            light <= 3 -> "Low light"
            light <= 5 -> "Part shade"
            light <= 7 -> "Bright indirect"
            else -> "Full sun"
        }

        private fun humidityToLabel(humidity: Int): String = when {
            humidity <= 2 -> "Minimum"
            humidity <= 4 -> "Average"
            humidity <= 6 -> "Frequent"
            else -> "Abundant"
        }
    }
}
