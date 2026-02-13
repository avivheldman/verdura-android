package com.verdura.app.util

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * Helper class for setting up SwipeRefreshLayout consistently across fragments
 */
object SwipeRefreshHelper {

    /**
     * Configure SwipeRefreshLayout with app theme colors and refresh action
     */
    fun setup(
        swipeRefreshLayout: SwipeRefreshLayout,
        onRefresh: () -> Unit
    ) {
        swipeRefreshLayout.apply {
            setColorSchemeResources(
                android.R.color.holo_green_dark,
                android.R.color.holo_green_light,
                android.R.color.holo_blue_dark
            )
            setOnRefreshListener {
                onRefresh()
            }
        }
    }

    /**
     * Stop the refresh animation
     */
    fun stopRefreshing(swipeRefreshLayout: SwipeRefreshLayout) {
        swipeRefreshLayout.isRefreshing = false
    }

    /**
     * Start the refresh animation programmatically
     */
    fun startRefreshing(swipeRefreshLayout: SwipeRefreshLayout) {
        swipeRefreshLayout.isRefreshing = true
    }
}
