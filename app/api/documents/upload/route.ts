import { NextResponse } from "next/server";

const DOCUMENTS_API_URL = "https://script.google.com/macros/s/AKfycby-7jOc_naI1_XDVzG1qAGvNc9w3tIU4ZwmCFGUUCLdg0_DEJh7oouF8a9iy5E93-p9zg/exec";

// Get token from environment variable (same as mobile app uses)
const getSecretToken = (): string => {
  // Try environment variable first
  const envToken = process.env.APPS_SCRIPT_SECRET_TOKEN || process.env.NEXT_PUBLIC_APPS_SCRIPT_SECRET_TOKEN;
  if (envToken && envToken !== "CHANGE_THIS_IN_PRODUCTION") {
    return envToken;
  }
  // Fallback to the token from helpers.gs (should match Apps Script)
  // This is a temporary fallback - should be set in environment variables
  return "Ravi@PMD_2025_Secure_Token";
};

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const { action, title, fileBase64, mimeType, category, description, userEmail, externalUrl } = body;

    // For Firebase uploads, externalUrl is provided instead of fileBase64
    if (!title || (!fileBase64 && !externalUrl)) {
      return NextResponse.json(
        { success: false, error: "Missing required fields: title and either fileBase64 or externalUrl" },
        { status: 400 }
      );
    }

    const token = getSecretToken();
    // Apps Script expects action in query parameter or body
    // Token should be in both query parameter AND body (for compatibility)
    const url = `${DOCUMENTS_API_URL}?action=upload&token=${encodeURIComponent(token)}`;
    
    if (externalUrl) {
      // Firebase upload - register URL in Google Sheet
      console.log("📝 Documents API Route: Registering Firebase document in Google Sheet");
      console.log("📝 Documents API Route: Title:", title);
      console.log("📝 Documents API Route: External URL:", externalUrl);
    } else {
      // Google Drive upload - upload file
      console.log("📤 Documents API Route: Uploading document to Google Drive");
      console.log("📤 Documents API Route: Title:", title);
      console.log("📤 Documents API Route: File size (base64):", fileBase64.length, "characters");
      console.log("📤 Documents API Route: MIME type:", mimeType);
    }
    console.log("📝 Documents API Route: Token present:", token ? "Yes" : "No");
    
    const requestBody: any = {
      action: "upload",
      title: title,
      category: category || "",
      description: description || "",
      userEmail: userEmail || "admin@pmd.com",
      token: token, // Include token in body as well (verifyToken checks both)
    };
    
    // Include either fileBase64 (for Drive upload) or externalUrl (for Firebase registration)
    if (externalUrl) {
      requestBody.externalUrl = externalUrl;
    } else {
      requestBody.fileBase64 = fileBase64;
      requestBody.mimeType = mimeType || "application/pdf";
    }
    
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(requestBody),
      cache: "no-store",
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error("API Route: Upload error response:", errorText);
      throw new Error(`HTTP error! status: ${response.status}, body: ${errorText}`);
    }

    const data = await response.json();
    console.log("API Route: Upload response:", data);

    return NextResponse.json(data, {
      status: 200,
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type, Authorization",
      },
    });
  } catch (error: any) {
    console.error("API Route: Error uploading document:", error);
    return NextResponse.json(
      { success: false, error: error.message || "Failed to upload document" },
      { status: 500 }
    );
  }
}

