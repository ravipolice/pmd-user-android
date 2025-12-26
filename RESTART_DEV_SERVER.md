# Restart Dev Server for Gallery Changes

## Important: API Route Changes Require Server Restart

Since we added a new API route (`/api/gallery/delete`), you **must restart** the Next.js dev server for the changes to take effect.

### Steps to Restart:

1. **Stop the current dev server:**
   - Press `Ctrl+C` in the terminal where `npm run dev` is running
   - Or close the terminal window

2. **Clear the build cache (optional but recommended):**
   ```powershell
   cd admin-panel
   Remove-Item -Recurse -Force .next -ErrorAction SilentlyContinue
   ```

3. **Restart the dev server:**
   ```powershell
   npm run dev
   ```

4. **Hard refresh the browser:**
   - Press `Ctrl+Shift+R` (Windows/Linux) or `Cmd+Shift+R` (Mac)
   - Or open DevTools (F12) → Right-click refresh button → "Empty Cache and Hard Reload"

### What Changed:

✅ Added delete functionality to Gallery page
✅ Created `/api/gallery/delete` API route
✅ Added delete buttons (appear on hover over images)
✅ Fixed image display issues

### After Restart:

- Delete buttons should appear when you hover over gallery images
- Images should display correctly
- Delete functionality should work



