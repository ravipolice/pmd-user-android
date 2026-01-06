# PMD Data Upload Web App

A comprehensive HTML web application for uploading employees, districts, and stations to Firebase Firestore.

## Features

### 👥 Employees Management
- **Single Entry Form**: Add individual employees with all required fields
- **Fields Supported**:
  - KGID (required)
  - Name (required)
  - Email
  - Mobile 1 (required)
  - Mobile 2
  - Rank
  - Metal Number
  - Blood Group
  - District (required, dropdown)
  - Station (required, dropdown - filtered by district)
  - Photo URL

### 🏛️ Districts Management
- Add new districts with optional range information
- Districts are automatically loaded in dropdowns after creation

### 🏢 Stations Management
- Add new stations linked to districts
- Optional STD code field
- Stations are automatically filtered by selected district

### 📤 Bulk Upload
- **CSV Upload**: Upload multiple employees at once via CSV file
- **Drag & Drop Support**: Drag CSV files directly onto the upload area
- **Progress Tracking**: Real-time progress bar during bulk upload
- **Batch Processing**: Handles large files efficiently using Firestore batches
- **Error Handling**: Reports successful uploads and skipped rows

## CSV Format for Bulk Upload

### Required Columns
- `kgid` - Employee KGID
- `name` - Employee name
- `mobile1` - Primary mobile number
- `district` - District name
- `station` - Station name

### Optional Columns
- `email` - Email address
- `mobile2` - Secondary mobile number
- `rank` - Employee rank
- `metalNumber` - Metal number
- `bloodGroup` - Blood group (A+, A-, B+, B-, AB+, AB-, O+, O-)
- `photoUrl` - Photo URL
- `isAdmin` - Admin status (true/false)
- `isApproved` - Approval status (true/false)

### Example CSV
```csv
kgid,name,mobile1,district,station,rank,email
KG001,John Doe,9876543210,Bengaluru City,Central PS,Inspector,john@example.com
KG002,Jane Smith,9876543211,Mysuru City,North PS,Sub-Inspector,jane@example.com
```

## Usage

1. **Open the HTML file** in a web browser
2. **Login** with your Google account (Firebase authentication required)
3. **Navigate** between tabs:
   - **Employees**: Add individual employees
   - **Districts**: Add new districts
   - **Stations**: Add new stations
   - **Bulk Upload**: Upload CSV files

## Authentication

The app uses Firebase Authentication with Google Sign-In. Only authenticated users can access the upload features.

## Firebase Configuration

The app is configured to connect to:
- **Project ID**: `pmd-police-mobile-directory`
- **Collections**:
  - `employees` - Employee records
  - `districts` - District master data
  - `stations` - Station master data

## Data Validation

- Required fields are validated before submission
- Mobile numbers are validated
- Email format is validated
- District-Station relationships are maintained
- CSV files are validated for required columns

## Error Handling

- Clear error messages for failed operations
- Success notifications for completed operations
- Progress indicators during bulk uploads
- Detailed error reporting for CSV upload issues

## Browser Compatibility

Works in modern browsers that support:
- ES6 Modules
- Firebase SDK v10.7.1
- File API
- Drag and Drop API

## Notes

- Bulk uploads process in batches of 500 records (Firestore limit)
- Progress bar shows upload progress
- All timestamps are set automatically using `serverTimestamp()`
- New employees are set as `isApproved: true` by default
- Districts and stations are set as `isActive: true` by default










