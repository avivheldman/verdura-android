package com.verdura.app.util

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

// Consistent SwipeRefreshLayout configuration across fragments
object SwipeRefreshHelper {

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

    fun stopRefreshing(swipeRefreshLayout: SwipeRefreshLayout) {
        swipeRefreshLayout.isRefreshing = false
    }

    fun startRefreshing(swipeRefreshLayout: SwipeRefreshLayout) {
        swipeRefreshLayout.isRefreshing = true
    }
}
