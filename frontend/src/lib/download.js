import { api } from '../lib/api';

/** Downloads a document through axios so the JWT header is included. */
export async function downloadDocument(documentId, name) {
  const response = await api.get(`/documents/${documentId}/file`, { responseType: 'blob' });
  const url = URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = url;
  link.download = name || 'document';
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
