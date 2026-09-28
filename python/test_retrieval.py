"""
Unit and Integration Tests for Python Retrieval System.
Part of the Retrieval System Lab.
"""

import unittest
from document_processor import DocumentProcessor
from embedding_service import DenseSemanticEmbeddingService, cosine_similarity, l2_normalize
from vector_store import VectorStore
from retrieval_engine import RetrievalEngine


class TestRetrievalSystem(unittest.TestCase):

    def setUp(self):
        self.processor = DocumentProcessor(default_chunk_size=20, default_chunk_overlap=5)
        self.embedding_service = DenseSemanticEmbeddingService(dimension=128)
        self.vector_store = VectorStore()
        self.engine = RetrievalEngine(
            embedding_service=self.embedding_service,
            vector_store=self.vector_store,
            document_processor=self.processor
        )

    def test_text_cleaning_and_chunking(self):
        text = "Deep learning is a subset of machine learning.\n\n\nIt is based on artificial neural networks."
        cleaned = self.processor.clean_text(text)
        self.assertNotIn("\n\n\n", cleaned)
        chunks = self.processor.chunk_text(cleaned, "doc_1", "Test Doc", chunk_size=5, chunk_overlap=2)
        self.assertTrue(len(chunks) >= 2)
        for c in chunks:
            self.assertIn("chunk_id", c)
            self.assertIn("text", c)
            self.assertTrue(c["word_count"] > 0)

    def test_embedding_generation_and_cosine_similarity(self):
        vec1 = self.embedding_service.generate_embedding("artificial intelligence machine learning")
        vec2 = self.embedding_service.generate_embedding("machine learning deep learning AI")
        vec_unrelated = self.embedding_service.generate_embedding("fresh organic apples and citrus fruits recipes")

        self.assertEqual(len(vec1), 128)
        self.assertEqual(len(vec2), 128)

        sim_related = cosine_similarity(vec1, vec2)
        sim_unrelated = cosine_similarity(vec1, vec_unrelated)

        # Related text should have significantly higher similarity than completely unrelated food text
        self.assertGreater(sim_related, sim_unrelated)
        self.assertGreater(sim_related, 0.3)

    def test_indexing_and_similarity_retrieval(self):
        doc1 = "Convolutional neural networks excel in computer vision, image classification and object detection."
        doc2 = "Recurrent neural networks and transformers are designed for natural language processing and sequence tasks."
        doc3 = "Photosynthesis is the biological process by which green plants transform light energy into chemical energy."

        self.engine.index_document_text("CNN.txt", doc1, chunk_size=15, chunk_overlap=3)
        self.engine.index_document_text("NLP.txt", doc2, chunk_size=15, chunk_overlap=3)
        self.engine.index_document_text("Biology.txt", doc3, chunk_size=15, chunk_overlap=3)

        results = self.engine.search("computer vision and image classification", top_k=2)
        self.assertTrue(len(results) > 0)
        top_result = results[0]
        self.assertEqual(top_result["doc_title"], "CNN.txt")
        self.assertTrue(top_result["similarity"] > 0.0)

    def test_deletion_and_vector_persistence(self):
        doc = "Testing deletion in vector store."
        res = self.engine.index_document_text("DeleteMe.txt", doc)
        doc_id = res["document"]["doc_id"]
        self.assertTrue(self.vector_store.get_document(doc_id) is not None)

        self.vector_store.delete_document(doc_id)
        self.assertIsNone(self.vector_store.get_document(doc_id))
        chunks = self.vector_store.get_chunks_for_document(doc_id)
        self.assertEqual(len(chunks), 0)


if __name__ == "__main__":
    unittest.main()
