import { NextRequest, NextResponse } from "next/server";

// Base URL should be only the Apps Script exec URL (no action or token)
// Gallery has its own separate Apps Script deployment (different from Documents)
const GALLERY_API_URL =
  process.env.NEXT_PUBLIC_GALLERY_API ||
  "https://script.google.com/macros/s/AKfycbwXIhqfYWER3Z2KBlcrqZjyWCBfacHOeKCo_buWaZ6nG7qQpWaN91V7Y-IclzmOvG73/exec";

export async function GET(req: NextRequest) {
  // Gallery Apps Script works without action parameter (test URL confirms this)
  // The Gallery script returns data directly when called without action
  // But include action for compatibility if needed
  
  // Get token from env or use fallback (must match helpers.gs: "Ravi@PMD_2025_Secure_Token")
  const token =
    process.env.APPS_SCRIPT_SECRET_TOKEN ||
    process.env.NEXT_PUBLIC_APPS_SCRIPT_SECRET_TOKEN ||
    "Ravi@PMD_2025_Secure_Token"; // Fallback to match Apps Script

  // Build URL - Gallery script works without action, but add token if needed
  // Test shows it works with just the base URL
  let targetUrl = GALLERY_API_URL;
  if (token) {
    targetUrl += `?token=${encodeURIComponent(token)}`;
  }

  console.log("🔥 Gallery Proxy - Forwarding to:", targetUrl.replace(token, "[REDACTED]"));

  try {
    const res = await fetch(targetUrl, { 
      method: "GET",
      cache: "no-store",
      redirect: "follow",
    });

    const text = await res.text();
    console.log("🔥 Gallery Proxy - Response status:", res.status);
    console.log("🔥 Gallery Proxy - Response text (first 200 chars):", text.substring(0, 200));

    return new NextResponse(text, {
      status: 200,
      headers: {
        "Content-Type": "application/json",
      },
    });
  } catch (error: any) {
    console.error("🔥 Gallery Proxy - Error:", error.message);
    return new NextResponse(
      JSON.stringify({ error: error.message || "Proxy error" }),
      {
        status: 500,
        headers: {
          "Content-Type": "application/json",
        },
      }
    );
  }
}

