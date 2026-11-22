# DocuMind — Verification Report

**Date:** 19 November 2026
**Result:** **64 / 64 backend checks passed**, frontend production build clean, browser flows verified.

---

## 1. Environment

| Item | Value |
| --- | --- |
| OS | Windows 11 (OneDrive-backed workspace) |
| Java | 21 (`C:\Program Files\Java\jdk-21`) |
| Backend | Spring Boot 4.0.9, Maven wrapper, H2 2.4 |
| Frontend | Node + npm, Vite 7.3.8, React 19, Tailwind 4 |
| Database used for the check run | Fresh in-memory H2 (`jdbc:h2:mem:verify`) so nothing touched `backend/data/` |
| AI mode during checks | Offline (no `GEMINI_API_KEY`), which is the default demo mode |

Every backend check below ran against a **newly started jar on port 8095 with an empty database**, seeded
automatically by `DataSeeder`.

---

## 2. Backend checks — 64/64

### Startup and health (3)
- Server starts and `/api/health` responds.
- Health reports `service = documind-backend`.
- Health reports an answer mode (`offline-extractive` without a key).

### Authentication and validation (7)
- Wrong password → `401`.
- Unknown email → `401`.
- Password shorter than 6 chars → `400` with a field error.
- Malformed JSON → `400` with a readable message.
- Demo login returns a JWT and `USER` role.
- Admin login returns `ADMIN` role.
- `/api/auth/me` returns the signed-in profile.
- Register returns a token, no password hash, and a new account starts with zero documents.

### Documents and indexing (18)
- Unauthenticated document list → `401`.
- Seeded account starts with exactly 3 documents; every seeded document has at least one passage.
- Document list returns the paging shape (`content`, `page`, `size`, `totalElements`, `totalPages`, `last`).
- Pasted text is indexed, keeps one page, and correctly reports `hasOriginalFile = false`.
- Document detail and passage list work; passages carry page numbers.
- Delete → `204`, then `404` on re-read; unknown id → `404`.
- TXT upload indexes and the original downloads with `200`.
- **PDF upload from a real 3-page PDF extracts `pageCount = 3`.**
- A question scoped to that PDF cites **page 2**, where the fact lives.
- `.exe` upload → `400` ("Only PDF, TXT and Markdown files are supported").
- Fake PDF (wrong bytes) → `400` with the OCR hint.
- Multipart request without a `file` part → `400`.

### RAG answers and conversations (20)
- A question returns a non-empty answer.
- The answer carries citations, each with document name, page number, snippet and score.
- Answer mode is `AI` or `OFFLINE`; latency is recorded.
- Seeded fact is answered correctly (24 paid leave days).
- Scoping with `documentIds` searches only that document (Atlas note → "February 2028").
- A follow-up with `sessionId` reuses the conversation.
- Stored conversation has ≥2 user messages and ≥2 assistant replies.
- Conversation list is populated and exposes the last question.
- Asking with zero documents → `400` "Add a document before asking a question".
- Empty question → `400`.
- Non-existent `documentIds` → `404`.
- Another user's document: cannot be fetched (`404`) and cannot be used in a question (`404`).

### Stats and admin (6)
- Personal stats count documents and questions correctly.
- Admin stats return platform-wide user/document/passage/chat counts.
- Admin user list includes the seeded users.
- Normal user on `/api/admin/stats` → `403`.
- Normal user on `/api/admin/users` → `403`.

### Protocol errors (5)
- Wrong method → `405`.
- Unknown path → `404`.
- Non-numeric id → `400` (not `500`).
- Delete conversation → `204`; re-read → `404`.

### Verified JSON error shape
```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "Validation failed", "details": { "password": "..." } }
```

---

## 3. Frontend checks

Production build:

```
vite v7.3.8 building client environment for production...
✓ 118 modules transformed.
dist/index.html                   0.54 kB
dist/assets/index-*.css          41.17 kB │ gzip: 7.06 kB
dist/assets/index-*.js          382.61 kB │ gzip: 121.89 kB
✓ built in 2.22s
```

