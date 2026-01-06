# Constants Sync - Complete Setup Guide

## 📁 File Structure

Your Google Apps Script project should have these files:

### Required Files:
1. **`Code.gs`** (or `CONSTANTS_GOOGLE_SHEETS_SCRIPT_COMPLETE.gs`)
   - Main constants sync script
   - Includes `doGet()` for Android app
   - Includes `syncConstantsToFirestore()` for Firestore sync
   - Uses `getConfigVersion()` to read version from Config sheet

2. **`helpers.gs`**
   - Token authentication (`verifyToken()`)
   - Admin validation (`isAdmin()`, `verifyAdminFromFirestore()`)
   - JSON response helper (`jsonResponse()`)
   - **`formatFirestoreData()`** - Converts JS objects to Firestore format
   - Utilities (`nowSeconds()`, `randomOtp()`)

3. **`firestoreService.gs`**
   - Service Account authentication (`getServiceAccountAccessToken_()`)
   - Firestore operations:
     - `firestoreCreateDocument()`
     - `firestorePatchDocument()`
     - `firestoreDeleteDocument()`
   - **Uses `formatFirestoreData()` from helpers.gs**

### Optional Files:
4. **`Sidebar.html`** (if you have a sidebar UI)

---

## 🔧 Script Properties Required

Set these in **File → Project Settings → Script Properties**:

```
PROJECT_ID = your-firebase-project-id
SERVICE_ACCOUNT_EMAIL = your-service-account@project.iam.gserviceaccount.com
SERVICE_ACCOUNT_KEY = -----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----
```

---

## 📊 Google Sheet Structure

Your sheet (`SHEET_ID = "1gmUXQn1Fp2JEmNWzicNNDJurUYeS-D5XMyc_pym0avI"`) should have:

### Sheet 1: "rank"
| Rank |
|------|
| APC |
| CPC |
| WPC |
| ... |

### Sheet 2: "district"
| District |
|----------|
| Bagalkot |
| Ballari |
| ... |

### Sheet 3: "station"
| District | Station |
|----------|---------|
| Bagalkot | Amengad PS |
| Bagalkot | Badami PS |
| ... |

### Sheet 4: "bloodgroup"
| BloodGroup |
|------------|
| A+ |
| A- |
| B+ |
| ... |

### Sheet 5: "Config" ⚠️ **REQUIRED**
| A | B |
|---|---|
| Version | 2 |
| Last Updated | (auto-updated) |

**Note**: Version is read from **Config sheet, cell B2**. Use menu "PMD Sync → Update Version Only" to increment it.

---

## 🔄 How It Works

### 1. Android App Fetches Constants
```
Android App → GET /exec?token=... → doGet() → Returns JSON
```

**Response Format:**
```json
{
  "success": true,
  "data": {
    "ranks": [...],
    "districts": [...],
    "stationsbydistrict": { "District": [...] },
    "bloodgroups": [...],
    "lastupdated": "2025-12-18T10:00:00Z",
    "version": 2
  }
}
```

### 2. Sync to Firestore
```
Menu: PMD Sync → Sync Constants → Firestore
→ syncConstantsToFirestore()
→ Writes to Firestore collections:
   - constants/ranks
   - constants/districts
   - constants/stations
   - constants/bloodgroups
   - constants/metadata
```

---

## ✅ Key Features

### Version Management
- ✅ **Dynamic version** from Config sheet (not hardcoded)
- ✅ Menu function to increment version
- ✅ Version included in all Firestore writes
- ✅ Android app checks version and shows toast if mismatch

### Error Handling
- ✅ Retry logic for Firestore writes (2 retries with backoff)
- ✅ Fallback to default version if Config sheet missing
- ✅ Proper error logging to "ConstantsSyncLogs" sheet

### Security
- ✅ Token authentication via `helpers.gs` (`verifyToken()`)
- ✅ Service Account auth for Firestore (via `firestoreService.gs`)
- ✅ Admin validation from Firestore (`isAdmin()`)

---

## 🚀 Deployment Steps

### 1. Upload Files to Apps Script
1. Open your Google Apps Script project
2. Upload/update these files:
   - `Code.gs` (use `CONSTANTS_GOOGLE_SHEETS_SCRIPT_COMPLETE.gs`)
   - `helpers.gs` (your existing file)
   - `firestoreService.gs` (updated version)

### 2. Set Script Properties
1. **File → Project Settings → Script Properties**
2. Add:
   - `PROJECT_ID`
   - `SERVICE_ACCOUNT_EMAIL`
   - `SERVICE_ACCOUNT_KEY`

### 3. Create Config Sheet
1. In your Google Sheet, create a sheet named **"Config"**
2. Put version number in **cell B2** (start with `2`)
3. Optionally put header in B1: "Version"

### 4. Deploy as Web App
1. **Deploy → New deployment**
2. Type: **Web app**
3. Settings:
   - **Execute as:** Me
   - **Who has access:** Anyone
4. Copy the **Web App URL**
5. Update `NetworkModule.kt` → `CONSTANTS_BASE_URL` with this URL

### 5. Test
1. Run `doGet()` function manually to test
2. Check Android app logs for successful fetch
3. Test Firestore sync via menu

---

## 🔍 Troubleshooting

### Issue: "Malformed JSON" error in Android
**Fix**: ✅ Already fixed - `NetworkModule.kt` uses lenient Gson

### Issue: Version mismatch toast always shows
**Fix**: Ensure Config sheet B2 has version `2` (matches `LOCAL_CONSTANTS_VERSION`)

### Issue: Firestore sync fails
**Check:**
- Script Properties are set correctly
- Service Account has Firestore permissions
- `firestoreService.gs` is included in project
- `helpers.gs` is included (for `formatFirestoreData`)

### Issue: "formatFirestoreData is not defined"
**Fix**: Make sure `helpers.gs` is included in your Apps Script project

---

## 📝 Function Dependencies

```
doGet()
  └─ getAllConstants()
      ├─ getRanks()
      ├─ getDistricts()
      ├─ getStationsByDistrict()
      ├─ getBloodGroups()
      └─ getConfigVersion()  ← Reads from Config sheet B2

syncConstantsToFirestore()
  └─ getAllConstants() (same as above)
  └─ firestoreCreateDocument() (from firestoreService.gs)
      └─ formatFirestoreData() (from helpers.gs)
      └─ getServiceAccountAccessToken_() (from firestoreService.gs)
```

---

## 🎯 Summary

✅ **Android App** → Fetches constants via `doGet()`  
✅ **Version** → Dynamic from Config sheet  
✅ **Firestore Sync** → Menu function writes to Firestore  
✅ **Security** → Token auth + Service Account  
✅ **Error Handling** → Retries, logging, fallbacks  

Everything is ready! 🚀











