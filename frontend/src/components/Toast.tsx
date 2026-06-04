import { useEffect } from 'react';

interface Props {
  message: string;
  type: 'success' | 'error';
  visible: boolean;
  onHide: () => void;
}

export default function Toast({ message, type, visible, onHide }: Props) {
  useEffect(() => {
    if (!visible) return;
    const t = setTimeout(onHide, 3500);
    return () => clearTimeout(t);
  }, [visible, onHide]);

  return (
    <div className={`toast-wrap${visible ? ' show' : ''}`}>
      <div className="toast">
        <div className={`toast-icon ${type}`}>{type === 'success' ? '✓' : '✕'}</div>
        <div className="toast-msg">{message}</div>
      </div>
    </div>
  );
}
