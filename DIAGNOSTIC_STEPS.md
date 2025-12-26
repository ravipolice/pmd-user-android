# Documents Diagnostic Steps

## 🔍 Root Cause Analysis

The Apps Script API is returning `[]` (empty array). Based on code analysis:

**Apps Script reads from Google Sheets, NOT Firestore!**

- Sheet ID: `1QKR1gHCTM53MhANbRjid7VBCkuJjyLUV_1nY60sCVXE`
- Sheet Name: `"Documents Url"` (case-sensitive!)
- Collection: The sheet tab named "Documents Url"

## ✅ Step-by-Step Diagnosis

### Step 1: Check Google Sheet Directly

1. Open Google Sheets
2. Open the spreadsheet with ID: `1QKR1gHCTM53MhANbRjid7VBCkuJjyLUV_1nY60sCVXE`
3. Check if tab "Documents Url" exists (exact name, case-sensitive)
4. Check if there are any data rows (excluding header row)

**Expected Sheet Structure:**
```
Title | URL | Category | UploadedBy | UploadedDate | Description | Delete
------|-----|----------|------------|--------------|-------------|-------
Doc 1 | ... | ...      | ...        | ...          | ...         |
```

**If sheet is empty → That's why API returns []**

### Step 2: Test Apps Script URL Directly

Open in browser (replace TOKEN with actual token):
```
https://script.google.com/macros/s/AKfycby-7jOc_naI1_XDVzG1qAGvNc9w3tIU4ZwmCFGUUCLdg0_DEJh7oouF8a9iy5E93-p9zg/exec?action=getDocuments&token=YOUR_TOKEN
```

**Expected:**
- If token is correct → Returns JSON array of documents
- If token is wrong → Returns error or empty array
- If sheet is empty → Returns `[]`

### Step 3: Check Apps Script Logs

1. Open Google Apps Script Editor
2. Go to Executions tab
3. Check recent executions of `doGet` function
4. Look for errors in logs

### Step 4: Verify Sheet Name

The code expects exactly: `"Documents Url"` (with space, capital D and U)

Common mistakes:
- ❌ "DocumentsUrl" (no space)
- ❌ "documents url" (lowercase)
- ❌ "Document Url" (singular)
- ✅ "Documents Url" (correct)

### Step 5: Check Firestore Rules (if using Firestore)

Even though Apps Script uses Sheets, admin panel also tries Firestore:

```javascript
match /documents/{docId} {
  allow read: if isAuthenticated();  // ← Requires login
  allow create, update, delete: if isAdmin();
}
```

**Temporary test rule:**
```javascript
match /documents/{docId} {
  allow read: if true;  // ← Allow all reads temporarily
}
```

## 🔧 Quick Fixes

### Fix 1: If Google Sheet is Empty
- Add test data to the "Documents Url" sheet
- Make sure header row exists
- Add at least one data row

### Fix 2: If Sheet Name is Wrong
- Rename the sheet tab to exactly: `"Documents Url"`
- Or update `SHEET_NAME` in `DOCUMENTS_Common.gs`

### Fix 3: If Token is Missing/Wrong
- Add `APPS_SCRIPT_SECRET_TOKEN` to `.env.local`
- Restart Next.js dev server
- Verify token matches mobile app token

### Fix 4: If Apps Script Project is Wrong
- Check Apps Script project ID matches Firebase project
- Verify service account has access to the Google Sheet

## 📊 Expected Data Flow

```
Mobile App → Apps Script API → Google Sheets → Returns JSON
Admin Panel → Next.js API Route → Apps Script API → Google Sheets → Returns JSON
```

**NOT:**
```
Admin Panel → Firestore "documents" collection ❌
```

## 🎯 Most Likely Issues (in order)

1. **Google Sheet is empty** (no data rows)
2. **Sheet name mismatch** ("Documents Url" vs actual name)
3. **Token not configured** (APPS_SCRIPT_SECRET_TOKEN missing)
4. **Apps Script not deployed** (web app not published)



