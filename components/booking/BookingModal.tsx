'use client';

import { useState, useEffect } from 'react';
import { X } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { TimeSlot } from '@/services/bookingService';

interface BookingModalProps {
  slot: TimeSlot;
  date: string;
  isPending: boolean;
  onConfirm: (racerCount: number, racerNames: string[]) => void;
  onClose: () => void;
}

export function BookingModal({ slot, date, isPending, onConfirm, onClose }: BookingModalProps) {
  const [racerCount, setRacerCount] = useState(1);
  const [racerNames, setRacerNames] = useState<string[]>(['']);

  useEffect(() => {
    setRacerNames(prev =>
      Array.from({ length: racerCount }, (_, i) => prev[i] ?? '')
    );
  }, [racerCount]);

  const handleCountChange = (n: number) => {
    setRacerCount(Math.min(Math.max(1, n), slot.remaining));
  };

  const handleNameChange = (index: number, value: string) => {
    setRacerNames(prev => prev.map((n, i) => (i === index ? value : n)));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = racerNames.map(n => n.trim()).filter(Boolean);
    if (trimmed.length !== racerCount) return;
    onConfirm(racerCount, trimmed);
  };

  const allFilled = racerNames.every(n => n.trim().length > 0);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={{ background: 'rgba(6,6,20,0.85)' }}>
      <div
        className="relative w-full max-w-md p-6"
        style={{ background: 'var(--surface)', border: '1px solid var(--border)' }}
      >
        {/* Corner brackets */}
        <span className="bracket-corner tl" />
        <span className="bracket-corner tr" />
        <span className="bracket-corner bl" />
        <span className="bracket-corner br" />

        {/* Header */}
        <div className="flex items-start justify-between mb-6">
          <div>
            <h2 className="font-orbitron font-bold text-base tracking-widest" style={{ color: 'var(--cyan)' }}>
              BOOK RACE SLOT
            </h2>
            <p className="font-mono text-[10px] mt-1" style={{ color: 'var(--dim)' }}>
              {slot.startTime} — {slot.endTime} · {date} ·{' '}
              <span style={{ color: 'var(--green)' }}>{slot.remaining} spots left</span>
            </p>
          </div>
          <button
            onClick={onClose}
            className="transition-colors"
            style={{ color: 'var(--dim)' }}
            onMouseEnter={e => (e.currentTarget.style.color = 'var(--pink)')}
            onMouseLeave={e => (e.currentTarget.style.color = 'var(--dim)')}
          >
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* Racer count */}
          <div>
            <label className="font-mono text-[10px] tracking-widest uppercase mb-2 block" style={{ color: 'var(--dim)' }}>
              NUMBER OF RACERS (max {slot.remaining})
            </label>
            <div className="flex items-center gap-4">
              <button
                type="button"
                onClick={() => handleCountChange(racerCount - 1)}
                disabled={racerCount <= 1}
                className="w-8 h-8 border font-orbitron text-lg leading-none transition-colors disabled:opacity-30"
                style={{ borderColor: 'var(--border)', color: 'var(--cyan)' }}
                onMouseEnter={e => !e.currentTarget.disabled && (e.currentTarget.style.borderColor = 'var(--cyan)')}
                onMouseLeave={e => (e.currentTarget.style.borderColor = 'var(--border)')}
              >
                −
              </button>
              <span className="font-orbitron font-bold text-2xl w-8 text-center" style={{ color: 'var(--white)' }}>
                {racerCount}
              </span>
              <button
                type="button"
                onClick={() => handleCountChange(racerCount + 1)}
                disabled={racerCount >= slot.remaining}
                className="w-8 h-8 border font-orbitron text-lg leading-none transition-colors disabled:opacity-30"
                style={{ borderColor: 'var(--border)', color: 'var(--cyan)' }}
                onMouseEnter={e => !e.currentTarget.disabled && (e.currentTarget.style.borderColor = 'var(--cyan)')}
                onMouseLeave={e => (e.currentTarget.style.borderColor = 'var(--border)')}
              >
                +
              </button>
            </div>
          </div>

          {/* Racer names */}
          <div className="space-y-2">
            <label className="font-mono text-[10px] tracking-widest uppercase block" style={{ color: 'var(--dim)' }}>
              RACER NAMES
            </label>
            {racerNames.map((name, i) => (
              <div key={i}>
                <input
                  className="tron-input"
                  type="text"
                  value={name}
                  onChange={e => handleNameChange(i, e.target.value)}
                  placeholder={`RACER ${i + 1}`}
                  required
                  maxLength={60}
                />
              </div>
            ))}
          </div>

          {/* Actions */}
          <div className="flex gap-3 pt-1">
            <Button type="button" variant="dim-ghost" onClick={onClose} className="flex-1 text-[10px]">
              CANCEL
            </Button>
            <Button
              type="submit"
              variant="cyan"
              disabled={isPending || !allFilled}
              className="flex-1 text-[10px]"
            >
              {isPending ? '...' : `CONFIRM ${racerCount} RACER${racerCount > 1 ? 'S' : ''} ▶`}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
