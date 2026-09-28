# Retrieval System Lab: Embedding-Based Document Retrieval System

A complete, production-grade Android and Python document retrieval system powered by real dense vector embeddings, persistent SQLite/Room vector storage, cosine similarity search, and information retrieval evaluation metrics.

---

## 1. Project Objective

The primary objective of this laboratory application is to demonstrate a fully functional, end-to-end information retrieval system based on dense vector representations:

$$\text{Document} \longrightarrow \text{Text Extraction} \longrightarrow \text{Sliding-Window Chunking} \longrightarrow \text{Embedding Generation} \longrightarrow \text{Vector Storage} \longrightarrow \text{Query Embedding} \longrightarrow \text{Cosine Similarity Search} \longrightarrow \text{Top-K Ranked Results}$$

Unlike traditional keyword-matching systems (e.g., lexical substring search), this application maps document chunks and queries into a shared continuous numerical vector space, retrieving semantically relevant content even when the query uses synonyms or paraphrased language.

---

## 2. Technologies

- **Mobile Client:** Android (Kotlin, Android SDK API 36, minSdk 24)
- **UI Architecture:** Native XML Layouts, Material Design 3 components, RecyclerViews, ViewBinding
- **Local Persistence:** Android Jetpack Room Database (SQLite with custom vector converters)
- **Embedding Engines:**
  1. *Dense Local Semantic Engine:* 384-dimensional dense semantic vector space projection utilizing subword n-gram hashing, inverse document frequency term weighting, contextual bigram convolutions, and $L_2$ normalization (100% offline, zero-configuration).
  2. *Google Gemini Cloud Embeddings:* 768-dimensional embeddings using Google's `text-embedding-004` model via Gemini REST API.
- **Python Engine (`python/`):**
  - `document_processor.py`: Text cleaning, tokenization, and configurable sliding-window chunking.
  - `embedding_service.py`: Real vector generation and cosine similarity calculation.
  - `vector_store.py`: Persistent vector storage, JSON serialization, and Top-K similarity search.
  - `retrieval_engine.py`: Orchestrator and Information Retrieval evaluation suite.

---

## 3. Mathematical Foundations

### Cosine Similarity Algorithm
For a query vector $\mathbf{q} \in \mathbb{R}^d$ and document chunk vector $\mathbf{d} \in \mathbb{R}^d$:

$$\text{Cosine Similarity}(\mathbf{q}, \mathbf{d}) = \frac{\mathbf{q} \cdot \mathbf{d}}{\|\mathbf{q}\|_2 \|\mathbf{d}\|_2} = \frac{\sum_{i=1}^d q_i d_i}{\sqrt{\sum_{i=1}^d q_i^2} \sqrt{\sum_{i=1}^d d_i^2}}$$

When vectors are $L_2$-normalized ($\|\mathbf{q}\|_2 = \|\mathbf{d}\|_2 = 1.0$), cosine similarity simplifies directly to the Euclidean dot product:
$$\text{Cosine Similarity}(\mathbf{q}, \mathbf{d}) = \mathbf{q} \cdot \mathbf{d}$$

### Evaluation Metrics
- **Precision@K:** Proportion of retrieved documents in Top-K that are relevant:
  $$\text{Precision@K} = \frac{|\text{Retrieved}_K \cap \text{Relevant}|}{K}$$
- **Recall@K:** Proportion of total relevant documents retrieved in Top-K:
  $$\text{Recall@K} = \frac{|\text{Retrieved}_K \cap \text{Relevant}|}{|\text{Relevant in Corpus}|}$$
- **Hit Rate@K:** Binary indicator if at least one relevant document is in Top-K:
  $$\text{Hit Rate@K} = \begin{cases} 1 & \text{if } |\text{Retrieved}_K \cap \text{Relevant}| > 0 \\ 0 & \text{otherwise} \end{cases}$$
- **Mean Reciprocal Rank (MRR):**
  $$\text{MRR} = \frac{1}{\text{rank of first relevant result}}$$

---

## 4. System Architecture

