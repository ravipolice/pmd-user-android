# Apps Script Deployment Check

## Issue
The Apps Script API returns 5 documents when tested directly, but returns empty array `[]` when called from Next.js API route.

## Possible Causes

### 1. Apps Script Web App Deployment Settings

The Apps Script web app must be deployed with these settings:

**In Google Apps Script Editor:**
1. Click **Deploy** → **Manage deployments**
2. Click **Edit** (pencil icon) on the active deployment
3. Verify these settings:
   - **Execute as:** `Me` (your email)
   - **Who has access:** `Anyone` (or `Anyone with Google account`)

**If settings are wrong:**
1. Create a new deployment:
   - Click **New deployment**
   - Select type: **Web app**
   - Execute as: **Me**
   - Who has access: **Anyone**
   - Click **Deploy**
2. Copy the new deployment URL
3. Update the URL in `admin-panel/app/api/documents/route.ts`

### 2. Check Server Console Logs

**In the terminal where `npm run dev` is running**, look for:

```
API Route: Response text (first 1000 chars): [SHOULD SHOW JSON]
API Route: Data length: [SHOULD BE 5, NOT 0]
```

**If you see HTML instead of JSON:**
- Apps Script is redirecting (deployment issue)
- Redeploy the web app with correct settings

**If you see `[]`:**
- Apps Script is returning empty array
- Check Apps Script execution logs in Google Apps Script editor

### 3. Test Apps Script Directly

Open in browser (no token needed for GET):
```
https://script.google.com/macros/s/AKfycby-7jOc_naI1_XDVzG1qAGvNc9w3tIU4ZwmCFGUUCLdg0_DEJh7oouF8a9iy5E93-p9zg/exec?action=getDocuments
```

**Expected:** JSON array with 5 documents

**If this works but API route doesn't:**
- Issue is in how Next.js API route calls Apps Script
- Check server console logs for actual response

### 4. Check Apps Script Execution Logs

**In Google Apps Script Editor:**
1. Click **Executions** (clock icon)
2. Check recent executions of `doGet`
3. Look for errors or warnings

**Common errors:**
- "Sheet not found: Documents Url" → Sheet name mismatch
- "Access denied" → Spreadsheet permissions issue
- "Script execution time exceeded" → Script timeout

## Quick Fix

If the Apps Script works when tested directly but not from API route:

1. **Check server console** - What does "API Route: Response text" show?
2. **Redeploy Apps Script** - Make sure deployment settings are correct
3. **Check token** - Even though GET doesn't require it, try with/without token

## Next Steps

1. Share the **server console logs** (from `npm run dev` terminal)
2. Share what you see when you **test the Apps Script URL directly** in browser
3. Check **Apps Script execution logs** in Google Apps Script editor

This will help identify the exact issue.



