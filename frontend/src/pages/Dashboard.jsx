import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../lib/api';
import { fmtNumber, relativeTime } from '../lib/format';
import { useAuth } from '../context/AuthContext';
import StatCard from '../components/StatCard';

export default function Dashboard() {
  const { user } = useAuth();
  const [health, setHealth] = useState(null);
  const [stats, setStats] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [sessions, setSessions] = useState([]);

  useEffect(() => {
    api.get('/health').then(({ data }) => setHealth(data)).catch(() => setHealth(null));
    api.get('/stats/me').then(({ data }) => setStats(data)).catch(() => setStats(null));
    api.get('/documents?size=4').then(({ data }) => setDocuments(data.content)).catch(() => setDocuments([]));
    api.get('/chat/sessions?size=4').then(({ data }) => setSessions(data.content)).catch(() => setSessions([]));
  }, []);

  const hasDocuments = (stats?.documents || 0) > 0;

  return (
    <div className="space-y-7">
      <section className="card relative overflow-hidden p-6 sm:p-8">
        <div className="absolute -right-20 -top-24 h-64 w-64 rounded-full bg-indigo-500/10 blur-3xl" />
        <div className="relative max-w-2xl">
          <p className="text-xs font-semibold uppercase tracking-wider text-indigo-600">Overview</p>
          <h1 className="mt-2 text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
            Welcome back, {user?.name?.split(' ')[0]}
          </h1>
          <p className="mt-2 text-sm leading-relaxed text-slate-600">
            Upload a document, ask a question, and open the citation to see the exact page the answer came from.
          </p>
          <div className="mt-5 flex flex-wrap gap-2">
            <Link to="/documents" className="btn-accent">
              Upload a document
            </Link>
            <Link to="/chat" className="btn-ghost">
              Ask a question
            </Link>
          </div>
        </div>
      </section>

      <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Documents" value={fmtNumber(stats?.documents)} hint="uploaded by you" />
        <StatCard label="Passages indexed" value={fmtNumber(stats?.chunks)} hint="searchable chunks" accent="emerald" />
        <StatCard label="Questions asked" value={fmtNumber(stats?.questions)} hint="across your chats" accent="amber" />
        <StatCard
          label="Answers"
          value={fmtNumber((stats?.aiAnswers || 0) + (stats?.offlineAnswers || 0))}
          hint={`${fmtNumber(stats?.aiAnswers)} AI · ${fmtNumber(stats?.offlineAnswers)} offline`}
          accent="slate"
        />
      </section>

      <section
        className={`card flex flex-col gap-3 p-5 sm:flex-row sm:items-center sm:justify-between ${
          health?.aiConfigured ? 'border-indigo-200 bg-indigo-50/40' : 'border-amber-200 bg-amber-50/40'
        }`}
      >
        <div>
          <p className="text-sm font-semibold text-slate-900">
            {health?.aiConfigured ? 'AI answers are switched on' : 'Running in offline mode'}
          </p>
          <p className="mt-1 text-sm text-slate-600">
            {health?.aiConfigured
              ? `Answers are written by ${health.aiModel} using only the passages retrieved from your documents.`
              : 'No AI key is configured, so DocuMind answers by quoting the best matching passages. Every answer is still cited.'}
          </p>
        </div>
        <code className="shrink-0 rounded-xl bg-white px-3 py-2 text-[11px] text-slate-500 ring-1 ring-inset ring-slate-200">
          {health?.aiConfigured ? 'mode: AI + retrieval' : 'mode: extractive + retrieval'}
        </code>
      </section>

      <section className="grid gap-4 lg:grid-cols-2">
        <div className="card p-5">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-500">Recent documents</h2>
            <Link to="/documents" className="text-xs font-semibold text-indigo-600 hover:underline">
              View all
            </Link>
          </div>
          {documents.length === 0 ? (
            <p className="mt-4 rounded-xl border border-dashed border-slate-200 px-4 py-6 text-center text-sm text-slate-500">
              No documents yet. Upload a PDF or paste text to get started.
            </p>
          ) : (
            <ul className="mt-3 divide-y divide-slate-100">
              {documents.map((document) => (
                <li key={document.id} className="flex items-center justify-between gap-3 py-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-slate-800">{document.name}</p>
                    <p className="mt-0.5 text-xs text-slate-500">
                      {document.pageCount} page{document.pageCount === 1 ? '' : 's'} ·{' '}
                      {document.chunkCount} passage{document.chunkCount === 1 ? '' : 's'} ·{' '}
                      {relativeTime(document.createdAt)}
                    </p>
                  </div>
                  <span className="chip shrink-0 bg-slate-100 text-slate-600">
                    {document.contentType?.includes('pdf') ? 'PDF' : 'Text'}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="card p-5">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-500">Recent conversations</h2>
            <Link to="/chat" className="text-xs font-semibold text-indigo-600 hover:underline">
              Open chat
            </Link>
          </div>
          {sessions.length === 0 ? (
            <p className="mt-4 rounded-xl border border-dashed border-slate-200 px-4 py-6 text-center text-sm text-slate-500">
              No conversations yet. Ask your first question.
            </p>
          ) : (
            <ul className="mt-3 divide-y divide-slate-100">
              {sessions.map((session) => (
                <li key={session.id} className="py-3">
                  <Link to="/chat" state={{ sessionId: session.id }} className="block">
                    <p className="truncate text-sm font-medium text-slate-800">{session.title}</p>
                    <p className="mt-0.5 text-xs text-slate-500">
                      {session.messageCount} message{session.messageCount === 1 ? '' : 's'} ·{' '}
                      {relativeTime(session.lastActivityAt)}
                    </p>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>

      <section className="grid gap-3 sm:grid-cols-3">
        {[
          ['1. Index', 'The upload is split into overlapping passages and stored with its page number.'],
          ['2. Retrieve', 'Your question is scored against every passage, and the best few are kept.'],
          ['3. Answer', 'The AI writes from those passages only, or offline mode quotes them directly.'],
        ].map(([title, body]) => (
          <div key={title} className="card p-4">
            <p className="text-sm font-semibold text-slate-900">{title}</p>
            <p className="mt-1 text-xs leading-relaxed text-slate-500">{body}</p>
          </div>
        ))}
      </section>

      {!hasDocuments ? (
        <p className="text-center text-xs text-slate-400">
          Tip: the demo account already has four seeded documents, so you can try the chat immediately.
        </p>
      ) : null}
    </div>
  );
}
