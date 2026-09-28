package com.example.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.databinding.ItemSearchResultBinding
import com.example.repository.SearchResult
import java.util.Locale

class SearchResultAdapter :
    ListAdapter<SearchResult, SearchResultAdapter.SearchResultViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val binding =
            ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SearchResultViewHolder(private val binding: ItemSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(result: SearchResult) {
            binding.tvResultRank.text = "#${result.rank}"
            binding.tvResultDocTitle.text = result.documentTitle
            binding.tvResultChunkInfo.text = "Chunk #${result.chunkIndex + 1} • Page ${result.pageNumber} • ${result.wordCount} words • ${result.vectorDimension}d"
            binding.tvResultContent.text = result.text

            val scoreStr = String.format(Locale.US, "%.4f", result.similarity)
            binding.tvResultSimilarity.text = scoreStr

            // Color coding based on real vector similarity
            when {
                result.similarity >= 0.70f -> {
                    binding.tvResultSimilarity.setTextColor(Color.parseColor("#065F46"))
                }
                result.similarity >= 0.40f -> {
                    binding.tvResultSimilarity.setTextColor(Color.parseColor("#B45309"))
                }
                else -> {
                    binding.tvResultSimilarity.setTextColor(Color.parseColor("#1E3A8A"))
                }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<SearchResult>() {
        override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return oldItem.chunkId == newItem.chunkId
        }

        override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return oldItem == newItem
        }
    }
}
