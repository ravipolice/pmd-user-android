/**
 * Documents Service
 * Handles fetching and normalizing documents from Apps Script API
 * 
 * ✅ Normalization happens here (single source of truth)
 * ✅ API shape decoupled from UI
 * ✅ Backend changes won't break UI
 */

export type DocumentItem = {
  id?: string;
  title: string;
  url: string;
  category?: string;
  uploadedBy?: string;
  uploadedDate?: string;
  description?: string;
  fileId?: string;
  fileType?: string;
  // For compatibility with existing Document interface
  name?: string;
  type?: string;
  createdAt?: Date;
};

/**
 * Raw API response type from Apps Script
 * Apps Script returns: Title, URL, Category, UploadedBy, UploadedDate, Description, Delete, fileId, fileType
 */
type RawApiDocument = {
  Title?: string;
  title?: string;
  name?: string;
  URL?: string;
  url?: string;
  Category?: string;
  category?: string;
  UploadedBy?: string;
  uploadedBy?: string;
  UploadedDate?: string;
  uploadedDate?: string;
  Description?: string;
  description?: string;
  Delete?: string;
  delete?: string;
  fileId?: string;
  fileType?: string;
  id?: string;
  createdAt?: any;
};

/**
 * Fetch documents from both Firestore and Apps Script API
 * Merges and normalizes the response to a consistent format
 */
export async function fetchDocuments(): Promise<DocumentItem[]> {
  try {
    // Fetch from Firestore first
    let firestoreDocs: DocumentItem[] = [];
    try {
      const { getDocumentsList } = await import("@/lib/firebase/firestore");
      const fsDocs = await getDocumentsList();
      firestoreDocs = fsDocs.map((doc: any) => ({
        id: doc.id,
        title: doc.title || doc.name || "Untitled Document",
        url: doc.url || doc.URL || "",
        category: doc.category || doc.Category,
        uploadedBy: doc.uploadedBy || doc.UploadedBy,
        uploadedDate: doc.uploadedDate || doc.UploadedDate || (doc.createdAt?.toDate ? doc.createdAt.toDate().toISOString() : undefined),
        description: doc.description || doc.Description,
        fileId: doc.fileId,
        fileType: doc.fileType || doc.type,
        name: doc.name || doc.title,
        type: doc.type || doc.fileType,
        createdAt: doc.createdAt?.toDate ? doc.createdAt.toDate() : (doc.createdAt ? new Date(doc.createdAt) : undefined),
      }));
      console.log(`Found ${firestoreDocs.length} documents in Firestore`);
    } catch (error: any) {
      console.warn("Error fetching from Firestore:", error?.message || "Unknown");
    }

    // Fetch from Apps Script API
    let apiDocs: RawApiDocument[] = [];
    try {
      const response = await fetch("/api/documents", {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
        cache: "no-store",
      });

      if (response.ok) {
        const data = await response.json();
        // Handle both array and object responses
        apiDocs = Array.isArray(data) 
          ? data 
          : (data?.data || data?.documents || data?.items || []);
        console.log(`Found ${apiDocs.length} documents from Apps Script API`);
      }
    } catch (error: any) {
      console.warn("Error fetching from Apps Script API:", error?.message || "Unknown");
    }

    // Combine both sources (Firestore takes precedence for duplicates by URL)
    const allDocs: RawApiDocument[] = [...firestoreDocs, ...apiDocs];
    
    // Remove duplicates by URL (prefer Firestore entries)
    const uniqueDocs = new Map<string, RawApiDocument>();
    firestoreDocs.forEach(doc => {
      if (doc.url) uniqueDocs.set(doc.url, doc);
    });
    apiDocs.forEach(doc => {
      const url = doc.URL || doc.url;
      if (url && !uniqueDocs.has(url)) {
        uniqueDocs.set(url, doc);
      }
    });

    const rawDocs = Array.from(uniqueDocs.values());

    if (!Array.isArray(rawDocs)) {
      console.warn("Combined documents is not an array");
      return [];
    }

    // 🔥 NORMALIZATION HAPPENS HERE (ONLY ONCE)
    const normalized = rawDocs
      .map((doc: RawApiDocument) => {
        // Filter out deleted documents
        const deleteStatus = doc.Delete || doc.delete || "";
        if (deleteStatus.toString().toLowerCase() === "deleted") {
          return null;
        }

        // Normalize field names - Apps Script uses Title/URL, we use title/url
        const normalized: DocumentItem = {
          id: doc.id || doc.fileId || undefined,
          // Map Title -> title (Apps Script format to our format)
          title: doc.Title || doc.title || doc.name || "Untitled Document",
          // Map URL -> url
          url: doc.URL || doc.url || "",
          // Map Category -> category
          category: doc.Category || doc.category || undefined,
          // Map UploadedBy -> uploadedBy
          uploadedBy: doc.UploadedBy || doc.uploadedBy || undefined,
          // Map UploadedDate -> uploadedDate
          uploadedDate: doc.UploadedDate || doc.uploadedDate || undefined,
          // Map Description -> description
          description: doc.Description || doc.description || undefined,
          // Keep fileId and fileType as-is
          fileId: doc.fileId || undefined,
          fileType: doc.fileType || undefined,
          // For compatibility with existing Document interface
          name: doc.Title || doc.title || doc.name || "Untitled Document",
          type: doc.fileType || doc.Category || doc.category || undefined,
          // Parse UploadedDate to Date if available
          createdAt: doc.UploadedDate 
            ? new Date(doc.UploadedDate) 
            : (doc.createdAt ? (doc.createdAt.toDate ? doc.createdAt.toDate() : new Date(doc.createdAt)) : undefined),
        };

        return normalized;
      })
      .filter((doc): doc is DocumentItem => doc !== null); // Remove null entries (deleted documents)

    // Sort by date (newest first)
    normalized.sort((a, b) => {
      const aDate = a.createdAt?.getTime() || (a.uploadedDate ? new Date(a.uploadedDate).getTime() : 0);
      const bDate = b.createdAt?.getTime() || (b.uploadedDate ? new Date(b.uploadedDate).getTime() : 0);
      return bDate - aDate; // Descending order (newest first)
    });

    return normalized;
  } catch (error: any) {
    console.error("Error fetching documents:", error);
    throw new Error(`Failed to fetch documents: ${error?.message || "Unknown error"}`);
  }
}

