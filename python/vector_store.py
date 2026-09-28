"""
Vector Store module for persistent embedding vectors and metadata storage.
Part of the Retrieval System Lab.
"""

import json
import os
import time
from typing import List, Dict, Any, Optional
from embedding_service import cosine_similarity


class VectorStore:
    """
    Persistent Vector Store that indexes documents, chunk texts,
    numerical embedding vectors, and metadata with similarity search capabilities.
    """

    def __init__(self, storage_path: Optional[str] = None):
        self.storage_path = storage_path
        self._documents: Dict[str, Dict[str, Any]] = {}
        self._chunks: Dict[str, Dict[str, Any]] = {}

        if storage_path and os.path.exists(storage_path):
            self.load_from_disk(storage_path)

    def add_document(
        self,
        doc_id: str,
        title: str,
        file_type: str,
        file_size: int,
        metadata: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """Registers a document record."""
        doc_record = {
            "doc_id": doc_id,
            "title": title,
            "file_type": file_type,
            "file_size": file_size,
            "chunk_count": 0,
            "created_at": time.time(),
            "metadata": metadata or {}
        }
        self._documents[doc_id] = doc_record
        return doc_record

    def add_embedding(
        self,
        doc_id: str,
        chunk_id: str,
        chunk_index: int,
        text: str,
        vector: List[float],
        page_num: int = 1,
        metadata: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """Stores a chunk along with its embedding vector and metadata."""
        if doc_id not in self._documents:
            raise KeyError(f"Document {doc_id} not registered in VectorStore")

        chunk_record = {
            "chunk_id": chunk_id,
            "doc_id": doc_id,
            "doc_title": self._documents[doc_id]["title"],
            "chunk_index": chunk_index,
            "text": text,
            "word_count": len(text.split()),
            "vector": vector,
            "vector_dimension": len(vector),
            "page_num": page_num,
            "created_at": time.time(),
            "metadata": metadata or {}
        }
        self._chunks[chunk_id] = chunk_record
        self._documents[doc_id]["chunk_count"] = sum(
            1 for c in self._chunks.values() if c["doc_id"] == doc_id
        )
        return chunk_record

    def search_similar(
        self,
        query_vector: List[float],
        top_k: int = 5,
        threshold: float = 0.0,
        filter_doc_id: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        """
        Calculates cosine similarity between query_vector and all stored chunk embeddings.
        Returns Top-K results sorted by similarity score in descending order.
        """
        results = []

        for chunk in self._chunks.values():
            if filter_doc_id and chunk["doc_id"] != filter_doc_id:
                continue

            chunk_vec = chunk["vector"]
            if len(chunk_vec) != len(query_vector):
                continue

            score = cosine_similarity(query_vector, chunk_vec)
            if score >= threshold:
                results.append({
                    "chunk_id": chunk["chunk_id"],
                    "doc_id": chunk["doc_id"],
                    "doc_title": chunk["doc_title"],
                    "chunk_index": chunk["chunk_index"],
                    "page_num": chunk["page_num"],
                    "text": chunk["text"],
                    "word_count": chunk["word_count"],
                    "similarity": round(score, 4),
                    "vector_dimension": chunk["vector_dimension"]
                })

        # Sort descending by similarity
        results.sort(key=lambda r: r["similarity"], reverse=True)
        return results[:top_k]

    def get_documents(self) -> List[Dict[str, Any]]:
        """Returns all registered documents."""
        return list(self._documents.values())

    def get_document(self, doc_id: str) -> Optional[Dict[str, Any]]:
        """Gets a document by its ID."""
        return self._documents.get(doc_id)

    def get_chunks_for_document(self, doc_id: str) -> List[Dict[str, Any]]:
        """Gets all chunks belonging to a specific document."""
        chunks = [c for c in self._chunks.values() if c["doc_id"] == doc_id]
        chunks.sort(key=lambda c: c["chunk_index"])
        return chunks

    def delete_document(self, doc_id: str) -> bool:
        """Deletes a document and all of its associated chunk embeddings."""
        if doc_id in self._documents:
            del self._documents[doc_id]
            # Cascade delete associated chunks
            to_delete = [cid for cid, c in self._chunks.items() if c["doc_id"] == doc_id]
            for cid in to_delete:
                del self._chunks[cid]
            return True
        return False

    def delete_all(self):
        """Clears all documents and embeddings."""
        self._documents.clear()
        self._chunks.clear()

    def get_document_count(self) -> int:
        """Returns total number of documents."""
        return len(self._documents)

    def get_chunk_count(self) -> int:
        """Returns total number of indexed chunk vectors."""
        return len(self._chunks)

    def save_to_disk(self, file_path: Optional[str] = None):
        """Persists the vector store to JSON format."""
        path = file_path or self.storage_path
        if not path:
            raise ValueError("No storage path provided for persistence")

        data = {
            "version": "1.0",
            "saved_at": time.time(),
            "documents": self._documents,
            "chunks": self._chunks
        }
        os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)

    def load_from_disk(self, file_path: str):
        """Loads vector store from JSON format."""
        if not os.path.exists(file_path):
            return
        with open(file_path, "r", encoding="utf-8") as f:
            data = json.load(f)
            self._documents = data.get("documents", {})
            self._chunks = data.get("chunks", {})
