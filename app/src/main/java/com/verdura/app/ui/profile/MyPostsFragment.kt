package com.verdura.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.verdura.app.R
import com.verdura.app.databinding.FragmentMyPostsBinding
import com.verdura.app.ui.PostAdapter
import com.verdura.app.viewmodel.PostViewModel
import kotlinx.coroutines.launch

class MyPostsFragment : Fragment() {
    private var _binding: FragmentMyPostsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PostViewModel by activityViewModels()
    private lateinit var adapter: PostAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        loadMyPosts()
    }

    private fun setupRecyclerView() {
        adapter = PostAdapter(
            onPostClick = { post ->
                val action = MyPostsFragmentDirections.actionMyPostsToPostDetail(post.id)
                findNavController().navigate(action)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.posts.collect { posts ->
                        adapter.submitList(posts)
                        binding.emptyStateLayout.isVisible = posts.isEmpty()
                        binding.recyclerView.isVisible = posts.isNotEmpty()
                    }
                }
                launch {
                    viewModel.uiState.collect { state ->
                        binding.progressIndicator.isVisible = state.isLoading && adapter.itemCount == 0
                    }
                }
            }
        }
    }

    private fun loadMyPosts() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModel.loadPostsByUser(userId)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
