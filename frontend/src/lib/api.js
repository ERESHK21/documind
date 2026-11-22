import axios from 'axios';

// In development this is empty, so requests go to /api and Vite proxies them to
// localhost:8080. In a deployed build set VITE_API_URL to the backend's origin.
export const API_ORIGIN = (import.meta.env.VITE_API_URL || '').replace(/\/+$/, '');

export const api = axios.create({ baseURL: `${API_ORIGIN}/api` });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('documind_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && localStorage.getItem('documind_token')) {
      localStorage.removeItem('documind_token');
      localStorage.removeItem('documind_user');
      window.dispatchEvent(new Event('documind:logout'));
    }
    return Promise.reject(error);
  },
);

export function apiError(error, fallback = 'Something went wrong') {
  const data = error?.response?.data;
  if (data?.details && typeof data.details === 'object') {
    const first = Object.values(data.details)[0];
    if (first) return first;
  }
  return data?.message || error?.message || fallback;
}
