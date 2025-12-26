import { NextResponse } from "next/server";

// Allow overriding the Apps Script URL via env so we can switch deployments without code changes
const DOCUMENTS_API_URL =
  process.env.NEXT_PUBLIC_DOCUMENTS_API ||
  "https://script.google.com/macros/s/AKfycby-7jOc_naI1_XDVzG1qAGvNc9w3tIU4ZwmCFGUUCLdg0_DEJh7oouF8a9iy5E93-p9zg/exec";

// Default action for fetching documents (can be overridden via env if Apps Script expects a different one)
const DOCUMENTS_GET_ACTION =
  process.env.NEXT_PUBLIC_DOCUMENTS_GET_ACTION || "getDocuments";

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

export async function GET() {
  try {
    const token = getSecretToken();
    console.log("📄 Documents API Route: Fetching documents from Google Apps Script...");
    console.log("📄 Documents API Route: Token present:", !!token);
    console.log("📄 Documents API Route: Documents API URL:", DOCUMENTS_API_URL);
    console.log("📄 Documents API Route: Get action:", DOCUMENTS_GET_ACTION);
    
    // Note: doGet may not require token, but we'll include it if available
    const url = `${DOCUMENTS_API_URL}?action=${DOCUMENTS_GET_ACTION}${
      token && token !== "CHANGE_THIS_IN_PRODUCTION"
        ? `&token=${encodeURIComponent(token)}`
        : ""
    }`;
    console.log("📄 Documents API Route: Full URL (token redacted):", url.replace(token || '', '[REDACTED]'));
    
    let response;
    try {
      response = await fetch(url, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          "Accept": "application/json",
        },
        // Don't cache the response
        cache: "no-store",
        // Follow redirects (Apps Script web apps may redirect)
        redirect: "follow",
      });
    } catch (fetchError: any) {
      console.error("📄 Documents API Route: Fetch failed:", fetchError.message);
      console.error("📄 Documents API Route: Fetch error stack:", fetchError.stack);
      return NextResponse.json(
        { error: `Network error: ${fetchError.message}`, documents: [] },
        { status: 500 }
      );
    }

    console.log("📄 Documents API Route: Response status:", response.status);
    console.log("📄 Documents API Route: Response ok:", response.ok);
    console.log("📄 Documents API Route: Response URL (final):", response.url);

    if (!response.ok) {
      const errorText = await response.text().catch(() => "Could not read error response");
      console.error("📄 Documents API Route: Error response status:", response.status);
      console.error("📄 Documents API Route: Error response text:", errorText);
      
      // Return empty array instead of throwing - let Firestore documents still show
      return NextResponse.json(
        { error: `Apps Script returned ${response.status}: ${errorText.substring(0, 200)}`, documents: [] },
        { status: 200 } // Return 200 so frontend can still show Firestore documents
      );
    }

    // Get response as text first
    let text: string;
    try {
      text = await response.text();
      console.log("📄 Documents API Route: Response text length:", text.length);
      console.log("📄 Documents API Route: Response text (first 500 chars):", text.substring(0, 500));
    } catch (textError: any) {
      console.error("📄 Documents API Route: Failed to read response text:", textError.message);
      return NextResponse.json(
        { error: "Failed to read response from Apps Script", documents: [] },
        { status: 200 } // Return 200 so frontend can still show Firestore documents
      );
    }
    
    let data;
    try {
      // Try parsing as JSON
      data = JSON.parse(text);
      console.log("📄 Documents API Route: Parsed JSON successfully");
      console.log("📄 Documents API Route: Data type:", Array.isArray(data) ? "array" : typeof data);
      console.log("📄 Documents API Route: Data length:", Array.isArray(data) ? data.length : "not an array");
      
      if (Array.isArray(data) && data.length > 0) {
        console.log("📄 Documents API Route: First document sample:", JSON.stringify(data[0], null, 2));
      }
    } catch (parseError: any) {
      console.error("📄 Documents API Route: JSON parse error:", parseError?.message);
      console.error("📄 Documents API Route: Raw response (first 500 chars):", text.substring(0, 500));
      
      // If it's not JSON, check if it's HTML (Apps Script might redirect)
      if (text.trim().startsWith("<")) {
        console.error("📄 Documents API Route: Received HTML instead of JSON - Apps Script may require authentication");
        return NextResponse.json(
          { error: "Apps Script returned HTML - check token authentication", documents: [] },
          { status: 200 } // Return 200 so frontend can still show Firestore documents
        );
      }
      
      // Return empty array if parse fails
      return NextResponse.json([], {
        status: 200,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type, Authorization",
        },
      });
    }
    
    // Ensure we return an array
    let documents = [];
    if (Array.isArray(data)) {
      documents = data;
    } else if (data && typeof data === 'object') {
      // Handle object responses
      documents = data.data || data.documents || data.items || [];
      if (Array.isArray(documents)) {
        console.log("📄 Documents API Route: Extracted array from object response");
      } else {
        // If it's a single object, wrap it in array
        documents = [data];
      }
    }
    
    console.log("📄 Documents API Route: Final documents count:", documents.length);
    if (documents.length > 0) {
      console.log("📄 Documents API Route: Sample document keys:", Object.keys(documents[0]));
    }
    
    return NextResponse.json(documents, {
      status: 200,
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type, Authorization",
      },
    });
  } catch (error: any) {
    console.error("📄 Documents API Route: Unexpected error:", error.message);
    console.error("📄 Documents API Route: Error stack:", error.stack);
    // Return 200 with empty array so frontend can still show Firestore documents
    return NextResponse.json(
      { error: error.message || "Failed to fetch documents", documents: [] },
      { status: 200 }
    );
  }
}

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const { action, title, userEmail, fileId } = body;

    if (!action || !title) {
      return NextResponse.json(
        { success: false, error: "Missing required fields: action and title" },
        { status: 400 }
      );
    }

    if (action !== "deleteDocument") {
      return NextResponse.json(
        { success: false, error: `Unsupported action: ${action}` },
        { status: 400 }
      );
    }
    
    // Apps Script requires fileId for deletion
    if (!fileId) {
      return NextResponse.json(
        { success: false, error: "Missing required field: fileId" },
        { status: 400 }
      );
    }

    const token = getSecretToken();
    const url = `${DOCUMENTS_API_URL}?action=delete${token && token !== "CHANGE_THIS_IN_PRODUCTION" ? `&token=${encodeURIComponent(token)}` : ''}`;
    
    console.log("🗑️ Documents API Route: Deleting document:", title);
    console.log("🗑️ Documents API Route: File ID:", fileId);
    console.log("🗑️ Documents API Route: User email:", userEmail || "admin@pmd.com");
    console.log("🗑️ Documents API Route: Target URL:", url.replace(token || '', '[REDACTED]'));
    
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        action: "delete", // Include action in body as well
        title: title,
        fileId: fileId, // Required by Apps Script
        userEmail: userEmail || "admin@pmd.com", // Fallback if not provided
        token: token, // Include token in body as well
      }),
      cache: "no-store",
    });

    console.log("🗑️ Documents API Route: Response status:", response.status);
    console.log("🗑️ Documents API Route: Response ok:", response.ok);

    if (!response.ok) {
      const errorText = await response.text();
      console.error("🗑️ Documents API Route: Delete error response:", errorText);
      throw new Error(`HTTP error! status: ${response.status}, body: ${errorText}`);
    }

    let data;
    try {
      const text = await response.text();
      console.log("🗑️ Documents API Route: Response text:", text);
      data = JSON.parse(text);
    } catch (parseError) {
      // If response is not JSON, treat it as success if status is 200
      if (response.status === 200) {
        data = { success: true, message: "Document deleted successfully" };
      } else {
        throw new Error("Failed to parse delete response");
      }
    }
    
    console.log("🗑️ Documents API Route: Delete response:", data);

    // If Apps Script returns an error, handle it
    if (data.error || (data.success === false)) {
      throw new Error(data.error || "Delete failed");
    }

    return NextResponse.json({
      success: true,
      message: data.message || "Document deleted successfully",
      ...data
    }, {
      status: 200,
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type, Authorization",
      },
    });
  } catch (error: any) {
    console.error("API Route: Error deleting document:", error);
    return NextResponse.json(
      { success: false, error: error.message || "Failed to delete document" },
      { status: 500 }
    );
  }
}

