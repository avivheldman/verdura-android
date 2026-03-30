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

        showQuickFacts(view, listCycle, listWatering, listSunlight)

        val descView = view.findViewById<TextView>(R.id.detailDescription)
        descView.text = "Loading plant details\u2026"
        descView.isVisible = true

        content.isVisible = true
        loading.isVisible = true

        val db = AppDatabase.getInstance(requireContext())
        val repository = PlantRepository(db.plantInfoDao(), db.trefleDetailCacheDao())

        lifecycleScope.launch {
            val result = repository.fetchPlantDetails(plantId, scientificName)
            ensureActive()
            if (!isAdded) return@launch

            result.fold(
                onSuccess = { detail -> bindTrefleDetail(view, detail) },
                onFailure = {
                    if (descView.text == "Loading plant details\u2026") {
                        descView.text = "No description available"
                    }
                    showFallbackSections(view)
                }
            )

            loading.isVisible = false
        }
    }

    private fun bindTrefleDetail(view: View, detail: TrefleSpeciesDetail) {
        val descView = view.findViewById<TextView>(R.id.detailDescription)
        descView.text = if (!detail.growth?.description.isNullOrBlank()) {
            detail.growth?.description
        } else {
            "No description available"
        }
        descView.isVisible = true

        val cycle = detail.duration?.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: listCycle
        val watering = detail.growth?.let {
            (it.soilHumidity ?: it.atmosphericHumidity)?.let { h -> PlantRepository.humidityToLabel(h) }
        } ?: listWatering
        val sunlight = detail.growth?.light?.let { PlantRepository.lightToLabel(it) } ?: listSunlight
        showQuickFacts(view, cycle, watering, sunlight)

        if (detail.specifications?.toxicity != null) {
            val tox = detail.specifications.toxicity
            val label = if (tox == "none") "Non-toxic" else tox.replaceFirstChar { it.uppercase() }
            addStyledChip(view.findViewById(R.id.factsChipGroup), "\u26A0\uFE0F $label", ChipCategory.CARE)
        }
        detail.specifications?.growthRate?.takeIf { it.isNotBlank() }?.let {
            addStyledChip(view.findViewById(R.id.factsChipGroup), "\uD83D\uDCC8 $it growth", ChipCategory.GROWTH)
        }
        detail.flower?.color?.takeIf { it.isNotEmpty() }?.let {
            addStyledChip(
                view.findViewById(R.id.factsChipGroup),
                "\uD83C\uDF3A ${it.joinToString(", ") { c -> c.replaceFirstChar { ch -> ch.uppercase() } }}",
                ChipCategory.FLOWER
            )
        }

        val originCard = view.findViewById<View>(R.id.originCard)
        val originText = view.findViewById<TextView>(R.id.originText)
        val originStr = buildOriginString(detail)
        originText.text = originStr.ifBlank { "N/A" }
        originCard.isVisible = true

        val propCard = view.findViewById<View>(R.id.propagationCard)
        val propText = view.findViewById<TextView>(R.id.propagationText)
        propText.text = buildPropagationString(detail).ifBlank { "N/A" }
        propCard.isVisible = true

        loadBestImage(view, detail)

        bindCareDetails(view, detail)
    }

    private fun buildOriginString(detail: TrefleSpeciesDetail): String {
        if (!detail.observations.isNullOrBlank()) return detail.observations

        val nativeZones = detail.distributions?.native
            ?.mapNotNull { it.name }
            ?.takeIf { it.isNotEmpty() }
            ?: return ""

        if (nativeZones.size <= 5) return nativeZones.joinToString(", ")

        val shown = nativeZones.take(3).joinToString(", ")
        return "$shown and ${nativeZones.size - 3} more regions"
    }

    private fun buildPropagationString(detail: TrefleSpeciesDetail): String {
        if (!detail.growth?.sowing.isNullOrBlank()) return detail.growth!!.sowing!!
        val hints = mutableListOf<String>()
        detail.specifications?.ligneousType?.let { hints.add(it.replaceFirstChar { c -> c.uppercase() }) }
        detail.specifications?.growthHabit?.let { hints.add(it) }
        return hints.joinToString(" \u2022 ")
    }

    private fun loadBestImage(view: View, detail: TrefleSpeciesDetail) {
        val bestUrl = detail.images?.let { imgs ->
            imgs.habit?.firstOrNull()?.imageUrl
                ?: imgs.leaf?.firstOrNull()?.imageUrl
                ?: imgs.flower?.firstOrNull()?.imageUrl
                ?: imgs.other?.firstOrNull()?.imageUrl
                ?: imgs.fruit?.firstOrNull()?.imageUrl
                ?: imgs.bark?.firstOrNull()?.imageUrl
        } ?: detail.imageUrl

        if (!bestUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(bestUrl)
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
        sunlight: String?
    ) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.factsChipGroup)
        val chips = mutableListOf<Pair<String, ChipCategory>>()

        chips.add("\uD83D\uDD04 ${cycle?.takeIf { it.isNotBlank() } ?: "N/A"}" to ChipCategory.LIFECYCLE)
        chips.add("\uD83D\uDCA7 ${watering?.takeIf { it.isNotBlank() }?.let { "$it watering" } ?: "N/A"}" to ChipCategory.WATER)
        chips.add("☀\uFE0F ${sunlight?.takeIf { it.isNotBlank() } ?: "N/A"}" to ChipCategory.SUN)

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

    private fun bindCareDetails(view: View, detail: TrefleSpeciesDetail) {
        val container = view.findViewById<LinearLayout>(R.id.careContainer)
        val items = mutableListOf<Triple<String, String, String>>()

        detail.growth?.light?.let {
            items.add(Triple("\uD83D\uDD06", "Light Level", PlantRepository.lightToLabel(it)))
        }
        (detail.growth?.soilHumidity ?: detail.growth?.atmosphericHumidity)?.let {
            items.add(Triple("\uD83D\uDCA7", "Humidity", PlantRepository.humidityToLabel(it)))
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
