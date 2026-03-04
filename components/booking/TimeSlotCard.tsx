'use client'

import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { TimeSlot } from '@/services/bookingService'

interface TimeSlotCardProps {
  timeSlot: TimeSlot
  onBook?: () => void
  onCancel?: () => void
  type: 'available' | 'booked'
  isPending?: boolean
  bookingDate?: string
  racerNames?: string[]
}

export function TimeSlotCard({ timeSlot, onBook, onCancel, type, isPending, bookingDate, racerNames }: TimeSlotCardProps) {
  const [showRacers, setShowRacers] = useState(false)

  const isFull = timeSlot.remaining === 0
  const filled = timeSlot.capacity - timeSlot.remaining
  const fillPct = timeSlot.capacity > 0 ? Math.round((filled / timeSlot.capacity) * 100) : 0

  const borderColor = type === 'booked' ? 'var(--pink)' : 'rgba(0, 207, 255, 0.35)'
  const hoverClass =
    type === 'booked'
      ? 'hover:shadow-[0_0_16px_rgba(255,45,107,0.25)]'
      : 'hover:shadow-[0_0_16px_rgba(0,207,255,0.25)]'
  const accentColor = type === 'booked' ? 'var(--pink)' : 'var(--cyan)'

  return (
    <div
      className={`relative p-4 transition-all duration-200 hover:-translate-y-0.5 ${hoverClass}`}
      style={{ background: 'var(--surface)', border: `1px dashed ${borderColor}` }}
    >
      {/* Corner brackets */}
      <span className="bracket-corner tl" style={{ borderColor: accentColor }} />
      <span className="bracket-corner tr" style={{ borderColor: accentColor }} />
      <span className="bracket-corner bl" style={{ borderColor: accentColor }} />
      <span className="bracket-corner br" style={{ borderColor: accentColor }} />

      {/* Status badge */}
      <div className="flex justify-end mb-2">
        {type === 'available' && !isFull && (
          <span className="flex items-center gap-1 font-mono text-[10px] tracking-widest" style={{ color: 'var(--green)' }}>
            <span className="pulse-dot inline-block w-1.5 h-1.5 rounded-full" style={{ background: 'var(--green)' }} />
            OPEN
          </span>
        )}
        {type === 'available' && isFull && (
          <span className="flex items-center gap-1 font-mono text-[10px] tracking-widest" style={{ color: 'var(--pink)' }}>
            <span className="inline-block w-1.5 h-1.5 rounded-full" style={{ background: 'var(--pink)' }} />
            FULL
          </span>
        )}
        {type === 'booked' && (
          <span className="flex items-center gap-1 font-mono text-[10px] tracking-widest" style={{ color: 'var(--pink)' }}>
            <span className="inline-block w-1.5 h-1.5 rounded-full" style={{ background: 'var(--pink)' }} />
            BOOKED
          </span>
        )}
      </div>

      {/* Time display */}
      <div className="mb-3">
        <div className="font-orbitron font-bold text-2xl leading-none" style={{ color: accentColor }}>
          {timeSlot.startTime}
        </div>
        <div className="font-mono text-xs mt-0.5" style={{ color: 'var(--dim)' }}>
          — {timeSlot.endTime}
        </div>
        {type === 'booked' && bookingDate && (
          <div className="font-mono text-[10px] mt-1 tracking-wider" style={{ color: 'var(--dim)' }}>
            {new Date(bookingDate).toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })}
          </div>
        )}
      </div>

      {/* Capacity bar */}
      <div className="mb-4">
        <div className="capacity-bar-track">
          <div className={`capacity-bar-fill${isFull ? ' full' : ''}`} style={{ width: `${fillPct}%` }} />
        </div>
        <div className="flex justify-end mt-1">
          <span className="font-mono text-[10px]" style={{ color: 'var(--dim)' }}>
            {filled}/{timeSlot.capacity}
          </span>
        </div>
      </div>

      {/* View Racers (booked only) */}
      {type === 'booked' && racerNames && racerNames.length > 0 && (
        <div className="mb-3">
          <button
            type="button"
            onClick={() => setShowRacers(v => !v)}
            className="font-mono text-[10px] tracking-widest transition-colors flex items-center gap-1"
            style={{ color: 'var(--dim)' }}
            onMouseEnter={e => (e.currentTarget.style.color = 'var(--cyan)')}
            onMouseLeave={e => (e.currentTarget.style.color = 'var(--dim)')}
          >
            {showRacers ? '▾' : '▸'} VIEW RACERS ({racerNames.length})
          </button>
          {showRacers && (
            <ul className="mt-2 space-y-0.5 pl-3 border-l" style={{ borderColor: 'var(--border)' }}>
              {racerNames.map((name, i) => (
                <li key={i} className="font-mono text-[10px]" style={{ color: 'var(--white)' }}>
                  {String(i + 1).padStart(2, '0')} {name}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {/* Action */}
      {type === 'available' && onBook && (
        <Button
          variant="cyan"
          onClick={onBook}
          disabled={isFull || isPending}
          className="w-full text-[10px]"
        >
          {isFull ? 'RACE FULL' : 'BOOK SLOT ▶'}
        </Button>
      )}
      {type === 'booked' && onCancel && (
        <Button
          variant="pink-ghost"
          onClick={onCancel}
          disabled={isPending}
          className="w-full text-[10px]"
        >
          ✕ CANCEL
        </Button>
      )}
    </div>
  )
}
