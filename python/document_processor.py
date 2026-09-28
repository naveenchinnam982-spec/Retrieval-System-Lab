"""
Document Processor module for text cleaning, normalization, and configurable chunking.
Part of the Retrieval System Lab.
"""

import re
import os
import uuid
from typing import List, Dict, Any, Optional


class DocumentProcessor:
    """
    Handles reading documents (TXT, PDF, Raw Text), cleaning text,
    and splitting into configurable overlapping chunks with rich metadata.
    """

    def __init__(self, default_chunk_size: int = 100, default_chunk_overlap: int = 20):
        """
        :param default_chunk_size: Target number of words per chunk.
        :param default_chunk_overlap: Number of overlapping words between consecutive chunks.
        """
        self.default_chunk_size = max(10, default_chunk_size)
        self.default_chunk_overlap = max(0, min(default_chunk_overlap, self.default_chunk_size - 1))

    def clean_text(self, text: str) -> str:
        """
        Cleans text: removes extraneous control characters, normalizes line breaks
        and collapses excessive whitespace while keeping sentence punctuation intact.
        """
        if not text:
            return ""
        # Remove null bytes and non-printable characters except standard whitespace
        text = re.sub(r'[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]', '', text)
        # Normalize carriage returns and tabs
        text = text.replace('\r\n', '\n').replace('\r', '\n').replace('\t', ' ')
        # Remove page break artifacts and excessive dashes
        text = re.sub(r'(\n\s*){3,}', '\n\n', text)
        # Collapse multiple inline spaces
        text = re.sub(r'[ ]{2,}', ' ', text)
        return text.strip()

    def chunk_text(
        self,
        text: str,
        doc_id: str,
        doc_title: str,
        chunk_size: Optional[int] = None,
        chunk_overlap: Optional[int] = None,
        page_num: int = 1
    ) -> List[Dict[str, Any]]:
        """
        Splits clean text into overlapping chunks based on words/tokens.
        Returns a list of chunk metadata dictionaries.
        """
        cleaned = self.clean_text(text)
        if not cleaned:
            return []

        size = chunk_size if chunk_size is not None and chunk_size > 0 else self.default_chunk_size
        overlap = chunk_overlap if chunk_overlap is not None and chunk_overlap >= 0 else self.default_chunk_overlap
        step = max(1, size - overlap)

        # Tokenize by whitespace while preserving punctuation
        words = cleaned.split()
        if not words:
            return []

        chunks = []
        chunk_index = 0
        total_words = len(words)

        for start_idx in range(0, total_words, step):
            end_idx = min(start_idx + size, total_words)
            chunk_words = words[start_idx:end_idx]
            chunk_content = " ".join(chunk_words)

            chunk_id = f"{doc_id}_chunk_{chunk_index}"
            chunk_metadata = {
                "chunk_id": chunk_id,
                "doc_id": doc_id,
                "doc_title": doc_title,
                "chunk_index": chunk_index,
                "text": chunk_content,
                "word_count": len(chunk_words),
                "char_count": len(chunk_content),
                "start_word_index": start_idx,
                "end_word_index": end_idx,
                "page_num": page_num
            }
            chunks.append(chunk_metadata)
            chunk_index += 1

            if end_idx >= total_words:
                break

        return chunks

    def process_file(
        self,
        file_path: str,
        chunk_size: Optional[int] = None,
        chunk_overlap: Optional[int] = None
    ) -> Dict[str, Any]:
        """
        Reads a TXT or PDF file and generates document metadata and chunks.
        """
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"File not found: {file_path}")

        file_name = os.path.basename(file_path)
        file_size = os.path.getsize(file_path)
        ext = os.path.splitext(file_name)[1].lower()
        doc_id = str(uuid.uuid4())

        extracted_text = ""
        page_chunks = []

        if ext == ".txt":
            with open(file_path, "r", encoding="utf-8", errors="replace") as f:
                extracted_text = f.read()
            chunks = self.chunk_text(
                extracted_text,
                doc_id=doc_id,
                doc_title=file_name,
                chunk_size=chunk_size,
                chunk_overlap=chunk_overlap,
                page_num=1
            )
        elif ext == ".pdf":
            # Native PDF stream extraction
            extracted_text = self._extract_pdf_text(file_path)
            chunks = self.chunk_text(
                extracted_text,
                doc_id=doc_id,
                doc_title=file_name,
                chunk_size=chunk_size,
                chunk_overlap=chunk_overlap,
                page_num=1
            )
        else:
            # Fallback text reading
            with open(file_path, "r", encoding="utf-8", errors="replace") as f:
                extracted_text = f.read()
            chunks = self.chunk_text(
                extracted_text,
                doc_id=doc_id,
                doc_title=file_name,
                chunk_size=chunk_size,
                chunk_overlap=chunk_overlap,
                page_num=1
            )

        return {
            "doc_id": doc_id,
            "title": file_name,
            "file_type": ext.lstrip(".").upper(),
            "file_size": file_size,
            "total_words": len(extracted_text.split()),
            "total_chars": len(extracted_text),
            "chunk_count": len(chunks),
            "chunks": chunks
        }

    def _extract_pdf_text(self, file_path: str) -> str:
        """
        Extracts plain text from a PDF file using basic stream parsing.
        """
        text_parts = []
        try:
            with open(file_path, "rb") as f:
                content = f.read()
                # Simple extraction of string objects and text blocks from uncompressed streams
                streams = re.findall(b'stream[\r\n]+(.*?)[\r\n]+endstream', content, re.DOTALL)
                for s in streams:
                    # Look for text between parentheses in BT ... ET blocks
                    matches = re.findall(rb'\(([^\)]+)\)\s*T[jJ]', s)
                    for m in matches:
                        try:
                            decoded = m.decode('latin1', errors='ignore')
                            if decoded.strip():
                                text_parts.append(decoded)
                        except Exception:
                            pass
        except Exception as e:
            text_parts.append(f"[Error reading PDF: {e}]")

        if not text_parts:
            # Fallback: scan for any readable ASCII strings
            with open(file_path, "rb") as f:
                raw = f.read()
                readable = re.findall(rb'[\x20-\x7E\s]{4,}', raw)
                text_parts = [r.decode('latin1', errors='ignore') for r in readable]

        return " ".join(text_parts)
