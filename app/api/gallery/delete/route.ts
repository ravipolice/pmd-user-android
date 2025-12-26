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
    const { title, userEmail } = body;

    if (!title) {
      return NextResponse.json(
        { success: false, error: "Missing required field: title" },
        { status: 400 }
      );
    }

    const token = getSecretToken();
    // Gallery Apps Script expects action=delete or deletegallery
    const url = `${GALLERY_API_URL}?action=delete&token=${encodeURIComponent(token)}`;
    
    console.log("Gallery Delete API Route: Deleting image:", title);
    console.log("Gallery Delete API Route: User:", userEmail || "admin@pmd.com");
    
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        action: "delete",
        title: title,
        userEmail: userEmail || "admin@pmd.com",
        token: token, // Include token in body as well
      }),
      cache: "no-store",
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error("Gallery Delete API Route: Delete error response:", errorText);
      throw new Error(`HTTP error! status: ${response.status}, body: ${errorText}`);
    }

    const data = await response.json();
    console.log("Gallery Delete API Route: Delete response:", data);

    return NextResponse.json(data, {
      status: 200,
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type, Authorization",
      },
    });
  } catch (error: any) {
    console.error("Gallery Delete API Route: Error deleting image:", error);
    return NextResponse.json(
      { success: false, error: error.message || "Failed to delete image" },
      { status: 500 }
    );
  }
}



