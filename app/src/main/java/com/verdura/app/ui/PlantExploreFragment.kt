package com.verdura.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.verdura.app.R
import com.verdura.app.data.AppDatabase
import com.verdura.app.repository.PlantRepository
import com.verdura.app.viewmodel.PlantViewModel
import com.verdura.app.viewmodel.PlantViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlantExploreFragment : Fragment() {

    private val viewModel: PlantViewModel by viewModels {
        val dao = AppDatabase.getInstance(requireContext()).plantInfoDao()
        PlantViewModelFactory(PlantRepository(dao))
    }

    private lateinit var searchEditText: TextInputEditText
    private lateinit var plantsRecyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var loadingIndicator: CircularProgressIndicator
    private lateinit var plantAdapter: PlantAdapter
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_plant_explore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchEditText = view.findViewById(R.id.searchEditText)
        plantsRecyclerView = view.findViewById(R.id.plantsRecyclerView)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)

        setupRecyclerView()
        setupSearch()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        plantAdapter = PlantAdapter { plant ->
            val existing = childFragmentManager.findFragmentByTag(PlantDetailBottomSheet.TAG)
            if (existing is PlantDetailBottomSheet) {
                existing.dismissAllowingStateLoss()
            }
            PlantDetailBottomSheet.newInstance(plant)
                .show(childFragmentManager, PlantDetailBottomSheet.TAG)
        }
        plantsRecyclerView.adapter = plantAdapter
    }

    private fun setupSearch() {
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchJob?.cancel()
                val query = searchEditText.text?.toString()?.trim()
                viewModel.searchPlants(query ?: "")
                true
            } else false
        }

        searchEditText.doAfterTextChanged { text ->
            searchJob?.cancel()
            val query = text?.toString()?.trim() ?: ""
            if (query.isEmpty()) {
                viewModel.searchPlants("")
                return@doAfterTextChanged
            }
            if (query.length < 2) return@doAfterTextChanged
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(400)
                viewModel.searchPlants(query)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.plants.collect { plants ->
                        plantAdapter.submitList(plants)
                        emptyStateText.isVisible = plants.isEmpty() && !viewModel.uiState.value.isLoading
                        plantsRecyclerView.isVisible = plants.isNotEmpty()
                    }
                }
                launch {
                    viewModel.uiState.collect { state ->
                        loadingIndicator.isVisible = state.isLoading && plantAdapter.itemCount == 0
                        state.error?.let { error ->
                            Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }
}
