package com.example.document

object SampleDocuments {

    data class BenchmarkDoc(
        val title: String,
        val fileType: String,
        val text: String
    )

    val BENCHMARK_PAPERS = listOf(
        BenchmarkDoc(
            title = "Introduction_to_Machine_Learning.txt",
            fileType = "TXT",
            text = """
                Machine learning is a transformative subfield of artificial intelligence that empowers computational systems to learn patterns and infer rules directly from empirical data, rather than following static, hardcoded instructions. In supervised learning, algorithms are provided with pairs of input features and ground-truth output labels, optimizing loss functions such as Mean Squared Error for continuous regression problems or Cross-Entropy Loss for discrete classification tasks.
                
                Unsupervised learning, conversely, discovers latent structural regularities, clustering patterns, and lower-dimensional manifolds without external supervision. Principal Component Analysis (PCA) and t-Distributed Stochastic Neighbor Embedding (t-SNE) are widely utilized for dimensionality reduction, projecting high-dimensional data into orthogonal components or visualizable spaces.
                
                Reinforcement learning structures learning as an iterative Markov Decision Process (MDP), wherein an autonomous agent interacts with an environment, taking sequential actions to maximize cumulative discounted rewards over time. Through exploration and exploitation strategies like Q-learning and Policy Gradient methods, the agent optimizes its decision policy.
            """.trimIndent()
        ),
        BenchmarkDoc(
            title = "Neural_Networks_and_Deep_Learning.txt",
            fileType = "TXT",
            text = """
                Deep neural networks represent hierarchical connectionist architectures composed of stacked layers of parameterized computational units called artificial neurons. Each individual neuron computes a weighted affine sum of its inputs, adds a learnable bias term, and applies a non-linear activation function such as Rectified Linear Unit (ReLU), GELU, or Sigmoid. This non-linearity equips multi-layer perceptrons with universal function approximation capabilities.
                
                Training deep neural networks relies on reverse-mode automatic differentiation known as backpropagation. During the forward pass, activations propagate through network layers to generate predictions, evaluated against targets using an objective loss function. During the backward pass, gradients of the loss with respect to all layer parameters are computed iteratively using the calculus chain rule.
                
                Optimization algorithms such as Stochastic Gradient Descent (SGD) with momentum, RMSprop, and Adam adaptively adjust learning rates and parameter update trajectories. Regularization techniques including Dropout, Layer Normalization, Batch Normalization, and weight decay prevent overfitting on finite training datasets.
            """.trimIndent()
        ),
        BenchmarkDoc(
            title = "Transformers_and_Attention_Mechanisms.txt",
            fileType = "TXT",
            text = """
                The Transformer architecture revolutionized sequence modeling by replacing sequential recurrence with self-attention mechanisms, facilitating parallel training over extensive context windows. The core operation is Scaled Dot-Product Attention, computed as Attention(Q, K, V) = softmax((Q * K^T) / sqrt(d_k)) * V, where queries Q, keys K, and values V are linear projections of input token representations.
                
                Multi-Head Attention projects queries, keys, and values into multiple distinct subspace representations simultaneously, allowing the network to jointly attend to semantic information at different positions and abstraction levels. Positional encodings, either sinusoidal or learned, inject sequential order into the permutation-invariant attention layers.
                
                Modern Large Language Models (LLMs) and encoder-decoder foundation models build upon stacked transformer blocks with residual connections, layer normalization, and feed-forward networks, powering state-of-the-art natural language processing, semantic code generation, and multimodal reasoning.
            """.trimIndent()
        ),
        BenchmarkDoc(
            title = "Vector_Embeddings_and_Information_Retrieval.txt",
            fileType = "TXT",
            text = """
                Vector embeddings map discrete textual tokens, sentences, or chunked documents into continuous, dense semantic vector spaces where geometric proximity reflects conceptual similarity. Traditional sparse lexical retrieval algorithms like BM25 and TF-IDF rely on exact token matching and term frequency statistics, frequently suffering from vocabulary mismatch and synonymy.
                
                Dense retrieval models overcome vocabulary mismatch by embedding both search queries and document chunks into shared embedding spaces. Cosine similarity, defined as the dot product of two normalized vectors divided by the product of their Euclidean norms, provides a bounded metric between -1.0 and 1.0 reflecting directional semantic alignment.
                
                Retrieval-Augmented Generation (RAG) pipelines combine dense vector search with generative language models. When a user submits a prompt, the system chunks candidate documents, computes query and chunk embeddings, retrieves the Top-K most relevant chunks via vector similarity, and augments the context provided to the model, producing grounded, hallucination-resistant responses.
            """.trimIndent()
        )
    )
}
