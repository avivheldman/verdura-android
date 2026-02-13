package com.verdura.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.verdura.app.R
import com.verdura.app.util.SwipeRefreshHelper
import com.verdura.app.viewmodel.PostViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var createPostFab: FloatingActionButton

    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        postsRecyclerView = view.findViewById(R.id.postsRecyclerView)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        createPostFab = view.findViewById(R.id.createPostFab)

        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            onPostClick = { post ->
                val action = HomeFragmentDirections.actionHomeToPostDetail(post.id)
                findNavController().navigate(action)
            },
            onPostLongClick = { post ->
                showPostOptions(post.id)
                true
            }
        )
        postsRecyclerView.adapter = postAdapter
    }

    private fun setupSwipeRefresh() {
        SwipeRefreshHelper.setup(swipeRefreshLayout) {
            viewModel.syncPosts()
        }
    }

    private fun setupFab() {
        createPostFab.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_createPost)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.posts.collect { posts ->
                        postAdapter.submitList(posts)
                        emptyStateText.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        SwipeRefreshHelper.stopRefreshing(swipeRefreshLayout)
                        loadingIndicator.visibility = if (state.isLoading && postAdapter.itemCount == 0) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                        state.error?.let { error ->
                            Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun showPostOptions(postId: String) {
        val action = HomeFragmentDirections.actionHomeToPostDetail(postId)
        findNavController().navigate(action)
    }
}
