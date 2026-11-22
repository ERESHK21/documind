import toast from 'react-hot-toast';
import { highlightParts } from '../lib/format';
import { downloadDocument } from '../lib/download';

export default function SourceDrawer({ citation, terms, onClose }) {
  if (!citation) return null;
  const parts = highlightParts(citation.snippet, terms);

  async function download() {
    try {
      await downloadDocument(citation.documentId, citation.documentName);
    } catch {
      toast.error('Could not download that file');
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-slate-900/30 backdrop-blur-sm">
      <button type="button" className="flex-1 cursor-default" aria-label="Close source" onClick={onClose} />
      <aside className="flex h-full w-full max-w-md flex-col border-l border-slate-200 bg-white shadow-2xl">
        <div className="flex items-start justify-between gap-3 border-b border-slate-100 px-5 py-4">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-wider text-indigo-600">Cited source</p>
            <h3 className="mt-1 text-base font-semibold leading-snug text-slate-900">{citation.documentName}</h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
            aria-label="Close"
          >
            <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M6 6l12 12M18 6L6 18" strokeLinecap="round" />
            </svg>
          </button>
        </div>

        <div className="flex items-center gap-2 border-b border-slate-100 bg-slate-50 px-5 py-3">
          <span className="chip bg-indigo-600 text-white">Page {citation.pageNumber}</span>
          <span className="chip bg-white text-slate-600 ring-1 ring-inset ring-slate-200">
            match score {Number(citation.score || 0).toFixed(2)}
          </span>
        </div>

        <div className="flex-1 overflow-y-auto px-5 py-5">
          <p className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">Highlighted passage</p>
          <blockquote className="mt-3 rounded-2xl border border-slate-200 bg-amber-50/40 px-4 py-4 text-[15px] leading-7 text-slate-800">
            {parts.map((part, index) =>
              part.hit ? (
                <mark key={index} className="mark">
                  {part.text}
                </mark>
              ) : (
                <span key={index}>{part.text}</span>
              ),
            )}
          </blockquote>
          <p className="mt-4 text-xs leading-relaxed text-slate-500">
            This is the passage DocuMind retrieved and used to write the answer. Highlighted words match your
            question.
          </p>
        </div>

        <div className="border-t border-slate-100 bg-slate-50 px-5 py-3">
          <button type="button" onClick={download} className="btn-ghost w-full">
            Download original file
          </button>
        </div>
      </aside>
    </div>
  );
}
