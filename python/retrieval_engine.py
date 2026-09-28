"""
Retrieval Engine coordinating Document Processing, Embedding Generation,
Vector Storage, Similarity Retrieval, and Retrieval Evaluation.
Part of the Retrieval System Lab.
"""

import os
import sys
from typing import List, Dict, Any, Optional

from document_processor import DocumentProcessor
from embedding_service import BaseEmbeddingService, DenseSemanticEmbeddingService, GeminiEmbeddingService
from vector_store import VectorStore


class RetrievalEngine:
    """
    Main retrieval orchestrator demonstrating the full pipeline:
    Document -> Text Cleaning -> Chunking -> Embedding -> Vector Storage -> Similarity Search -> Evaluation
    """

    def __init__(
        self,
        embedding_service: Optional[BaseEmbeddingService] = None,
        vector_store: Optional[VectorStore] = None,
        document_processor: Optional[DocumentProcessor] = None
    ):
        self.embedding_service = embedding_service or DenseSemanticEmbeddingService(dimension=384)
        self.vector_store = vector_store or VectorStore()
        self.document_processor = document_processor or DocumentProcessor(default_chunk_size=100, default_chunk_overlap=20)

    def index_document_text(
        self,
        title: str,
        text: str,
        file_type: str = "TXT",
        chunk_size: Optional[int] = None,
        chunk_overlap: Optional[int] = None
    ) -> Dict[str, Any]:
        """
        Indexes a raw string document:
        1. Cleans text
        2. Splits into chunks
        3. Generates real embeddings
        4. Stores in vector store
        """
        import uuid
        doc_id = str(uuid.uuid4())
        doc_record = self.vector_store.add_document(
            doc_id=doc_id,
            title=title,
            file_type=file_type,
            file_size=len(text.encode("utf-8")),
            metadata={"source": "text_input"}
        )

        chunks = self.document_processor.chunk_text(
            text=text,
            doc_id=doc_id,
            doc_title=title,
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap
        )

        indexed_chunks = []
        for c in chunks:
            vector = self.embedding_service.generate_embedding(c["text"])
            chunk_record = self.vector_store.add_embedding(
                doc_id=doc_id,
                chunk_id=c["chunk_id"],
                chunk_index=c["chunk_index"],
                text=c["text"],
                vector=vector,
                page_num=c["page_num"]
            )
            indexed_chunks.append(chunk_record)

        return {
            "document": doc_record,
            "chunk_count": len(indexed_chunks),
            "vector_dimension": self.embedding_service.dimension,
            "provider": self.embedding_service.provider_name
        }

    def index_file(
        self,
        file_path: str,
        chunk_size: Optional[int] = None,
        chunk_overlap: Optional[int] = None
    ) -> Dict[str, Any]:
        """Indexes an existing PDF or TXT file from disk."""
        processed = self.document_processor.process_file(file_path, chunk_size, chunk_overlap)
        doc_id = processed["doc_id"]

        self.vector_store.add_document(
            doc_id=doc_id,
            title=processed["title"],
            file_type=processed["file_type"],
            file_size=processed["file_size"],
            metadata={"path": file_path}
        )

        for c in processed["chunks"]:
            vec = self.embedding_service.generate_embedding(c["text"])
            self.vector_store.add_embedding(
                doc_id=doc_id,
                chunk_id=c["chunk_id"],
                chunk_index=c["chunk_index"],
                text=c["text"],
                vector=vec,
                page_num=c["page_num"]
            )

        return {
            "doc_id": doc_id,
            "title": processed["title"],
            "chunk_count": processed["chunk_count"],
            "vector_dimension": self.embedding_service.dimension
        }

    def search(
        self,
        query: str,
        top_k: int = 5,
        threshold: float = 0.0,
        filter_doc_id: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        """
        Executes semantic retrieval for a natural-language query:
        1. Generate query embedding vector
        2. Query vector store for cosine similarity against all indexed chunks
        3. Returns Top-K results sorted descending by score
        """
        if not query or not query.strip():
            return []

        query_vector = self.embedding_service.generate_embedding(query.strip())
        results = self.vector_store.search_similar(
            query_vector=query_vector,
            top_k=top_k,
            threshold=threshold,
            filter_doc_id=filter_doc_id
        )

        for rank, r in enumerate(results, 1):
            r["rank"] = rank

        return results

    def evaluate_retrieval(
        self,
        query: str,
        relevant_keywords: List[str],
        top_k: int = 5
    ) -> Dict[str, Any]:
        """
        Evaluates retrieval quality against user relevance criteria:
        Calculates Precision@K, Recall@K, HitRate@K, and Mean Reciprocal Rank (MRR).
        """
        results = self.search(query, top_k=top_k)

        relevant_retrieved = 0
        first_relevant_rank = None

        for idx, item in enumerate(results):
            # Check if text contains any of the target relevance keywords
            text_lower = item["text"].lower()
            is_relevant = any(kw.lower() in text_lower for kw in relevant_keywords)
            item["evaluated_relevant"] = is_relevant

            if is_relevant:
                relevant_retrieved += 1
                if first_relevant_rank is None:
                    first_relevant_rank = idx + 1

        k = max(1, len(results))
        precision_at_k = relevant_retrieved / k
        hit_rate = 1.0 if relevant_retrieved > 0 else 0.0
        mrr = (1.0 / first_relevant_rank) if first_relevant_rank else 0.0

        return {
            "query": query,
            "top_k": top_k,
            "retrieved_count": len(results),
            "relevant_count": relevant_retrieved,
            "precision_at_k": round(precision_at_k, 4),
            "hit_rate_at_k": round(hit_rate, 4),
            "mrr": round(mrr, 4),
            "results": results
        }


if __name__ == "__main__":
    print("=== RETRIEVAL SYSTEM LAB - PYTHON ENGINE ===")
    engine = RetrievalEngine()

    sample_doc_1 = (
        "Machine learning is a field of artificial intelligence focused on building applications "
        "that learn from data and improve their accuracy over time without being explicitly programmed. "
        "In data science, an algorithm is a sequence of statistical processing steps. In machine learning, "
        "algorithms are trained to find patterns and features in massive amounts of data in order to make "
        "decisions and predictions based on new data."
    )
    sample_doc_2 = (
        "A neural network is a machine learning model inspired by the structure and function of the human brain. "
        "It consists of interconnected nodes or neurons organized in layers: input layer, hidden layers, and output layer. "
        "Backpropagation is the primary algorithm used to train neural networks by calculating the gradient of the loss function "
        "with respect to each weight by the chain rule."
    )

    print("\n1. Indexing Document 1: Machine Learning Basics...")
    engine.index_document_text("ML_Overview.txt", sample_doc_1, chunk_size=30, chunk_overlap=10)

    print("2. Indexing Document 2: Neural Networks & Backprop...")
    engine.index_document_text("Neural_Networks.txt", sample_doc_2, chunk_size=30, chunk_overlap=10)

    print(f"\nTotal Indexed Documents: {engine.vector_store.get_document_count()}")
    print(f"Total Indexed Chunks: {engine.vector_store.get_chunk_count()}")

    query = "What is machine learning?"
    print(f"\n3. Searching for Query: '{query}'")
    results = engine.search(query, top_k=3)
    for r in results:
        print(f"  [Rank #{r['rank']}] Doc: {r['doc_title']} | Sim: {r['similarity']:.4f}")
        print(f"    Excerpt: {r['text'][:90]}...")

    print("\n4. Running Evaluation Metric Test...")
    eval_res = engine.evaluate_retrieval(query, relevant_keywords=["machine learning", "artificial intelligence"], top_k=3)
    print(f"  Precision@3: {eval_res['precision_at_k']}")
    print(f"  HitRate@3:   {eval_res['hit_rate_at_k']}")
    print(f"  MRR:         {eval_res['mrr']}")
    print("\n=== LAB COMPLETE ===")
