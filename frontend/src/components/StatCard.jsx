export default function StatCard({ label, value, hint, accent = 'indigo' }) {
  const tones = {
    indigo: 'from-indigo-500/10 to-indigo-500/0 text-indigo-600',
    emerald: 'from-emerald-500/10 to-emerald-500/0 text-emerald-600',
    amber: 'from-amber-500/10 to-amber-500/0 text-amber-600',
    slate: 'from-slate-500/10 to-slate-500/0 text-slate-600',
  };

  return (
    <div className={`card bg-gradient-to-br p-4 ${tones[accent] || tones.indigo}`}>
      <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">{label}</p>
      <p className="mt-2 text-2xl font-bold text-slate-900">{value}</p>
      {hint ? <p className="mt-1 text-xs text-slate-500">{hint}</p> : null}
    </div>
  );
}
