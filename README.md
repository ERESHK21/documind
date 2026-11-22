# DocuMind

**Upload your documents, ask a question in plain English, and get an answer with the exact page it came from.**

DocuMind is a small, complete RAG (Retrieval-Augmented Generation) app: Spring Boot + React, JWT auth,
PDF text extraction, passage search and cited answers. It runs with **no AI key** (offline mode), so a
demo never breaks — and with a free Gemini key it writes smoother answers from the same retrieved text.

---

## 1. What it does

| Step | What happens |
| --- | --- |
| Upload | A PDF, TXT or Markdown file is stored and its text is extracted page by page |
| Index | Each page is split into overlapping passages ("chunks") with its page number |
| Ask | The question is scored against every passage you own, and the top few are kept |
| Answer | The AI (or offline quoter) answers using only those passages |
| Cite | Every answer lists sources; clicking one opens the passage with your words highlighted and the page number |

Demo flow:

1. Sign in as `demo@documind.dev` / `Password123!`.
2. Open **Ask** and try: *How many paid leave days do full-time employees get?*
3. Click the citation under the answer — the source drawer opens on **page 1** of the Employee Handbook
   with *paid*, *leave*, *days* highlighted.

---

## 2. Tech stack

| Layer | Choice |
| --- | --- |
| Backend | Java 21, Spring Boot 4 (Web MVC, Data JPA, Security), Maven wrapper |
| Auth | JWT (HS256) + BCrypt passwords, roles `USER` / `ADMIN` |
| Database | H2 file database (zero setup). Swap to Postgres by overriding `spring.datasource.*` |
| PDF text | Apache PDFBox 3 |
| AI | Google Gemini `generateContent` (optional) with a built-in offline extractive answerer |
| Frontend | React 19, Vite 7, Tailwind CSS 4, React Router, Axios, react-hot-toast |
| API testing | Postman collection (25 requests with assertions) |

No vector database, no model training, no Python service — retrieval is a readable TF-IDF scorer in
`backend/src/main/java/com/documind/search/RetrievalService.java`.

---

## 3. Run it

### Backend (port 8080)

**Eclipse:** `File > Import > Maven > Existing Maven Projects` → select the `backend` folder → Run
`DocumindApplication.java` as a Java Application.

**Command line:**

```bash
cd backend
mvnw.cmd -DskipTests package        # macOS/Linux: ./mvnw -DskipTests package
java -jar target/documind-backend-0.0.1-SNAPSHOT.jar
```

First start creates `backend/data/documind.mv.db` and seeds the demo accounts and documents.
Health check: <http://localhost:8080/api/health>

### Frontend (port 5173)

```bash
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173>. Vite proxies `/api` to `http://localhost:8080`.

### Demo accounts

| Email | Password | Role |
| --- | --- | --- |
| `demo@documind.dev` | `Password123!` | USER — 3 seeded documents |
| `riya@documind.dev` | `Password123!` | USER — 1 seeded document (Travel Policy) |
| `admin@documind.dev` | `Password123!` | ADMIN — platform stats and user list |

Any new account starts empty, which is useful for showing upload → index → ask yourself.

---

## 4. AI mode vs offline mode

The app checks the key at startup and reports the mode on `/api/health` and in the UI.

**Offline (default, no key):** answers quote the strongest retrieved sentences, still with citations.
This is the safest option for a college demo or viva.

**With Gemini (free tier):**

1. Get a key at <https://aistudio.google.com/app/apikey>.
2. Set it before starting the backend:

```powershell
# PowerShell
$env:GEMINI_API_KEY = "your-key-here"
```

```bash
# bash
export GEMINI_API_KEY="your-key-here"
```

Or put it in `backend/src/main/resources/application.properties`:
`app.ai.api-key=your-key-here` (do not commit a real key).

If the key is missing, invalid, rate-limited or the call times out, DocuMind silently falls back to offline
answers — it never returns an error page for that.

Useful knobs in `application.properties`:

| Property | Default | Meaning |
| --- | --- | --- |
| `app.rag.chunk-size` | 800 | target characters per passage |
| `app.rag.chunk-overlap` | 120 | characters repeated between passages |
| `app.rag.top-k` | 5 | passages retrieved per question |
| `app.ai.model` | `gemini-2.5-flash` | Gemini model name |
| `app.ai.provider` | `auto` | `auto`, `gemini` or `offline` |

---

## 5. API map

All responses are JSON. Authenticated calls need `Authorization: Bearer <token>`.

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| GET | `/api/health` | — | Service status and answer mode |
| POST | `/api/auth/register` | — | Create account (201) |
| POST | `/api/auth/login` | — | Get JWT |
| GET | `/api/auth/me` | user | Current profile |
| GET | `/api/documents?q=&page=&size=` | user | List own documents (paged) |
| POST | `/api/documents` | user | Upload `multipart/form-data` field `file` (201) |
| POST | `/api/documents/text` | user | Create from pasted text (201) |
| GET | `/api/documents/{id}` | owner | One document |
| GET | `/api/documents/{id}/chunks` | owner | Indexed passages with page numbers |
| GET | `/api/documents/{id}/file` | owner | Download the original file |
| DELETE | `/api/documents/{id}` | owner | Delete document and passages (204) |
| POST | `/api/chat/ask` | user | Ask a question, optionally scoped with `documentIds` and continued with `sessionId` |
| GET | `/api/chat/sessions` | user | Conversation history |
| GET | `/api/chat/sessions/{id}` | owner | Full conversation with citations |
| DELETE | `/api/chat/sessions/{id}` | owner | Delete a conversation (204) |
| GET | `/api/stats/me` | user | Personal counters |
| GET | `/api/admin/stats` | admin | Platform counters |
| GET | `/api/admin/users` | admin | All users with document/chat counts |

