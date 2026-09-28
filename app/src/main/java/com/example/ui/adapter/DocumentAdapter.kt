package com.example.ui.adapter

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.data.local.entity.DocumentEntity
import com.example.databinding.ItemDocumentBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentAdapter(
    private val onViewChunks: (DocumentEntity) -> Unit,
    private val onSearch: (DocumentEntity) -> Unit,
    private val onDelete: (DocumentEntity) -> Unit
) : ListAdapter<DocumentEntity, DocumentAdapter.DocumentViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val binding = ItemDocumentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DocumentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DocumentViewHolder(private val binding: ItemDocumentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        fun bind(doc: DocumentEntity) {
            binding.tvDocTitle.text = doc.title
            binding.tvDocTypeBadge.text = doc.fileType.uppercase()

            val sizeStr = Formatter.formatShortFileSize(binding.root.context, doc.fileSize)
            val dateStr = dateFormat.format(Date(doc.createdAt))
            binding.tvDocMeta.text = "${doc.chunkCount} Chunks • ${doc.vectorDimension} dims • $sizeStr • $dateStr"

            binding.btnDocViewChunks.setOnClickListener { onViewChunks(doc) }
            binding.btnDocSearch.setOnClickListener { onSearch(doc) }
            binding.btnDocDelete.setOnClickListener { onDelete(doc) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<DocumentEntity>() {
        override fun areItemsTheSame(oldItem: DocumentEntity, newItem: DocumentEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DocumentEntity, newItem: DocumentEntity): Boolean {
            return oldItem == newItem
        }
    }
}
