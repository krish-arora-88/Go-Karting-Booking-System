import { TimeSlot } from './types';

export function generateTimeSlots(): TimeSlot[] {
  const slots: TimeSlot[] = [];
  let currentTime = new Date();
  currentTime.setHours(12, 0, 0, 0); // Start at 12:00

  for (let i = 0; i < 24; i++) {
    const startTime = new Date(currentTime);
    const endTime = new Date(currentTime.getTime() + 30 * 60 * 1000); // Add 30 minutes

    const slot: TimeSlot = {
      startTime: formatTime(startTime),
      endTime: formatTime(endTime),
      capacity: 10,
      bookedRacers: [],
      remainingSlots: 10
    };

    slots.push(slot);
    currentTime.setTime(currentTime.getTime() + 30 * 60 * 1000); // Move to next slot
  }

  return slots;
}

export function formatTime(date: Date): string {
  return date.toTimeString().substring(0, 5); // Returns HH:MM format
}

export function isSlotAvailable(slot: TimeSlot): boolean {
  return slot.bookedRacers.length < slot.capacity;
}

export function bookSlot(slot: TimeSlot, racerName: string): boolean {
  if (isSlotAvailable(slot) && !slot.bookedRacers.includes(racerName)) {
    slot.bookedRacers.push(racerName);
    slot.remainingSlots = slot.capacity - slot.bookedRacers.length;
    return true;
  }
  return false;
}

export function cancelSlot(slot: TimeSlot, racerName: string): boolean {
  const index = slot.bookedRacers.indexOf(racerName);
  if (index > -1) {
    slot.bookedRacers.splice(index, 1);
    slot.remainingSlots = slot.capacity - slot.bookedRacers.length;
    return true;
  }
  return false;
}

export function findSlotByTime(slots: TimeSlot[], startTime: string): TimeSlot | undefined {
  return slots.find(slot => slot.startTime === startTime);
} 