package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.data.local.entity.ChunkEntity
import com.example.data.local.entity.DocumentEntity
import com.example.databinding.ActivityMainBinding
import com.example.databinding.DialogAddDocumentBinding
import com.example.databinding.DialogDocumentDetailsBinding
import com.example.databinding.DialogSettingsBinding
import com.example.databinding.DialogVectorInspectBinding
import com.example.embedding.EmbeddingManager
import com.example.repository.SearchResult
import com.example.ui.adapter.ChunkDetailAdapter
import com.example.ui.adapter.DocumentAdapter
import com.example.ui.adapter.EvaluationChunkAdapter
import com.example.ui.adapter.SearchResultAdapter
import com.example.ui.viewmodel.RetrievalViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: RetrievalViewModel by viewModels()

    // Adapters
    private lateinit var documentAdapter: DocumentAdapter
    private lateinit var searchResultAdapter: SearchResultAdapter
    private lateinit var evaluationChunkAdapter: EvaluationChunkAdapter

    // File Picker State
    private var pendingAddDialogBinding: DialogAddDocumentBinding? = null
    private var selectedDocumentUri: Uri? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedDocumentUri = uri
            val fileName = uri.lastPathSegment ?: "Selected Document"
            pendingAddDialogBinding?.let { dBinding ->
                dBinding.tvPickedFileName.text = fileName
                dBinding.tvPickedFileMeta.text = "Ready to index (PDF/TXT)"
                if (dBinding.etDocTitle.text.isNullOrBlank()) {
                    dBinding.etDocTitle.setText(fileName)
                }
            }
        }
    }

    // Evaluation State
    private var currentEvalResults = listOf<SearchResult>()
    private val currentRelevantChunkIds = mutableSetOf<Long>()
    private var currentEvalQuery = ""
    private var currentEvalK = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupAdapters()
        setupHomeScreen()
        setupDocumentsScreen()
        setupSearchScreen()
        setupEvaluationScreen()
        setupSettings()
        observeViewModel()
        setupBackNavigation()
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.bottomNavigation.selectedItemId != R.id.nav_home) {
                    binding.bottomNavigation.selectedItemId = R.id.nav_home
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            hideAllScreens()
            when (item.itemId) {
                R.id.nav_home -> {
                    binding.layoutHome.root.visibility = View.VISIBLE
                    true
                }
                R.id.nav_documents -> {
                    binding.layoutDocuments.root.visibility = View.VISIBLE
                    true
                }
                R.id.nav_search -> {
                    binding.layoutSearch.root.visibility = View.VISIBLE
                    true
                }
                R.id.nav_evaluation -> {
                    binding.layoutEvaluation.root.visibility = View.VISIBLE
                    true
                }
                else -> false
            }
        }
    }

    private fun hideAllScreens() {
        binding.layoutHome.root.visibility = View.GONE
        binding.layoutDocuments.root.visibility = View.GONE
        binding.layoutSearch.root.visibility = View.GONE
        binding.layoutEvaluation.root.visibility = View.GONE
    }

    private fun setupAdapters() {
        documentAdapter = DocumentAdapter(
            onViewChunks = { doc -> showDocumentDetailsDialog(doc) },
            onSearch = { doc ->
                binding.bottomNavigation.selectedItemId = R.id.nav_search
                binding.layoutSearch.etSearchQuery.setText("")
                binding.layoutSearch.etSearchQuery.requestFocus()
            },
            onDelete = { doc ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Document?")
                    .setMessage("Are you sure you want to delete '${doc.title}'? All ${doc.chunkCount} vector embeddings will also be permanently deleted.")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteDocument(doc.id)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        searchResultAdapter = SearchResultAdapter()

        evaluationChunkAdapter = EvaluationChunkAdapter { chunkId, isRelevant ->
            if (isRelevant) {
                currentRelevantChunkIds.add(chunkId)
            } else {
                currentRelevantChunkIds.remove(chunkId)
            }
            viewModel.recalculateLiveMetrics(
                query = currentEvalQuery,
                topK = currentEvalK,
                relevantChunkIds = currentRelevantChunkIds,
                currentResults = currentEvalResults
            )
        }

        binding.layoutDocuments.rvDocuments.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = documentAdapter
        }

        binding.layoutSearch.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = searchResultAdapter
        }

        binding.layoutEvaluation.rvEvalResults.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = evaluationChunkAdapter
        }
    }

    private fun setupHomeScreen() {
        binding.layoutHome.btnHomeAddDocument.setOnClickListener {
            showAddDocumentDialog()
        }
        binding.layoutHome.btnHomeDocuments.setOnClickListener {
            binding.bottomNavigation.selectedItemId = R.id.nav_documents
        }
        binding.layoutHome.btnHomeSearch.setOnClickListener {
            binding.bottomNavigation.selectedItemId = R.id.nav_search
        }
        binding.layoutHome.btnHomeTestRetrieval.setOnClickListener {
            binding.bottomNavigation.selectedItemId = R.id.nav_evaluation
        }
        binding.layoutHome.btnHomeLoadBenchmarks.setOnClickListener {
            viewModel.loadBenchmarkPapers()
        }

        // Preset Chips on Home Screen
        binding.layoutHome.chipQ1.setOnClickListener { executeQueryFromChip(binding.layoutHome.chipQ1.text.toString()) }
        binding.layoutHome.chipQ2.setOnClickListener { executeQueryFromChip(binding.layoutHome.chipQ2.text.toString()) }
        binding.layoutHome.chipQ3.setOnClickListener { executeQueryFromChip(binding.layoutHome.chipQ3.text.toString()) }
        binding.layoutHome.chipQ4.setOnClickListener { executeQueryFromChip(binding.layoutHome.chipQ4.text.toString()) }
    }

    private fun executeQueryFromChip(query: String) {
        binding.bottomNavigation.selectedItemId = R.id.nav_search
        binding.layoutSearch.etSearchQuery.setText(query)
        performSearch()
    }

    private fun setupDocumentsScreen() {
        binding.layoutDocuments.btnDocAdd.setOnClickListener {
            showAddDocumentDialog()
        }
        binding.layoutDocuments.btnEmptyLoadBenchmarks.setOnClickListener {
            viewModel.loadBenchmarkPapers()
        }
    }

    private fun setupSearchScreen() {
        binding.layoutSearch.btnRunSearch.setOnClickListener {
            performSearch()
        }

        binding.layoutSearch.etSearchQuery.setOnEditorActionListener { _, _, _ ->
            performSearch()
            true
        }

        binding.layoutSearch.sliderThreshold.addOnChangeListener { _, value, _ ->
            binding.layoutSearch.tvThresholdLabel.text =
                String.format(Locale.US, "Min Similarity: %.2f", value)
        }
    }

    private fun performSearch() {
        val query = binding.layoutSearch.etSearchQuery.text?.toString().orEmpty()
        if (query.isBlank()) {
            Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show()
            return
        }

        var topK = 3
        when (binding.layoutSearch.chipGroupTopK.checkedChipId) {
            R.id.chip_k1 -> topK = 1
            R.id.chip_k3 -> topK = 3
            R.id.chip_k5 -> topK = 5
            R.id.chip_k10 -> topK = 10
        }

        val threshold = binding.layoutSearch.sliderThreshold.value

        // Check document filter
        val selectedDocPosition = binding.layoutSearch.spinnerDocFilter.selectedItemPosition
        val filterDocId: Long? = if (selectedDocPosition > 0) {
            val docs = viewModel.documents.value
            if (selectedDocPosition - 1 in docs.indices) docs[selectedDocPosition - 1].id else null
        } else null

        viewModel.search(
            query = query,
            topK = topK,
            threshold = threshold,
            filterDocId = filterDocId
        )
    }

    private fun setupEvaluationScreen() {
        // Preset queries
        binding.layoutEvaluation.chipEval1.setOnClickListener {
            binding.layoutEvaluation.etEvalQuery.setText(binding.layoutEvaluation.chipEval1.text.toString())
            runEvaluation()
        }
        binding.layoutEvaluation.chipEval2.setOnClickListener {
            binding.layoutEvaluation.etEvalQuery.setText(binding.layoutEvaluation.chipEval2.text.toString())
            runEvaluation()
        }
        binding.layoutEvaluation.chipEval3.setOnClickListener {
            binding.layoutEvaluation.etEvalQuery.setText(binding.layoutEvaluation.chipEval3.text.toString())
            runEvaluation()
        }
        binding.layoutEvaluation.chipEval4.setOnClickListener {
            binding.layoutEvaluation.etEvalQuery.setText(binding.layoutEvaluation.chipEval4.text.toString())
            runEvaluation()
        }

        binding.layoutEvaluation.btnRunEvaluation.setOnClickListener {
            runEvaluation()
        }
    }

    private fun runEvaluation() {
        val query = binding.layoutEvaluation.etEvalQuery.text?.toString().orEmpty()
        if (query.isBlank()) {
            Toast.makeText(this, "Please enter an evaluation query", Toast.LENGTH_SHORT).show()
            return
        }

        var topK = 3
        when (binding.layoutEvaluation.chipGroupEvalK.checkedChipId) {
            R.id.chip_eval_k3 -> topK = 3
            R.id.chip_eval_k5 -> topK = 5
            R.id.chip_eval_k10 -> topK = 10
        }

        currentEvalQuery = query
        currentEvalK = topK

        lifecycleScope.launch {
            binding.layoutEvaluation.progressEvaluation.visibility = View.VISIBLE
            binding.layoutEvaluation.tvEvalEmpty.visibility = View.GONE

            val repository = com.example.repository.RetrievalRepository(this@MainActivity)
            val results = repository.search(query = query, topK = topK, threshold = -1.0f)

            currentEvalResults = results
            currentRelevantChunkIds.clear()
            // By default mark all retrieved as relevant initially so user can toggle
            for (r in results) {
                currentRelevantChunkIds.add(r.chunkId)
            }

            evaluationChunkAdapter.setInitialRelevance(currentRelevantChunkIds)
            evaluationChunkAdapter.submitList(results)

            binding.layoutEvaluation.progressEvaluation.visibility = View.GONE

            if (results.isEmpty()) {
                binding.layoutEvaluation.cardEvalMetrics.visibility = View.GONE
                binding.layoutEvaluation.tvEvalResultsHeader.visibility = View.GONE
                binding.layoutEvaluation.tvEvalEmpty.visibility = View.VISIBLE
                binding.layoutEvaluation.tvEvalEmpty.text = "No indexed documents found in database. Please index documents first."
            } else {
                binding.layoutEvaluation.cardEvalMetrics.visibility = View.VISIBLE
                binding.layoutEvaluation.tvEvalResultsHeader.visibility = View.VISIBLE
                binding.layoutEvaluation.tvEvalEmpty.visibility = View.GONE

                viewModel.recalculateLiveMetrics(
                    query = query,
                    topK = topK,
                    relevantChunkIds = currentRelevantChunkIds,
                    currentResults = results
                )
            }
        }
    }

    private fun setupSettings() {
        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.documents.collectLatest { docs ->
                        documentAdapter.submitList(docs)
                        binding.layoutHome.tvHomeDocCount.text = docs.size.toString()
                        binding.layoutDocuments.tvDocCountSummary.text = "${docs.size} documents indexed"

                        if (docs.isEmpty()) {
                            binding.layoutDocuments.layoutEmptyDocuments.visibility = View.VISIBLE
                            binding.layoutDocuments.rvDocuments.visibility = View.GONE
                        } else {
                            binding.layoutDocuments.layoutEmptyDocuments.visibility = View.GONE
                            binding.layoutDocuments.rvDocuments.visibility = View.VISIBLE
                        }

                        // Update Search document filter spinner
                        val spinnerItems = mutableListOf("All Documents (${docs.size})")
                        for (d in docs) {
                            spinnerItems.add(d.title)
                        }
                        val spinnerAdapter = ArrayAdapter(
                            this@MainActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            spinnerItems
                        )
                        binding.layoutSearch.spinnerDocFilter.adapter = spinnerAdapter
                    }
                }

                launch {
                    viewModel.chunkCount.collectLatest { count ->
                        binding.layoutHome.tvHomeChunkCount.text = count.toString()
                    }
                }

                launch {
                    viewModel.activeEngineName.collectLatest { name ->
                        binding.chipEngineStatus.text = if (name.contains("384")) "384d Local" else "768d Gemini"
                        binding.layoutHome.tvHomeEngineInfo.text = name
                    }
                }

                launch {
                    viewModel.searchResults.collectLatest { results ->
                        searchResultAdapter.submitList(results)
                        if (results.isEmpty()) {
                            binding.layoutSearch.tvSearchEmpty.visibility = View.VISIBLE
                            binding.layoutSearch.tvSearchStats.visibility = View.GONE
                        } else {
                            binding.layoutSearch.tvSearchEmpty.visibility = View.GONE
                            binding.layoutSearch.tvSearchStats.visibility = View.VISIBLE
                            binding.layoutSearch.tvSearchStats.text =
                                "Retrieved ${results.size} chunks sorted by cosine similarity"
                        }
                    }
                }

                launch {
                    viewModel.isSearching.collectLatest { isSearching ->
                        binding.layoutSearch.progressSearch.visibility =
                            if (isSearching) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.evaluationMetrics.collectLatest { metrics ->
                        metrics?.let {
                            binding.layoutEvaluation.tvMetricPrecision.text =
                                String.format(Locale.US, "%.2f", it.precisionAtK)
                            binding.layoutEvaluation.tvLabelPrecision.text = "Precision@${it.topK}"

                            binding.layoutEvaluation.tvMetricRecall.text =
                                String.format(Locale.US, "%.2f", it.recallAtK)
                            binding.layoutEvaluation.tvLabelRecall.text = "Recall@${it.topK}"

                            binding.layoutEvaluation.tvMetricHitrate.text =
                                String.format(Locale.US, "%.2f", it.hitRateAtK)
                            binding.layoutEvaluation.tvLabelHitrate.text = "Hit Rate@${it.topK}"

                            binding.layoutEvaluation.tvMetricMrr.text =
                                String.format(Locale.US, "%.2f", it.mrr)
                        }
                    }
                }

                launch {
                    viewModel.messages.collectLatest { msg ->
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showAddDocumentDialog() {
        val dBinding = DialogAddDocumentBinding.inflate(LayoutInflater.from(this))
        pendingAddDialogBinding = dBinding
        selectedDocumentUri = null

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dBinding.root)
            .create()

        dBinding.btnPickFile.setOnClickListener {
            filePickerLauncher.launch(arrayOf("application/pdf", "text/plain", "*/*"))
        }

        dBinding.btnCancelAdd.setOnClickListener {
            dialog.dismiss()
        }

        dBinding.btnConfirmIndex.setOnClickListener {
            val chunkSize = dBinding.etChunkSize.text.toString().toIntOrNull() ?: 100
            val overlap = dBinding.etChunkOverlap.text.toString().toIntOrNull() ?: 20

            dBinding.cardIndexingProgress.visibility = View.VISIBLE
            dBinding.btnConfirmIndex.isEnabled = false

            lifecycleScope.launch {
                viewModel.indexingProgress.collectLatest { progress ->
                    progress?.let {
                        dBinding.tvProgressStage.text = it.stage
                        if (it.isCompleted) {
                            Toast.makeText(this@MainActivity, "Document indexed successfully", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        } else if (it.error != null) {
                            Toast.makeText(this@MainActivity, "Error: ${it.error}", Toast.LENGTH_LONG).show()
                            dBinding.btnConfirmIndex.isEnabled = true
                        }
                    }
                }
            }

            if (selectedDocumentUri != null) {
                viewModel.indexFileUri(selectedDocumentUri!!, chunkSize, overlap)
            } else {
                val title = dBinding.etDocTitle.text?.toString().orEmpty().ifBlank { "Text_Document.txt" }
                val content = dBinding.etDocContent.text?.toString().orEmpty()
                if (content.isBlank()) {
                    Toast.makeText(this, "Please select a file or enter document content", Toast.LENGTH_SHORT).show()
                    dBinding.cardIndexingProgress.visibility = View.GONE
                    dBinding.btnConfirmIndex.isEnabled = true
                    return@setOnClickListener
                }
                viewModel.indexRawText(title, content, chunkSize, overlap)
            }
        }

        dialog.show()
    }

    private fun showDocumentDetailsDialog(doc: DocumentEntity) {
        val dBinding = DialogDocumentDetailsBinding.inflate(LayoutInflater.from(this))
        dBinding.tvDetailsTitle.text = doc.title
        dBinding.tvDetailsMeta.text = "${doc.fileType} • ${android.text.format.Formatter.formatShortFileSize(this, doc.fileSize)}"
        dBinding.tvDetailsProvider.text = "Provider: ${doc.embeddingProvider}"
        dBinding.tvDetailsChunksSummary.text = "Total Chunks: ${doc.chunkCount} • ${doc.vectorDimension} dimensions"

        val chunkAdapter = ChunkDetailAdapter { chunk ->
            showVectorInspectDialog(chunk)
        }

        dBinding.rvDetailsChunks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = chunkAdapter
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dBinding.root)
            .create()

        dBinding.btnCloseDetails.setOnClickListener {
            dialog.dismiss()
        }

        lifecycleScope.launch {
            val chunks = viewModel.getChunksForDocument(doc.id)
            chunkAdapter.submitList(chunks)
        }

        dialog.show()
    }

    private fun showVectorInspectDialog(chunk: ChunkEntity) {
        val dBinding = DialogVectorInspectBinding.inflate(LayoutInflater.from(this))
        dBinding.tvInspectTitle.text = "Chunk #${chunk.chunkIndex + 1} Vector Inspection"
        dBinding.tvInspectSubtitle.text = "Dimension: ${chunk.embedding.size} • ||v|| = ${String.format(Locale.US, "%.4f", chunk.l2Norm)}"

        val formattedVector = StringBuilder("[")
        for (i in chunk.embedding.indices) {
            formattedVector.append(String.format(Locale.US, "%.4f", chunk.embedding[i]))
            if (i < chunk.embedding.size - 1) {
                formattedVector.append(", ")
                if ((i + 1) % 6 == 0) formattedVector.append("\n ")
            }
        }
        formattedVector.append("]")

        val vectorStr = formattedVector.toString()
        dBinding.tvInspectVectorContent.text = vectorStr

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dBinding.root)
            .create()

        dBinding.btnInspectClose.setOnClickListener {
            dialog.dismiss()
        }

        dBinding.btnInspectCopy.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Embedding Vector", vectorStr)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Vector coordinates copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun showSettingsDialog() {
        val dBinding = DialogSettingsBinding.inflate(LayoutInflater.from(this))
        val manager = viewModel.embeddingManager

        if (manager.isGeminiActive()) {
            dBinding.rbProviderGemini.isChecked = true
        } else {
            dBinding.rbProviderLocal.isChecked = true
        }

        dBinding.etSettingsApiKey.setText(manager.getApiKey())
        dBinding.etSettingsChunkSize.setText(manager.getDefaultChunkSize().toString())
        dBinding.etSettingsChunkOverlap.setText(manager.getDefaultOverlap().toString())

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dBinding.root)
            .create()

        dBinding.btnSettingsCancel.setOnClickListener {
            dialog.dismiss()
        }

        dBinding.btnSettingsClearAll.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Clear All Data?")
                .setMessage("This will delete all indexed documents, all chunk embeddings, and evaluation records.")
                .setPositiveButton("Clear Everything") { _, _ ->
                    viewModel.clearAllData()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dBinding.btnSettingsSave.setOnClickListener {
            val newProvider = if (dBinding.rbProviderGemini.isChecked) {
                EmbeddingManager.PROVIDER_GEMINI
            } else {
                EmbeddingManager.PROVIDER_LOCAL
            }

            manager.setProvider(newProvider)
            manager.setApiKey(dBinding.etSettingsApiKey.text?.toString().orEmpty())

            val chunkSize = dBinding.etSettingsChunkSize.text.toString().toIntOrNull() ?: 100
            val overlap = dBinding.etSettingsChunkOverlap.text.toString().toIntOrNull() ?: 20
            manager.setDefaultChunkSize(chunkSize)
            manager.setDefaultOverlap(overlap)

            viewModel.refreshEngineStatus()
            Toast.makeText(this, "Settings updated successfully", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}
