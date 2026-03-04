# 🚀 Deployment Guide

This guide will walk you through deploying the Go-Karting Booking System to Vercel.

## 🎯 Prerequisites

1. **GitHub Account**: Your code should be in a GitHub repository
2. **Vercel Account**: Sign up at [vercel.com](https://vercel.com)
3. **Node.js**: Version 16 or later installed locally

## 📋 Quick Deployment (Recommended)

### Option 1: One-Click Deploy
[![Deploy with Vercel](https://vercel.com/button)](https://vercel.com/new/clone?repository-url=https://github.com/your-username/go-karting-booking-system)

### Option 2: Vercel Dashboard
1. Go to [vercel.com/dashboard](https://vercel.com/dashboard)
2. Click "New Project"
3. Import your GitHub repository
4. Vercel will auto-detect it's a Next.js project
5. Click "Deploy"

## 🔧 Manual Deployment

### Step 1: Install Vercel CLI
```bash
npm install -g vercel
```

### Step 2: Login to Vercel
```bash
vercel login
```

### Step 3: Deploy
```bash
# From your project directory
vercel --prod
```

### Step 4: Use Deployment Script
```bash
chmod +x scripts/deploy.sh
./scripts/deploy.sh
```

## ⚙️ Environment Variables

After deployment, set these in your Vercel dashboard:

1. Go to your project dashboard on Vercel
2. Navigate to Settings → Environment Variables
3. Add the following:

| Variable | Value | Description |
|----------|-------|-------------|
| `JWT_SECRET` | `your-super-secret-jwt-key-here` | JWT signing secret (must be 32+ characters) |
| `NODE_ENV` | `production` | Environment mode |

### Generating a Secure JWT Secret
```bash
# Option 1: Using Node.js
node -e "console.log(require('crypto').randomBytes(64).toString('hex'))"

# Option 2: Using OpenSSL
openssl rand -hex 64

# Option 3: Online generator
# Visit: https://generate-random.org/string-generator
```

## 🌐 Custom Domain (Optional)

1. In your Vercel project dashboard
2. Go to Settings → Domains
3. Add your custom domain
4. Follow Vercel's DNS configuration instructions

## 🔍 Verification

After deployment, verify everything works:

### 1. Landing Page
- Visit your deployment URL
- Check if the landing page loads properly
- Test the responsive design

### 2. Authentication
- Test user registration
- Test login with demo credentials:
  - Username: `admin`
  - Password: `admin`

### 3. Booking System
- View available time slots
- Book a test slot
- Cancel a booking
- Check event logs

### 4. API Endpoints
Test these endpoints manually or with curl:

```bash
# Health check (should load the landing page)
curl https://your-app.vercel.app

# Login test
curl -X POST https://your-app.vercel.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

## 🐛 Troubleshooting

### Common Issues

#### Build Failures
```bash
# Clear cache and reinstall
rm -rf .next node_modules package-lock.json
npm install
npm run build
```

#### Environment Variable Issues
- Ensure JWT_SECRET is set in Vercel dashboard
- Check variable names match exactly
- Redeploy after adding variables

#### API Route 500 Errors
- Check Vercel function logs in dashboard
- Verify file permissions for data directory
- Ensure all dependencies are in package.json

#### Authentication Issues
- Clear browser localStorage
- Check JWT_SECRET is properly set
- Verify token expiration (24h default)

### Debugging Steps

1. **Check Vercel Logs**
   ```bash
   vercel logs your-app-url
   ```

2. **Local Testing**
   ```bash
   npm run build
   npm start
   ```

3. **Environment Variables**
   ```bash
   vercel env ls
   ```

## 📊 Performance Optimization

### Automatic Optimizations
Vercel provides these out of the box:
- CDN distribution
- Image optimization
- Automatic HTTPS
- Gzip compression
- Cache headers

### Manual Optimizations
1. **Static Generation**: Already configured for landing page
2. **API Caching**: Consider Redis for production
3. **Image Optimization**: Use Next.js Image component
4. **Bundle Analysis**: 
   ```bash
   npm install --save-dev @next/bundle-analyzer
   ```

## 🔐 Security Considerations

### Production Checklist
- [ ] Strong JWT_SECRET (64+ characters)
- [ ] Environment variables set in Vercel
- [ ] HTTPS enabled (automatic with Vercel)
- [ ] Rate limiting (consider implementing)
- [ ] Input validation (already implemented)
- [ ] CORS headers (Next.js handles this)

### Additional Security
Consider adding:
- Rate limiting middleware
- Request logging
- Monitoring with Sentry
- Regular security audits

## 📈 Monitoring

### Vercel Analytics
Enable in your dashboard:
1. Go to Analytics tab
2. Enable Vercel Analytics
3. View real-time usage data

### Custom Monitoring
Consider integrating:
- Sentry for error tracking
- LogRocket for session replay
- Google Analytics for user insights

## 🔄 CI/CD Setup

### Automatic Deployments
Vercel automatically deploys:
- `main` branch → Production
- Other branches → Preview deployments

### GitHub Integration
1. Connect repository in Vercel
2. Enable automatic deployments
3. Configure branch protection rules

### Custom Build Commands
In `vercel.json`:
```json
{
  "buildCommand": "npm run build",
  "installCommand": "npm install"
}
```

## 📚 Additional Resources

- [Vercel Documentation](https://vercel.com/docs)
- [Next.js Deployment](https://nextjs.org/docs/deployment)
- [Environment Variables Guide](https://vercel.com/docs/concepts/projects/environment-variables)
- [Custom Domains](https://vercel.com/docs/concepts/projects/custom-domains)

## 🆘 Getting Help

If you encounter issues:

1. **Vercel Support**: [vercel.com/support](https://vercel.com/support)
2. **GitHub Issues**: Create an issue in your repository
3. **Community**: Next.js Discord, Stack Overflow
4. **Documentation**: This guide and official docs

---

**Happy Deploying! 🚀** 