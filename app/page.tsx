'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Image from 'next/image';
import { Car, Clock, Users, CheckCircle } from 'lucide-react';

export default function LandingPage() {
  const [showLogin, setShowLogin] = useState(false);
  const [showRegister, setShowRegister] = useState(false);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const router = useRouter();

  const handleAuth = async (endpoint: string, username: string, password: string) => {
    setLoading(true);
    setMessage('');

    try {
      const response = await fetch(`/api/auth/${endpoint}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });

      const data = await response.json();

      if (data.success) {
        localStorage.setItem('token', data.data.token);
        localStorage.setItem('username', username);
        setMessage(data.message);
        setTimeout(() => router.push('/dashboard'), 1000);
      } else {
        setMessage(data.message);
      }
    } catch (error) {
      setMessage('Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const AuthForm = ({ type }: { type: 'login' | 'register' }) => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');

    const handleSubmit = (e: React.FormEvent) => {
      e.preventDefault();
      handleAuth(type, username, password);
    };

    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div className="bg-white rounded-lg p-8 max-w-md w-full mx-4">
          <h2 className="text-2xl font-bold mb-6 text-center">
            {type === 'login' ? 'Sign In' : 'Create Account'}
          </h2>
          
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Email/Username
              </label>
                             <input
                 type="text"
                 value={username}
                 onChange={(e) => setUsername(e.target.value)}
                 className="input-field"
                 required
                 minLength={5}
               />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Password
              </label>
                             <input
                 type="password"
                 value={password}
                 onChange={(e) => setPassword(e.target.value)}
                 className="input-field"
                 required
                 minLength={5}
               />
            </div>
            
            {message && (
              <div className={`text-sm p-2 rounded ${
                message.includes('successful') 
                  ? 'bg-green-100 text-green-700' 
                  : 'bg-red-100 text-red-700'
              }`}>
                {message}
              </div>
            )}
            
            <div className="flex gap-3">
              <button
                type="submit"
                disabled={loading}
                className="btn-primary flex-1"
              >
                {loading ? 'Processing...' : (type === 'login' ? 'Sign In' : 'Register')}
              </button>
              <button
                type="button"
                onClick={() => {
                  setShowLogin(false);
                  setShowRegister(false);
                  setMessage('');
                }}
                className="btn-secondary"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-900 via-blue-800 to-blue-600">
      {/* Hero Background */}
      <div className="absolute inset-0 bg-black bg-opacity-30"></div>
      
      {/* Navigation */}
      <nav className="relative z-10 p-6">
        <div className="max-w-7xl mx-auto flex justify-between items-center">
          <div className="flex items-center space-x-2">
            <Car className="w-8 h-8 text-white" />
            <span className="text-xl font-bold text-white">GoKart Pro</span>
          </div>
          
          <div className="space-x-4">
            <button 
              onClick={() => setShowLogin(true)}
              className="bg-white bg-opacity-20 text-white px-4 py-2 rounded-lg hover:bg-opacity-30 transition-all"
            >
              Sign In
            </button>
            <button 
              onClick={() => setShowRegister(true)}
              className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-all"
            >
              Get Started
            </button>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <div className="relative z-10 max-w-7xl mx-auto px-6 pt-20 pb-32">
        <div className="text-center">
          <h1 className="text-5xl md:text-6xl font-bold text-white mb-6">
            Book Your
            <span className="bg-gradient-to-r from-yellow-400 to-orange-500 bg-clip-text text-transparent">
              {' '}Racing Experience
            </span>
          </h1>
          
          <p className="text-xl text-blue-100 mb-8 max-w-2xl mx-auto">
            Experience the thrill of go-kart racing with our modern booking system. 
            Choose your time slot, gather your friends, and get ready for an adrenaline-fueled adventure!
          </p>
          
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <button 
              onClick={() => setShowRegister(true)}
              className="bg-gradient-to-r from-orange-500 to-red-600 text-white px-8 py-3 rounded-lg text-lg font-semibold hover:from-orange-600 hover:to-red-700 transition-all transform hover:scale-105"
            >
              Book Now
            </button>
            <button 
              onClick={() => setShowLogin(true)}
              className="bg-white bg-opacity-20 text-white px-8 py-3 rounded-lg text-lg font-semibold hover:bg-opacity-30 transition-all"
            >
              Existing User
            </button>
          </div>
        </div>
      </div>

      {/* Features Section */}
      <div className="relative z-10 bg-white bg-opacity-10 backdrop-blur-lg">
        <div className="max-w-7xl mx-auto px-6 py-16">
          <div className="grid md:grid-cols-3 gap-8">
            <div className="text-center">
              <Clock className="w-12 h-12 text-yellow-400 mx-auto mb-4" />
              <h3 className="text-xl font-semibold text-white mb-2">24 Time Slots</h3>
              <p className="text-blue-100">
                Choose from 24 available time slots throughout the day, 
                each lasting 30 minutes of pure racing excitement.
              </p>
            </div>
            
            <div className="text-center">
              <Users className="w-12 h-12 text-yellow-400 mx-auto mb-4" />
              <h3 className="text-xl font-semibold text-white mb-2">Up to 10 Racers</h3>
              <p className="text-blue-100">
                Each time slot accommodates up to 10 racers, perfect for 
                groups, parties, or competitive racing sessions.
              </p>
            </div>
            
            <div className="text-center">
              <CheckCircle className="w-12 h-12 text-yellow-400 mx-auto mb-4" />
              <h3 className="text-xl font-semibold text-white mb-2">Easy Management</h3>
              <p className="text-blue-100">
                Simple booking and cancellation system with real-time 
                availability updates and booking history.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Demo Section */}
      <div className="relative z-10 max-w-4xl mx-auto px-6 py-16 text-center">
        <h2 className="text-3xl font-bold text-white mb-4">Quick Demo</h2>
        <p className="text-blue-100 mb-6">
          Want to see how it works? Try logging in with these demo credentials:
        </p>
        <div className="bg-white bg-opacity-20 backdrop-blur-lg rounded-lg p-6 max-w-md mx-auto">
          <div className="text-white space-y-2">
            <p><strong>Username:</strong> admin</p>
            <p><strong>Password:</strong> admin</p>
          </div>
        </div>
      </div>

      {/* Auth Modals */}
      {showLogin && <AuthForm type="login" />}
      {showRegister && <AuthForm type="register" />}
    </div>
  );
} 