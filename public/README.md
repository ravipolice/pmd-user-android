# Public Assets

This directory contains static assets that are served directly by Next.js.

## Logo

Place your PMD logo image here as `logo.png`.

### Requirements:
- **File name**: `logo.png` (or update the code to use a different name)
- **Recommended size**: 200x200px or larger (will be scaled down automatically)
- **Supported formats**: PNG (recommended), SVG, JPG
- **Location**: `/public/logo.png`

### Usage:
The logo is automatically used in:
- Sidebar header
- Login page
- Main header (if enabled)

### Alternative:
If you prefer to use a different file name or format, update the image source in:
- `components/layout/Sidebar.tsx`
- `app/login/page.tsx`
- `components/layout/Header.tsx`

Change `/logo.png` to your preferred path (e.g., `/logo.svg` or `/images/pmd-logo.png`).



