import { NextRequest, NextResponse } from 'next/server';
import { loadUsers, addEventLog } from '@/lib/storage';
import { verifyPassword, generateToken } from '@/lib/auth';
import { ApiResponse } from '@/lib/types';

export async function POST(request: NextRequest) {
  try {
    const { username, password } = await request.json();

    if (!username || !password) {
      return NextResponse.json<ApiResponse<null>>({
        success: false,
        message: 'Username and password are required'
      }, { status: 400 });
    }

    const users = loadUsers();
    const user = users.find(u => u.username === username);

    if (!user || !verifyPassword(password, user.password)) {
      addEventLog(`Failed login attempt for user: ${username}`);
      return NextResponse.json<ApiResponse<null>>({
        success: false,
        message: 'Invalid credentials'
      }, { status: 401 });
    }

    const token = generateToken(username);
    addEventLog(`User logged in: ${username}`);

    return NextResponse.json<ApiResponse<{ token: string }>>({
      success: true,
      data: { token },
      message: 'Login successful'
    });

  } catch (error) {
    console.error('Login error:', error);
    return NextResponse.json<ApiResponse<null>>({
      success: false,
      message: 'Internal server error'
    }, { status: 500 });
  }
} 