/**
 * Delete a document via Apps Script API
 * @param title - Document title (required by Apps Script)
 * @param userEmail - User email for logging
 */
export async function deleteDocument(title: string, userEmail: string, fileId?: string): Promise<void> {
  try {
    const response = await fetch("/api/documents", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        action: "deleteDocument",
        title: title,
        fileId: fileId, // Include fileId if available (required by Apps Script)
        userEmail: userEmail,
      }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ error: "Unknown error" }));
      throw new Error(errorData.error || `Failed to delete document: ${response.status}`);
    }

    const data = await response.json();
    if (!data.success) {
      throw new Error(data.error || "Failed to delete document");
    }
  } catch (error: any) {
    console.error("Error deleting document:", error);
    throw new Error(`Failed to delete document: ${error?.message || "Unknown error"}`);
  }
}

/**
 * Get Google Drive preview URL
 * Converts various Google Drive URL formats to preview URL
 */
export function getPreviewUrl(url: string): string {
  if (!url) return url;
  
  // Extract file ID from various Google Drive URL formats
  let fileId: string | null = null;
  
  // Pattern 1: /file/d/FILE_ID/view or /file/d/FILE_ID/
  const match1 = url.match(/\/file\/d\/([-\w]{25,})/);
  if (match1) fileId = match1[1];
  
  // Pattern 2: ?id=FILE_ID or &id=FILE_ID
  if (!fileId) {
    const match2 = url.match(/[?&]id=([-\w]{25,})/);
    if (match2) fileId = match2[1];
  }
  
  // Pattern 3: /uc?export=view&id=FILE_ID
  if (!fileId) {
    const match3 = url.match(/\/uc\?export=view&id=([-\w]{25,})/);
    if (match3) fileId = match3[1];
  }
  
  // If we found a file ID, return preview URL
  if (fileId) {
    return `https://drive.google.com/file/d/${fileId}/preview`;
  }
  
  // Otherwise, return original URL
  return url;
}

/**
 * Get Google Drive download URL
 * Converts various Google Drive URL formats to direct download URL
 */
export function getDownloadUrl(url: string): string {
  if (!url) return url;
  
  // Extract file ID from various Google Drive URL formats
  let fileId: string | null = null;
  
  // Pattern 1: /file/d/FILE_ID/view or /file/d/FILE_ID/
  const match1 = url.match(/\/file\/d\/([-\w]{25,})/);
  if (match1) fileId = match1[1];
  
  // Pattern 2: ?id=FILE_ID or &id=FILE_ID
  if (!fileId) {
    const match2 = url.match(/[?&]id=([-\w]{25,})/);
    if (match2) fileId = match2[1];
  }
  
  // Pattern 3: /uc?export=view&id=FILE_ID
  if (!fileId) {
    const match3 = url.match(/\/uc\?export=view&id=([-\w]{25,})/);
    if (match3) fileId = match3[1];
  }
  
  // If we found a file ID, return download URL
  if (fileId) {
    return `https://drive.google.com/uc?export=download&id=${fileId}`;
  }
  
  // Otherwise, return original URL
  return url;
}

