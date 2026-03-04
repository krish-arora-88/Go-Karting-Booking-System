'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { 
  Car, 
  Clock, 
  Users, 
  Plus, 
  X, 
  Calendar,
  FileText,
  LogOut,
  User
} from 'lucide-react';
import { TimeSlot, EventLog } from '@/lib/types';

export default function Dashboard() {
  const [user, setUser] = useState<string>('');
  const [timeSlots, setTimeSlots] = useState<TimeSlot[]>([]);
  const [logs, setLogs] = useState<EventLog[]>([]);
  const [showBookingModal, setShowBookingModal] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [showLogsModal, setShowLogsModal] = useState(false);
  const [selectedSlot, setSelectedSlot] = useState<TimeSlot | null>(null);
  const [racerName, setRacerName] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const router = useRouter();

  useEffect(() => {
    const token = localStorage.getItem('token');
    const username = localStorage.getItem('username');
    
    if (!token || !username) {
      router.push('/');
      return;
    }
    
    setUser(username);
    fetchTimeSlots();
  }, [router]);

  const fetchTimeSlots = async () => {
    const token = localStorage.getItem('token');
    try {
      const response = await fetch('/api/bookings', {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });
      
      const data = await response.json();
      if (data.success) {
        console.log('Fetched time slots:', data.data); // Debug log
        setTimeSlots(data.data);
      } else {
        setMessage('Failed to load time slots');
      }
    } catch (error) {
      setMessage('Error loading time slots');
    }
  };

  const fetchLogs = async () => {
    const token = localStorage.getItem('token');
    try {
      const response = await fetch('/api/logs', {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });
      
      const data = await response.json();
      if (data.success) {
        setLogs(data.data);
      }
    } catch (error) {
      console.error('Error loading logs');
    }
  };

  const handleBooking = async () => {
    if (!selectedSlot || !racerName.trim()) return;
    
    setLoading(true);
    const token = localStorage.getItem('token');
    
    try {
      const response = await fetch('/api/bookings', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          action: 'book',
          startTime: selectedSlot.startTime,
          racerName: racerName.trim(),
        }),
      });
      
      const data = await response.json();
      setMessage(data.message);
      
      if (data.success) {
        fetchTimeSlots();
        setShowBookingModal(false);
        setRacerName('');
        setSelectedSlot(null);
      }
    } catch (error) {
      setMessage('Error booking slot');
    } finally {
      setLoading(false);
    }
  };

  const handleCancellation = async () => {
    if (!racerName.trim()) return;
    
    setLoading(true);
    const token = localStorage.getItem('token');
    
    try {
      // Find slots with this racer name
      const slotsWithRacer = timeSlots.filter(slot => 
        slot.bookedRacers.includes(racerName.trim())
      );
      
      if (slotsWithRacer.length === 0) {
        setMessage(`No bookings found for ${racerName}`);
        setLoading(false);
        return;
      }
      
      // Cancel the first matching booking
      const slotToCancel = slotsWithRacer[0];
      const response = await fetch('/api/bookings', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          action: 'cancel',
          startTime: slotToCancel.startTime,
          racerName: racerName.trim(),
        }),
      });
      
      const data = await response.json();
      setMessage(data.message);
      
      if (data.success) {
        fetchTimeSlots();
        setShowCancelModal(false);
        setRacerName('');
      }
    } catch (error) {
      setMessage('Error canceling booking');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    router.push('/');
  };

  const availableSlots = timeSlots.filter(slot => slot.remainingSlots > 0);
  const fullyBookedSlots = timeSlots.filter(slot => slot.remainingSlots === 0);

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <Car className="w-8 h-8 text-blue-600" />
              <h1 className="text-2xl font-bold text-gray-900">GoKart Pro Dashboard</h1>
            </div>
            
            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-2 text-gray-600">
                <User className="w-5 h-5" />
                <span>Welcome, {user}</span>
              </div>
              <button
                onClick={handleLogout}
                className="flex items-center space-x-2 text-red-600 hover:text-red-700"
              >
                <LogOut className="w-5 h-5" />
                <span>Logout</span>
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-6 py-8">
        {/* Message Display */}
        {message && (
          <div className={`mb-6 p-4 rounded-lg ${
            message.includes('successful') || message.includes('confirmed') || message.includes('cancelled')
              ? 'bg-green-100 text-green-700'
              : 'bg-red-100 text-red-700'
          }`}>
            {message}
            <button 
              onClick={() => setMessage('')}
              className="ml-2 text-gray-500 hover:text-gray-700"
            >
              ×
            </button>
          </div>
        )}

        {/* Action Buttons */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
          <button
            onClick={() => setShowBookingModal(true)}
            className="btn-primary flex items-center justify-center space-x-2 py-4"
          >
            <Plus className="w-5 h-5" />
            <span>Book a Slot</span>
          </button>
          
          <button
            onClick={() => setShowCancelModal(true)}
            className="btn-secondary flex items-center justify-center space-x-2 py-4"
          >
            <X className="w-5 h-5" />
            <span>Cancel Booking</span>
          </button>
          
          <button
            onClick={() => {
              fetchLogs();
              setShowLogsModal(true);
            }}
            className="btn-secondary flex items-center justify-center space-x-2 py-4"
          >
            <FileText className="w-5 h-5" />
            <span>View Logs</span>
          </button>
          
          <button
            onClick={fetchTimeSlots}
            className="btn-secondary flex items-center justify-center space-x-2 py-4"
          >
            <Calendar className="w-5 h-5" />
            <span>Refresh Slots</span>
          </button>
        </div>

        {/* Available Slots */}
        <div className="mb-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-4 flex items-center">
            <Clock className="w-5 h-5 mr-2 text-green-600" />
            Available Time Slots ({availableSlots.length})
          </h2>
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {availableSlots.map((slot, index) => (
              <div key={index} className="card hover:shadow-lg transition-shadow">
                <div className="flex items-center justify-between mb-2">
                  <span className="font-semibold text-lg text-gray-900">
                    {slot.startTime} - {slot.endTime}
                  </span>
                  <span className="bg-green-100 text-green-800 px-2 py-1 rounded-full text-sm">
                    Available
                  </span>
                </div>
                
                <div className="flex items-center space-x-4 text-sm text-gray-600 mb-3">
                  <div className="flex items-center">
                    <Users className="w-4 h-4 mr-1" />
                    <span>{slot.remainingSlots} slots left</span>
                  </div>
                </div>
                
                {slot.bookedRacers.length > 0 && (
                  <div className="mb-3">
                    <p className="text-sm font-medium text-gray-700 mb-1">Booked Racers:</p>
                    <div className="flex flex-wrap gap-1">
                      {slot.bookedRacers.map((racer, i) => (
                        <span key={i} className="bg-blue-100 text-blue-800 px-2 py-1 rounded text-xs">
                          {racer}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
                
                <button
                  onClick={() => {
                    setSelectedSlot(slot);
                    setShowBookingModal(true);
                  }}
                  className="w-full btn-primary text-sm py-2"
                >
                  Book This Slot
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* Fully Booked Slots */}
        {fullyBookedSlots.length > 0 && (
          <div>
            <h2 className="text-xl font-semibold text-gray-900 mb-4 flex items-center">
              <X className="w-5 h-5 mr-2 text-red-600" />
              Fully Booked Slots ({fullyBookedSlots.length})
            </h2>
            
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {fullyBookedSlots.map((slot, index) => (
                <div key={index} className="card bg-gray-50">
                                  <div className="flex items-center justify-between mb-2">
                  <span className="font-semibold text-lg text-gray-900">
                    {slot.startTime} - {slot.endTime}
                  </span>
                  <span className="bg-red-100 text-red-800 px-2 py-1 rounded-full text-sm">
                    Full
                  </span>
                </div>
                  
                  <div className="mb-3">
                    <p className="text-sm font-medium text-gray-700 mb-1">Booked Racers:</p>
                    <div className="flex flex-wrap gap-1">
                      {slot.bookedRacers.map((racer, i) => (
                        <span key={i} className="bg-red-100 text-red-800 px-2 py-1 rounded text-xs">
                          {racer}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </main>

      {/* Booking Modal */}
      {showBookingModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-xl font-bold mb-4">Book a Time Slot</h3>
            
            {selectedSlot && (
              <div className="mb-4 p-3 bg-blue-50 rounded-lg">
                <p className="font-medium">Selected Slot:</p>
                <p>{selectedSlot.startTime} - {selectedSlot.endTime}</p>
                <p className="text-sm text-gray-600">{selectedSlot.remainingSlots} slots remaining</p>
              </div>
            )}
            
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Racer Name
              </label>
              <input
                type="text"
                value={racerName}
                onChange={(e) => setRacerName(e.target.value)}
                className="input-field"
                placeholder="Enter racer name"
              />
            </div>
            
            <div className="flex gap-3">
              <button
                onClick={handleBooking}
                disabled={loading || !racerName.trim()}
                className="btn-primary flex-1"
              >
                {loading ? 'Booking...' : 'Confirm Booking'}
              </button>
              <button
                onClick={() => {
                  setShowBookingModal(false);
                  setSelectedSlot(null);
                  setRacerName('');
                }}
                className="btn-secondary"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Cancel Modal */}
      {showCancelModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-xl font-bold mb-4">Cancel a Booking</h3>
            
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Racer Name
              </label>
              <input
                type="text"
                value={racerName}
                onChange={(e) => setRacerName(e.target.value)}
                className="input-field"
                placeholder="Enter racer name to cancel"
              />
            </div>
            
            <div className="flex gap-3">
              <button
                onClick={handleCancellation}
                disabled={loading || !racerName.trim()}
                className="bg-red-600 hover:bg-red-700 text-white font-semibold py-2 px-4 rounded-lg flex-1"
              >
                {loading ? 'Canceling...' : 'Cancel Booking'}
              </button>
              <button
                onClick={() => {
                  setShowCancelModal(false);
                  setRacerName('');
                }}
                className="btn-secondary"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Logs Modal */}
      {showLogsModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-2xl w-full mx-4 max-h-96">
            <h3 className="text-xl font-bold mb-4">Event Logs</h3>
            
            <div className="overflow-y-auto max-h-64 space-y-2">
              {logs.length > 0 ? (
                logs.map((log) => (
                  <div key={log.id} className="p-3 bg-gray-50 rounded border-l-4 border-blue-500">
                    <p className="text-sm">{log.description}</p>
                    <p className="text-xs text-gray-500 mt-1">
                      {new Date(log.timestamp).toLocaleString()}
                    </p>
                  </div>
                ))
              ) : (
                <p className="text-gray-500 text-center py-4">No logs available</p>
              )}
            </div>
            
            <div className="mt-4">
              <button
                onClick={() => setShowLogsModal(false)}
                className="btn-secondary w-full"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
} 