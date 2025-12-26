# Testing the Documents API

## Quick Test

To test if the Apps Script API is working, check the **server console** (terminal where `npm run dev` is running).

You should see logs like:
```
API Route: Fetching documents from Google Apps Script...
API Route: Token present: true/false
API Route: Full URL: [URL]
API Route: Response status: 200
API Route: Response text length: [number]
API Route: Response text (first 1000 chars): [JSON or HTML]
API Route: Data length: [number]
```

## Expected Behavior

1. **If Apps Script returns data**: You should see:
   - `Response text length: > 0`
   - `Data length: 5` (or however many documents)
   - `First document sample: {...}`

2. **If Apps Script returns empty**: You should see:
   - `Response text length: 2` (just `[]`)
   - `Data length: 0`

3. **If Apps Script returns HTML** (redirect/error):
   - `Response text` starts with `<`
   - This means Apps Script web app needs to be redeployed

## Manual Test

You can also test the Apps Script directly in your browser:

```
https://script.google.com/macros/s/AKfycby-7jOc_naI1_XDVzG1qAGvNc9w3tIU4ZwmCFGUUCLdg0_DEJh7oouF8a9iy5E93-p9zg/exec?action=getDocuments
```

**Expected**: Should return JSON array with 5 documents (or however many are in the sheet)

## Common Issues

1. **Empty array returned**: 
   - Check if Google Sheet has data
   - Check if sheet tab name is exactly "Documents Url"
   - Check Apps Script logs in Google Apps Script editor

2. **HTML returned instead of JSON**:
   - Apps Script web app needs to be redeployed
   - Go to Apps Script editor → Deploy → Manage deployments
   - Make sure it's deployed as "Web app" with "Execute as: Me" and "Who has access: Anyone"

3. **CORS errors**:
   - Should not happen with server-side API route
   - If you see CORS errors, the request is going client-side (check the fetch URL)



