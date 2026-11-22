package com.documind.search;

import com.documind.document.DocumentChunk;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Retrieval part of RAG.
 *
 * <p>Every chunk is scored with a small TF-IDF model: rare words that appear in the
 * question count more than common ones, and extremely long chunks are penalised so a
 * single huge passage cannot win by length alone. A boost is added when the exact
 * question phrase appears in the chunk. This is explainable and needs no vector database.</p>
 */
@Service
public class RetrievalService {

    private final TextTokenizer tokenizer;

    public RetrievalService(TextTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public List<RetrievedChunk> rank(List<DocumentChunk> chunks, String question, int topK) {
        List<String> queryTerms = new ArrayList<>();
        for (String term : tokenizer.tokenize(question)) {
            if (!queryTerms.contains(term)) {
                queryTerms.add(term);
            }
        }
        if (queryTerms.isEmpty() || chunks.isEmpty()) {
            return List.of();
        }

        Map<Long, Map<String, Integer>> countsByChunk = new HashMap<>();
        Map<Long, Integer> lengths = new HashMap<>();
        Map<String, Integer> documentFrequency = new HashMap<>();

        for (DocumentChunk chunk : chunks) {
            List<String> tokens = tokenizer.tokenize(chunk.getContent());
            Map<String, Integer> counts = new HashMap<>();
            for (String token : tokens) {
                counts.merge(token, 1, Integer::sum);
            }
            countsByChunk.put(chunk.getId(), counts);
            lengths.put(chunk.getId(), Math.max(tokens.size(), 1));
            for (String term : new HashSet<>(counts.keySet())) {
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }

        double totalChunks = Math.max(chunks.size(), 1);
        String lowerQuestion = question.toLowerCase(Locale.ROOT).trim();
        List<RetrievedChunk> scored = new ArrayList<>();

        for (DocumentChunk chunk : chunks) {
            Map<String, Integer> counts = countsByChunk.get(chunk.getId());
            double score = 0.0;
            for (String term : queryTerms) {
                int frequency = counts.getOrDefault(term, 0);
                if (frequency == 0) {
                    continue;
                }
                int df = documentFrequency.getOrDefault(term, 1);
                double idf = Math.log(1 + totalChunks / Math.max(df, 1));
                double tf = 1 + Math.log(frequency);
                score += tf * idf / Math.sqrt(lengths.get(chunk.getId()));
            }
            if (score > 0 && lowerQuestion.length() > 3
                    && chunk.getContent().toLowerCase(Locale.ROOT).contains(lowerQuestion)) {
                score += 3.0;
            }
            if (score > 0) {
                scored.add(new RetrievedChunk(chunk, Math.round(score * 1000.0) / 1000.0));
            }
        }

        scored.sort((left, right) -> Double.compare(right.score(), left.score()));
        return scored.size() > topK ? scored.subList(0, topK) : scored;
    }
}
