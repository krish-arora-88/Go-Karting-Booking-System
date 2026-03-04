'use client';

import { useState, useEffect, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { LogOut, Zap, BookOpen } from 'lucide-react';
import { addDays, format } from 'date-fns';
import { TimeSlotCard } from '@/components/booking/TimeSlotCard';
import { BookingModal } from '@/components/booking/BookingModal';
import { bookingService, TimeSlot } from '@/services/bookingService';
import { authService } from '@/services/authService';
import { useAuthStore } from '@/store/authStore';
import toast from 'react-hot-toast';

type Tab = 'available' | 'bookings';

function buildDateStrip() {
  const today = new Date();
  return Array.from({ length: 7 }, (_, i) => {
    const d = addDays(today, i);
    return {
      value: format(d, 'yyyy-MM-dd'),
      day: format(d, 'EEE').toUpperCase(),
      date: format(d, 'dd'),
    };
  });
}

export default function DashboardPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { isAuthenticated, user, hasHydrated } = useAuthStore();
  const DATE_STRIP = useMemo(() => buildDateStrip(), []);
  const [activeTab, setActiveTab] = useState<Tab>('available');
  const [selectedDate, setSelectedDate] = useState(() => buildDateStrip()[0].value);
  const [bookingSlot, setBookingSlot] = useState<TimeSlot | null>(null);

  useEffect(() => {
    if (hasHydrated && !isAuthenticated) router.push('/');
  }, [hasHydrated, isAuthenticated, router]);

  useEffect(() => {
    if (activeTab !== 'available') return;
    const API = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
    const es = new EventSource(`${API}/api/v1/slots/stream?date=${selectedDate}`);
    es.addEventListener('slot-update', () => {
      queryClient.invalidateQueries({ queryKey: ['slots', selectedDate] });
    });
    es.onerror = () => es.close();
    return () => es.close();
  }, [selectedDate, activeTab, queryClient]);

  const { data: slots = [], isLoading: slotsLoading, isFetching: slotsFetching } = useQuery({
    queryKey: ['slots', selectedDate],
    queryFn: () => bookingService.getSlots(selectedDate),
    refetchInterval: 30_000,
  });

  const { data: myBookings = [], isLoading: bookingsLoading } = useQuery({
    queryKey: ['my-bookings'],
    queryFn: () => bookingService.getMyBookings(),
  });

  const bookMutation = useMutation({
    mutationFn: ({ slotId, date, racerCount, racerNames }: {
      slotId: string; date: string; racerCount: number; racerNames: string[];
    }) => bookingService.bookSlot(slotId, date, racerCount, racerNames, crypto.randomUUID()),
    onSuccess: () => {
      toast.success('SLOT BOOKED');
      setBookingSlot(null);
      queryClient.invalidateQueries({ queryKey: ['slots'] });
      queryClient.invalidateQueries({ queryKey: ['my-bookings'] });
    },
    onError: (err: Error) => toast.error(err.message),
  });

  const cancelMutation = useMutation({
    mutationFn: (bookingId: string) => bookingService.cancelBooking(bookingId),
    onSuccess: () => {
      toast.success('BOOKING CANCELLED');
      queryClient.invalidateQueries({ queryKey: ['slots'] });
      queryClient.invalidateQueries({ queryKey: ['my-bookings'] });
    },
    onError: (err: Error) => toast.error(err.message),
  });

  const handleLogout = async () => {
    await authService.logout();
    router.push('/');
  };

  if (!hasHydrated || !isAuthenticated) {
    return (
      <div className="min-h-screen flex items-center justify-center font-orbitron text-sm"
        style={{ background: 'var(--bg)', color: 'var(--dim)' }}>
        LOADING...
      </div>
    );
  }

  const availableSlots = slots.filter(s => s.available);

  const navItems: { tab: Tab; icon: React.ReactNode; label: string; count?: number }[] = [
    { tab: 'available', icon: <Zap size={14} />, label: 'SLOTS' },
    { tab: 'bookings', icon: <BookOpen size={14} />, label: 'BOOKINGS', count: myBookings.length },
  ];

  return (
    <div className="min-h-screen flex" style={{ background: 'var(--bg)' }}>

      {/* BookingModal */}
      {bookingSlot && (
        <BookingModal
          slot={bookingSlot}
          date={selectedDate}
          isPending={bookMutation.isPending}
          onConfirm={(racerCount, racerNames) =>
            bookMutation.mutate({ slotId: bookingSlot.id, date: selectedDate, racerCount, racerNames })
          }
          onClose={() => setBookingSlot(null)}
        />
      )}

      {/* Sidebar (desktop) */}
      <aside
        className="hidden md:flex flex-col w-48 flex-shrink-0 border-r"
        style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}
      >
        <div className="px-4 py-5 border-b" style={{ borderColor: 'var(--border)' }}>
          <div className="font-orbitron font-black text-sm" style={{ color: 'var(--cyan)' }}>
            ◈ APEX
          </div>
          <div className="font-mono text-[9px] tracking-widest mt-0.5" style={{ color: 'var(--dim)' }}>
            RACE CONTROL
          </div>
        </div>

        <nav className="flex-1 py-4">
          {navItems.map(({ tab, icon, label, count }) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`flex items-center gap-2 w-full px-4 py-3 font-orbitron text-[10px] tracking-widest transition-all ${
                activeTab === tab ? 'nav-item-active' : 'nav-item-inactive'
              }`}
            >
              {icon}
              {label}
              {count !== undefined && count > 0 && (
                <span
                  className="ml-auto font-mono text-[9px] px-1.5 py-0.5"
                  style={{ background: 'rgba(0,207,255,0.15)', color: 'var(--cyan)' }}
                >
                  {count}
                </span>
              )}
            </button>
          ))}
        </nav>

        <div className="p-4 border-t" style={{ borderColor: 'var(--border)' }}>
          <p className="font-mono text-[10px] truncate mb-2" style={{ color: 'var(--dim)' }}>
            {user?.username}
          </p>
          <button
            onClick={handleLogout}
            className="flex items-center gap-1.5 font-orbitron text-[9px] tracking-widest transition-colors"
            style={{ color: 'var(--dim)' }}
            onMouseEnter={e => (e.currentTarget.style.color = 'var(--pink)')}
            onMouseLeave={e => (e.currentTarget.style.color = 'var(--dim)')}
          >
            <LogOut size={12} />
            EJECT
          </button>
        </div>
      </aside>

      {/* Main */}
      <div className="flex-1 flex flex-col min-w-0 pb-16 md:pb-0">

        {/* Header */}
        <header
          className="flex items-center justify-between px-4 md:px-6 py-4 border-b flex-shrink-0"
          style={{ borderColor: 'var(--border)' }}
        >
          <div className="flex items-center gap-3">
            <h1 className="font-orbitron font-bold text-lg" style={{ color: 'var(--white)' }}>
              RACE CONTROL
            </h1>
            {slotsFetching && <span className="fetch-dot" />}
          </div>
          <span className="md:hidden font-mono text-xs" style={{ color: 'var(--dim)' }}>
            {user?.username}
          </span>
        </header>

        <div className="flex-1 px-4 md:px-6 py-5 overflow-auto">

          {/* Date strip (available tab only) */}
          {activeTab === 'available' && (
            <div className="flex gap-2 overflow-x-auto pb-2 mb-5">
              {DATE_STRIP.map(({ value, day, date }) => (
                <button
                  key={value}
                  onClick={() => setSelectedDate(value)}
                  className="flex-shrink-0 flex flex-col items-center px-3 py-2 border transition-all font-orbitron text-[10px] tracking-wider"
                  style={
                    selectedDate === value
                      ? { borderColor: 'var(--cyan)', background: 'rgba(0,207,255,0.1)', color: 'var(--cyan)', boxShadow: '0 0 10px rgba(0,207,255,0.2)' }
                      : { borderColor: 'var(--border)', background: 'transparent', color: 'var(--dim)' }
                  }
                >
                  <span className="text-[8px]">{day}</span>
                  <span className="text-base font-black leading-tight">{date}</span>
                </button>
              ))}
            </div>
          )}

          {/* Content */}
          {(slotsLoading || bookingsLoading) ? (
            <div className="flex justify-center py-16">
              <div className="w-8 h-8 rounded-full border-t border-r animate-spin" style={{ borderColor: 'var(--cyan)' }} />
            </div>
          ) : (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">

                {activeTab === 'available' && availableSlots.map(slot => (
                  <TimeSlotCard
                    key={slot.id}
                    timeSlot={slot}
                    onBook={() => setBookingSlot(slot)}
                    type="available"
                  />
                ))}

                {activeTab === 'bookings' && myBookings.map(booking => (
                  <TimeSlotCard
                    key={booking.id}
                    timeSlot={{
                      id: booking.timeSlotId,
                      startTime: booking.startTime,
                      endTime: booking.endTime,
                      capacity: booking.racerCount,
                      remaining: 0,
                      available: false,
                    }}
                    onCancel={() => cancelMutation.mutate(booking.id)}
                    type="booked"
                    isPending={cancelMutation.isPending}
                  />
                ))}
              </div>

              {activeTab === 'available' && availableSlots.length === 0 && (
                <div className="flex flex-col items-center justify-center py-20 gap-3">
                  <div className="font-orbitron font-black text-2xl tracking-widest" style={{ color: 'var(--dim)' }}>
                    NO RACES SCHEDULED
                  </div>
                  <div className="font-mono text-xs" style={{ color: 'var(--dim)' }}>select another date</div>
                </div>
              )}

              {activeTab === 'bookings' && myBookings.length === 0 && (
                <div className="flex flex-col items-center justify-center py-20 gap-3">
                  <div className="font-orbitron font-black text-2xl tracking-widest" style={{ color: 'var(--dim)' }}>
                    NO ACTIVE BOOKINGS
                  </div>
                  <div className="font-mono text-xs" style={{ color: 'var(--dim)' }}>head to slots to book a race</div>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* Bottom tab bar (mobile) */}
      <nav
        className="md:hidden fixed bottom-0 left-0 right-0 flex border-t"
        style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}
      >
        {navItems.map(({ tab, icon, label }) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className="flex-1 flex flex-col items-center py-3 gap-1 font-orbitron text-[9px] tracking-widest transition-colors"
            style={{ color: activeTab === tab ? 'var(--cyan)' : 'var(--dim)' }}
          >
            {icon}
            {label}
          </button>
        ))}
        <button
          onClick={handleLogout}
          className="flex-1 flex flex-col items-center py-3 gap-1 font-orbitron text-[9px] tracking-widest"
          style={{ color: 'var(--dim)' }}
        >
          <LogOut size={14} />
          EJECT
        </button>
      </nav>
    </div>
  );
}
