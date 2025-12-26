# Setup Guide

## Quick Start

1. **Install Dependencies**
   ```bash
   cd admin-panel
   npm install
   ```

2. **Configure Firebase**
   - Open `lib/firebase/config.ts`
   - Update the `firebaseConfig` object with your Firebase project credentials:
     - `apiKey`
     - `authDomain`
     - `projectId`
     - `storageBucket`
     - `messagingSenderId` (optional)
     - `appId` (optional)

3. **Enable Google Authentication in Firebase Console**
   - Go to Firebase Console > Authentication > Sign-in method
   - Enable Google provider
   - Add authorized domains if needed

4. **Run Development Server**
   ```bash
   npm run dev
   ```

5. **Access the Admin Panel**
   - Open http://localhost:3000
   - Sign in with Google
   - Make sure your email is marked as admin in Firestore

## Admin Setup

To make a user an admin, add their email or UID to the `admins` collection in Firestore:

```javascript
// Option 1: Using email as document ID
admins/{email} {
  email: "admin@example.com",
  isActive: true,
  uid: "firebase-uid-here",
  updatedAt: Timestamp
}

// Option 2: Using UID as document ID
admins/{uid} {
  email: "admin@example.com",
  isActive: true,
  uid: "firebase-uid-here",
  updatedAt: Timestamp
}
```

Alternatively, set `isAdmin: true` in the employee document.

## Environment Variables (Optional)

You can create a `.env.local` file for environment-specific configuration:

```env
NEXT_PUBLIC_FIREBASE_API_KEY=your-api-key
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=your-auth-domain
NEXT_PUBLIC_FIREBASE_PROJECT_ID=your-project-id
NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=your-storage-bucket

# Apps Script API Token (Required for Documents API)
APPS_SCRIPT_SECRET_TOKEN=your-secret-token-here
```

**Important**: The `APPS_SCRIPT_SECRET_TOKEN` is required to fetch documents from the Google Apps Script API. This should be the same token used in the mobile app's `SecurityConfig`. Add it to `.env.local` (which is gitignored for security).

Then update `lib/firebase/config.ts` to use these variables:

```typescript
const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
  // ...
};
```

## Troubleshooting

### Authentication Issues
- Make sure Google provider is enabled in Firebase Console
- Check that your email is in the `admins` collection or has `isAdmin: true` in employees

### Firestore Permission Errors
- Verify Firestore security rules allow admin access
- Check that the user is properly authenticated

### Build Errors
- Run `npm install` to ensure all dependencies are installed
- Clear `.next` folder and rebuild: `rm -rf .next && npm run build`

## Exporting the Project

After completing setup, you can export your Next.js project in several ways:

### Option 1: Static Export (Recommended for Static Hosting)

This creates a fully static site that can be hosted on any static hosting service (Firebase Hosting, Netlify, Vercel, GitHub Pages, etc.).

1. **Update `next.config.js`** to enable static export:
   ```javascript
   /** @type {import('next').NextConfig} */
   const nextConfig = {
     output: 'export',
     reactStrictMode: true,
     images: {
       unoptimized: true, // Required for static export
       domains: ['firebasestorage.googleapis.com'],
     },
   };
   
   module.exports = nextConfig;
   ```

2. **Build and Export**:
   ```bash
   npm run build
   ```

3. **Export Location**:
   - The exported static files will be in the `out/` directory
   - You can deploy the entire `out/` folder to any static hosting service

### Option 2: Production Build (For Node.js Server)

This creates an optimized production build that requires a Node.js server.

1. **Build the project**:
   ```bash
   npm run build
   ```

2. **Start the production server**:
   ```bash
   npm start
   ```

3. **Deploy**:
   - Deploy to platforms that support Node.js (Vercel, Railway, Heroku, etc.)
   - Or use your own server with Node.js installed

### Option 3: Standalone Build (Self-Contained)

This creates a minimal, self-contained build with only necessary dependencies.

1. **Update `next.config.js`**:
   ```javascript
   /** @type {import('next').NextConfig} */
   const nextConfig = {
     output: 'standalone',
     reactStrictMode: true,
     images: {
       domains: ['firebasestorage.googleapis.com'],
     },
   };
   
   module.exports = nextConfig;
   ```

2. **Build**:
   ```bash
   npm run build
   ```

3. **Deploy**:
   - The `.next/standalone` folder contains a minimal Node.js server
   - Copy this folder along with `.next/static` to your deployment target

### Deployment Recommendations

- **Firebase Hosting**: Use Option 1 (Static Export)
- **Vercel**: Use Option 2 (Production Build) - Vercel handles it automatically
- **Netlify**: Use Option 1 (Static Export)
- **Custom Server**: Use Option 2 or Option 3

### Important Notes

- **Static Export (Option 1)**: Best for client-side only apps. All Firebase operations will work client-side.
- **Production Build (Option 2)**: Better if you plan to add API routes or server-side features later.
- Make sure your Firebase configuration is set up before exporting.
- Environment variables must be set in your hosting platform if using `.env.local`.

## Deploying to Firebase Hosting

Your project is already configured for Firebase Hosting deployment to **pmd-cms-04653171**.

### Prerequisites

1. **Install Firebase CLI** (if not already installed):
   ```bash
   npm install -g firebase-tools
   ```

2. **Login to Firebase**:
   ```bash
   firebase login
   ```

3. **Verify Firebase Project**:
   ```bash
   firebase use default
   ```
   This should show: `Now using project pmd-cms-04653171`

### Deployment Steps

1. **Navigate to admin-panel directory**:
   ```bash
   cd admin-panel
   ```

2. **Build and Deploy** (using the convenient script):
   ```bash
   npm run deploy
   ```
   
   Or manually:
   ```bash
   npm run build
   cd ..
   firebase deploy --only hosting
   ```

3. **Deploy Everything** (if you want to deploy Firestore rules and functions too):
   ```bash
   cd admin-panel
   npm run build
   cd ..
   firebase deploy
   ```

### After Deployment

- Your admin panel will be available at: `https://pmd-cms-04653171.web.app` or `https://pmd-cms-04653171.firebaseapp.com`
- You can also set up a custom domain in Firebase Console > Hosting

### Firebase Hosting Configuration

The project is configured with:
- **Public directory**: `admin-panel/out` (Next.js static export output)
- **Rewrites**: All routes redirect to `index.html` for client-side routing
- **Project ID**: `pmd-cms-04653171`

### Troubleshooting Deployment

- **Build fails**: Make sure all dependencies are installed (`npm install`)
- **Deploy fails**: Verify you're logged in (`firebase login`) and using the correct project (`firebase use default`)
- **404 errors on routes**: The rewrite rules in `firebase.json` should handle this, but verify the build completed successfully
- **Firebase config errors**: Make sure `lib/firebase/config.ts` has the correct Firebase project credentials

