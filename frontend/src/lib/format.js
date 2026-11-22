export function fmtDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
}

export function fmtDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleString(undefined, {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function relativeTime(value) {
  if (!value) return '';
  const diff = Date.now() - new Date(value).getTime();
  const minutes = Math.round(diff / 60000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.round(hours / 24);
  if (days < 30) return `${days}d ago`;
  return fmtDate(value);
}

export function initials(name = '') {
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('');
}

export function fmtBytes(bytes) {
  if (!bytes && bytes !== 0) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function fmtNumber(value) {
  if (value === null || value === undefined) return '0';
  return new Intl.NumberFormat().format(value);
}

/** Splits text into [{ text, hit }] pieces so matched words can be highlighted. */
export function highlightParts(text = '', terms = []) {
  const usable = (terms || [])
    .map((term) => String(term).trim())
    .filter((term) => term.length > 2)
    .sort((left, right) => right.length - left.length);

  if (!usable.length || !text) {
    return [{ text, hit: false }];
  }

  const escaped = usable.map((term) => term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|');
  const pattern = new RegExp(`(${escaped})`, 'gi');
  return text
    .split(pattern)
    .map((part, index) => ({ text: part, hit: index % 2 === 1 }))
    .filter((part) => part.text !== '');
}

const STOPWORDS = new Set([
  'the', 'and', 'for', 'with', 'that', 'this', 'what', 'when', 'where', 'which', 'who',
  'why', 'how', 'does', 'did', 'are', 'was', 'were', 'has', 'have', 'had', 'from', 'into',
  'about', 'their', 'there', 'they', 'them', 'our', 'you', 'your', 'can', 'could',
  'should', 'would', 'will', 'get', 'got', 'give', 'many', 'much', 'per', 'its', 'his', 'her',
]);

export function questionTerms(question = '') {
  return Array.from(
    new Set(
      question
        .toLowerCase()
        .replace(/[^a-z0-9\s]/g, ' ')
        .split(/\s+/)
        .filter((word) => word.length > 2 && !STOPWORDS.has(word)),
    ),
  );
}
