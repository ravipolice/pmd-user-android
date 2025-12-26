# Quick Gallery Test - Debug Steps

## ✅ Step 1: Test Apps Script URL Directly

Open this URL in your browser (no token needed for GET requests):

```
https://script.google.com/macros/s/AKfycby-7jOc_naI1_XDVzG1qAGvNc9w3tIU4ZwmCFGUUCLdg0_DEJh7oouF8a9iy5E93-p9zg/exec?action=getGallery
```

**What to look for:**
- ✅ If you see `[{...}]` with data → Apps Script is working, issue is in Next.js
- ❌ If you see `[]` → Apps Script is returning empty (check sheet data)
- ❌ If you see HTML error page → Apps Script not deployed or wrong URL

## ✅ Step 2: Verify Sheet Has Data Rows

In your Google Sheet "Gallery" tab:

1. **Check Row 1** - Should be headers: `Title | URL | UploadedBy | UploadedDate | Description | Delete`
2. **Check Row 2+** - Should have actual data (not empty)
3. **Count data rows** - If only Row 1 exists, Apps Script returns `[]`

**The Apps Script code checks:**
```javascript
if (rows.length <= 1) return jsonResponse([]);
```

This means if you only have a header row, it returns empty array.

## ✅ Step 3: Check Apps Script Execution Logs

1. Go to https://script.google.com
2. Open your Apps Script project
3. Click **"Executions"** (clock icon) in left sidebar
4. Find recent `doGet` executions
5. Click on one to see logs
6. Look for:
   - `getGallery ERROR: ...` - Shows what went wrong
   - Or check if function executed successfully

## ✅ Step 4: Verify Sheet Structure

Your sheet should have at least:
- **Row 1 (Headers):** Title | URL | UploadedBy | UploadedDate | Description | Delete
- **Row 2+ (Data):** Actual image data

**Example valid row:**
```
PMD Logo | https://drive.google.com/uc?export=view&id=1Se3JHTyAU8vvpNxDzOle-71fhedOPZr1 | Gallery | ravipolice@gmail.com | 2025-01-15 | Description text | 
```

## 🔧 Quick Fix: Add Test Data

If your sheet only has headers, add a test row:

1. In Row 2, add:
   - **Title:** `Test Image`
   - **URL:** `https://via.placeholder.com/300`
   - **UploadedBy:** `test@example.com`
   - **UploadedDate:** `2025-01-15`
   - **Description:** `Test description`
   - **Delete:** (leave empty)

2. Test the Apps Script URL again
3. Should now return: `[{"Title":"Test Image","URL":"https://via.placeholder.com/300",...}]`

## 🐛 Common Issues

### Issue 1: Sheet Only Has Headers
**Symptom:** Apps Script returns `[]`  
**Fix:** Add at least one data row below headers

### Issue 2: Wrong Sheet Name
**Symptom:** Apps Script throws error "Sheet not found"  
**Fix:** Ensure tab is named exactly "Gallery" (case-sensitive)

### Issue 3: All Rows Marked as Deleted
**Symptom:** Apps Script returns `[]` even with data  
**Fix:** Check if "Delete" column (column 7) has "Deleted" value, remove it

### Issue 4: Apps Script Not Deployed
**Symptom:** Browser shows HTML error page  
**Fix:** Deploy Apps Script as web app with "Execute as: Me" and "Who has access: Anyone"

## 📊 Expected Response Format

When working correctly, Apps Script should return:

```json
[
  {
    "Title": "PMD Logo",
    "URL": "https://drive.google.com/uc?export=view&id=1Se3JHTyAU8vvpNxDzOle-71fhedOPZr1",
    "UploadedBy": "ravipolice@gmail.com",
    "UploadedDate": "2025-01-15",
    "Description": "Description text",
    "Delete": ""
  }
]
```



