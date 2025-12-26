# Logo Setup Guide

The PMD Admin Panel uses a logo in multiple places. Here's how to set it up:

## Option 1: Use Firebase Storage Logo (Recommended)

The app tries to load the logo from Firebase Storage first:
```
https://firebasestorage.googleapis.com/v0/b/pmd-police-mobile-directory.appspot.com/o/app_logo.png?alt=media
```

### To Upload Logo to Firebase Storage:

1. Go to Firebase Console > Storage
2. Upload your logo file as `app_logo.png` in the root of your storage bucket
3. Make sure the file is publicly accessible
4. The logo will automatically load

## Option 2: Use Local Logo File

If Firebase Storage logo is not available, the app falls back to a local file.

1. Place your logo file in: `admin-panel/public/logo.png`
2. Supported formats: PNG, JPG, SVG
3. Recommended size: 200x200px or larger (will be scaled automatically)

## Option 3: Default Shield Icon

If neither Firebase nor local logo is available, a shield icon will be displayed as a fallback.

## Logo Locations

The logo appears in:
- **Login Page** - Large logo (128x128px) above the sign-in form
- **Sidebar** - Medium logo (80x80px) above "PMD Admin" text
- **Header** - Small logo (40x40px) next to "Police Mobile Directory" text

## Troubleshooting

### Logo Not Showing?

1. **Check Browser Console** - Open Developer Tools (F12) and check for errors
2. **Check Firebase Storage** - Verify the file exists and is publicly accessible
3. **Check Local File** - Ensure `public/logo.png` exists if using local logo
4. **Check Network Tab** - See if the image request is failing (404, CORS, etc.)

### Common Issues:

- **404 Error**: Logo file doesn't exist at the specified path
- **CORS Error**: Firebase Storage rules may need to allow public access
- **Permission Error**: File might not be publicly accessible

## Testing

After setting up the logo:
1. Refresh the page
2. Check browser console for "Logo loaded successfully" messages
3. If you see "Logo load error" messages, check the troubleshooting steps above