Browser pass (dev server + backend, real clicks and real API calls):

| Flow | Result |
| --- | --- |
| Login page loads with demo-account shortcuts | ✅ |
| One-click demo fill + sign in | ✅ lands on dashboard |
| Dashboard counters, answer-mode banner, recent documents/conversations | ✅ |
| Documents grid lists seeded files with pages/passages/size | ✅ |
| Drag-and-drop TXT upload | ✅ toast "indexed (1 passages)", card appears |
| Passage viewer modal | ✅ shows page chip and the indexed text |
| Paste-text modal → index | ✅ new document appears |
| Ask a question in chat | ✅ answer + citation chip with page number |
| Open citation | ✅ source drawer, highlighted words, page, match score |
| Highlight correctness | ✅ exactly `Orion`, `migration`, `start` were marked |
| Conversation history and follow-up | ✅ session stored and reopened |
| Admin login → Admin page | ✅ platform counters and user table |
| Console errors during the whole pass | ✅ none |

Deep links added after the pass: recent conversations open their session, and a document's **Ask** button
pre-scopes the chat to that document.

---

## 4. Defects found and fixed during the build

| # | Problem | Fix |
| --- | --- | --- |
| 1 | Backend did not compile: imported Jackson 2 (`com.fasterxml`) but Spring Boot 4 ships Jackson 3 | Imports switched to `tools.jackson.databind` |
| 2 | Offline answers began with the document's generic intro sentence | Extractive answerer now scores every sentence by question overlap and picks the best 1–3 |
| 3 | All-caps headings ("LEAVE AND NOTICE") leaked into quoted answers | Heading lines are skipped |
| 4 | Source highlighting marked the wrong words (off-by-one after removing the empty split segment) | Keep index parity before filtering empty segments |
| 5 | Upload with no `file` part returned a generic error | Added `MissingServletRequestPartException` / `MultipartException` handlers → `400` with a clear message |
| 6 | 415 message said "Send application/json" for a file endpoint | Message made generic |
| 7 | Passage modal briefly showed the previous document's counts | State reset when a new document is opened |
| 8 | Offline answers were cut mid-sentence when the source line wrapped (`"...per calendar year in"`) | Wrapped lines are rebuilt into whole paragraphs before sentence scoring |
| 9 | Offline answers added nearby sentences that shared no new keywords, and said "selected documents" when nothing was selected | Extra sentences must add a new question term; prefix changed to "From your documents:" |
| 10 | The Ask page briefly showed "You have no documents yet" with a disabled box while the document list was still loading | A loading flag keeps the banner and disabled state hidden until the list arrives |
| 11 | The source drawer could show a different part of the page than the sentence the answer quoted (it used the first place any keyword appeared) | The snippet now picks the window containing the most question terms |

---

## 5. Known limitations (state them honestly)

- **Scanned PDFs are not OCR'd.** Image-only PDFs return a clear `400`. Use text-based PDFs.
- **Retrieval is lexical (TF-IDF), not embeddings.** Great for policy/handbook wording; a paraphrase with
  no shared words can miss. This is the intended trade-off: explainable and dependency-free.
- **Follow-ups are retrieval-only.** Each question is searched independently; there is no chat-history
  rewriting before retrieval.
- **No background queue.** Indexing happens inside the upload request; fine at the 10 MB / 300 page limits.
- **Default JWT secret is in `application.properties`** for easy setup; change it for any real deployment.
- **H2 file database** is single-node; production would use Postgres (only `spring.datasource.*` changes).

---

## 6. How to reproduce

Backend (fresh database, spare port):

```powershell
cd backend
.\mvnw.cmd -DskipTests package
java -jar target\documind-backend-0.0.1-SNAPSHOT.jar `
  --server.port=8095 `
  "--spring.datasource.url=jdbc:h2:mem:verify;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
```

Then run the Postman collection in `postman/DocuMind.postman_collection.json` with `baseUrl` pointing at
that port, starting with **Auth → Login (demo)** and **Auth → Login (admin)**.

Frontend:

```powershell
cd frontend
npm install
npm run build
npm run dev
```
