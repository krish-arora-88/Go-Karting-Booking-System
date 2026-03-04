export interface User {
  username: string;
  password: string;
}

export interface TimeSlot {
  startTime: string;
  endTime: string;
  capacity: number;
  bookedRacers: string[];
  remainingSlots: number;
}

export interface Booking {
  racerName: string;
  timeSlot: TimeSlot;
  bookingDate: string;
}

export interface AuthToken {
  username: string;
  iat: number;
  exp: number;
}

export interface EventLog {
  id: string;
  description: string;
  timestamp: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
} 