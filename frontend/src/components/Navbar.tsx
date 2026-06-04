import { useEffect, useState } from 'react';

interface Props {
  search: string;
  onSearch: (v: string) => void;
  onRegister: () => void;
}

export default function Navbar({ search, onSearch, onRegister }: Props) {
  const [time, setTime] = useState('');

  useEffect(() => {
    const tick = () => setTime(new Date().toLocaleTimeString('en-GB', { hour12: false }));
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, []);

  return (
    <nav className="topbar">
      <div className="topbar-brand">
        <div className="brand-icon" />
        <div className="brand-name">Net<span>Watch</span></div>
      </div>

      <div className="topbar-center">
        <div className="search-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
          </svg>
          <input
            className="search-input"
            placeholder="Search devices…"
            autoComplete="off"
            value={search}
            onChange={e => onSearch(e.target.value)}
          />
        </div>
      </div>

      <div className="topbar-right">
        <span className="topbar-time">{time}</span>
        <button className="btn-register" onClick={onRegister}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M12 5v14M5 12h14" />
          </svg>
          Register
        </button>
      </div>
    </nav>
  );
}
