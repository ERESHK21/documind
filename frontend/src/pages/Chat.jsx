import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import toast from 'react-hot-toast';
import { api, apiError } from '../lib/api';
import { questionTerms, relativeTime } from '../lib/format';
import ModeBadge from '../components/ModeBadge';
import Modal from '../components/Modal';
import SourceDrawer from '../components/SourceDrawer';

const SUGGESTIONS = [
  'How many paid leave days do full-time employees get?',
  'What is the notice period for managers?',
  'What are the support hours?',
  'How long does a password reset link last?',
];

export default function Chat() {
  const location = useLocation();
  const [documents, setDocuments] = useState([]);
  const [docsLoading, setDocsLoading] = useState(true);
  const [sessions, setSessions] = useState([]);
  const [activeId, setActiveId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [scope, setScope] = useState([]);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const [loadingSession, setLoadingSession] = useState(false);
  const [source, setSource] = useState(null);
  const [historyOpen, setHistoryOpen] = useState(false);
  const bottomRef = useRef(null);
  const inputRef = useRef(null);

  useEffect(() => {
    api
      .get('/documents?size=100')
      .then(({ data }) => setDocuments(data.content))
      .catch(() => setDocuments([]))
      .finally(() => setDocsLoading(false));
    loadSessions();
    const state = location.state || {};
    if (state.sessionId) {
      openSession(state.sessionId);
    } else if (state.documentId) {
      setScope([state.documentId]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [messages, busy]);

  function loadSessions() {
    api
      .get('/chat/sessions?size=30')
      .then(({ data }) => setSessions(data.content))
      .catch(() => setSessions([]));
  }

  async function openSession(id) {
    setHistoryOpen(false);
    setLoadingSession(true);
    try {
      const { data } = await api.get(`/chat/sessions/${id}`);
      setActiveId(data.id);
      setMessages(data.messages || []);
    } catch (error) {
      toast.error(apiError(error, 'Could not open that conversation'));
    } finally {
      setLoadingSession(false);
    }
  }

  function newChat() {
    setActiveId(null);
    setMessages([]);
    setHistoryOpen(false);
    inputRef.current?.focus();
  }

  async function removeSession(id, event) {
    event.stopPropagation();
    if (!window.confirm('Delete this conversation?')) return;
    try {
      await api.delete(`/chat/sessions/${id}`);
      if (id === activeId) newChat();
      loadSessions();
      toast.success('Conversation deleted');
    } catch (error) {
      toast.error(apiError(error, 'Could not delete the conversation'));
    }
  }

  async function ask(question) {
    const text = (question ?? input).trim();
    if (!text || busy) return;
    setBusy(true);
    setInput('');

    const optimistic = {
      id: `pending-${Date.now()}`,
      role: 'USER',
      content: text,
      createdAt: new Date().toISOString(),
      citations: [],
    };
    setMessages((current) => [...current, optimistic]);

    try {
      const { data } = await api.post('/chat/ask', {
        question: text,
        documentIds: scope,
        sessionId: activeId,
      });
      setActiveId(data.sessionId);
      setMessages((current) => [
        ...current.filter((message) => message.id !== optimistic.id),
        data.userMessage,
        data.answer,
      ]);
      loadSessions();
    } catch (error) {
      setMessages((current) => current.filter((message) => message.id !== optimistic.id));
      setInput(text);
      toast.error(apiError(error, 'Could not answer that question'));
    } finally {
      setBusy(false);
    }
  }

  function onKeyDown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      ask();
    }
  }

  function toggleScope(id) {
    setScope((current) => (current.includes(id) ? current.filter((value) => value !== id) : [...current, id]));
  }

  const scopeLabel = docsLoading
    ? 'loading your documents…'
    : scope.length === 0
      ? `All documents (${documents.length})`
      : `${scope.length} selected`;

  const questionsByMessage = useMemo(() => {
    const map = new Map();
    let lastQuestion = '';
    for (const message of messages) {
      if (message.role === 'USER') {
        lastQuestion = message.content;
      } else {
        map.set(message.id, lastQuestion);
      }
    }
    return map;
  }, [messages]);

  return (
    <div className="grid gap-4 lg:grid-cols-[250px,minmax(0,1fr)]">
      <aside className="card hidden h-fit p-3 lg:block">
        <button type="button" className="btn-accent w-full" onClick={newChat}>
          + New question
        </button>
        <p className="mt-4 px-1 text-[11px] font-semibold uppercase tracking-wider text-slate-400">History</p>
        <div className="mt-1 max-h-[420px] space-y-1 overflow-y-auto">
          {sessions.length === 0 ? (
            <p className="px-1 py-2 text-xs text-slate-500">No conversations yet.</p>
          ) : (
            sessions.map((session) => (
              <div
                key={session.id}
                className={`group flex items-start gap-1 rounded-xl px-2 py-2 transition ${
                  session.id === activeId ? 'bg-indigo-50' : 'hover:bg-slate-50'
                }`}
              >
                <button type="button" className="min-w-0 flex-1 text-left" onClick={() => openSession(session.id)}>
                  <p className="truncate text-xs font-semibold text-slate-800">{session.title}</p>
                  <p className="mt-0.5 text-[10px] text-slate-400">
                    {session.messageCount} msg · {relativeTime(session.lastActivityAt)}
                  </p>
                </button>
                <button
                  type="button"
                  onClick={(event) => removeSession(session.id, event)}
                  className="rounded p-1 text-slate-300 opacity-0 transition hover:bg-rose-50 hover:text-rose-500 group-hover:opacity-100"
                  aria-label="Delete conversation"
                >
                  <svg viewBox="0 0 24 24" className="h-3.5 w-3.5" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M6 6l12 12M18 6L6 18" strokeLinecap="round" />
                  </svg>
                </button>
              </div>
            ))
          )}
        </div>
        <p className="mt-3 border-t border-slate-100 pt-3 px-1 text-[11px] leading-relaxed text-slate-400">
          DocuMind only searches the documents you own.
        </p>
      </aside>

      <section className="card flex min-h-[600px] flex-col overflow-hidden">
        <header className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 px-4 py-3">
          <div>
            <h1 className="text-sm font-semibold text-slate-900">Ask your documents</h1>
            <p className="text-[11px] text-slate-500">
              Searching: <span className="font-semibold text-indigo-600">{scopeLabel}</span>
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button type="button" className="btn-ghost px-2.5 py-1.5 text-xs lg:hidden" onClick={() => setHistoryOpen(true)}>
              History
            </button>
            <button type="button" className="btn-ghost px-2.5 py-1.5 text-xs" onClick={newChat}>
              New
            </button>
          </div>
        </header>

        {documents.length > 0 ? (
          <div className="flex gap-1.5 overflow-x-auto border-b border-slate-100 bg-slate-50/60 px-4 py-2">
            <button
              type="button"
              onClick={() => setScope([])}
              className={`chip shrink-0 ring-1 ring-inset transition ${
                scope.length === 0
                  ? 'bg-slate-900 text-white ring-slate-900'
                  : 'bg-white text-slate-600 ring-slate-200 hover:bg-slate-100'
              }`}
            >
              All documents ({documents.length})
            </button>
            {documents.map((document) => {
              const selected = scope.includes(document.id);
              return (
                <button
                  key={document.id}
                  type="button"
                  onClick={() => toggleScope(document.id)}
                  title={document.name}
                  className={`chip max-w-[220px] shrink-0 truncate ring-1 ring-inset transition ${
                    selected
                      ? 'bg-indigo-600 text-white ring-indigo-600'
                      : 'bg-white text-slate-600 ring-slate-200 hover:bg-indigo-50'
                  }`}
                >
                  {selected ? '✓ ' : ''}
                  {document.name}
                </button>
              );
            })}
          </div>
        ) : !docsLoading ? (
          <div className="border-b border-amber-100 bg-amber-50 px-4 py-2 text-xs text-amber-800">
            You have no documents yet.{' '}
            <Link to="/documents" className="font-semibold underline">
              Upload one first
            </Link>
            .
          </div>
        ) : null}

        <div className="flex-1 space-y-4 overflow-y-auto px-4 py-5">
          {loadingSession ? (
            <p className="py-10 text-center text-sm text-slate-500">Opening conversation…</p>
          ) : messages.length === 0 ? (
            <div className="mx-auto max-w-lg py-6 text-center">
              <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-600 to-violet-500 text-xl font-black text-white">
                D
              </span>
              <h2 className="mt-4 text-lg font-bold text-slate-900">What should DocuMind look up?</h2>
              <p className="mt-1 text-sm text-slate-500">
                Answers are written only from your documents, and every answer shows its source page.
              </p>
              <div className="mt-5 grid gap-2 text-left">
                {SUGGESTIONS.map((suggestion) => (
                  <button
                    key={suggestion}
                    type="button"
                    onClick={() => ask(suggestion)}
                    className="rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-left text-sm text-slate-700 transition hover:border-indigo-300 hover:bg-indigo-50"
                  >
                    {suggestion}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            messages.map((message) => {
              const mine = message.role === 'USER';
              return (
                <div key={message.id} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
                  <div className={`flex max-w-[85%] gap-2 ${mine ? 'flex-row-reverse' : ''}`}>
                    {!mine ? (
                      <span className="mt-1 flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-indigo-600 to-violet-500 text-[11px] font-black text-white">
                        D
                      </span>
                    ) : null}
                    <div>
                      <div
                        className={`rounded-2xl px-4 py-3 text-sm leading-relaxed ${
                          mine
                            ? 'rounded-tr-sm bg-slate-900 text-white'
                            : 'rounded-tl-sm border border-slate-200 bg-white text-slate-800 shadow-sm'
                        }`}
                      >
                        <p className="whitespace-pre-wrap">{message.content}</p>
                      </div>

                      {!mine && message.citations?.length ? (
                        <div className="mt-2 space-y-1.5">
                          {message.citations.map((citation, index) => (
                            <button
                              key={`${citation.documentId}-${citation.pageNumber}-${index}`}
                              type="button"
                              onClick={() =>
                                setSource({
                                  ...citation,
                                  terms: questionTerms(questionsByMessage.get(message.id) || ''),
                                })
                              }
                              className="flex w-full items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-2.5 py-2 text-left transition hover:border-indigo-300 hover:bg-indigo-50"
                            >
                              <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-md bg-indigo-600 text-[10px] font-bold text-white">
                                {index + 1}
                              </span>
                              <span className="min-w-0 flex-1">
                                <span className="block truncate text-[11px] font-semibold text-slate-700">
                                  {citation.documentName}
                                </span>
                                <span className="block text-[10px] text-slate-500">page {citation.pageNumber}</span>
                              </span>
                              <span className="shrink-0 text-[10px] font-semibold text-indigo-600">Open source</span>
                            </button>
                          ))}
                        </div>
                      ) : null}

                      {!mine ? (
                        <div className="mt-1.5 flex items-center gap-2">
                          <ModeBadge mode={message.mode} latencyMs={message.latencyMs} />
                          <span className="text-[10px] text-slate-400">{relativeTime(message.createdAt)}</span>
                        </div>
                      ) : null}
                    </div>
                  </div>
                </div>
              );
            })
          )}

          {busy ? (
            <div className="flex justify-start">
              <div className="flex items-center gap-2 rounded-2xl rounded-tl-sm border border-slate-200 bg-white px-4 py-3 shadow-sm">
                <span className="flex gap-1">
                  {[0, 1, 2].map((dot) => (
                    <span
                      key={dot}
                      className="h-1.5 w-1.5 animate-bounce rounded-full bg-indigo-400"
                      style={{ animationDelay: `${dot * 120}ms` }}
                    />
                  ))}
                </span>
                <span className="text-xs text-slate-500">Searching your documents…</span>
              </div>
            </div>
          ) : null}
          <div ref={bottomRef} />
        </div>

        <div className="border-t border-slate-100 bg-white px-4 py-3">
          <div className="flex items-end gap-2">
            <textarea
              ref={inputRef}
              rows={2}
              className="input max-h-32 min-h-[46px] resize-none py-2.5"
              placeholder={
                docsLoading
                  ? 'Loading your documents…'
                  : documents.length === 0
                    ? 'Upload a document first…'
                    : 'Ask a question about your documents… (Enter to send)'
              }
              value={input}
              onChange={(event) => setInput(event.target.value)}
              onKeyDown={onKeyDown}
              disabled={docsLoading || documents.length === 0}
            />
            <button
              type="button"
              className="btn-accent h-[46px] px-4"
              onClick={() => ask()}
              disabled={busy || docsLoading || !input.trim() || documents.length === 0}
            >
              {busy ? '…' : 'Ask'}
            </button>
          </div>
          <div className="mt-2 flex items-center justify-between text-[10px] text-slate-400">
            <span>Shift + Enter for a new line</span>
            <span>
              {scope.length > 0 ? `Scoped to ${scope.length} document(s)` : 'Searching all your documents'}
            </span>
          </div>
        </div>
      </section>

      <Modal open={historyOpen} title="Conversation history" onClose={() => setHistoryOpen(false)}>
        {sessions.length === 0 ? (
          <p className="py-4 text-sm text-slate-500">No conversations yet.</p>
        ) : (
          <div className="space-y-1">
            {sessions.map((session) => (
              <div
                key={session.id}
                className={`flex items-center gap-2 rounded-xl px-2 py-2 ${
                  session.id === activeId ? 'bg-indigo-50' : 'hover:bg-slate-50'
                }`}
              >
                <button type="button" className="min-w-0 flex-1 text-left" onClick={() => openSession(session.id)}>
                  <p className="truncate text-sm font-medium text-slate-800">{session.title}</p>
                  <p className="text-xs text-slate-500">
                    {session.messageCount} messages · {relativeTime(session.lastActivityAt)}
                  </p>
                </button>
                <button type="button" className="text-xs text-rose-500" onClick={(event) => removeSession(session.id, event)}>
                  Delete
                </button>
              </div>
            ))}
          </div>
        )}
      </Modal>

      <SourceDrawer citation={source} terms={source?.terms || []} onClose={() => setSource(null)} />
    </div>
  );
}
