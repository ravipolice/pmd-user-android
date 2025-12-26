# Fix Next.js ChunkLoadError

## 🔍 Issue

**Error:** `ChunkLoadError: Loading chunk app/layout failed. (timeout)`

This usually means:
- Next.js dev server crashed or is stuck
- Build cache is corrupted
- Port conflict or network issue

## ✅ Quick Fix Steps

### Step 1: Stop the Dev Server
Press `Ctrl+C` in the terminal where Next.js is running to stop it completely.

### Step 2: Clear Next.js Cache
Delete the `.next` folder:

**Windows (PowerShell):**
```powershell
cd admin-panel
Remove-Item -Recurse -Force .next
```

**Or manually:**
- Navigate to `admin-panel/` folder
- Delete the `.next` folder

### Step 3: Clear Node Modules (Optional but Recommended)
If the issue persists:

```powershell
Remove-Item -Recurse -Force node_modules
Remove-Item package-lock.json
npm install
```

### Step 4: Restart Dev Server
```powershell
npm run dev
```

## 🔧 Alternative: Update Next.js

The error mentions Next.js 14.2.35 is outdated. You can update:

```powershell
npm install next@latest react@latest react-dom@latest
```

Then restart:
```powershell
npm run dev
```

## 🐛 If Still Not Working

### Check Port 3000
Make sure port 3000 is not in use by another process:

**Windows:**
```powershell
netstat -ano | findstr :3000
```

If something is using it, kill the process or use a different port:
```powershell
$env:PORT=3001; npm run dev
```

### Check for File System Issues
- Make sure you have write permissions in the `admin-panel/` directory
- Check if antivirus is blocking file access
- Try running as administrator

### Nuclear Option: Full Clean
```powershell
# Stop server
# Delete everything
Remove-Item -Recurse -Force .next
Remove-Item -Recurse -Force node_modules
Remove-Item package-lock.json

# Reinstall
npm install

# Restart
npm run dev
```

## ✅ Expected Result

After clearing cache and restarting, you should see:
```
▲ Next.js 14.2.x
- Local:        http://localhost:3000
- Ready in X.Xs
```

The error should be gone and the app should load normally.



