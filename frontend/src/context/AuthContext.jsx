import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { api } from '../lib/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('documind_user') || 'null');
    } catch {
      return null;
    }
  });

  function persist(data) {
    const profile = { id: data.id, name: data.name, email: data.email, role: data.role };
    localStorage.setItem('documind_token', data.token);
    localStorage.setItem('documind_user', JSON.stringify(profile));
    setUser(profile);
  }

  async function login(email, password) {
    const { data } = await api.post('/auth/login', { email, password });
    persist(data);
    return data;
  }

  async function register(payload) {
    const { data } = await api.post('/auth/register', payload);
    persist(data);
    return data;
  }

  function logout() {
    localStorage.removeItem('documind_token');
    localStorage.removeItem('documind_user');
    setUser(null);
  }

  useEffect(() => {
    const handler = () => setUser(null);
    window.addEventListener('documind:logout', handler);
    return () => window.removeEventListener('documind:logout', handler);
  }, []);

  const value = useMemo(
    () => ({
      user,
      login,
      register,
      logout,
      isAdmin: user?.role === 'ADMIN',
    }),
    [user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
