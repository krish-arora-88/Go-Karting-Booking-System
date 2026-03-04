import fs from 'fs';
import path from 'path';
import { User, TimeSlot, EventLog } from './types';
import { generateTimeSlots } from './timeSlots';
import { hashPassword } from './auth';

const DATA_DIR = path.join(process.cwd(), 'data');
const USERS_FILE = path.join(process.cwd(), 'loginData.json'); // Use existing loginData.json
const BOOKINGS_FILE = path.join(process.cwd(), 'Bookings.json'); // Use existing Bookings.json
const LOGS_FILE = path.join(DATA_DIR, 'logs.json');



// Initialize files with default data if they don't exist
function initializeFiles() {
  // Migrate existing loginData.json to hashed passwords if needed
  if (fs.existsSync(USERS_FILE)) {
    try {
      const data = fs.readFileSync(USERS_FILE, 'utf8');
      const users = JSON.parse(data);
      
      // Check if passwords are already hashed (bcrypt hashes start with $2a$ or $2b$)
      let needsMigration = false;
      const migratedUsers = users.map((user: any) => {
        if (!user.password.startsWith('$2')) {
          needsMigration = true;
          return {
            username: user.username,
            password: hashPassword(user.password)
          };
        }
        return user;
      });
      
      // Write back migrated data if needed
      if (needsMigration) {
        fs.writeFileSync(USERS_FILE, JSON.stringify(migratedUsers, null, 2));
        console.log('✅ Migrated existing passwords to hashed format');
      }
    } catch (error) {
      console.log('Error migrating users:', error);
    }
  } else {
    // Create default admin user if no file exists
    const defaultUsers: User[] = [
      { username: 'admin', password: hashPassword('admin') },
    ];
    fs.writeFileSync(USERS_FILE, JSON.stringify(defaultUsers, null, 2));
  }

  // Use existing Bookings.json or create default
  if (!fs.existsSync(BOOKINGS_FILE)) {
    const defaultSlots = generateTimeSlots();
    fs.writeFileSync(BOOKINGS_FILE, JSON.stringify(defaultSlots, null, 2));
  }

  // Ensure data directory exists for logs
  if (!fs.existsSync(DATA_DIR)) {
    fs.mkdirSync(DATA_DIR, { recursive: true });
  }
  
  if (!fs.existsSync(LOGS_FILE)) {
    const defaultLogs: EventLog[] = [];
    fs.writeFileSync(LOGS_FILE, JSON.stringify(defaultLogs, null, 2));
  }
}

export function loadUsers(): User[] {
  initializeFiles();
  const data = fs.readFileSync(USERS_FILE, 'utf8');
  return JSON.parse(data);
}

export function saveUsers(users: User[]): void {
  fs.writeFileSync(USERS_FILE, JSON.stringify(users, null, 2));
}

export function loadTimeSlots(): TimeSlot[] {
  initializeFiles();
  const data = fs.readFileSync(BOOKINGS_FILE, 'utf8');
  const bookings = JSON.parse(data);
  
  console.log('Raw bookings data:', bookings.slice(0, 2)); // Debug log
  
  // Convert existing format to our TimeSlot interface
  const slots = bookings.map((booking: any) => ({
    startTime: booking.start_time,
    endTime: booking.end_time,
    capacity: booking.capacity,
    bookedRacers: booking.booked_racers || [],
    remainingSlots: booking.remaining_slots || (booking.capacity - (booking.booked_racers?.length || 0))
  }));
  
  console.log('Converted slots:', slots.slice(0, 2)); // Debug log
  return slots;
}

export function saveTimeSlots(slots: TimeSlot[]): void {
  // Convert our TimeSlot format back to the existing format
  const bookings = slots.map(slot => ({
    start_time: slot.startTime,
    end_time: slot.endTime,
    capacity: slot.capacity,
    booked_racers: slot.bookedRacers,
    remaining_slots: slot.remainingSlots
  }));
  
  fs.writeFileSync(BOOKINGS_FILE, JSON.stringify(bookings, null, 2));
}

export function loadEventLogs(): EventLog[] {
  initializeFiles();
  const data = fs.readFileSync(LOGS_FILE, 'utf8');
  return JSON.parse(data);
}

export function saveEventLogs(logs: EventLog[]): void {
  fs.writeFileSync(LOGS_FILE, JSON.stringify(logs, null, 2));
}

export function addEventLog(description: string): void {
  const logs = loadEventLogs();
  const newLog: EventLog = {
    id: Date.now().toString(),
    description,
    timestamp: new Date().toISOString()
  };
  logs.push(newLog);
  saveEventLogs(logs);
} 