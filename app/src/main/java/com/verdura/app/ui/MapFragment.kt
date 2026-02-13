package com.verdura.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.verdura.app.R
import com.verdura.app.model.Post
import com.verdura.app.viewmodel.PostViewModel
import kotlinx.coroutines.launch

class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private val viewModel: PostViewModel by activityViewModels()

    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var myLocationFab: FloatingActionButton
    private lateinit var loadingIndicator: ProgressBar

    private val postMarkers = mutableMapOf<String, Post>()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            enableMyLocation()
            moveToCurrentLocation()
        } else {
            Snackbar.make(requireView(), "Location permission required for map", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        myLocationFab = view.findViewById(R.id.myLocationFab)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        myLocationFab.setOnClickListener {
            requestLocationPermissionAndMoveToUser()
        }

        observeViewModel()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.setOnMarkerClickListener(this)

        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMapToolbarEnabled = false
        }

        requestLocationPermissionAndMoveToUser()
    }

    private fun requestLocationPermissionAndMoveToUser() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableMyLocation()
                moveToCurrentLocation()
            }
            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun enableMyLocation() {
        try {
            googleMap?.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    private fun moveToCurrentLocation() {
        loadingIndicator.visibility = View.VISIBLE

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                loadingIndicator.visibility = View.GONE
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f)
                    )
                    viewModel.loadPostsNearLocation(location.latitude, location.longitude, 10.0)
                } else {
                    // Default to a location if current location unavailable
                    val defaultLocation = LatLng(32.0853, 34.7818) // Tel Aviv
                    googleMap?.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f)
                    )
                }
            }.addOnFailureListener {
                loadingIndicator.visibility = View.GONE
            }
        } catch (e: SecurityException) {
            loadingIndicator.visibility = View.GONE
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.nearbyPosts.collect { posts ->
                    displayPostMarkers(posts)
                }
            }
        }
    }

    private fun displayPostMarkers(posts: List<Post>) {
        googleMap?.let { map ->
            // Clear existing markers
            map.clear()
            postMarkers.clear()

            // Add markers for posts with location
            posts.forEach { post ->
                if (post.latitude != null && post.longitude != null) {
                    val position = LatLng(post.latitude, post.longitude)
                    val marker = map.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title(post.text.take(30) + if (post.text.length > 30) "..." else "")
                    )
                    marker?.let {
                        postMarkers[it.id] = post
                    }
                }
            }
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val post = postMarkers[marker.id]
        if (post != null) {
            val action = MapFragmentDirections.actionMapToPostDetail(post.id)
            findNavController().navigate(action)
            return true
        }
        return false
    }
}
