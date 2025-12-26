# Quick Deploy to Firebase Hosting

## ✅ Build Status

Your Next.js app has been successfully built! The static export is ready in the `out/` folder.

## 🚀 Deploy Now

### Step 1: Make sure you're logged in to Firebase

```bash
firebase login
```

### Step 2: Verify Firebase project

```bash
firebase use default
```

Should show: `Now using project pmd-cms-04653171`

### Step 3: Deploy to Firebase Hosting

From the **root directory** (not admin-panel):

```bash
firebase deploy --only hosting
```

Or use the convenient script from admin-panel:

```bash
cd admin-panel
npm run deploy
```

## 📍 Your App Will Be Available At:

- **Primary URL:** https://pmd-cms-04653171.web.app
- **Alternative URL:** https://pmd-cms-04653171.firebaseapp.com

## ⚠️ Important Notes

### API Routes Limitation

**Your current setup uses static export**, which means:

- ✅ **Frontend pages work perfectly** - All your React pages will work
- ❌ **API routes won't work** - `/api/documents`, `/api/gallery` endpoints won't function

### Why?

Static export creates a fully static site. Next.js API routes require a Node.js server, which Firebase Hosting doesn't provide by default.

### Solutions

**Option 1: Use Cloud Functions (Recommended for Firebase)**
- Move API logic to Firebase Cloud Functions
- Update frontend to call Cloud Functions URLs
- More setup required, but fully integrated with Firebase

**Option 2: Use Vercel (Easiest for Next.js)**
- Vercel supports Next.js API routes out of the box
- Zero configuration
- Import from GitHub: https://vercel.com
- Free tier available

**Option 3: Keep Static Export**
- Move API logic to client-side Firebase SDK calls
- Use Firebase directly from the browser
- Simpler, but less secure (client-side only)

## 🔧 Current Configuration

- **Build output:** `admin-panel/out/`
- **Firebase project:** `pmd-cms-04653171`
- **Hosting config:** `firebase.json`

## 📝 Next Steps After Deployment

1. ✅ Test your deployed app
2. 🔄 Set up API routes (if needed)
3. 🌐 Configure custom domain (optional)
4. 📊 Set up analytics (optional)

## 🆘 Troubleshooting

### Build fails
- Run `npm install` in admin-panel
- Clear `.next` folder: `rm -rf .next`
- Check Node.js version (18+)

### Deploy fails
- Verify Firebase login: `firebase login`
- Check project: `firebase use default`
- Verify `firebase.json` exists

### 404 errors on routes
- This is normal for client-side routing
- Firebase Hosting rewrites should handle this
- Check browser console for errors

### API routes return 404
- This is expected with static export
- Need to use Cloud Functions or different hosting

## 📚 More Information

See `DEPLOY_TO_FIREBASE.md` for detailed deployment guide.
See `FIREBASE_HOSTING_DEPLOYMENT.md` for deployment options.

