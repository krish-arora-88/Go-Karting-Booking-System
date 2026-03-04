# 🏎️ Go-Karting Booking System

A modern, full-stack web application for managing go-kart racing bookings. Built with Next.js 14, TypeScript, and Tailwind CSS, optimized for Vercel deployment.

![Demo Screenshot](https://via.placeholder.com/800x400/3B82F6/FFFFFF?text=GoKart+Pro+Dashboard)

## ✨ Features

### 🎯 Core Functionality
- **User Authentication**: Secure login/registration with JWT tokens
- **Time Slot Management**: 24 time slots (12:00-23:30) with 30-minute intervals
- **Booking System**: Book and cancel racing slots for up to 10 racers per slot
- **Real-time Updates**: Live availability tracking and booking status
- **Event Logging**: Complete audit trail of all booking activities
- **Data Persistence**: Automatic saving and loading of bookings and user data

### 🎨 Modern UI/UX
- **Responsive Design**: Optimized for desktop, tablet, and mobile
- **Beautiful Landing Page**: Engaging hero section with gradient backgrounds
- **Interactive Dashboard**: Intuitive booking management interface
- **Modal System**: Clean popup forms for booking and cancellation
- **Real-time Feedback**: Success/error messages with auto-dismiss
- **Professional Styling**: Modern cards, buttons, and layouts

### 🛠️ Technical Features
- **Next.js 14**: Latest App Router with server-side rendering
- **TypeScript**: Full type safety and IntelliSense support
- **Tailwind CSS**: Utility-first styling with custom components
- **API Routes**: RESTful backend with proper error handling
- **JWT Authentication**: Secure token-based authentication
- **JSON Storage**: File-based data persistence (easily upgradeable to database)
- **Vercel Ready**: Optimized configuration for seamless deployment

## 🚀 Quick Start

### Demo Access
Try the live demo with these credentials:
- **Username**: `admin`
- **Password**: `admin`

### Local Development

1. **Clone and Install**
   ```bash
   git clone <your-repo-url>
   cd go-karting-booking-system
   npm install
   ```

2. **Environment Setup**
   ```bash
   cp .env.local.example .env.local
   # Edit .env.local with your JWT secret
   ```

3. **Run Development Server**
   ```bash
   npm run dev
   ```

4. **Open Browser**
   Navigate to [http://localhost:3000](http://localhost:3000)

## 📁 Project Structure

```
├── app/                      # Next.js App Router
│   ├── api/                  # API Routes
│   │   ├── auth/            # Authentication endpoints
│   │   ├── bookings/        # Booking management
│   │   └── logs/            # Event logging
│   ├── dashboard/           # Protected dashboard page
│   ├── globals.css          # Global styles
│   ├── layout.tsx           # Root layout
│   └── page.tsx             # Landing page
├── lib/                     # Utility libraries
│   ├── auth.ts              # JWT authentication
│   ├── storage.ts           # Data persistence
│   ├── timeSlots.ts         # Time slot management
│   └── types.ts             # TypeScript definitions
├── data/                    # Runtime data storage
│   ├── users.json           # User accounts
│   ├── bookings.json        # Time slot bookings
│   └── logs.json            # Event logs
└── public/                  # Static assets
```

## 🔌 API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - New user registration

### Bookings
- `GET /api/bookings` - Fetch all time slots
- `POST /api/bookings` - Book or cancel a slot

### Logs
- `GET /api/logs` - Retrieve event logs

## 🎮 Usage Guide

### For Users
1. **Landing Page**: Sign up or log in to access the system
2. **Dashboard**: View available time slots and current bookings
3. **Book Slot**: Click "Book a Slot" and select your preferred time
4. **Cancel Booking**: Use "Cancel Booking" to remove reservations
5. **View History**: Check "View Logs" for booking activity

### For Administrators
- Default admin account: `admin` / `admin`
- Full access to all booking operations
- Complete event log access
- User registration management

## 🚀 Deployment on Vercel

### One-Click Deploy
[![Deploy with Vercel](https://vercel.com/button)](https://vercel.com/new/clone?repository-url=https://github.com/your-username/go-karting-booking-system)

### Manual Deployment
1. **Connect Repository**
   ```bash
   vercel --prod
   ```

2. **Set Environment Variables**
   In Vercel Dashboard > Settings > Environment Variables:
   ```
   JWT_SECRET=your-production-jwt-secret-key-here
   NODE_ENV=production
   ```

3. **Deploy**
   ```bash
   vercel deploy --prod
   ```

## 🛡️ Security Features

- **JWT Authentication**: Secure token-based auth with expiration
- **Password Hashing**: bcrypt for secure password storage
- **Input Validation**: Server-side validation for all endpoints
- **Protected Routes**: Dashboard requires valid authentication
- **Environment Variables**: Sensitive data in environment config

## 🔧 Configuration

### Environment Variables
```bash
JWT_SECRET=your-secret-key-here    # JWT signing secret
NODE_ENV=development               # Environment mode
```

### Time Slot Configuration
Default: 24 slots from 12:00-23:30 (30-min intervals, 10 racers each)

Modify in `lib/timeSlots.ts`:
```typescript
export function generateTimeSlots(): TimeSlot[] {
  // Customize start time, duration, capacity, etc.
}
```

## 🎯 Features Comparison

| Feature | Original Java App | New Web App |
|---------|------------------|-------------|
| User Interface | Swing GUI | Modern Web UI |
| Authentication | Local storage | JWT + Secure storage |
| Time Slots | 24 slots | 24 slots (configurable) |
| Capacity | 10 racers | 10 racers (configurable) |
| Data Persistence | JSON files | JSON files (DB ready) |
| Deployment | Desktop only | Web + Mobile responsive |
| Event Logging | Console output | Persistent logs with UI |
| Real-time Updates | Manual refresh | Automatic updates |

## 🚀 Future Enhancements

### Planned Features
- [ ] Database integration (PostgreSQL/MongoDB)
- [ ] Email notifications for bookings
- [ ] Calendar integration
- [ ] Payment processing
- [ ] Multiple date support
- [ ] Recurring bookings
- [ ] Admin dashboard with analytics
- [ ] SMS notifications
- [ ] Social login (Google, Facebook)
- [ ] Booking history export

### Technical Improvements
- [ ] Redis caching
- [ ] Rate limiting
- [ ] Advanced logging with Winston
- [ ] Monitoring with Sentry
- [ ] CI/CD pipeline
- [ ] Unit and integration tests
- [ ] Performance optimization
- [ ] PWA capabilities

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Documentation**: Check this README and inline code comments
- **Issues**: Use GitHub Issues for bug reports and feature requests
- **Discussions**: GitHub Discussions for questions and ideas

## 🙏 Acknowledgments

- Original Java application for functionality inspiration
- Next.js team for the amazing framework
- Tailwind CSS for the utility-first styling approach
- Lucide React for beautiful icons
- Vercel for seamless deployment platform

---

**Built with ❤️ using Next.js 14, TypeScript, and Tailwind CSS**