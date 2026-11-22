package com.documind.chat;

import com.documind.ai.AiAnswerer;
import com.documind.ai.ExtractiveAnswerer;
import com.documind.common.ApiException;
import com.documind.common.PageResponse;
import com.documind.document.ChunkRepository;
import com.documind.document.DocumentChunk;
import com.documind.document.DocumentRepository;
import com.documind.search.RetrievalService;
import com.documind.search.RetrievedChunk;
import com.documind.search.TextTokenizer;
import com.documind.user.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;
    private final ChunkRepository chunks;
    private final DocumentRepository documents;
    private final RetrievalService retrieval;
    private final AiAnswerer aiAnswerer;
    private final ExtractiveAnswerer extractive;
    private final TextTokenizer tokenizer;
    private final int topK;

    public ChatService(
            ChatSessionRepository sessions,
            ChatMessageRepository messages,
            ChunkRepository chunks,
            DocumentRepository documents,
            RetrievalService retrieval,
            AiAnswerer aiAnswerer,
            ExtractiveAnswerer extractive,
            TextTokenizer tokenizer,
            @Value("${app.rag.top-k:5}") int topK) {
        this.sessions = sessions;
        this.messages = messages;
        this.chunks = chunks;
        this.documents = documents;
        this.retrieval = retrieval;
        this.aiAnswerer = aiAnswerer;
        this.extractive = extractive;
        this.tokenizer = tokenizer;
        this.topK = Math.max(1, Math.min(topK, 10));
    }

    @Transactional
    public AskResponse ask(AskRequest request, User user) {
        long started = System.nanoTime();
        String question = request.question().trim();

        List<Long> documentIds = request.documentIds() == null ? List.of()
                : request.documentIds().stream().filter(Objects::nonNull).distinct().toList();
        if (!documentIds.isEmpty()) {
            long owned = documents.countByOwnerIdAndIdIn(user.getId(), documentIds);
            if (owned != documentIds.size()) {
                throw ApiException.notFound("Document");
            }
        }

        List<DocumentChunk> available = documentIds.isEmpty()
                ? chunks.findByDocumentOwnerId(user.getId())
                : chunks.findByDocumentOwnerIdAndDocumentIdIn(user.getId(), documentIds);
        if (available.isEmpty()) {
            throw ApiException.badRequest("Add a document before asking a question");
        }

        List<RetrievedChunk> ranked = retrieval.rank(available, question, topK);
        List<String> contextBlocks = new ArrayList<>();
        for (RetrievedChunk retrieved : ranked) {
            DocumentChunk chunk = retrieved.chunk();
            contextBlocks.add("(" + chunk.getDocument().getName() + ", page " + chunk.getPageNumber() + ")\n"
                    + truncate(chunk.getContent().replaceAll("\\s+", " ").trim(), 1400));
        }

        String answer;
        AnswerMode mode = AnswerMode.OFFLINE;
        if (ranked.isEmpty()) {
            answer = "I couldn't find that in the selected documents. "
                    + "Try rephrasing the question, or add the document that covers it.";
        } else {
            Optional<String> ai = aiAnswerer.answer(question, contextBlocks);
            if (ai.isPresent()) {
                answer = ai.get().trim();
                mode = AnswerMode.AI;
            } else {
                answer = extractive.answer(question, ranked);
            }
        }

        Instant now = Instant.now();
        ChatSession session = resolveSession(request.sessionId(), question, user, now);

        ChatMessage userMessage = new ChatMessage();
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent(question);
        userMessage.setCreatedAt(now);
        session.addMessage(userMessage);

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setRole(MessageRole.ASSISTANT);
        assistantMessage.setContent(truncate(answer, 6000));
        assistantMessage.setMode(mode);
        assistantMessage.setCreatedAt(now);
        assistantMessage.setLatencyMs((int) Math.max(0, (System.nanoTime() - started) / 1_000_000));

        int rank = 0;
        for (RetrievedChunk retrieved : ranked) {
            assistantMessage.addCitation(toCitation(retrieved, rank + 1, question));
            rank++;
            if (rank >= 4) {
                break;
            }
        }
        session.addMessage(assistantMessage);
        session.setLastActivityAt(now);

        sessions.save(session);
        return new AskResponse(session.getId(), session.getTitle(),
                toMessage(userMessage), toMessage(assistantMessage));
    }

    @Transactional(readOnly = true)
    public PageResponse<SessionResponse> list(int page, int size, User user) {
        Page<ChatSession> result = sessions.findByOwnerIdOrderByLastActivityAtDesc(
                user.getId(), PageRequest.of(Math.max(page, 0), clampSize(size)));
        return PageResponse.of(result, session -> new SessionResponse(
                session.getId(),
                session.getTitle(),
                session.getCreatedAt(),
                session.getLastActivityAt(),
                messages.countBySessionId(session.getId()),
                messages.findFirstBySessionIdAndRoleOrderByCreatedAtDesc(session.getId(), MessageRole.USER)
                        .map(ChatMessage::getContent)
                        .map(value -> truncate(value, 120))
                        .orElse(null)));
    }

    @Transactional(readOnly = true)
    public SessionDetailResponse detail(Long id, User user) {
        ChatSession session = sessions.findByIdAndOwnerId(id, user.getId())
                .orElseThrow(() -> ApiException.notFound("Conversation"));
        return new SessionDetailResponse(
                session.getId(),
                session.getTitle(),
                session.getCreatedAt(),
                session.getLastActivityAt(),
                session.getMessages().stream().map(this::toMessage).toList());
    }

    @Transactional
    public void delete(Long id, User user) {
        ChatSession session = sessions.findByIdAndOwnerId(id, user.getId())
                .orElseThrow(() -> ApiException.notFound("Conversation"));
        sessions.delete(session);
    }

    private ChatSession resolveSession(Long sessionId, String question, User user, Instant now) {
        if (sessionId == null) {
            ChatSession session = new ChatSession();
            session.setOwner(user);
            session.setTitle(titleFor(question));
            session.setCreatedAt(now);
            session.setLastActivityAt(now);
            return session;
        }
        ChatSession session = sessions.findByIdAndOwnerId(sessionId, user.getId())
                .orElseThrow(() -> ApiException.notFound("Conversation"));
        session.setLastActivityAt(now);
        return session;
    }

    private Citation toCitation(RetrievedChunk retrieved, int rank, String question) {
        DocumentChunk chunk = retrieved.chunk();
        Citation citation = new Citation();
        citation.setRank(rank);
        citation.setDocumentId(chunk.getDocument().getId());
        citation.setDocumentName(chunk.getDocument().getName());
        citation.setPageNumber(chunk.getPageNumber());
        citation.setSnippet(snippet(chunk, tokenizer.tokenize(question)));
        citation.setScore(retrieved.score());
        return citation;
    }

    private MessageResponse toMessage(ChatMessage message) {
        return new MessageResponse(
                message.getId(),
                message.getRole().name(),
                message.getContent(),
                message.getMode() == null ? null : message.getMode().name(),
                message.getLatencyMs(),
                message.getCreatedAt(),
                message.getCitations().stream()
                        .map(citation -> new CitationResponse(
                                citation.getDocumentId(),
                                citation.getDocumentName(),
                                citation.getPageNumber(),
                                citation.getSnippet(),
                                citation.getScore()))
                        .toList());
    }

    /**
     * Picks the window of the passage that contains the most question terms. The snippet the
     * user opens in the source drawer should be the part of the page the answer came from, not
     * simply the first place a word happens to appear.
     */
    private static String snippet(DocumentChunk chunk, List<String> terms) {
        String content = chunk.getContent().replaceAll("\\s+", " ").trim();
        String lower = content.toLowerCase(Locale.ROOT);
        int window = 280;
        List<Integer> positions = new ArrayList<>();
        for (String term : terms) {
            if (term.isBlank()) {
                continue;
            }
            int from = 0;
            while (from < lower.length()) {
                int index = lower.indexOf(term, from);
                if (index < 0) {
                    break;
                }
                positions.add(index);
                from = index + term.length();
            }
        }
        if (positions.isEmpty()) {
            return truncate(content, window);
        }
        positions.sort(Integer::compareTo);
        int bestStart = 0;
        int bestHits = -1;
        for (int position : positions) {
            int start = Math.max(0, position - 90);
            int end = Math.min(content.length(), start + window);
            String candidate = lower.substring(start, end);
            int hits = 0;
            for (String term : terms) {
                if (!term.isBlank() && candidate.contains(term)) {
                    hits++;
                }
            }
            if (hits > bestHits) {
                bestHits = hits;
                bestStart = start;
            }
        }
        int end = Math.min(content.length(), bestStart + window);
        String value = content.substring(bestStart, end).trim();
        return (bestStart > 0 ? "..." : "") + value + (end < content.length() ? "..." : "");
    }

    private static String titleFor(String question) {
        String clean = question.replaceAll("\\s+", " ").trim();
        return clean.length() <= 80 ? clean : clean.substring(0, 80).trim() + "...";
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max).trim() + "...";
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), 50);
    }
}
