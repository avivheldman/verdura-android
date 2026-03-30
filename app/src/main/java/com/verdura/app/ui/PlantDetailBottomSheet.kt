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
import kotlinx.coroutines.ensureActive
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

        val descView = view.findViewById<TextView>(R.id.detailDescription)
        descView.text = "Loading plant details\u2026"
        descView.isVisible = true

        content.isVisible = true
        loading.isVisible = true

        val db = AppDatabase.getInstance(requireContext())
        val repository = PlantRepository(
            db.plantInfoDao(), db.plantDetailCacheDao(), db.trefleDetailCacheDao()
        )

        lifecycleScope.launch {
            val detailResult = repository.fetchPlantDetails(plantId)
            ensureActive()
            if (!isAdded) return@launch

            detailResult.fold(
                onSuccess = { detail -> bindPerenualDetail(view, detail) },
                onFailure = {
                    if (descView.text == "Loading plant details\u2026") {
                        descView.text = "No description available"
                    }
                    showFallbackSections(view)
                }
            )

            if (!scientificName.isNullOrBlank()) {
                val trefleResult = repository.fetchTrefleDetails(scientificName, name)
                ensureActive()
                if (!isAdded) return@launch
                trefleResult.onSuccess { detail ->
                    bindTrefleData(view, detail)
                    updateQuickFactsFromTrefle(view, detail)
                    fillOriginFromTrefle(view, detail)
                    repository.backfillPlantInfoFromTrefle(plantId, detail)
                }
            }

            if (descView.text == "Loading plant details\u2026") {
                descView.text = "No description available"
            }
            showFallbackSections(view)
            loading.isVisible = false
        }
    }

    private fun bindPerenualDetail(view: View, detail: PlantDetailResponse) {
        val descView = view.findViewById<TextView>(R.id.detailDescription)
        descView.text = if (!detail.description.isNullOrBlank()) {
            detail.description
        } else {
            "No description available"
        }
        descView.isVisible = true

        val cycle = detail.cycle?.takeUnless { it.startsWith("Upgrade") } ?: listCycle
        val watering = detail.watering?.takeUnless { it.startsWith("Upgrade") } ?: listWatering
        val sunlight = detail.sunlight
            ?.filter { !it.startsWith("Upgrade") }
            ?.joinToString(", ")
            ?.takeIf { it.isNotBlank() }
            ?: listSunlight
        showQuickFacts(
            view, cycle, watering, sunlight,
            detail.careLevel, detail.type, detail.growthRate,
            detail.indoor, detail.flowers
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
        originText.text = if (!detail.origin.isNullOrEmpty()) {
            detail.origin.joinToString(", ")
        } else {
            "N/A"
        }
        originCard.isVisible = true

        val propCard = view.findViewById<View>(R.id.propagationCard)
        val propText = view.findViewById<TextView>(R.id.propagationText)
        propText.text = if (!detail.propagation.isNullOrEmpty()) {
            detail.propagation.joinToString(", ")
        } else {
            "N/A"
        }
        propCard.isVisible = true

        if (!detail.defaultImage?.originalUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(detail.defaultImage?.originalUrl)
                .placeholder(R.drawable.ic_plant)
                .error(R.drawable.ic_plant)
                .into(view.findViewById<ImageView>(R.id.detailImage))
        }
    }

    private fun showFallbackSections(view: View) {
        val originCard = view.findViewById<View>(R.id.originCard)
        if (!originCard.isVisible) {
            view.findViewById<TextView>(R.id.originText).text = "N/A"
            originCard.isVisible = true
        }
        val propCard = view.findViewById<View>(R.id.propagationCard)
        if (!propCard.isVisible) {
            view.findViewById<TextView>(R.id.propagationText).text = "N/A"
            propCard.isVisible = true
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

        chips.add("\uD83D\uDD04 ${cycle?.takeIf { it.isNotBlank() } ?: "N/A"}" to ChipCategory.LIFECYCLE)
        chips.add("\uD83D\uDCA7 ${watering?.takeIf { it.isNotBlank() }?.let { "$it watering" } ?: "N/A"}" to ChipCategory.WATER)
        chips.add("☀\uFE0F ${sunlight?.takeIf { it.isNotBlank() } ?: "N/A"}" to ChipCategory.SUN)
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

        chipGroup.removeAllViews()
        view.findViewById<View>(R.id.factsHeading).isVisible = true
        chipGroup.isVisible = true
        for ((label, category) in chips) {
            addStyledChip(chipGroup, label, category)
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

    private fun updateQuickFactsFromTrefle(view: View, detail: TrefleSpeciesDetail) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.factsChipGroup)
        val sun = detail.growth?.light?.let { lightToLabel(it) }
        val water = detail.growth?.let {
            (it.soilHumidity ?: it.atmosphericHumidity)?.let { h -> humidityToLabel(h) }
        }
        val cycle = detail.duration?.firstOrNull()?.replaceFirstChar { it.uppercase() }

        if (sun != null || water != null || cycle != null) {
            val existingChips = (0 until chipGroup.childCount).map {
                (chipGroup.getChildAt(it) as? Chip)?.text?.toString()
            }
            val hasNACycle = existingChips.any { it?.contains("N/A") == true && it.contains("\uD83D\uDD04") }
            val hasNAWater = existingChips.any { it?.contains("N/A") == true && it.contains("\uD83D\uDCA7") }
            val hasNASun = existingChips.any { it?.contains("N/A") == true && it.contains("☀") }

            if (hasNACycle && cycle != null) replaceChipText(chipGroup, "\uD83D\uDD04", "\uD83D\uDD04 $cycle")
            if (hasNAWater && water != null) replaceChipText(chipGroup, "\uD83D\uDCA7", "\uD83D\uDCA7 $water watering")
            if (hasNASun && sun != null) replaceChipText(chipGroup, "☀", "☀\uFE0F $sun")
        }
    }

    private fun replaceChipText(chipGroup: ChipGroup, iconPrefix: String, newText: String) {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            if (chip.text?.toString()?.startsWith(iconPrefix) == true) {
                chip.text = newText
                return
            }
        }
    }

    private fun fillOriginFromTrefle(view: View, detail: TrefleSpeciesDetail) {
        val originText = view.findViewById<TextView>(R.id.originText)
        if (originText.text == "N/A" && !detail.observations.isNullOrBlank()) {
            originText.text = detail.observations
        }
        val propText = view.findViewById<TextView>(R.id.propagationText)
        if (propText.text == "N/A" && !detail.growth?.sowing.isNullOrBlank()) {
            propText.text = detail.growth?.sowing
        }
        if (propText.text == "N/A") {
            val hints = mutableListOf<String>()
            detail.specifications?.ligneousType?.let { hints.add(it.replaceFirstChar { c -> c.uppercase() }) }
            detail.specifications?.growthHabit?.let { hints.add(it) }
            if (hints.isNotEmpty()) propText.text = hints.joinToString(" \u2022 ")
        }
    }

    private fun bindTrefleData(view: View, detail: TrefleSpeciesDetail) {
        val container = view.findViewById<LinearLayout>(R.id.careContainer)
        val items = mutableListOf<Triple<String, String, String>>()

        detail.growth?.light?.let {
            items.add(Triple("\uD83D\uDD06", "Light Level", lightToLabel(it)))
        }
        (detail.growth?.soilHumidity ?: detail.growth?.atmosphericHumidity)?.let {
            items.add(Triple("\uD83D\uDCA7", "Humidity", humidityToLabel(it)))
        }
        detail.growth?.minimumTemperature?.degC?.let {
            items.add(Triple("\uD83C\uDF21\uFE0F", "Min Temperature", "${it.toInt()}\u00B0C"))
        }
        detail.specifications?.toxicity?.let { tox ->
            val label = if (tox == "none") "Non-toxic" else tox.replaceFirstChar { it.uppercase() }
            items.add(Triple("\u26A0\uFE0F", "Toxicity", label))
        }
        val phMin = detail.growth?.phMinimum
        val phMax = detail.growth?.phMaximum
        if (phMin != null && phMax != null) {
            items.add(Triple("\uD83E\uDDEA", "Soil pH", "$phMin \u2013 $phMax"))
        }
        detail.flower?.color?.takeIf { it.isNotEmpty() }?.let { colors ->
            items.add(Triple("\uD83C\uDF3B", "Flower Color",
                colors.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } }))
        }
        detail.foliage?.color?.takeIf { it.isNotEmpty() }?.let { colors ->
            items.add(Triple("\uD83C\uDF43", "Foliage",
                colors.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } }))
        }
        detail.duration?.takeIf { it.isNotEmpty() }?.let { durations ->
            items.add(Triple("\uD83D\uDD04", "Lifecycle",
                durations.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } }))
        }

        if (items.isNotEmpty()) {
            view.findViewById<View>(R.id.careInfoHeading).isVisible = true
            container.isVisible = true
            container.removeAllViews()
            for ((icon, label, value) in items) {
                addCareCard(container, icon, label, value)
            }
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
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8f) }
        }
        val iconView = TextView(container.context).apply {
            text = icon
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            layoutParams = LinearLayout.LayoutParams(dp(32f), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val textContainer = LinearLayout(container.context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        textContainer.addView(TextView(container.context).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(ContextCompat.getColor(container.context, R.color.md_theme_onSurfaceVariant))
        })
        textContainer.addView(TextView(container.context).apply {
            text = value
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(container.context, R.color.md_theme_onBackground))
        })
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

    }
}
