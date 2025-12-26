# Test Gallery API Directly

## Quick Test Steps

### Step 1: Test Apps Script URL Directly in Browser

Open this URL in your browser (replace `YOUR_TOKEN` with your actual token or remove `&token=...` if not needed):

```
https://script.google.com/macros/s/AKfycby-7jOc_naI1_XDVzG1qAGvNc9w3tIU4ZwmCFGUUCLdg0_DEJh7oouF8a9iy5E93-p9zg/exec?action=getGallery
```

**Expected Results:**
- ✅ If sheet has data → Returns JSON array with image objects
- ❌ If sheet is empty → Returns `[]`
- ❌ If sheet name is wrong → Returns `[]` or error
- ❌ If Apps Script not deployed → Shows HTML error page

### Step 2: Check Google Sheet Directly

1. Open Google Sheets
2. Open spreadsheet: `https://docs.google.com/spreadsheets/d/1m8z-ryBbFTUsox-sEoZgFm_lOvbMb1_XjsaUky1VUjY/edit`
3. Look for a tab named **"Gallery"** (exact name, case-sensitive)
4. Check if the sheet has:
   - Header row: `Title | URL | Category | Uploaded By | Uploaded Date | Description | Delete`
   - At least one data row below the header

### Step 3: Verify Sheet Name

The Apps Script expects the sheet tab to be named exactly: **"Gallery"**

Common issues:
- ❌ "gallery" (lowercase)
- ❌ "Galleries" (plural)
- ❌ "Image Gallery" (different name)
- ✅ "Gallery" (correct)

### Step 4: Check Apps Script Logs

1. Go to https://script.google.com
2. Open your Apps Script project
3. Click on "Executions" (clock icon) in the left sidebar
4. Look for recent executions of `doGet` with `action=getGallery`
5. Check for any errors

### Step 5: Test with Sample Data

If the sheet is empty, add a test row:

| Title | URL | Category | Uploaded By | Uploaded Date | Description | Delete |
|-------|-----|----------|-------------|---------------|-------------|--------|
| Test Image | https://via.placeholder.com/300 | Gallery | test@example.com | 2025-01-01 | Test description | |

Then test the API URL again.

## Debugging in Browser Console

After adding the test data, check your browser console. You should see:

```
Gallery API Route: Response text (full): [{"Title":"Test Image","URL":"https://via.placeholder.com/300",...}]
Gallery API response length: 1
✅ Using 1 gallery images from API
```

## Common Issues

### Issue 1: Sheet is Empty
**Solution:** Add data to the "Gallery" sheet

### Issue 2: Sheet Name Mismatch
**Solution:** Rename the sheet tab to exactly "Gallery"

### Issue 3: Apps Script Not Deployed
**Solution:** 
1. Go to Apps Script
2. Click "Deploy" → "Manage deployments"
3. Ensure web app is deployed and active

### Issue 4: Wrong Spreadsheet ID
**Solution:** Verify the spreadsheet ID in `GALLERY_Common.gs` matches your actual sheet



