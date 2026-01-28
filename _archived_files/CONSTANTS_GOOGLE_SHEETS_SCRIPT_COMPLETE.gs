/**
 * ✅ PMD PRODUCTION CONSTANTS SYNC SCRIPT — COMPLETE
 *
 * Reads constants from Google Sheet and:
 * 1. Serves them to Android app via Web App (doGet)
 * 2. Writes data securely into Firestore (syncConstantsToFirestore)
 *
 * Uses SERVICE ACCOUNT authentication for Firestore (via firestoreService.gs helpers).
 *
 * Improvements:
 * - Validates required Script Properties
 * - Checks and logs Firestore responses
 * - Small transient retry for writes
 * - Exposes helper functions used by Sidebar UI
 * - Dynamic version from Config sheet (not hardcoded)
 *
 * Requirements (set as Script Properties):
 * - PROJECT_ID
 * - SERVICE_ACCOUNT_EMAIL (used by firestoreService.getServiceAccountAccessToken_)
 * - SERVICE_ACCOUNT_KEY   (used by firestoreService.getServiceAccountAccessToken_)
 *
 * NOTE: firestoreCreateDocument must exist in firestoreService.gs (returns an object with .code/.body)
 */

const SHEET_ID = "1gmUXQn1Fp2JEmNWzicNNDJurUYeS-D5XMyc_pym0avI";
const MAX_FIRESTORE_RETRIES = 2; // number of retries for transient failures

/**
 * ✅ Web App Entry Point - Called by Android app via HTTP GET
 * URL: https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec
 */
