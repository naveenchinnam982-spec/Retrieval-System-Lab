package com.example.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.data.local.entity.ChunkEntity
import com.example.databinding.ItemChunkDetailBinding
import java.util.Locale

class ChunkDetailAdapter(
    private val onInspectVector: (ChunkEntity) -> Unit
) : ListAdapter<ChunkEntity, ChunkDetailAdapter.ChunkDetailViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChunkDetailViewHolder {
        val binding =
            ItemChunkDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChunkDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChunkDetailViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChunkDetailViewHolder(private val binding: ItemChunkDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chunk: ChunkEntity) {
            binding.tvChunkTitle.text = "Chunk #${chunk.chunkIndex + 1}"
            binding.tvChunkBadge.text = "${chunk.wordCount} words • Page ${chunk.pageNumber}"
            binding.tvChunkText.text = chunk.text

            val normStr = String.format(Locale.US, "%.3f", chunk.l2Norm)
            binding.tvChunkVectorInfo.text = "Embedding Dimension: ${chunk.embedding.size} • ||v|| = $normStr"

            // Preview first 5 coordinates
            val previewCoords = chunk.embedding.take(5).joinToString(", ") {
                String.format(Locale.US, "%.4f", it)
            }
            binding.tvChunkVectorPreview.text = "[$previewCoords, ...]"

            binding.btnInspectVector.setOnClickListener {
                onInspectVector(chunk)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ChunkEntity>() {
        override fun areItemsTheSame(oldItem: ChunkEntity, newItem: ChunkEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChunkEntity, newItem: ChunkEntity): Boolean {
            return oldItem == newItem
        }
    }
}
