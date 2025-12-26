# Firebase Admin SDK Setup Guide

## Quick Setup for Gallery Uploads

### Step 1: Download Service Account Key

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: `pmd-police-mobile-directory`
3. Click the gear icon ⚙️ → **Project Settings**
4. Go to **Service Accounts** tab
5. Click **Generate New Private Key**
6. Save the JSON file (e.g., `firebase-service-account.json`)

### Step 2: Add to Project

1. Place the JSON file in the `admin-panel` directory (or a secure location)
2. **IMPORTANT:** Add to `.gitignore`:
   ```
   firebase-service-account.json
   *.json
   !package.json
   !tsconfig.json
   ```

### Step 3: Set Environment Variable

Create or update `.env.local` in the `admin-panel` directory:

```env
GOOGLE_APPLICATION_CREDENTIALS=./firebase-service-account.json
```

Or use absolute path:
```env
GOOGLE_APPLICATION_CREDENTIALS=C:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\admin-panel\firebase-service-account.json
```

### Step 4: Restart Dev Server

```bash
# Stop the current server (Ctrl+C)
# Then restart
cd admin-panel
npm run dev
```

### Step 5: Verify

Check the server console - you should see:
```
✅ Firebase Admin SDK initialized successfully
```

## Alternative: Update Storage Rules (Development Only)

If you can't set up Admin SDK right now, you can temporarily update Firebase Storage rules:

1. Go to Firebase Console → Storage → Rules
2. Add this rule before the default rule:

```javascript
// 🔹 Gallery images
match /gallery/{allPaths=**} {
  allow read: if true; // Anyone can view
  allow write: if request.auth != null
               && request.auth.token.email == "ravipolice@gmail.com";
}
```

⚠️ **Warning:** This still requires authentication. For server-side uploads without user auth, Admin SDK is required.