function doGet(e) {
  try {
    const ss = SpreadsheetApp.openById(SHEET_ID);
    const constants = getAllConstants(ss);
    return ContentService
      .createTextOutput(JSON.stringify({ success: true, data: constants }))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    Logger.log("Error in doGet: " + err.toString());
    // Fallback: try to get version from Config sheet, or default to 2
    let fallbackVersion = 2;
    try {
      const ss = SpreadsheetApp.openById(SHEET_ID);
      fallbackVersion = getConfigVersion(ss);
    } catch (e) {
      Logger.log("Could not read version from Config sheet: " + e);
    }
    
    return ContentService
      .createTextOutput(JSON.stringify({ 
        success: false, 
        error: err.toString(),
        data: {
          ranks: [],
          districts: [],
          stationsbydistrict: {},
          bloodgroups: [],
          lastupdated: new Date().toISOString(),
          version: fallbackVersion
        }
      }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * Main entry — sync constants to Firestore.
 * Use from menu: PMD Sync -> Sync Constants → Firestore
 */
function syncConstantsToFirestore() {
  const projectId = PropertiesService.getScriptProperties().getProperty("PROJECT_ID");
  if (!projectId) {
    SpreadsheetApp.getUi().alert("Missing PROJECT_ID Script Property. Set PROJECT_ID before running.");
    return { success: false, error: "Missing PROJECT_ID" };
  }

  try {
    const ss = SpreadsheetApp.openById(SHEET_ID);
    const constants = getAllConstants(ss);

    const writes = [
      { collection: "constants", docId: "ranks", payload: { values: constants.ranks, updatedAt: nowSeconds(), version: constants.version } },
      { collection: "constants", docId: "districts", payload: { values: constants.districts, updatedAt: nowSeconds(), version: constants.version } },
      { collection: "constants", docId: "stations", payload: { map: constants.stationsbydistrict, updatedAt: nowSeconds(), version: constants.version } },
      { collection: "constants", docId: "bloodgroups", payload: { values: constants.bloodgroups, updatedAt: nowSeconds(), version: constants.version } },
      { collection: "constants", docId: "metadata", payload: { version: constants.version, updatedAt: nowSeconds() } }
    ];

    const results = [];
    let failed = 0;

    writes.forEach(w => {
      let attempt = 0;
      let ok = false;
      let resp = null;

      while (attempt <= MAX_FIRESTORE_RETRIES && !ok) {
        attempt++;
        try {
          // firestoreCreateDocument should be provided by firestoreService.gs
          resp = firestoreCreateDocument(projectId, w.collection, w.docId, w.payload);
          // If firestoreCreateDocument returns { code, body } (as recommended)
          const code = (resp && resp.code) ? resp.code : (resp && resp.getResponseCode ? resp.getResponseCode() : null);
          if (code === 200 || code === 201) {
            ok = true;
            results.push({ collection: w.collection, docId: w.docId, status: "ok", code, attempt });
            break;
          } else {
            // non-200 -> treat as failure but maybe transient
            Logger.log(`Constants sync write failed (attempt ${attempt}) -> ${w.collection}/${w.docId} : ${code} ${resp && resp.body ? resp.body : ""}`);
            Utilities.sleep(500 * attempt); // backoff
          }
        } catch (err) {
          Logger.log(`Error writing ${w.collection}/${w.docId} (attempt ${attempt}): ${err}`);
          Utilities.sleep(500 * attempt);
        }
      }

      if (!ok) {
        failed++;
        results.push({ collection: w.collection, docId: w.docId, status: "failed", lastResp: resp });
      }
    });

    const summary = {
      success: failed === 0,
      timestamp: new Date().toISOString(),
      version: constants.version,
      processed: results.length,
      failed: failed,
      details: results
    };

    SpreadsheetApp.getUi().alert("Constants sync finished. Failed: " + failed);
    writeSyncLog("SYNC_CONSTANTS", summary);
    return summary;

  } catch (err) {
    Logger.log("Error syncing constants: " + err);
    SpreadsheetApp.getUi().alert("Error syncing constants:\n" + err);
    return { success: false, error: String(err) };
  }
}

/* -----------------------
   Reading constants from sheet
   ----------------------- */
function getAllConstants(ss) {
  try {
    const districts = getDistricts(ss);
    const stations = getStationsByDistrict(ss, districts);

    return {
      ranks: getRanks(ss),
      districts: districts,
      stationsbydistrict: stations,
      bloodgroups: getBloodGroups(ss),
      lastupdated: new Date().toISOString(),
      version: getConfigVersion(ss)  // ✅ Dynamic version from Config sheet
    };

  } catch (err) {
    Logger.log("Error in getAllConstants: " + err);
    // Fallback version - try to read from Config sheet
    let fallbackVersion = 2;
    try {
      fallbackVersion = getConfigVersion(ss);
    } catch (e) {
      Logger.log("Could not read version in error fallback: " + e);
    }
    
    return {
      ranks: [],
      districts: [],
      stationsbydistrict: {},
      bloodgroups: [],
      lastupdated: new Date().toISOString(),
      version: fallbackVersion
    };
  }
}

function getRanks(ss) {
  try {
    const sheet = ss.getSheetByName("rank");
    if (!sheet) return [];
    const lastRow = sheet.getLastRow();
    if (lastRow <= 1) return [];
    const values = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
    return values.map(r => (r[0] || "").toString().trim()).filter(v => v !== "").sort();
  } catch (err) {
    Logger.log("Error reading ranks: " + err);
    return [];
  }
}

function getDistricts(ss) {
  try {
    const sheet = ss.getSheetByName("district");
    if (!sheet) return [];
    const lastRow = sheet.getLastRow();
    if (lastRow <= 1) return [];
    const values = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
    return values.map(r => (r[0] || "").toString().trim()).filter(v => v !== "").sort();
  } catch (err) {
    Logger.log("Error reading districts: " + err);
    return [];
  }
}

function getStationsByDistrict(ss, districts) {
  try {
    const sheet = ss.getSheetByName("station");
    if (!sheet) return {};
    const allData = sheet.getDataRange().getValues();
    const result = {};
    const lookup = {};

    districts.forEach(d => {
      result[d] = [];
      lookup[d.toLowerCase()] = d;
    });

    for (let i = 1; i < allData.length; i++) {
      const dRaw = allData[i][0];
      const sRaw = allData[i][1];
      if (!dRaw) continue;
      const d = dRaw.toString().trim();
      const dLower = d.toLowerCase();
      let matchedDistrict = lookup[dLower];
      if (!matchedDistrict) {
        matchedDistrict = districts.find(x => x.toLowerCase().includes(dLower) || dLower.includes(x.toLowerCase()));
        if (!matchedDistrict) {
          matchedDistrict = d;
          result[d] = [];
          lookup[dLower] = d;
        }
      }
      if (sRaw) {
        const station = sRaw.toString().trim();
        if (station !== "" && !result[matchedDistrict].includes(station)) {
          result[matchedDistrict].push(station);
        }
      }
    }

    Object.keys(result).forEach(d => result[d].sort());
    return result;

  } catch (err) {
    Logger.log("Error in stations: " + err);
    const emptyMap = {};
    districts.forEach(d => emptyMap[d] = []);
    return emptyMap;
  }
}

function getBloodGroups(ss) {
  try {
    const sheet = ss.getSheetByName("bloodgroup");
    if (!sheet) return [];
    const lastRow = sheet.getLastRow();
    if (lastRow <= 1) return [];
    const values = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
    return values.map(r => (r[0] || "").toString().trim()).filter(v => v !== "").sort();
  } catch (err) {
    Logger.log("Error reading bloodgroups: " + err);
    return [];
  }
}

/**
 * ✅ Reads version from Config sheet (cell B2)
 * This is better than hardcoding - version can be updated via menu or sheet
 */
function getConfigVersion(ss) {
  try {
    const sheet = ss.getSheetByName("Config");
    if (!sheet) {
      Logger.log("Config sheet not found, using default version 2");
      return 2;
    }
    const version = Number(sheet.getRange("B2").getValue());
    return version || 2; // Default to 2 if empty/invalid
  } catch (err) {
    Logger.log("Error reading version from Config sheet: " + err);
    return 2; // Safe fallback
  }
}

/* -----------------------
   Small utilities & Sidebar helpers
   ----------------------- */

function nowSeconds() { return Math.floor(Date.now() / 1000); }

function isAdminSidebar() {
  try {
    const user = Session.getActiveUser().getEmail();
    // relies on helpers.gs isAdmin(email)
    return isAdmin(user);
  } catch (err) {
    return false;
  }
}

/* Write a quick sync log in sheet "ConstantsSyncLogs" - helpful for debugging */
function writeSyncLog(action, obj) {
  try {
    const ss = SpreadsheetApp.openById(SHEET_ID);
    let sh = ss.getSheetByName("ConstantsSyncLogs");
    if (!sh) {
      sh = ss.insertSheet("ConstantsSyncLogs");
      sh.appendRow(["Timestamp", "Action", "Summary"]);
    }
    sh.appendRow([new Date().toISOString(), action, JSON.stringify(obj)]);
  } catch (e) {
    Logger.log("writeSyncLog failed: " + e);
  }
}

/* -----------------------
   Sidebar helper functions used by Sidebar.html
   (getDistrictList, addRankToSheet, addDistrictToSheet, addStationToSheet)
   ----------------------- */

function getDistrictList() {
  try {
    const ss = SpreadsheetApp.openById(SHEET_ID);
    return getDistricts(ss);
  } catch (err) {
    Logger.log("getDistrictList ERROR: " + err);
    return [];
  }
}

function addRankToSheet(rankName) {
  if (!rankName) return { success: false, error: "Missing rank" };
  try {
    const ss = SpreadsheetApp.openById(SHEET_ID);
    let sh = ss.getSheetByName("rank");
    if (!sh) sh = ss.insertSheet("rank");
    // append at bottom
    sh.appendRow([rankName]);
    return { success: true };
  } catch (err) {
    Logger.log("addRankToSheet ERROR: " + err);
    return { success: false, error: String(err) };
  }
}

function addDistrictToSheet(districtName) {
  if (!districtName) return { success: false, error: "Missing district" };
  try {
    const ss = SpreadsheetApp.openById(SHEET_ID);
    let sh = ss.getSheetByName("district");
    if (!sh) sh = ss.insertSheet("district");
    sh.appendRow([districtName]);
    return { success: true };
  } catch (err) {
    Logger.log("addDistrictToSheet ERROR: " + err);
    return { success: false, error: String(err) };
  }
}

function addStationToSheet(districtName, stationName) {
  if (!districtName || !stationName) return { success: false, error: "Missing params" };
  try {
    const ss = SpreadsheetApp.openById(SHEET_ID);
    let sh = ss.getSheetByName("station");
    if (!sh) sh = ss.insertSheet("station");
    sh.appendRow([districtName, stationName]);
    return { success: true };
  } catch (err) {
    Logger.log("addStationToSheet ERROR: " + err);
    return { success: false, error: String(err) };
  }
}

/* -----------------------
   Menu
   ----------------------- */
function onOpen() {
  SpreadsheetApp.getUi()
    .createMenu("PMD Sync")
    .addItem("Sync Constants → Firestore", "syncConstantsToFirestore")
    .addItem("Update Version Only", "updateConstantsVersion")
    .addToUi();
}

/**
 * ✅ Increments version in Config sheet (cell B2)
 * Also updates timestamp in B3
 */
function updateConstantsVersion() {
  try {
    const ss = SpreadsheetApp.openById(SHEET_ID);
    const sheet = ss.getSheetByName("Config");
    if (!sheet) {
      SpreadsheetApp.getUi().alert("Config sheet not found. Please create it with version in B2.");
      return;
    }
    let v = Number(sheet.getRange("B2").getValue()) || 2;
    const newVersion = v + 1;
    sheet.getRange("B2").setValue(newVersion);
    sheet.getRange("B3").setValue(new Date());
    SpreadsheetApp.getUi().alert("Version updated to: " + newVersion);
    Logger.log("Version updated to: " + newVersion);
  } catch (err) {
    Logger.log("Error updating version: " + err);
    SpreadsheetApp.getUi().alert("Error updating version: " + err);
  }
}











