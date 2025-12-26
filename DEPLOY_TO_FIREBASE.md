# Deploy to Firebase Hosting - Quick Guide

## Prerequisites

1. **Firebase CLI installed:**
   ```bash
   npm install -g firebase-tools
   ```

2. **Logged in to Firebase:**
   ```bash
   firebase login
   ```

3. **Verify project:**
   ```bash
   firebase use default
   ```
   Should show: `Now using project pmd-cms-04653171`

## Deployment Steps

### Step 1: Build the Next.js App

```bash
cd admin-panel
npm run build
```

This creates the static export in `admin-panel/out/` directory.

### Step 2: Deploy to Firebase Hosting

```bash
cd ..
firebase deploy --only hosting
```

Or use the convenient script:

```bash
cd admin-panel
npm run deploy
```

### Step 3: Access Your App

After deployment, your app will be available at:
- **Primary URL:** `https://pmd-cms-04653171.web.app`
- **Alternative URL:** `https://pmd-cms-04653171.firebaseapp.com`

## Important Notes

### ⚠️ API Routes Limitation

**Current setup uses static export**, which means:
- ✅ Frontend pages work perfectly
- ❌ API routes (`/api/documents`, `/api/gallery`) **won't work**

### Solutions for API Routes

**Option 1: Move API logic to Cloud Functions**
- Create Firebase Cloud Functions for each API endpoint
- Update frontend to call Cloud Functions instead of Next.js API routes

**Option 2: Use Vercel (Recommended for Next.js)**
- Vercel supports Next.js API routes out of the box
- Zero configuration needed
- Free tier available
- Import from GitHub: https://vercel.com

**Option 3: Use Firebase Hosting + Cloud Run**
- More complex setup
- Requires Docker containerization
- Better for production scale

## Environment Variables

If your app uses environment variables (`.env.local`), you need to:

1. **For Firebase Hosting:** Set them in Firebase Console > Hosting > Environment Variables
2. **For Cloud Functions:** Set them in Firebase Console > Functions > Configuration

Or create a `.env.production` file (make sure it's in `.gitignore`).

## Custom Domain

To use a custom domain:

1. Go to Firebase Console > Hosting
2. Click "Add custom domain"
3. Follow the DNS configuration steps

## Continuous Deployment

### Option 1: GitHub Actions

Create `.github/workflows/firebase-deploy.yml`:

```yaml
name: Deploy to Firebase Hosting

on:
  push:
    branches:
      - main

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      - run: cd admin-panel && npm install && npm run build
      - uses: FirebaseExtended/action-hosting-deploy@v0
        with:
          repoToken: '${{ secrets.GITHUB_TOKEN }}'
          firebaseServiceAccount: '${{ secrets.FIREBASE_SERVICE_ACCOUNT }}'
          projectId: pmd-cms-04653171
          channelId: live
```

### Option 2: Manual Deployment

Just run:
```bash
cd admin-panel
npm run deploy
```

## Troubleshooting

### Build Errors
- Make sure all dependencies are installed: `npm install`
- Check Node.js version: `node --version` (should be 18+)
- Clear build cache: `rm -rf .next out`

### Deployment Errors
- Verify Firebase login: `firebase login`
- Check project: `firebase use default`
- Verify `firebase.json` configuration

### 404 Errors on Routes
- This is normal for client-side routing
- Firebase Hosting rewrites should handle this
- Check `firebase.json` rewrites configuration

### API Routes Not Working
- This is expected with static export
- Need to use Cloud Functions or different hosting

## Next Steps

1. ✅ Deploy static version (current setup)
2. 🔄 Set up Cloud Functions for API routes (if needed)
3. 🌐 Consider Vercel for easier Next.js deployment
4. 📝 Set up custom domain (optional)

