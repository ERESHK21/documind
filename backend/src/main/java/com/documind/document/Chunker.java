package com.documind.document;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Splits page text into overlapping passages ("chunks").
 * Small chunks make retrieval precise; the overlap keeps a sentence that
 * straddles two chunks readable.
 */
@Component
public class Chunker {

    private final int targetSize;
    private final int overlap;

    public Chunker(
            @Value("${app.rag.chunk-size:800}") int targetSize,
            @Value("${app.rag.chunk-overlap:120}") int overlap) {
        this.targetSize = Math.max(targetSize, 200);
        this.overlap = Math.max(0, Math.min(overlap, this.targetSize / 2));
    }

    public List<String> split(String text) {
        String clean = text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n')
                .replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
        if (clean.isEmpty()) {
            return List.of();
        }

        List<String> units = new ArrayList<>();
        for (String paragraph : clean.split("\\n\\s*\\n")) {
            String value = paragraph.trim();
            if (value.isEmpty()) {
                continue;
            }
            if (value.length() <= targetSize) {
                units.add(value);
            } else {
                units.addAll(splitSentences(value));
            }
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String unit : units) {
            if (current.length() > 0 && current.length() + unit.length() + 2 > targetSize) {
                chunks.add(current.toString().trim());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(unit);
        }
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }

        if (overlap > 0 && chunks.size() > 1) {
            for (int i = 1; i < chunks.size(); i++) {
                String previous = chunks.get(i - 1);
                String tail = previous.substring(Math.max(0, previous.length() - overlap)).trim();
                if (!tail.isEmpty()) {
                    chunks.set(i, tail + "\n\n" + chunks.get(i));
                }
            }
        }
        return chunks;
    }

    private List<String> splitSentences(String paragraph) {
        List<String> parts = new ArrayList<>();
        for (String sentence : paragraph.split("(?<=[.!?])\\s+")) {
            if (sentence.length() <= targetSize) {
                parts.add(sentence);
            } else {
                for (int start = 0; start < sentence.length(); start += targetSize) {
                    parts.add(sentence.substring(start, Math.min(sentence.length(), start + targetSize)));
                }
            }
        }
        return parts;
    }
}
