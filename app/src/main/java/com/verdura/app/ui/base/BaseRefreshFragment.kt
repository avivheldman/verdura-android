package com.verdura.app.ui.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.verdura.app.util.SwipeRefreshHelper

/**
 * Base fragment that provides pull-to-refresh functionality.
 * Subclasses should override [getSwipeRefreshLayout] and [onRefreshTriggered].
 */
abstract class BaseRefreshFragment : Fragment() {

    /**
     * Return the SwipeRefreshLayout from your layout
     */
    protected abstract fun getSwipeRefreshLayout(): SwipeRefreshLayout?

    /**
     * Called when user triggers a refresh. Implement your data loading logic here.
     * Remember to call [stopRefreshing] when loading completes.
     */
    protected abstract fun onRefreshTriggered()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSwipeRefresh()
    }

    private fun setupSwipeRefresh() {
        getSwipeRefreshLayout()?.let { swipeRefresh ->
            SwipeRefreshHelper.setup(swipeRefresh) {
                onRefreshTriggered()
            }
        }
    }

    /**
     * Stop the refresh animation. Call this when data loading completes.
     */
    protected fun stopRefreshing() {
        getSwipeRefreshLayout()?.let { swipeRefresh ->
            SwipeRefreshHelper.stopRefreshing(swipeRefresh)
        }
    }

    /**
     * Start the refresh animation programmatically
     */
    protected fun startRefreshing() {
        getSwipeRefreshLayout()?.let { swipeRefresh ->
            SwipeRefreshHelper.startRefreshing(swipeRefresh)
        }
    }

    /**
     * Check if refresh is currently in progress
     */
    protected fun isRefreshing(): Boolean {
        return getSwipeRefreshLayout()?.isRefreshing == true
    }
}