### Example: ask a question

```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"question":"What is the notice period for managers?"}'
```

```json
{
  "sessionId": 1,
  "sessionTitle": "What is the notice period for managers?",
  "answer": {
    "role": "ASSISTANT",
    "mode": "OFFLINE",
    "latencyMs": 22,
    "citations": [
      {
        "documentId": 2,
        "documentName": "Employee Handbook 2026",
        "pageNumber": 1,
        "snippet": "...The notice period is 30 days for full-time employees and 60 days for managers...",
        "score": 2.03
      }
    ]
  }
}
```

Errors use one shape: `{ "timestamp", "status", "error", "message", "details?" }`.
Status codes: `400` validation, `401` no/invalid token, `403` wrong role, `404` not found or not yours,
`405` wrong method, `413` file too large, `415` wrong content type.

---

## 6. Project layout

```
DocuMind/
├── backend/
│   ├── src/main/java/com/documind/
│   │   ├── auth/         JWT service, filter, login/register
│   │   ├── chat/         conversations, messages, citations, ask endpoint
│   │   ├── document/     upload, PDF extraction, chunking, passage API
│   │   ├── search/       tokenizer + TF-IDF retrieval (the "R" in RAG)
│   │   ├── ai/           Gemini client + offline extractive answerer
│   │   ├── stats/        personal and admin counters
│   │   ├── admin/        user list for admins
│   │   └── seed/         demo accounts and documents
│   └── src/main/resources/application.properties
├── frontend/
│   └── src/
│       ├── pages/        Login, Register, Dashboard, Documents, Chat, Admin
│       ├── components/   Navbar, Modal, SourceDrawer, ModeBadge, StatCard
│       ├── context/      AuthContext
│       └── lib/          axios client, formatting, highlighted-search helper
├── postman/DocuMind.postman_collection.json
├── README.md
└── TEST_RESULTS.md
```

---

## 7. Postman

Import `postman/DocuMind.postman_collection.json` and set `baseUrl` to `http://localhost:8080`.

Run in this order:

1. **Auth → Login (demo)** — stores the JWT in `{{token}}`.
2. **Auth → Login (admin)** — stores the admin JWT in `{{adminToken}}`.
3. Any other folder. Requests save `{{docId}}` and `{{sessionId}}` as they go.

The upload request uses a file body; pick any PDF/TXT/MD before sending. Everything else is ready to run.
25 requests include assertions for status codes, citation pages, validation and permission errors.

---

## 8. Limits and troubleshooting

| Symptom | Fix |
| --- | --- |
| `Port 8080 was already in use` | Stop the other backend, or run `--server.port=8081` and change the Vite proxy |
| `Invalid email or password` | Passwords are case-sensitive; demo users use `Password123!` |
| PDF upload says "Could not read this PDF" | Scanned PDFs are images. Use a text-based PDF (or run OCR first) |
| Answer says it could not find the fact | Ask about a document you actually uploaded; use shorter, keyword-rich questions |
| `data` folder locked on Windows | Stop the running backend before rebuilding (H2 keeps the file open) |
| Frontend calls fail with 401 | Token expired (24h) — sign in again |

Upload limits: 10 MB per file, 300 pages, 500,000 characters. Supported: `.pdf`, `.txt`, `.md`, `.markdown`.

---

## 9. How to explain it in a viva (30 seconds)

> DocuMind is a RAG app: Retrieval-Augmented Generation. When a document is uploaded, the backend pulls the
> text out page by page and cuts it into overlapping passages. When you ask a question, the same words in the
> question and each passage are compared — this is a TF-IDF score — and only the best passages are sent
> forward. The AI model is told to answer *only* from those passages and to number its sources, so every
> answer can be traced to a page. If no AI key is configured, the app quotes the best sentences instead,
> which is why it still works offline. There is no training and no vector database: the intelligence is in
> the retrieval, and the model only writes the sentence.

Common follow-up answers:

- **Why chunks?** One big document would exceed the model context and hide the relevant sentence. Small
  passages make matching precise, and a 120-character overlap keeps sentences that straddle a cut readable.
- **Why page numbers?** So the UI can highlight the exact page, which is the trust feature of the app.
- **What stops hallucination?** The prompt allows only the retrieved context, the model must cite `[1]`, and
  the backend stores the retrieved snippets next to the answer so the user can verify them.
- **Where is the AI?** One HTTP call in `GeminiAnswerer`. Swapping providers means changing that class only.
