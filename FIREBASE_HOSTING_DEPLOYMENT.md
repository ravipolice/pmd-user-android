# Firebase Hosting Deployment Guide

This guide explains how to deploy your Next.js admin panel to Firebase Hosting with API routes support.

## Current Setup

Your project is configured to deploy to Firebase project: **pmd-cms-04653171**

## Deployment Options

### Option 1: Static Export (Current - API Routes Won't Work) ❌

**Current Configuration:**
- `next.config.js` uses `output: 'export'` for production
- This creates a static site but **disables API routes**
- Your API routes (`/api/documents`, `/api/gallery`) won't work

**When to use:** Only if you don't need API routes

### Option 2: Firebase Hosting + Cloud Functions (Recommended) ✅

**Best for:** Next.js apps with API routes

**How it works:**
1. Frontend (static pages) → Firebase Hosting
2. API routes → Cloud Functions
3. Firebase Hosting rewrites API calls to Cloud Functions

**Setup Steps:**

1. **Update `next.config.js`** (already done):
   ```javascript
   output: 'standalone'
   ```

2. **Create Cloud Functions wrapper** (see below)

3. **Update `firebase.json`** to handle both static and API routes

4. **Deploy:**
   ```bash
   npm run build
   firebase deploy
   ```

### Option 3: Vercel (Easiest for Next.js) 🚀

**Best for:** Next.js apps (Vercel is made by Next.js creators)

**Advantages:**
- Zero configuration
- Automatic API routes support
- Built-in CI/CD
- Free tier available

**Setup:**
1. Push code to GitHub (already done!)
2. Import project in Vercel
3. Deploy automatically

## Recommended: Firebase Hosting with Cloud Functions

Since you're already using Firebase, let's set up Cloud Functions for API routes.

### Step 1: Install Dependencies

```bash
cd admin-panel
npm install --save-dev @vercel/ncc
```

### Step 2: Create Cloud Functions Structure

We'll create a Cloud Function that serves your Next.js API routes.

### Step 3: Update firebase.json

```json
{
  "hosting": {
    "public": "admin-panel/out",
    "rewrites": [
      {
        "source": "/api/**",
        "function": "nextjsApi"
      },
      {
        "source": "**",
        "destination": "/index.html"
      }
    ]
  },
  "functions": {
    "source": "functions",
    "runtime": "nodejs18"
  }
}
```

## Quick Deploy (Current Static Setup)

If you want to deploy the static version now (without API routes):

```bash
cd admin-panel
npm run build
cd ..
firebase deploy --only hosting
```

**Note:** API routes won't work with this setup. You'll need to move API logic to Cloud Functions or use a different hosting solution.

## Environment Variables

Make sure to set environment variables in Firebase:

1. Go to Firebase Console > Functions > Configuration
2. Add environment variables:
   - `APPS_SCRIPT_SECRET_TOKEN`
   - `NEXT_PUBLIC_FIREBASE_API_KEY`
   - `NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN`
   - etc.

Or use `.env.production` file (not committed to git).

## Troubleshooting

### Build Fails
- Check Node.js version (should be 18+)
- Run `npm install` to ensure dependencies are installed
- Clear `.next` folder: `rm -rf .next`

### API Routes Return 404
- Static export doesn't support API routes
- Need to use Cloud Functions or different hosting

### Authentication Issues
- Verify Firebase config in `lib/firebase/config.ts`
- Check Firestore security rules
- Ensure Google OAuth is enabled in Firebase Console

## Next Steps

1. **For now:** Deploy static version to see the UI
2. **Later:** Set up Cloud Functions for API routes
3. **Alternative:** Consider Vercel for easier Next.js deployment

