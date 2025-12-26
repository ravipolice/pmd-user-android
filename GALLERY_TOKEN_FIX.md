# Gallery Token Fix

## 🔍 Issue

The Gallery API is returning: `{"success":false,"error":"Unauthorized: Invalid token"`

This means the Apps Script is validating the token, but it's not matching.

## ✅ Solution

### Step 1: Check Your Token

The Apps Script expects the token: `Ravi@PMD_2025_Secure_Token` (from `helpers.gs`)

### Step 2: Set Environment Variable

Create or update `.env.local` in the `admin-panel/` directory:

```bash
APPS_SCRIPT_SECRET_TOKEN=Ravi@PMD_2025_Secure_Token
```

### Step 3: Restart Next.js Dev Server

After adding/updating `.env.local`:
1. Stop your Next.js dev server (Ctrl+C)
2. Start it again: `npm run dev`

### Step 4: Verify Token is Loaded

Check the browser console when loading the Gallery page. You should see:
```
Gallery API Route: Using token from environment variable
```

If you see "Using fallback token", the environment variable isn't being read.

## 🔧 Alternative: Test Without Token

If the Gallery `doGet` function doesn't actually require a token (like Documents), you can test by removing the token from the URL temporarily.

However, based on the error message, it seems the Apps Script project might have a wrapper that validates tokens for all requests.

## 📝 Notes

- The token must match exactly: `Ravi@PMD_2025_Secure_Token`
- Environment variables in Next.js are only loaded on server start
- `.env.local` should be in the `admin-panel/` directory, not the root
- Never commit `.env.local` to git (it should be in `.gitignore`)

## ✅ Verification

After setting the token and restarting:

1. Open Gallery page
2. Check browser console
3. Look for: "Gallery API Route: Using token from environment variable"
4. The API should now return gallery images instead of token error



