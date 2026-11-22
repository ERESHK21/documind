import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { api, apiError } from '../lib/api';
import { downloadDocument } from '../lib/download';
import { fmtBytes, fmtDate } from '../lib/format';
import Modal from '../components/Modal';
import Empty from '../components/Empty';

const ACCEPT = '.pdf,.txt,.md,.markdown';

export default function Documents() {
  const [data, setData] = useState({ content: [], page: 0, totalPages: 0, totalElements: 0, last: true });
  const [query, setQuery] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState('');
  const [dragging, setDragging] = useState(false);
  const [pasteOpen, setPasteOpen] = useState(false);
  const [paste, setPaste] = useState({ name: '', content: '' });
  const [chunkDoc, setChunkDoc] = useState(null);
  const [chunks, setChunks] = useState({ content: [], totalElements: 0 });
  const [chunksLoading, setChunksLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const fileInput = useRef(null);

  async function load() {
    setLoading(true);
    try {
      const { data: response } = await api.get('/documents', { params: { q: search || undefined, page, size: 9 } });
      setData(response);
    } catch (error) {
      toast.error(apiError(error, 'Could not load documents'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, search]);

  useEffect(() => {
    const timer = setTimeout(() => {
      setPage(0);
      setSearch(query.trim());
    }, 300);
    return () => clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    if (!chunkDoc) return;
    setChunks({ content: [], totalElements: 0 });
    setChunksLoading(true);
    api
      .get(`/documents/${chunkDoc.id}/chunks`, { params: { size: 50 } })
      .then(({ data: response }) => setChunks(response))
      .catch((error) => toast.error(apiError(error, 'Could not load passages')))
      .finally(() => setChunksLoading(false));
  }, [chunkDoc]);

  async function uploadFiles(fileList) {
    const files = Array.from(fileList || []);
    if (!files.length) return;
    for (const file of files) {
      setUploading(file.name);
      const body = new FormData();
      body.append('file', file);
      try {
        const { data: created } = await api.post('/documents', body);
        toast.success(`${created.name} indexed (${created.chunkCount} passages)`);
      } catch (error) {
        toast.error(`${file.name}: ${apiError(error, 'upload failed')}`);
      }
    }
    setUploading('');
    if (fileInput.current) fileInput.current.value = '';
    load();
  }

  async function savePaste(event) {
    event.preventDefault();
    if (!paste.content.trim()) return;
    setSaving(true);
    try {
      const { data: created } = await api.post('/documents/text', {
        name: paste.name.trim() || 'Pasted note',
        content: paste.content,
      });
      toast.success(`${created.name} added`);
      setPaste({ name: '', content: '' });
      setPasteOpen(false);
      load();
    } catch (error) {
      toast.error(apiError(error, 'Could not save the text'));
    } finally {
      setSaving(false);
    }
  }

  async function remove(document) {
    if (!window.confirm(`Delete "${document.name}" and its passages? This cannot be undone.`)) return;
    try {
      await api.delete(`/documents/${document.id}`);
      toast.success('Document deleted');
      load();
    } catch (error) {
      toast.error(apiError(error, 'Could not delete'));
    }
  }

  async function download(document) {
    try {
      await downloadDocument(document.id, document.name);
    } catch (error) {
      toast.error(apiError(error, 'Original file is not available'));
    }
  }

  function onDrop(event) {
    event.preventDefault();
    setDragging(false);
    uploadFiles(event.dataTransfer.files);
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-indigo-600">Library</p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-slate-900">Your documents</h1>
          <p className="mt-1 text-sm text-slate-500">
            PDF, TXT or Markdown. Each upload is split into passages and indexed for search.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" className="btn-ghost" onClick={() => setPasteOpen(true)}>
            Paste text
          </button>
          <button type="button" className="btn-accent" onClick={() => fileInput.current?.click()}>
            Choose files
          </button>
          <input
            ref={fileInput}
            type="file"
            accept={ACCEPT}
            multiple
            className="hidden"
            onChange={(event) => uploadFiles(event.target.files)}
          />
        </div>
      </div>

      <div
        onDragOver={(event) => {
          event.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        className={`card flex flex-col items-center justify-center gap-2 border-dashed px-6 py-10 text-center transition ${
          dragging ? 'border-indigo-400 bg-indigo-50/60' : 'border-slate-300'
        }`}
      >
        <span className="text-3xl">📄</span>
        <p className="text-sm font-semibold text-slate-800">
          {uploading ? `Indexing ${uploading}…` : 'Drop files here'}
        </p>
        <p className="text-xs text-slate-500">PDF, TXT or Markdown · up to 10 MB · 300 pages max</p>
        {uploading ? (
          <span className="mt-1 inline-flex h-1.5 w-40 overflow-hidden rounded-full bg-slate-100">
            <span className="h-full w-1/2 animate-pulse rounded-full bg-indigo-500" />
          </span>
        ) : null}
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <input
          className="input sm:max-w-xs"
          placeholder="Search documents by name…"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
        <p className="text-xs text-slate-500">
          {data.totalElements} document{data.totalElements === 1 ? '' : 's'}
          {search ? ` matching "${search}"` : ''}
        </p>
      </div>

      {loading ? (
        <div className="card px-6 py-14 text-center text-sm text-slate-500">Loading documents…</div>
      ) : data.content.length === 0 ? (
        <Empty
          icon="🗂️"
          title={search ? 'No documents match that search' : 'No documents yet'}
          hint={
            search
              ? 'Try a shorter name, or clear the search box.'
              : 'Upload a PDF or paste some text, then ask a question about it.'
          }
          action={
            !search ? (
              <button type="button" className="btn-accent" onClick={() => fileInput.current?.click()}>
                Upload your first document
              </button>
            ) : null
          }
        />
      ) : (
        <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
          {data.content.map((document) => (
            <article key={document.id} className="card flex flex-col p-4">
              <div className="flex items-start justify-between gap-2">
                <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-lg">
                  {document.contentType?.includes('pdf') ? '📕' : '📝'}
                </span>
                <span className="chip bg-slate-100 text-slate-600">
                  {document.contentType?.includes('pdf') ? 'PDF' : 'Text'}
                </span>
              </div>
              <h2 className="mt-3 line-clamp-2 text-sm font-semibold leading-snug text-slate-900" title={document.name}>
                {document.name}
              </h2>
              <dl className="mt-3 grid grid-cols-3 gap-2 text-center">
                {[
                  ['Pages', document.pageCount],
                  ['Passages', document.chunkCount],
                  ['Size', fmtBytes(document.sizeBytes)],
                ].map(([label, value]) => (
                  <div key={label} className="rounded-lg bg-slate-50 px-1 py-1.5">
                    <dt className="text-[10px] uppercase tracking-wider text-slate-400">{label}</dt>
                    <dd className="text-xs font-semibold text-slate-700">{value}</dd>
                  </div>
                ))}
              </dl>
              <p className="mt-2 text-[11px] text-slate-400">Added {fmtDate(document.createdAt)}</p>
              <div className="mt-3 flex flex-wrap gap-2 border-t border-slate-100 pt-3">
                <button type="button" className="btn-ghost px-2.5 py-1.5 text-xs" onClick={() => setChunkDoc(document)}>
                  Passages
                </button>
                <button
                  type="button"
                  className="btn-ghost px-2.5 py-1.5 text-xs"
                  onClick={() => download(document)}
                  disabled={!document.hasOriginalFile}
                  title={document.hasOriginalFile ? 'Download the original file' : 'Pasted text has no file'}
                >
                  Original
                </button>
                <Link to="/chat" state={{ documentId: document.id }} className="btn-ghost px-2.5 py-1.5 text-xs">
                  Ask
                </Link>
                <button type="button" className="btn-danger ml-auto px-2.5 py-1.5 text-xs" onClick={() => remove(document)}>
                  Delete
                </button>
              </div>
            </article>
          ))}
        </div>
      )}

      {data.totalPages > 1 ? (
        <div className="flex items-center justify-between">
          <button type="button" className="btn-ghost" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>
            Previous
          </button>
          <span className="text-xs text-slate-500">
            Page {data.page + 1} of {data.totalPages}
          </span>
          <button
            type="button"
            className="btn-ghost"
            disabled={data.last}
            onClick={() => setPage((value) => value + 1)}
          >
            Next
          </button>
        </div>
      ) : null}

      <Modal
        open={pasteOpen}
        title="Paste text"
        subtitle="Useful for notes, emails or anything you do not have as a file."
        onClose={() => setPasteOpen(false)}
        footer={
          <>
            <button type="button" className="btn-ghost" onClick={() => setPasteOpen(false)}>
              Cancel
            </button>
            <button type="submit" form="paste-form" className="btn-accent" disabled={saving || !paste.content.trim()}>
              {saving ? 'Saving…' : 'Index text'}
            </button>
          </>
        }
      >
        <form id="paste-form" onSubmit={savePaste} className="space-y-3">
          <div>
            <label className="label" htmlFor="paste-name">
              Title
            </label>
            <input
              id="paste-name"
              className="input"
              placeholder="Meeting notes, 14 Nov"
              value={paste.name}
              onChange={(event) => setPaste({ ...paste, name: event.target.value })}
            />
          </div>
          <div>
            <label className="label" htmlFor="paste-content">
              Text
            </label>
            <textarea
              id="paste-content"
              className="input min-h-40 resize-y font-mono text-xs leading-6"
              placeholder="Paste the text to index…"
              value={paste.content}
              onChange={(event) => setPaste({ ...paste, content: event.target.value })}
            />
          </div>
        </form>
      </Modal>

      <Modal
        open={Boolean(chunkDoc)}
        wide
        title={chunkDoc?.name}
        subtitle={`${chunks.totalElements} searchable passage${chunks.totalElements === 1 ? '' : 's'}`}
        onClose={() => setChunkDoc(null)}
      >
        {chunksLoading ? (
          <p className="py-6 text-center text-sm text-slate-500">Loading passages…</p>
        ) : chunks.content.length === 0 ? (
          <p className="py-6 text-center text-sm text-slate-500">No passages stored for this document.</p>
        ) : (
          <ol className="space-y-3">
            {chunks.content.map((chunk, index) => (
              <li key={chunk.id} className="rounded-xl border border-slate-200 bg-slate-50/60 p-3">
                <div className="mb-2 flex items-center gap-2">
                  <span className="chip bg-indigo-600 text-white">Page {chunk.pageNumber}</span>
                  <span className="text-[11px] text-slate-400">
                    passage {index + 1} · {chunk.charCount} characters
                  </span>
                </div>
                <p className="whitespace-pre-wrap text-xs leading-6 text-slate-700">{chunk.content}</p>
              </li>
            ))}
          </ol>
        )}
      </Modal>
    </div>
  );
}
