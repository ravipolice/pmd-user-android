// --- firestoreService.gs ---

/**
 * Firestore Service for Google Apps Script
 * 
 * DEPENDENCIES:
 * - helpers.gs (must be included in project) - provides formatFirestoreData()
 * 
 * Requires these Script Properties:
 * - SERVICE_ACCOUNT_EMAIL
 * - SERVICE_ACCOUNT_KEY (the full private_key text including BEGIN/END markers)
 * - PROJECT_ID
 * 
 * Functions:
 * - firestoreCreateDocument(projectId, collection, docId, fieldsObj) → { code, body }
 * - firestorePatchDocument(projectId, collection, docId, fieldsObj) → { code, body }
 * - firestoreDeleteDocument(projectId, docPath) → { code, body }
 */

/**
 * Get OAuth2 access token using Service Account credentials
 * @private
 */
function getServiceAccountAccessToken_() {
  const props = PropertiesService.getScriptProperties();
  const serviceEmail = props.getProperty('SERVICE_ACCOUNT_EMAIL');
  const key = props.getProperty('SERVICE_ACCOUNT_KEY');
  const projectId = props.getProperty('PROJECT_ID');

  if (!serviceEmail || !key || !projectId) {
    throw new Error('SERVICE_ACCOUNT_EMAIL / SERVICE_ACCOUNT_KEY / PROJECT_ID not set in Script Properties');
  }

  const header = {
    alg: "RS256",
    typ: "JWT"
  };

  const iat = Math.floor(Date.now() / 1000);
  const exp = iat + 3600;

  const claim = {
    iss: serviceEmail,
    sub: serviceEmail,
    aud: "https://www.googleapis.com/oauth2/v4/token",
    iat: iat,
    exp: exp,
    scope: "https://www.googleapis.com/auth/datastore"
  };

  const encodedHeader = Utilities.base64EncodeWebSafe(JSON.stringify(header));
  const encodedClaim = Utilities.base64EncodeWebSafe(JSON.stringify(claim));
  const unsignedJwt = encodedHeader + "." + encodedClaim;

  // Compute signature with RSA SHA256
  const signatureBytes = Utilities.computeRsaSha256Signature(unsignedJwt, key);
  const signature = Utilities.base64EncodeWebSafe(signatureBytes);

  const signedJwt = unsignedJwt + "." + signature;

  const tokenResponse = UrlFetchApp.fetch("https://www.googleapis.com/oauth2/v4/token", {
    method: "post",
    payload: {
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: signedJwt
    },
    muteHttpExceptions: false
  });

  const tokenJson = JSON.parse(tokenResponse.getContentText());
  if (!tokenJson.access_token) {
    throw new Error("Failed to obtain access token: " + tokenResponse.getContentText());
  }
  return tokenJson.access_token;
}

/**
 * ✅ NOTE: formatFirestoreData is provided by helpers.gs
 * 
 * This file uses formatFirestoreData from helpers.gs which handles:
 * - strings, numbers, booleans, arrays, objects, null, timestamps
 * 
 * Make sure helpers.gs is included in your Apps Script project.
 * If you don't have helpers.gs, you can copy formatFirestoreData from helpers.gs here.
 */

/**
 * Create a new document in Firestore
 * @param {string} projectId - Firebase project ID
 * @param {string} collectionPath - Collection path (e.g., "constants")
 * @param {string} docId - Document ID
 * @param {object} fieldsObj - Fields to write (will be converted to Firestore format)
 * @returns {object} { code: number, body: string }
 */
function firestoreCreateDocument(projectId, collectionPath, docId, fieldsObj) {
  try {
    const accessToken = getServiceAccountAccessToken_();
    const url = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/${collectionPath}?documentId=${encodeURIComponent(docId)}`;

    // formatFirestoreData from helpers.gs returns fields in correct format
    // Returns: { fieldName: { typeValue: value }, ... }
    const formattedFields = formatFirestoreData(fieldsObj);
    const payload = { fields: formattedFields };

    const resp = UrlFetchApp.fetch(url, {
      method: "post",
      contentType: "application/json",
      headers: {
        Authorization: "Bearer " + accessToken
      },
      muteHttpExceptions: true,
      payload: JSON.stringify(payload)
    });

    return {
      code: resp.getResponseCode(),
      body: resp.getContentText()
    };
  } catch (err) {
    Logger.log("firestoreCreateDocument error: " + err);
    return {
      code: 500,
      body: JSON.stringify({ error: String(err) })
    };
  }
}

/**
 * Update (patch) an existing document in Firestore
 * @param {string} projectId - Firebase project ID
 * @param {string} collectionPath - Collection path (e.g., "constants")
 * @param {string} docId - Document ID
 * @param {object} fieldsObj - Fields to update (will be converted to Firestore format)
 * @returns {object} { code: number, body: string }
 */
function firestorePatchDocument(projectId, collectionPath, docId, fieldsObj) {
  try {
    const accessToken = getServiceAccountAccessToken_();
    const url = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/${collectionPath}/${encodeURIComponent(docId)}?currentDocument.exists=true`;

    // formatFirestoreData from helpers.gs returns fields in correct format
    // Returns: { fieldName: { typeValue: value }, ... }
    const formattedFields = formatFirestoreData(fieldsObj);
    const payload = { fields: formattedFields };

    const resp = UrlFetchApp.fetch(url, {
      method: "patch",
      contentType: "application/json",
      headers: {
        Authorization: "Bearer " + accessToken
      },
      muteHttpExceptions: true,
      payload: JSON.stringify(payload)
    });

    return {
      code: resp.getResponseCode(),
      body: resp.getContentText()
    };
  } catch (err) {
    Logger.log("firestorePatchDocument error: " + err);
    return {
      code: 500,
      body: JSON.stringify({ error: String(err) })
    };
  }
}

/**
 * Delete a document from Firestore
 * @param {string} projectId - Firebase project ID
 * @param {string} docPath - Full document path (e.g., "constants/ranks")
 * @returns {object} { code: number, body: string }
 */
function firestoreDeleteDocument(projectId, docPath) {
  try {
    const accessToken = getServiceAccountAccessToken_();
    const url = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/${docPath}`;
    
    const resp = UrlFetchApp.fetch(url, {
      method: "delete",
      headers: {
        Authorization: "Bearer " + accessToken
      },
      muteHttpExceptions: true
    });
    
    return {
      code: resp.getResponseCode(),
      body: resp.getContentText()
    };
  } catch (err) {
    Logger.log("firestoreDeleteDocument error: " + err);
    return {
      code: 500,
      body: JSON.stringify({ error: String(err) })
    };
  }
}

