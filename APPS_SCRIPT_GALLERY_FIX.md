# Apps Script Gallery Handler Fix

## 🔴 Problem

The proxy is working correctly (sending `action=getGallery`), but Apps Script returns:
```json
{"success":false,"error":"Invalid GET action"}
```

This means the **deployed Apps Script doesn't have the Gallery handler**.

## ✅ Solution

The Apps Script deployment needs to include a `doGet` function that handles `getGallery`.

### Option 1: Use Combined Script (Recommended)

The file `DOCUMENTS_APPS_SCRIPT_FIXED.js` has a `doGet` that handles both:

```javascript
function doGet(e) {
  try {
    const action = e?.parameter?.action || "";
    if (action === "getDocuments") return getDocuments();
    if (action === "getGallery") return getGallery();  // ✅ This line
    return jsonResponse([]);
  } catch (err) {
    return jsonResponse([]);
  }
}
```

**Steps:**
1. Go to https://script.google.com
2. Open the Apps Script project with deployment ID: `AKfycby-7jOc_naI1_XDVzG1qAGvNc9w3tIU4ZwmCFGUUCLdg0_DEJh7oouF8a9iy5E93-p9zg`
3. Check if `doGet` function includes `getGallery` handler
4. If not, add it or use `DOCUMENTS_APPS_SCRIPT_FIXED.js` code
5. Save and redeploy

### Option 2: Test Direct URL

Test if the Apps Script actually supports Gallery:

Open in browser:
```
https://script.google.com/macros/s/AKfycby-7jOc_naI1_XDVzG1qAGvNc9w3tIU4ZwmCFGUUCLdg0_DEJh7oouF8a9iy5E93-p9zg/exec?action=getGallery
```

**Expected:**
- ✅ If Gallery handler exists → Returns `[]` or array of images
- ❌ If Gallery handler missing → Returns `{"success":false,"error":"Invalid GET action"}`

### Option 3: Check Apps Script Files

The deployed project should have one of these:
- `DOCUMENTS_APPS_SCRIPT_FIXED.js` (has both getDocuments and getGallery)
- `GALLERY_Api.gs` (has getGallery)
- A combined script with both handlers

**NOT:**
- `MAIN_SCRIPT.gs` (only has "health")
- `MAIN_ROUTER.gs` (only has uploadImage and employee sync)

## 🔧 Quick Fix Code

If you need to add Gallery support to existing `doGet`, add this:

```javascript
function doGet(e) {
  try {
    const action = e?.parameter?.action || "";
    
    // Documents
    if (action === "getDocuments") return getDocuments();
    
    // Gallery - ADD THIS
    if (action === "getGallery") return getGallery();
    
    // Other handlers...
    
    return jsonResponse({ success: false, error: "Invalid GET action" }, 400);
  } catch (err) {
    return jsonResponse({ success: false, error: err.toString() }, 500);
  }
}
```

## ✅ Verification

After updating Apps Script:
1. Save the script
2. Deploy as new version (or update existing deployment)
3. Test direct URL: `.../exec?action=getGallery`
4. Should return `[]` or array (not error)
5. Refresh Gallery page in admin panel



