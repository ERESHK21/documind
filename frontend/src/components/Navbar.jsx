import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { initials } from '../lib/format';

const navLink = ({ isActive }) =>
  `whitespace-nowrap rounded-lg px-3 py-2 text-sm font-medium transition ${
    isActive ? 'bg-slate-100 text-slate-900' : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
  }`;

export default function Navbar() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  function signOut() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/90 backdrop-blur">
      <div className="mx-auto flex w-full max-w-6xl items-center justify-between gap-3 px-4 py-3 sm:px-6">
        <Link to="/" className="flex items-center gap-2.5">
          <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-600 to-violet-500 text-sm font-black text-white">
            D
          </span>
          <span className="text-base font-bold tracking-tight text-slate-900">DocuMind</span>
          <span className="hidden rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-slate-500 sm:inline">
            RAG
          </span>
        </Link>

        <nav className="hidden items-center gap-1 md:flex">
          {user ? (
            <>
              <NavLink to="/" end className={navLink}>
                Overview
              </NavLink>
              <NavLink to="/documents" className={navLink}>
                Documents
              </NavLink>
              <NavLink to="/chat" className={navLink}>
                Ask
              </NavLink>
              {isAdmin ? (
                <NavLink to="/admin" className={navLink}>
                  Admin
                </NavLink>
              ) : null}
            </>
          ) : (
            <>
              <NavLink to="/" end className={navLink}>
                Overview
              </NavLink>
            </>
          )}
        </nav>

        <div className="flex items-center gap-2">
          {user ? (
            <>
              <div className="hidden items-center gap-2 rounded-xl border border-slate-200 px-2.5 py-1.5 sm:flex">
                <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-slate-900 text-[11px] font-bold text-white">
                  {initials(user.name)}
                </span>
                <span className="text-xs leading-tight">
                  <span className="block font-semibold text-slate-800">{user.name}</span>
                  <span className="block text-[10px] uppercase tracking-wider text-slate-400">{user.role}</span>
                </span>
              </div>
              <button type="button" onClick={signOut} className="btn-ghost">
                Sign out
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn-ghost">
                Sign in
              </Link>
              <Link to="/register" className="btn-primary">
                Create account
              </Link>
            </>
          )}
          <button
            type="button"
            onClick={() => setOpen((value) => !value)}
            className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 md:hidden"
            aria-label="Toggle menu"
          >
            <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M4 7h16M4 12h16M4 17h16" strokeLinecap="round" />
            </svg>
          </button>
        </div>
      </div>

      {open && user ? (
        <div className="border-t border-slate-100 bg-white px-4 py-2 md:hidden">
          <div className="flex flex-col gap-1">
            <NavLink to="/" end className={navLink} onClick={() => setOpen(false)}>
              Overview
            </NavLink>
            <NavLink to="/documents" className={navLink} onClick={() => setOpen(false)}>
              Documents
            </NavLink>
            <NavLink to="/chat" className={navLink} onClick={() => setOpen(false)}>
              Ask
            </NavLink>
            {isAdmin ? (
              <NavLink to="/admin" className={navLink} onClick={() => setOpen(false)}>
                Admin
              </NavLink>
            ) : null}
          </div>
        </div>
      ) : null}
    </header>
  );
}
