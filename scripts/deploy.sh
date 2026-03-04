#!/bin/bash

# Go-Karting Booking System - Deployment Script
# This script helps deploy the application to Vercel

echo "🏎️ Go-Karting Booking System - Deployment Script"
echo "================================================"

# Check if Vercel CLI is installed
if ! command -v vercel &> /dev/null; then
    echo "❌ Vercel CLI is not installed. Installing..."
    npm install -g vercel
else
    echo "✅ Vercel CLI is installed"
fi

# Check if we're in the right directory
if [ ! -f "package.json" ]; then
    echo "❌ package.json not found. Please run this script from the project root."
    exit 1
fi

echo "📦 Installing dependencies..."
npm install

echo "🔧 Building the project..."
npm run build

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
else
    echo "❌ Build failed. Please fix the errors and try again."
    exit 1
fi

echo "🚀 Deploying to Vercel..."
vercel --prod

echo ""
echo "🎉 Deployment complete!"
echo ""
echo "📝 Don't forget to set these environment variables in your Vercel dashboard:"
echo "   JWT_SECRET=your-super-secret-jwt-key"
echo "   NODE_ENV=production"
echo ""
echo "🔗 Your application should be live at the URL provided above."
echo "👤 Demo login: admin / admin" 