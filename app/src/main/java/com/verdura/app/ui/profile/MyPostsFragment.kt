package com.verdura.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.verdura.app.databinding.FragmentMyPostsBinding
import com.verdura.app.model.Post
import com.verdura.app.ui.PostAdapter

class MyPostsFragment : Fragment() {
    private var _binding: FragmentMyPostsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PostAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadMyPosts()
    }

    private fun setupRecyclerView() {
        adapter = PostAdapter(emptyList()) { post -> onPostClicked(post) }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
    }

    private fun loadMyPosts() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        binding.progressIndicator.isVisible = true
        // Posts would be loaded from PostRepository
        binding.progressIndicator.isVisible = false
        updateEmptyState(emptyList())
    }

    private fun updateEmptyState(posts: List<Post>) {
        binding.emptyStateLayout.isVisible = posts.isEmpty()
        binding.recyclerView.isVisible = posts.isNotEmpty()
    }

    private fun onPostClicked(post: Post) {
        // Navigate to post detail
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
