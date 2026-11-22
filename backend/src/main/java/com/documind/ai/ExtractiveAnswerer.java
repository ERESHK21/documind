package com.documind.ai;

import com.documind.search.RetrievedChunk;
import com.documind.search.TextTokenizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Fallback answerer used when no AI key is configured (or the AI call fails).
 *
 * <p>It splits the best retrieved passages into sentences, scores every sentence by how
 * many question terms it contains, and quotes the strongest ones. The result is a real,
 * cited answer built only from the uploaded documents - no model required.</p>
 */
@Component
public class ExtractiveAnswerer {

    private final TextTokenizer tokenizer;

    public ExtractiveAnswerer(TextTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public String answer(String question, List<RetrievedChunk> ranked) {
        List<String> terms = tokenizer.tokenize(question);
        List<String> questionSentences = sentenceSplit(question);
        Set<String> chosen = new LinkedHashSet<>();

        for (RetrievedChunk retrieved : ranked.stream().limit(3).toList()) {
            for (String line : logicalLines(retrieved.chunk().getContent())) {
                if (line.isEmpty() || isHeading(line)) {
                    continue;
                }
                for (String sentence : sentenceSplit(line)) {
                    String value = sentence.trim();
                    if (value.length() < 25) {
                        continue;
                    }
                    double score = relevance(value, terms, questionSentences);
                    if (score > 0) {
                        chosen.add(value);
                    }
                }
            }
        }

        if (chosen.isEmpty()) {
            String fallback = ranked.get(0).chunk().getContent().replaceAll("\\s+", " ").trim();
            return "From your documents: " + truncate(fallback, 500);
        }

        List<RankedSentence> rankedSentences = rank(chosen, terms, questionSentences);
        double best = rankedSentences.isEmpty() ? 0 : rankedSentences.get(0).score();
        double minimum = Math.max(1.0, best * 0.5);
        List<String> picked = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        Set<String> covered = new LinkedHashSet<>();
        for (RankedSentence candidate : rankedSentences) {
            if (picked.size() >= 3 || candidate.score() < minimum) {
                break;
            }
            Set<String> matches = new LinkedHashSet<>(tokenizer.tokenize(candidate.text()));
            matches.retainAll(terms);
            if (!matches.isEmpty() && covered.containsAll(matches)) {
                // Nothing new for the question: keep the answer short and on topic.
                continue;
            }
            if (seen.add(candidate.text().toLowerCase(Locale.ROOT))) {
                picked.add(candidate.text());
                covered.addAll(matches);
            }
        }
        return "From your documents: " + truncate(String.join(" ", picked), 700);
    }

    /** Section titles such as "LEAVE AND NOTICE" are not answers. */
    private static boolean isHeading(String line) {
        if (line.length() > 90 || !line.matches(".*[A-Za-z].*")) {
            return false;
        }
        return line.equals(line.toUpperCase(Locale.ROOT));
    }

    /**
     * Rebuilds wrapped lines into whole paragraphs. PDF text and pasted text often break a
     * sentence across physical lines, and a half sentence cannot be quoted as an answer.
     */
    private static List<String> logicalLines(String content) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        for (String raw : normalized.split("\n")) {
            String line = raw.replaceAll("[ \\t]+", " ").trim();
            if (line.isEmpty()) {
                flush(lines, current);
                continue;
            }
            if (current.length() > 0 && !endsSentence(current) && !isHeading(current.toString())) {
                current.append(' ').append(line);
            } else {
                flush(lines, current);
                current.append(line);
            }
        }
        flush(lines, current);
        return lines;
    }

    private static void flush(List<String> lines, StringBuilder current) {
        if (current.length() > 0) {
            lines.add(current.toString());
            current.setLength(0);
        }
    }

    private static boolean endsSentence(CharSequence value) {
        char last = value.charAt(value.length() - 1);
        return last == '.' || last == '!' || last == '?' || last == ':';
    }

    private List<RankedSentence> rank(Set<String> sentences, List<String> terms, List<String> questionSentences) {
        List<RankedSentence> ranked = new ArrayList<>();
        for (String sentence : sentences) {
            ranked.add(new RankedSentence(sentence, relevance(sentence, terms, questionSentences)));
        }
        ranked.sort((left, right) -> {
            int byScore = Double.compare(right.score(), left.score());
            return byScore != 0 ? byScore : Integer.compare(left.text().length(), right.text().length());
        });
        return ranked;
    }

    private double relevance(String sentence, List<String> terms, List<String> questionSentences) {
        List<String> tokens = tokenizer.tokenize(sentence);
        if (tokens.isEmpty() || terms.isEmpty()) {
            return 0;
        }
        Set<String> unique = new LinkedHashSet<>(tokens);
        long overlap = terms.stream().filter(unique::contains).count();
        if (overlap == 0) {
            return 0;
        }
        double score = overlap;
        String lower = sentence.toLowerCase(Locale.ROOT);
        for (String phrase : questionSentences) {
            if (phrase.length() > 12 && lower.contains(phrase)) {
                score += 2;
            }
        }
        if (!questionSentences.isEmpty() && asksForQuantity(questionSentences.get(0)) && lower.matches(".*\\d.*")) {
            score += 0.5;
        }
        return score;
    }

    private static boolean asksForQuantity(String question) {
        String lower = question.toLowerCase(Locale.ROOT);
        return lower.contains("how many") || lower.contains("how much") || lower.contains("number of");
    }

    private static List<String> sentenceSplit(String text) {
        List<String> sentences = new ArrayList<>();
        for (String part : text.split("(?<=[.!?])\\s+")) {
            String value = part.trim();
            if (!value.isEmpty()) {
                sentences.add(value);
            }
        }
        return sentences;
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max).trim() + "...";
    }

    private record RankedSentence(String text, double score) {
    }
}
