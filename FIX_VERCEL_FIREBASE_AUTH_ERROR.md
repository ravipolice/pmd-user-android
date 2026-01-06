# Fix Firebase Unauthorized Domain Error on Vercel

## Problem
Your Vercel deployment is showing: `Firebase: Error (auth/unauthorized-domain)`

This happens because Firebase Authentication only allows requests from domains that are explicitly authorized in your Firebase project settings.

## Solution: Add Vercel Domain to Firebase Authorized Domains

### Step 1: Get Your Vercel Domain

Your Vercel app is accessible at one of these URLs:
- `https://your-project-name.vercel.app` (default)
- `https://your-custom-domain.com` (if you added a custom domain)

### Step 2: Add Domain to Firebase Console

1. **Go to Firebase Console**
   - Visit: https://console.firebase.google.com/
   - Select your project: `pmd-police-mobile-directory`

2. **Navigate to Authentication Settings**
   - Click on **Authentication** in the left sidebar
   - Click on **Sign-in method** tab
   - Scroll down to **Authorized domains** section

3. **Add Your Vercel Domain**
   - Click **Add domain** button
   - Enter your Vercel domain (e.g., `your-project-name.vercel.app`)
   - **Important**: Enter ONLY the domain, NOT the full URL
     - ✅ Correct: `your-project-name.vercel.app`
     - ❌ Wrong: `https://your-project-name.vercel.app`
   - Click **Add**

4. **Add All Vercel Domains (if needed)**
   - If you have multiple deployments, add:
     - `your-project-name.vercel.app` (production)
     - `your-project-name-git-main.vercel.app` (if using preview deployments)
     - Your custom domain (if you have one)

### Step 3: Wait for Propagation

- Changes may take 1-5 minutes to propagate
- Clear your browser cache or use incognito mode
- Try accessing your app again

## Quick Checklist

- [ ] Identified your Vercel deployment domain
- [ ] Opened Firebase Console → Authentication → Sign-in method
- [ ] Added Vercel domain to Authorized domains
- [ ] Waited a few minutes for changes to propagate
- [ ] Tested authentication again

## Common Domains to Add

If you're not sure which domain to add, check your Vercel dashboard:
- Production: `your-project-name.vercel.app`
- Preview: `your-project-name-git-branch-name.vercel.app`
- Custom domain: `your-custom-domain.com`

## Additional Notes

### For Development (localhost)
- `localhost` is already authorized by default
- `127.0.0.1` may need to be added if you use IP address

### For Multiple Environments
If you have staging/production environments:
- Add both domains
- Or use environment variables to switch Firebase projects

## Still Having Issues?

1. **Check the exact domain in browser console**
   - Open browser DevTools → Console
   - Look for the exact domain in error messages

2. **Verify Firebase Project**
   - Make sure you're using the correct Firebase project
   - Check `lib/firebase/config.ts` matches your Firebase project

3. **Check Vercel Environment Variables**
   - Ensure Firebase config is correct in Vercel
   - Go to Vercel Dashboard → Settings → Environment Variables

## Example: Adding Domain

```
Firebase Console → Authentication → Sign-in method → Authorized domains

Current authorized domains:
- localhost
- your-project.firebaseapp.com
- your-project.web.app

Click "Add domain"
Enter: your-project-name.vercel.app
Click "Add"

New authorized domains:
- localhost
- your-project.firebaseapp.com
- your-project.web.app
- your-project-name.vercel.app  ← New!
```





