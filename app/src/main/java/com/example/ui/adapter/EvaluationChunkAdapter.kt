package com.example.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.databinding.ItemEvaluationChunkBinding
import com.example.repository.SearchResult
import java.util.Locale

class EvaluationChunkAdapter(
    private val onRelevanceToggled: (chunkId: Long, isRelevant: Boolean) -> Unit
) : ListAdapter<SearchResult, EvaluationChunkAdapter.EvaluationViewHolder>(DiffCallback) {

    private val relevanceMap = mutableMapOf<Long, Boolean>()

    fun setInitialRelevance(chunkIds: Set<Long>) {
        relevanceMap.clear()
        for (id in chunkIds) {
            relevanceMap[id] = true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EvaluationViewHolder {
        val binding =
            ItemEvaluationChunkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EvaluationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EvaluationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EvaluationViewHolder(private val binding: ItemEvaluationChunkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(result: SearchResult) {
            binding.tvEvalItemRank.text = "#${result.rank}"
            binding.tvEvalItemDoc.text = "${result.documentTitle} (Chunk #${result.chunkIndex + 1})"

            val simStr = String.format(Locale.US, "%.4f", result.similarity)
            binding.tvEvalItemSim.text = "Cosine Similarity: $simStr"
            binding.tvEvalItemText.text = result.text

            val isCurrentlyRelevant = relevanceMap[result.chunkId] ?: true
            updateChipState(isCurrentlyRelevant)

            binding.chipToggleRelevance.setOnClickListener {
                val newState = !isCurrentlyRelevant
                relevanceMap[result.chunkId] = newState
                updateChipState(newState)
                onRelevanceToggled(result.chunkId, newState)
            }
        }

        private fun updateChipState(isRelevant: Boolean) {
            if (isRelevant) {
                binding.chipToggleRelevance.isChecked = true
                binding.chipToggleRelevance.text = "Relevant"
                binding.chipToggleRelevance.setTextColor(Color.parseColor("#065F46"))
            } else {
                binding.chipToggleRelevance.isChecked = false
                binding.chipToggleRelevance.text = "Not Relevant"
                binding.chipToggleRelevance.setTextColor(Color.parseColor("#991B1B"))
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
