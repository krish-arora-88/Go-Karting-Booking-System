import { NextRequest, NextResponse } from 'next/server';
import { loadTimeSlots, saveTimeSlots, addEventLog } from '@/lib/storage';
import { getTokenFromRequest, verifyToken } from '@/lib/auth';
import { bookSlot, cancelSlot, findSlotByTime } from '@/lib/timeSlots';
import { ApiResponse, TimeSlot } from '@/lib/types';

export async function GET(request: NextRequest) {
  try {
    const token = getTokenFromRequest(request);
    if (!token || !verifyToken(token)) {
      return NextResponse.json<ApiResponse<null>>({
        success: false,
        message: 'Unauthorized'
      }, { status: 401 });
    }

    const timeSlots = loadTimeSlots();
    return NextResponse.json<ApiResponse<TimeSlot[]>>({
      success: true,
      data: timeSlots
    });

  } catch (error) {
    console.error('Get bookings error:', error);
    return NextResponse.json<ApiResponse<null>>({
      success: false,
      message: 'Internal server error'
    }, { status: 500 });
  }
}

export async function POST(request: NextRequest) {
  try {
    const token = getTokenFromRequest(request);
    const user = verifyToken(token || '');
    
    if (!user) {
      return NextResponse.json<ApiResponse<null>>({
        success: false,
        message: 'Unauthorized'
      }, { status: 401 });
    }

    const { action, startTime, racerName } = await request.json();

    if (!action || !startTime || !racerName) {
      return NextResponse.json<ApiResponse<null>>({
        success: false,
        message: 'Missing required fields'
      }, { status: 400 });
    }

    const timeSlots = loadTimeSlots();
    const slot = findSlotByTime(timeSlots, startTime);

    if (!slot) {
      return NextResponse.json<ApiResponse<null>>({
        success: false,
        message: 'Time slot not found'
      }, { status: 404 });
    }

    let success = false;
    let message = '';

    if (action === 'book') {
      success = bookSlot(slot, racerName);
      if (success) {
        message = `Booking confirmed for ${racerName} at ${slot.startTime} - ${slot.endTime}`;
        addEventLog(`Time Slot Booked, Racer: ${racerName}, Slot: ${slot.startTime} - ${slot.endTime}`);
      } else {
        message = 'Unable to book slot - slot may be full or racer already booked';
      }
    } else if (action === 'cancel') {
      success = cancelSlot(slot, racerName);
      if (success) {
        message = `Booking cancelled for ${racerName}`;
        addEventLog(`Booking cancelled for Racer: ${racerName}`);
      } else {
        message = 'Unable to cancel booking - booking not found';
      }
    } else {
      return NextResponse.json<ApiResponse<null>>({
        success: false,
        message: 'Invalid action'
      }, { status: 400 });
    }

    if (success) {
      saveTimeSlots(timeSlots);
    }

    return NextResponse.json<ApiResponse<TimeSlot>>({
      success,
      data: slot,
      message
    }, { status: success ? 200 : 400 });

  } catch (error) {
    console.error('Booking operation error:', error);
    return NextResponse.json<ApiResponse<null>>({
      success: false,
      message: 'Internal server error'
    }, { status: 500 });
  }
} 