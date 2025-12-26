import { NextResponse } from "next/server";
import path from "path";
import fs from "fs";

// Try to use Firebase Admin SDK for server-side operations
let adminInitialized = false;
let adminStorage: any = null;
let adminFirestore: any = null;

// Lazy initialization function (called when API route is hit)
function initializeAdminSDK() {
  if (adminInitialized) {
    return { success: true, storage: adminStorage, firestore: adminFirestore };
  }

  try {
    const admin = require("firebase-admin");
    if (!admin.apps.length) {
      // Try to initialize with default credentials (works in production/Cloud Run)
      // For local dev, you may need GOOGLE_APPLICATION_CREDENTIALS env var
      // pointing to a service account JSON file
      try {
        // First try with explicit credential if available
        if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
          // Resolve the path (handles both absolute and relative paths)
          const credentialsPath = path.isAbsolute(process.env.GOOGLE_APPLICATION_CREDENTIALS)
            ? process.env.GOOGLE_APPLICATION_CREDENTIALS
            : path.resolve(process.cwd(), process.env.GOOGLE_APPLICATION_CREDENTIALS);
          
          console.log("📁 Loading service account from:", credentialsPath);
          console.log("📁 Current working directory:", process.cwd());
          console.log("📁 GOOGLE_APPLICATION_CREDENTIALS:", process.env.GOOGLE_APPLICATION_CREDENTIALS);
          
          // Check if file exists
          if (!fs.existsSync(credentialsPath)) {
            throw new Error(`Service account file not found at: ${credentialsPath}`);
          }
          
          // Read and parse the JSON file
          const serviceAccountJson = fs.readFileSync(credentialsPath, "utf8");
          const serviceAccount = JSON.parse(serviceAccountJson);
          
          console.log("📧 Service account email:", serviceAccount.client_email);
          console.log("📋 Project ID:", serviceAccount.project_id);
          
          admin.initializeApp({
            credential: admin.credential.cert(serviceAccount),
            storageBucket: "pmd-police-mobile-directory.firebasestorage.app",
            projectId: "pmd-police-mobile-directory",
          });
        } else {
          console.log("⚠️ GOOGLE_APPLICATION_CREDENTIALS not set, trying default credentials");
          // Try with default credentials (Application Default Credentials)
          // This works in Cloud Run, Cloud Functions, etc.
          admin.initializeApp({
            storageBucket: "pmd-police-mobile-directory.firebasestorage.app",
            projectId: "pmd-police-mobile-directory",
          });
        }
        adminInitialized = true;
        adminStorage = admin.storage();
        adminFirestore = admin.firestore();
        console.log("✅ Firebase Admin SDK initialized successfully");
        return { success: true, storage: adminStorage, firestore: adminFirestore };
      } catch (initError: any) {
        console.warn("⚠️ Firebase Admin SDK initialization failed:", initError.message);
        console.warn("⚠️ Error code:", initError.code);
        if (initError.code === "app/no-app") {
          console.warn("⚠️ This usually means Admin SDK credentials are missing.");
          console.warn("⚠️ For local development, set GOOGLE_APPLICATION_CREDENTIALS env var");
          console.warn("⚠️ Or use 'gcloud auth application-default login' for ADC");
        }
        return { success: false, error: initError.message };
      }
    } else {
      adminInitialized = true;
      adminStorage = admin.storage();
      adminFirestore = admin.firestore();
      console.log("✅ Firebase Admin SDK already initialized");
      return { success: true, storage: adminStorage, firestore: adminFirestore };
    }
  } catch (error: any) {
    console.warn("⚠️ Firebase Admin SDK not available:", error.message);
    return { success: false, error: error.message };
  }
}

