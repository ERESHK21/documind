package com.documind.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Lowercases, strips punctuation and drops stopwords.
 * Keeps short numeric tokens ("14", "30") because they often carry the answer.
 */
@Component
public class TextTokenizer {

    private static final Set<String> STOPWORDS = Set.of(
            "the", "a", "an", "and", "or", "of", "to", "in", "on", "at", "is", "it", "this", "that",
            "with", "for", "my", "was", "were", "has", "have", "had", "me", "its", "from", "by",
            "near", "about", "there", "when", "where", "which", "some", "very", "really", "how",
            "what", "who", "why", "does", "do", "did", "can", "could", "should", "would", "will",
            "are", "be", "been", "being", "not", "no", "yes", "you", "your", "i", "we", "they",
            "he", "she", "them", "our", "us", "but", "if", "then", "than", "so", "such", "also",
            "into", "over", "under", "after", "before", "between", "per", "each", "any", "all");

    public List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        for (String raw : text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ").split("\\s+")) {
            if (raw.isBlank()) {
                continue;
            }
            boolean numeric = raw.chars().allMatch(Character::isDigit);
            if (numeric ? raw.length() < 2 : raw.length() < 3) {
                continue;
            }
            if (STOPWORDS.contains(raw)) {
                continue;
            }
            tokens.add(raw);
        }
        return tokens;
    }
}
