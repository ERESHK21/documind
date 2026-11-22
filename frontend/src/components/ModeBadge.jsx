export default function ModeBadge({ mode, latencyMs }) {
  const ai = mode === 'AI';
  return (
    <span className="inline-flex items-center gap-1.5">
      <span
        className={`chip ${
          ai ? 'bg-indigo-50 text-indigo-700' : 'bg-amber-50 text-amber-700'
        }`}
      >
        <span className={`h-1.5 w-1.5 rounded-full ${ai ? 'bg-indigo-500' : 'bg-amber-500'}`} />
        {ai ? 'AI answer' : 'Offline answer'}
      </span>
      {latencyMs ? <span className="text-[11px] text-slate-400">{latencyMs} ms</span> : null}
    </span>
  );
}