// Fallback: Initialize Firebase client SDK for server-side use
function initializeClientSDK() {
  try {
    const { initializeApp, getApps } = require("firebase/app");
    const firebaseConfig = {
      apiKey: "AIzaSyB_d5ueTul9vKeNw3pmEtCmbF9w1BVkrAQ",
      authDomain: "pmd-police-mobile-directory.firebaseapp.com",
      projectId: "pmd-police-mobile-directory",
      storageBucket: "pmd-police-mobile-directory.firebasestorage.app",
    };
    let clientApp;
    if (!getApps().length) {
      clientApp = initializeApp(firebaseConfig);
    } else {
      clientApp = getApps()[0];
    }
    console.log("✅ Firebase Client SDK initialized as fallback");
    return { success: true, app: clientApp };
  } catch (error: any) {
    console.error("❌ Failed to initialize Firebase:", error.message);
    return { success: false, error: error.message };
  }
}

export async function POST(request: Request) {
  console.log("📥 Documents Firebase Upload API: Request received");
  
  try {
    const body = await request.json();
    const { title, fileBase64, mimeType, category, description, userEmail } = body;

    if (!fileBase64) {
      return NextResponse.json(
        { success: false, error: "Missing required field: fileBase64" },
        { status: 400 }
      );
    }

    console.log("📥 Upload request:", {
      title: title || "Untitled",
      mimeType: mimeType,
      fileSize: fileBase64 ? `${(fileBase64.length * 3/4 / 1024).toFixed(2)} KB` : "unknown",
    });

    // Decode base64 to buffer
    let fileBuffer: Buffer;
    try {
      fileBuffer = Buffer.from(fileBase64, "base64");
    } catch (error) {
      return NextResponse.json(
        { success: false, error: "Invalid base64 file data" },
        { status: 400 }
      );
    }

    // Validate file size (max 10MB for documents)
    const maxSize = 10 * 1024 * 1024; // 10MB
    if (fileBuffer.length > maxSize) {
      return NextResponse.json(
        { success: false, error: "File too large (max 10MB)" },
        { status: 400 }
      );
    }

    // Determine file extension from MIME type or use generic
    const fileMimeType = mimeType || "application/octet-stream";
    let ext = "bin";
    if (fileMimeType.includes("pdf")) ext = "pdf";
    else if (fileMimeType.includes("word") || fileMimeType.includes("document")) ext = "docx";
    else if (fileMimeType.includes("excel") || fileMimeType.includes("spreadsheet")) ext = "xlsx";
    else if (fileMimeType.includes("powerpoint") || fileMimeType.includes("presentation")) ext = "pptx";
    else if (fileMimeType.includes("image")) {
      if (fileMimeType.includes("png")) ext = "png";
      else if (fileMimeType.includes("jpeg") || fileMimeType.includes("jpg")) ext = "jpg";
      else ext = "img";
    } else {
      // Try to extract from MIME type
      const mimeMatch = fileMimeType.match(/\/([a-z0-9]+)$/);
      if (mimeMatch) ext = mimeMatch[1];
    }

    const fileId = `${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
    const storagePath = `documents/${fileId}.${ext}`;

    let downloadUrl: string;

    // Initialize Admin SDK (lazy initialization - reads .env.local at runtime)
    const adminResult = initializeAdminSDK();
    if (adminResult.success && adminResult.storage) {
      // Use Admin SDK (preferred for server-side)
      try {
        console.log("📤 Uploading via Firebase Admin SDK to:", storagePath);
        // Explicitly specify the bucket name (without gs:// prefix)
        // Use the actual bucket name from Firebase Console: pmd-police-mobile-directory.firebasestorage.app
        const bucketName = "pmd-police-mobile-directory.firebasestorage.app";
        console.log("📦 Using bucket:", bucketName);
        
        // Try to get the bucket - this will throw if it doesn't exist or no permissions
        const bucket = adminResult.storage.bucket(bucketName);
        console.log("📦 Bucket object created, attempting to verify access...");
        
        // Verify bucket exists and we have access (non-blocking - continue even if check fails)
        try {
          const [exists] = await Promise.race([
            bucket.exists(),
            new Promise<[boolean]>((_, reject) => 
              setTimeout(() => reject(new Error("Bucket check timeout")), 5000)
            )
          ]);
          
          if (!exists) {
            console.warn("⚠️ Bucket exists check returned false, but continuing with upload attempt...");
          } else {
            console.log("✅ Bucket verified and accessible:", bucketName);
          }
        } catch (checkError: any) {
          console.warn("⚠️ Bucket check failed (may be permissions propagation delay):", checkError.message);
          console.warn("⚠️ Continuing with upload attempt anyway...");
          // Don't throw - sometimes exists() check fails but upload works
        }
        
        const file = bucket.file(storagePath);

        // Upload file
        await file.save(fileBuffer, {
          metadata: {
            contentType: fileMimeType,
            metadata: {
              uploadedBy: userEmail || "admin@pmd.com",
              title: title || "Untitled",
            },
          },
          public: true, // Make file publicly accessible
        });

        // Get download URL
        downloadUrl = `https://storage.googleapis.com/${bucket.name}/${storagePath}`;
        console.log("✅ Admin SDK upload successful:", downloadUrl);
      } catch (adminError: any) {
        console.error("❌ Admin SDK upload failed:", adminError);
        
        // Check if it's a bucket permission issue
        if (adminError.response?.data?.error?.code === 404) {
          const errorMsg = adminError.response.data.error.message || adminError.message;
          throw new Error(
            `Bucket not found or service account lacks permissions: ${errorMsg}\n\n` +
            `💡 To fix:\n` +
            `1. Go to Firebase Console → Project Settings → Service Accounts\n` +
            `2. Ensure the service account has "Storage Admin" or "Storage Object Admin" role\n` +
            `3. Or go to Google Cloud Console → IAM & Admin → IAM\n` +
            `4. Find the service account email and grant Storage permissions`
          );
        }
        
        throw new Error(`Admin SDK upload failed: ${adminError.message}`);
      }
    } else {
      // Try client SDK as fallback
      const clientResult = initializeClientSDK();
      if (clientResult.success && clientResult.app) {
        // Fallback to client SDK
        try {
          console.log("📤 Uploading via Firebase Client SDK to:", storagePath);
          const { getStorage, ref, uploadBytes, getDownloadURL } = require("firebase/storage");
          const storage = getStorage(clientResult.app);
          const storageRef = ref(storage, storagePath);

          // Convert Buffer to Uint8Array for Firebase Storage
          const uint8Array = new Uint8Array(fileBuffer);
          await uploadBytes(storageRef, uint8Array, {
            contentType: fileMimeType,
            customMetadata: {
              uploadedBy: userEmail || "admin@pmd.com",
              title: title || "Untitled",
            },
          });

          downloadUrl = await getDownloadURL(storageRef);
          console.log("✅ Client SDK upload successful:", downloadUrl);
        } catch (clientError: any) {
          console.error("❌ Client SDK upload failed:", clientError);
          console.error("❌ Error code:", clientError.code);
          console.error("❌ Error message:", clientError.message);
          console.error("❌ Error serverResponse:", clientError.serverResponse);
          console.error("❌ Full error:", JSON.stringify(clientError, Object.getOwnPropertyNames(clientError), 2));
          
          // Provide helpful error message
          let errorMessage = `Upload failed: ${clientError.message || clientError.code || "Unknown error"}`;
          if (clientError.code === "storage/unauthorized" || clientError.code === "storage/unknown") {
            errorMessage += "\n\n💡 This might be due to Firebase Storage security rules. Please check your Firebase Console > Storage > Rules to ensure uploads to 'documents/*' are allowed.";
          }
          throw new Error(errorMessage);
        }
      } else {
        throw new Error(`Firebase initialization failed: ${clientResult.error || "Unknown error"}`);
      }
    }

    // Save metadata to Firestore
    console.log("💾 Attempting to save document metadata to Firestore...");
    console.log("💾 Document ID:", fileId);
    console.log("💾 Collection: documents");
    
    try {
      if (adminResult.success && adminResult.firestore) {
        // Use Admin SDK Firestore
        console.log("💾 Using Admin SDK Firestore...");
        const admin = require("firebase-admin");
        const now = new Date();
        const docData = {
          title: title || "Untitled",
          url: downloadUrl,
          URL: downloadUrl,
          category: category || undefined,
          description: description || undefined,
          uploadedBy: userEmail || "admin@pmd.com",
          createdAt: admin.firestore.Timestamp.fromDate(now),
          updatedAt: admin.firestore.Timestamp.fromDate(now),
          storagePath: storagePath,
          fileType: fileMimeType,
        };
        console.log("💾 Document data to save:", JSON.stringify(docData, null, 2));
        
        await adminResult.firestore.collection("documents").doc(fileId).set(docData);
        console.log("✅ Firestore metadata saved via Admin SDK");
        console.log("✅ Document saved to: documents/" + fileId);
      } else {
        // Try client SDK Firestore as fallback
        console.log("💾 Admin SDK not available, trying Client SDK Firestore...");
        const clientResult = initializeClientSDK();
        if (clientResult.success && clientResult.app) {
          // Use client SDK Firestore
          const { getFirestore, doc, setDoc, Timestamp } = require("firebase/firestore");
          const db = getFirestore(clientResult.app);
          const now = Timestamp.now();
          
          const docData = {
            title: title || "Untitled",
            url: downloadUrl,
            URL: downloadUrl,
            category: category || undefined,
            description: description || undefined,
            uploadedBy: userEmail || "admin@pmd.com",
            createdAt: now,
            updatedAt: now,
            storagePath: storagePath,
            fileType: fileMimeType,
          };
          console.log("💾 Document data to save:", JSON.stringify(docData, null, 2));
          
          await setDoc(doc(db, "documents", fileId), docData);
          console.log("✅ Firestore metadata saved via Client SDK");
          console.log("✅ Document saved to: documents/" + fileId);
        } else {
          console.error("❌ Both Admin SDK and Client SDK Firestore failed to initialize");
        }
      }
    } catch (firestoreError: any) {
      console.error("❌ Failed to save to Firestore:", firestoreError.message);
      console.error("❌ Error code:", firestoreError.code);
      console.error("❌ Error stack:", firestoreError.stack);
      // Continue even if Firestore save fails - file is already uploaded
    }

    console.log("✅ Documents Firebase Upload: Successfully uploaded:", title || "Untitled");
    console.log("✅ Documents Firebase Upload: Download URL:", downloadUrl);
    console.log("✅ Documents Firebase Upload: File ID:", fileId);
    console.log("✅ Documents Firebase Upload: Storage Path:", storagePath);

    return NextResponse.json({
      success: true,
      url: downloadUrl,
      imageUrl: downloadUrl, // For compatibility
      title: title || "Untitled",
      uploader: userEmail || "admin@pmd.com",
      fileId: fileId,
      storagePath: storagePath,
    });
  } catch (error: any) {
    console.error("Documents Firebase Upload: Error uploading document:", error);
    console.error("Documents Firebase Upload: Error stack:", error.stack);
    
    // Provide detailed error message
    let errorMessage = error.message || "Failed to upload document";
    let errorDetails = "";
    
    if (error.message?.includes("Admin SDK")) {
      errorDetails = "\n\n💡 To fix this:\n1. Download Firebase service account key from Firebase Console\n2. Set GOOGLE_APPLICATION_CREDENTIALS env variable\n3. Restart dev server";
    } else if (error.message?.includes("Client SDK") || error.code === "storage/unknown" || error.code === "storage/unauthorized") {
      errorDetails = "\n\n💡 This is likely due to Firebase Storage security rules.\n\nOption 1 (Recommended): Set up Firebase Admin SDK with service account credentials\nOption 2 (Development only): Update Firebase Storage rules to allow uploads";
    } else if (error.message?.includes("Firebase not initialized")) {
      errorDetails = "\n\n💡 Firebase initialization failed. Check server console for details.";
    }
    
    return NextResponse.json(
      { 
        success: false, 
        error: errorMessage + errorDetails,
        errorCode: error.code || "unknown",
        details: process.env.NODE_ENV === "development" ? error.stack : undefined
      },
      { status: 500 }
    );
  }
}

