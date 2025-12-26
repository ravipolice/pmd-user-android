# PMD Admin Panel

A modern Next.js admin panel for the Police Mobile Directory (PMD) application.

## Features

- 🔐 **Google Authentication** - Secure login with Google OAuth
- 📊 **Dashboard** - Real-time statistics and analytics
- 👥 **Employee Management** - Add, edit, and manage employees
- ✅ **Pending Approvals** - Review and approve/reject registrations
- 🛡️ **Officer Management** - Manage officer records
- 🗺️ **Districts & Stations** - Master data management
- 🔔 **Notifications** - Send push notifications to users
- 📄 **Documents** - Upload and manage documents
- 🖼️ **Gallery** - Manage gallery images
- 🔗 **Useful Links** - Add and manage useful links
- 📤 **CSV Upload** - Bulk upload employees via CSV

## Tech Stack

- **Next.js 14** - React framework with App Router
- **TypeScript** - Type-safe development
- **Tailwind CSS** - Utility-first CSS framework
- **Firebase** - Authentication, Firestore, and Storage
- **Lucide React** - Icon library

## Getting Started

### Prerequisites

- Node.js 18+ and npm/yarn
- Firebase project with Firestore enabled
- Google OAuth credentials configured

### Installation

1. Navigate to the admin panel directory:
```bash
cd admin-panel
```

2. Install dependencies:
```bash
npm install
```

3. Update Firebase configuration in `lib/firebase/config.ts` with your Firebase project credentials.

4. Run the development server:
```bash
npm run dev
```

5. Open [http://localhost:3000](http://localhost:3000) in your browser.

### Building for Production

```bash
npm run build
npm start
```

## Project Structure

```
admin-panel/
├── app/
│   ├── (dashboard)/          # Protected dashboard routes
│   │   ├── page.tsx          # Dashboard home
│   │   ├── employees/        # Employee management
│   │   ├── approvals/        # Pending approvals
│   │   ├── officers/         # Officer management
│   │   ├── districts/        # District management
│   │   ├── stations/         # Station management
│   │   ├── notifications/    # Send notifications
│   │   ├── documents/        # Document management
│   │   ├── gallery/          # Gallery management
│   │   ├── links/            # Useful links
│   │   └── upload/           # CSV upload
│   ├── login/                # Login page
│   ├── layout.tsx            # Root layout
│   └── globals.css           # Global styles
├── components/
│   ├── layout/               # Layout components
│   └── providers/            # Context providers
├── lib/
│   ├── firebase/             # Firebase utilities
│   └── utils.ts              # Utility functions
└── package.json
```

## Firebase Configuration

Make sure your Firebase project has:

1. **Authentication** - Google provider enabled
2. **Firestore** - Collections: employees, officers, districts, stations, pending_registrations, notifications_queue, documents, gallery, useful_links
3. **Storage** - For file uploads (optional)
4. **Firestore Rules** - Proper security rules configured

## CSV Upload Format

When uploading employees via CSV, ensure the following columns:

**Required:**
- `kgid` - Employee KGID
- `name` - Employee name
- `mobile1` - Primary mobile number
- `district` - District name
- `station` - Station name

**Optional:**
- `email` - Email address
- `mobile2` - Secondary mobile number
- `rank` - Employee rank
- `metalNumber` - Metal number
- `bloodGroup` - Blood group (A+, A-, B+, B-, AB+, AB-, O+, O-)
- `photoUrl` - Photo URL
- `isAdmin` - Admin status (true/false)
- `isApproved` - Approval status (true/false)

## Security

- All routes are protected and require authentication
- Admin-only features check user permissions
- Firebase security rules enforce data access control

## License

This project is part of the Police Mobile Directory system.



