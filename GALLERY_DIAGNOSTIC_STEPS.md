# Gallery Diagnostic Steps

## 🔍 Root Cause Analysis

The Gallery page is not showing any content. Based on code analysis:

**Gallery reads from Google Sheets via Apps Script, NOT just Firestore!**

- Sheet ID: `1m8z-ryBbFTUsox-sEoZgFm_lOvbMb1_XjsaUky1VUjY`
- Sheet Name: `"Gallery"` (case-sensitive!)
- Collection: The sheet tab named "Gallery"
- Apps Script URL: Same as Documents (`AKfycby-7jOc_naI1_XDVzG1qAGvNc9w3tIU4ZwmCFGUUCLdg0_DEJh7oouF8a9iy5E93-p9zg`)
- Action: `getGallery`

## ✅ Step-by-Step Diagnosis

### Step 1: Check Google Sheet Directly

1. Open Google Sheets
2. Open the spreadsheet with ID: `1m8z-ryBbFTUsox-sEoZgFm_lOvbMb1_XjsaUky1VUjY`
3. Check if tab "Gallery" exists (exact name, case-sensitive)
4. Check if there are any data rows (excluding header row)

**Expected Sheet Structure:**
```
Title | URL | Category | UploadedBy | UploadedDate | Description | Delete
------|-----|----------|------------|--------------|-------------|-------
Image 1 | https://drive.google.com/uc?export=view&id=... | Gallery | ... | ... | ... |
```

**If sheet is empty → That's why API returns []**

### Step 2: Test Apps Script URL Directly

Open in browser (replace TOKEN with actual token):
```
https://script.google.com/macros/s/AKfycby-7jOc_naI1_XDVzG1qAGvNc9w3tIU4ZwmCFGUUCLdg0_DEJh7oouF8a9iy5E93-p9zg/exec?action=getGallery&token=YOUR_TOKEN
```

**Expected:**
- If token is correct → Returns JSON array of gallery images
- If token is wrong → Returns error or empty array
- If sheet is empty → Returns `[]`

### Step 3: Check Apps Script Logs

1. Open Google Apps Script Editor
2. Go to Executions tab
3. Check recent executions of `doGet` function with `action=getGallery`
4. Look for errors in logs

### Step 4: Verify Sheet Name

The code expects exactly: `"Gallery"` (capital G)

Common mistakes:
- ❌ "gallery" (lowercase)
- ❌ "Galleries" (plural)
- ❌ "Image Gallery" (different name)
- ✅ "Gallery" (correct)

### Step 5: Check Firestore Rules (if using Firestore)

Even though Apps Script uses Sheets, admin panel also tries Firestore:

```javascript
match /gallery/{imageId} {
  allow read: if isAuthenticated();  // ← Requires login
  allow create, update, delete: if isAdmin();
}
```

**Temporary test rule:**
```javascript
match /gallery/{imageId} {
  allow read: if true;  // ← Allow all reads temporarily
}
```

### Step 6: Check Next.js API Route

1. Open browser console (F12)
2. Navigate to Gallery page
3. Check Network tab for `/api/gallery` request
4. Verify response status and data

**Expected:**
- Status: 200
- Response: Array of image objects with `URL` or `imageUrl` field

## 🔧 Quick Fixes

### Fix 1: If Google Sheet is Empty
- Add test data to the "Gallery" sheet
- Make sure header row exists: `Title | URL | Category | UploadedBy | UploadedDate | Description | Delete`
- Add at least one data row with a valid image URL

### Fix 2: If Sheet Name is Wrong
- Rename the sheet tab to exactly: `"Gallery"`
- Or update `SHEET_NAME` in `GALLERY_Common.gs`

### Fix 3: If Token is Missing/Wrong
- Add `APPS_SCRIPT_SECRET_TOKEN` to `.env.local` in `admin-panel/` directory
- Restart Next.js dev server
- Verify token matches mobile app token

### Fix 4: If Apps Script Project is Wrong
- Check Apps Script project ID matches Firebase project
- Verify service account has access to the Google Sheet
- Ensure `getGallery()` function exists in Apps Script

### Fix 5: If Images Don't Display
- Check that image URLs are valid and publicly accessible
- Verify URLs are in format: `https://drive.google.com/uc?export=view&id=FILE_ID`
- Check browser console for image loading errors

## 📊 Expected Data Flow

```
Mobile App → Apps Script API → Google Sheets → Returns JSON
Admin Panel → Next.js API Route (/api/gallery) → Apps Script API → Google Sheets → Returns JSON
```

**NOT:**
```
Admin Panel → Firestore "gallery" collection only ❌
```

## 🎯 Most Likely Issues (in order)

1. **Google Sheet is empty** (no data rows)
2. **Sheet name mismatch** ("Gallery" vs actual name)
3. **Token not configured** (APPS_SCRIPT_SECRET_TOKEN missing)
4. **Apps Script not deployed** (web app not published)
5. **Image URLs are invalid** (not publicly accessible)

## 🔍 Debugging in Browser Console

Open browser console (F12) and check:

1. **Network Tab:**
   - Look for `/api/gallery` request
   - Check response status and body

2. **Console Tab:**
   - Look for logs starting with "Gallery API Route:" or "Fetching gallery images"
   - Check for any error messages

3. **Expected Console Logs:**
   ```
   Fetching gallery images from Google Apps Script API...
   Attempting to fetch from Firestore...
   Fetching from Apps Script API via proxy...
   Gallery API response: First image sample: {...}
   Returning X normalized gallery images
   ```

## ✅ Verification Checklist

- [ ] Google Sheet "Gallery" tab exists
- [ ] Sheet has at least one data row (excluding header)
- [ ] Sheet name is exactly "Gallery" (case-sensitive)
- [ ] Apps Script URL is correct
- [ ] `APPS_SCRIPT_SECRET_TOKEN` is set in `.env.local`
- [ ] Next.js dev server has been restarted after adding token
- [ ] Browser console shows no errors
- [ ] Network tab shows successful `/api/gallery` request
- [ ] Image URLs are valid and publicly accessible



