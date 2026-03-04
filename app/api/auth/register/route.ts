import { NextRequest, NextResponse } from 'next/server';
import { loadUsers, saveUsers, addEventLog } from '@/lib/storage';
import { hashPassword, generateToken } from '@/lib/auth';
import { ApiResponse, User } from '@/lib/types';

export async function POST(request: NextRequest) {
  try {
    const { username, password } = await request.json();

    if (!username || !password) {
      return NextResponse.json<ApiResponse<null>>({
        success: false,
        message: 'Username and password are required'
      }, { status: 400 });
    }

    if (username.length < 5) {
      return NextResponse.json<ApiResponse<null>>({
        success: false,
        message: 'Username must be at least 5 characters long'
      }, { status: 400 });
    }

    if (password.length < 5) {
      return NextResponse.json<ApiResponse<null>>({
        success: false,
        message: 'Password must be at least 5 characters long'
      }, { status: 400 });
    }

    const users = loadUsers();
    const existingUser = users.find(u => u.username === username);

    if (existingUser) {
      return NextResponse.json<ApiResponse<null>>({
        success: false,
        message: 'Username already exists'
      }, { status: 409 });
    }

    const hashedPassword = hashPassword(password);
    const newUser: User = {
      username,
      password: hashedPassword
    };

    users.push(newUser);
    saveUsers(users);
    
    const token = generateToken(username);
    addEventLog(`New user registered: ${username}`);

    return NextResponse.json<ApiResponse<{ token: string }>>({
      success: true,
      data: { token },
      message: 'Registration successful'
    });

  } catch (error) {
    console.error('Registration error:', error);
    return NextResponse.json<ApiResponse<null>>({
      success: false,
      message: 'Internal server error'
    }, { status: 500 });
  }
} 