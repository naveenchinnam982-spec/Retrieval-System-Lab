# Python Retrieval Component - Retrieval System Lab

This directory contains the Python implementation of the retrieval pipeline matching the Android architecture.

## Modules

1. **`document_processor.py`**:
   - Cleans document text (removes non-printable characters, normalizes line breaks).
   - Splits text into overlapping sliding-window chunks with configurable `chunk_size` and `chunk_overlap`.
   - Generates chunk metadata (document ID, chunk ID, word counts, page references).

2. **`embedding_service.py`**:
   - `BaseEmbeddingService`: Abstract interface for embedding models.
   - `DenseSemanticEmbeddingService`: 384-dimensional dense semantic embedding engine using subword hashing, term weighting, and L2 normalization (pure standard Python + numpy compatible).
   - `GeminiEmbeddingService`: Cloud embedding via Google Gemini `text-embedding-004` (768 dimensions).
   - `cosine_similarity(vec_a, vec_b)`: Computes exact cosine similarity.

3. **`vector_store.py`**:
   - Indexes documents and chunk vectors with persistent disk serialization (JSON).
   - Cosine similarity search with Top-K ranking and threshold filtering.
   - Cascading document and embedding deletion.

4. **`retrieval_engine.py`**:
   - Orchestrates the full pipeline: Document -> Chunks -> Embeddings -> Storage -> Query -> Top-K Ranking.
   - Computes retrieval metrics: Precision@K, Recall@K, HitRate@K, Mean Reciprocal Rank (MRR).

5. **`test_retrieval.py`**:
   - Comprehensive test suite for verification.

## Running Tests
```bash
python3 -m unittest test_retrieval.py
```

## Running Demo
```bash
python3 retrieval_engine.py
```
