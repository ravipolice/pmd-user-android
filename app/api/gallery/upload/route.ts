import { NextResponse } from "next/server";

const GALLERY_API_URL = "https://script.google.com/macros/s/AKfycbwXIhqfYWER3Z2KBlcrqZjyWCBfacHOeKCo_buWaZ6nG7qQpWaN91V7Y-IclzmOvG73/exec";

// Get token from environment variable (same as mobile app uses)
const getSecretToken = (): string => {
  // Try environment variable first
  const envToken = process.env.APPS_SCRIPT_SECRET_TOKEN || process.env.NEXT_PUBLIC_APPS_SCRIPT_SECRET_TOKEN;
  if (envToken && envToken !== "CHANGE_THIS_IN_PRODUCTION") {
    return envToken;
  }
  // Fallback to the token from helpers.gs (should match Apps Script)
  return "Ravi@PMD_2025_Secure_Token";
};

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const { title, fileBase64, mimeType, category, description, userEmail } = body;

    if (!fileBase64) {
      return NextResponse.json(
        { success: false, error: "Missing required field: fileBase64" },
        { status: 400 }
      );
    }

    const token = getSecretToken();
    // Gallery Apps Script expects action=upload or uploadgallery
    const url = `${GALLERY_API_URL}?action=upload&token=${encodeURIComponent(token)}`;
    
    console.log("Gallery Upload API Route: Uploading image:", title || "Untitled");
    console.log("Gallery Upload API Route: File size (base64):", fileBase64.length, "characters");
    console.log("Gallery Upload API Route: MIME type:", mimeType);
    console.log("Gallery Upload API Route: Token present:", token ? "Yes" : "No");
    
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        action: "upload",
        title: title || "Untitled",
        fileBase64: fileBase64,
        mimeType: mimeType || "image/jpeg",
        category: category || "Gallery",
        description: description || "",
        userEmail: userEmail || "admin@pmd.com",
        token: token, // Include token in body as well
      }),
      cache: "no-store",
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error("Gallery Upload API Route: Upload error response:", errorText);
      throw new Error(`HTTP error! status: ${response.status}, body: ${errorText}`);
    }

    const data = await response.json();
    console.log("Gallery Upload API Route: Upload response:", data);

    return NextResponse.json(data, {
      status: 200,
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type, Authorization",
      },
    });
  } catch (error: any) {
    console.error("Gallery Upload API Route: Error uploading image:", error);
    return NextResponse.json(
      { success: false, error: error.message || "Failed to upload image" },
      { status: 500 }
    );
  }
}