```text
app/
 └── src/main/
     ├── java/com/example/
     │   ├── MainActivity.kt               # Central navigation and UI controller
     │   ├── data/local/
     │   │   ├── AppDatabase.kt            # Room database instance
     │   │   ├── converters/               # FloatArray Room type converters
     │   │   ├── dao/                      # DocumentDao, ChunkDao, EvaluationDao
     │   │   └── entity/                   # DocumentEntity, ChunkEntity, EvaluationRecordEntity
     │   ├── document/
     │   │   ├── DocumentProcessor.kt      # Text cleaning and sliding-window chunker
     │   │   └── SampleDocuments.kt        # Pre-loaded academic benchmark papers
     │   ├── embedding/
     │   │   ├── EmbeddingService.kt       # Common service abstraction
     │   │   ├── DenseLocalEmbeddingService.kt # 384-dim deterministic local engine
     │   │   ├── GeminiEmbeddingService.kt # 768-dim cloud API engine
     │   │   └── EmbeddingManager.kt       # Provider switching & preferences
     │   ├── repository/
     │   │   ├── RetrievalModels.kt        # SearchResult, EvaluationMetrics, IndexingProgress
     │   │   └── RetrievalRepository.kt    # Data orchestration & background coroutines
     │   ├── ui/
     │   │   ├── adapter/                  # DocumentAdapter, SearchResultAdapter, ChunkDetailAdapter, EvaluationChunkAdapter
     │   │   └── viewmodel/                # RetrievalViewModel
     │   └── vector/
     │       └── VectorMath.kt             # Real vector dot product, L2 norm & cosine similarity
     └── res/
         ├── layout/                       # XML layouts: activity_main, view_home, view_documents, view_search, view_evaluation, dialogs
         └── values/                       # colors.xml, strings.xml, themes.xml

python/
 ├── document_processor.py                 # Python document processing & chunking
 ├── embedding_service.py                  # Python embedding engine (Local 384d & Gemini 768d)
 ├── vector_store.py                       # Python persistent vector storage
 ├── retrieval_engine.py                   # Python retrieval pipeline & interactive lab
 ├── test_retrieval.py                     # Python unit & integration tests
 └── requirements.txt                      # Dependencies
```

---

## 5. Getting Started

### Android Studio Setup
1. Open the project folder in **Android Studio Meerkat / Ladybug or newer**.
2. Ensure Android SDK 36 is installed (Android Gradle Plugin 9.1.1, Kotlin 2.2.10).
3. Build the project:
   ```bash
   gradle assembleDebug
   ```
4. Run unit tests:
   ```bash
   gradle testDebugUnitTest
   ```

### Python Setup
1. Ensure Python 3.8+ is installed.
2. Install optional requirements (the core implementation runs using standard library):
   ```bash
   pip install -r python/requirements.txt
   ```
3. Run the automated test suite:
   ```bash
   python3 python/test_retrieval.py
   ```
4. Run the demo CLI retrieval script:
   ```bash
   python3 python/retrieval_engine.py
   ```

---

## 6. How to Use the Application

### 1. Adding Documents
- **Option A (File Import):** Tap **Add Document** on the Dashboard or Documents screen. Tap **Select** to choose any PDF or TXT file using the Android system file picker.
- **Option B (Direct Text Input):** Enter a title and paste or type document text directly into the input fields.
- **Option C (One-Click Benchmarks):** Tap **Load Benchmark Documents** on the Home screen to instantly index 4 academic research papers covering Machine Learning, Neural Networks, Transformers, and Vector Retrieval.

### 2. Inspecting Documents & Vector Embeddings
- Open the **Documents** tab.
- Tap **View Chunks** on any document card.
- Inspect chunk text, word count, embedding dimension, and $L_2$ norm.
- Tap **Inspect** to view all raw coordinates of the numerical vector and copy them to your clipboard.

### 3. Executing Semantic Search
- Switch to the **Search** tab.
- Enter a natural-language query (e.g., *"What is machine learning?"* or *"Explain the attention mechanism"*).
- Adjust **Top-K** (1, 3, 5, 10) and the **Min Similarity** slider (0.00 to 0.80).
- Optionally filter by a specific document.
- Tap **Retrieve Relevant Chunks**. Chunks are retrieved, ranked by actual vector cosine similarity, and displayed with similarity score badges.

### 4. Running Retrieval Evaluations
- Switch to the **Evaluation** tab.
- Tap one of the preset benchmark queries or type your own.
- Tap **Run Test**.
- The retrieved chunks are displayed with interactive **Relevant / Not Relevant** toggles.
- Toggling chunks live-updates **Precision@K**, **Recall@K**, **Hit Rate@K**, and **Mean Reciprocal Rank (MRR)**.

---

## 7. API Configuration (Optional)
The application works 100% offline out-of-the-box using the built-in **Dense Local Semantic Engine (384 dimensions)**.
To use Google Gemini Cloud Embeddings (`text-embedding-004`):
1. In the app, tap the **Settings** icon on the top right.
2. Select **Google Gemini Cloud Embeddings (768 dimensions)**.
3. Enter your Gemini API key (or configure `GEMINI_API_KEY` in the AI Studio Secrets panel).
4. Tap **Save Settings**.

---

## 8. Troubleshooting
- **No Search Results:** Verify that documents have been indexed (check Document Library). If documents exist, check if the *Min Similarity* slider threshold is set too high.
- **Vector Dimension Note:** When switching between embedding providers (384-dim Local vs 768-dim Gemini), re-index your documents so chunks share the same vector dimension as query embeddings.
