import {
  collection,
  doc,
  getDoc,
  getDocs,
  addDoc,
  setDoc,
  updateDoc,
  deleteDoc,
  query,
  where,
  orderBy,
  limit,
  Timestamp,
  QueryConstraint,
  getDocsFromCache,
  getDocsFromServer,
} from "firebase/firestore";
import { db } from "./config";

// Types
export interface Employee {
  id?: string;
  kgid: string;
  name: string;
  email?: string;
  mobile1: string;
  mobile2?: string;
  rank?: string;
  metalNumber?: string;
  displayRank?: string; // Computed: rank + " " + metalNumber (if metalNumber exists)
  district: string;
  station: string;
  bloodGroup?: string;
  photoUrl?: string;
  photoUrlFromGoogle?: string;
  fcmToken?: string;
  firebaseUid?: string;
  isAdmin?: boolean;
  isApproved?: boolean;
  pin?: string;
  createdAt?: Timestamp;
  updatedAt?: Timestamp;
}

export interface Officer {
  id?: string;
  agid?: string; // Auto-generated ID (from cfd field)
  cfd?: string; // Source field for agid
  rank: string;
  name: string;
  mobile: string;
  email?: string;
  landline?: string;
  district: string;
  office?: string;
  createdAt?: Timestamp;
}

export interface District {
  id?: string;
  name: string;
  range?: string;
  isActive?: boolean;
  createdAt?: Timestamp;
}

export interface Station {
  id?: string;
  name: string;
  district: string;
  stdCode?: string;
  isActive?: boolean;
  createdAt?: Timestamp;
}

export interface Rank {
  rank_id: string; // Document ID (IMMUTABLE, e.g., "DG_IGP", "DSP", "PSI")
  rank_label: string; // Display name (e.g., "Director General & Inspector General of Police")
  staffType: "POLICE" | "MINISTERIAL"; // Cadre type
  category: string; // DISTRICT | COMMISSIONERATE | BOTH
  equivalent_rank: string; // Normalization (ACP → DSP)
  seniority_order: number; // Hierarchy order (1 = highest)
  aliases: string[]; // Text variants (DYSP, PSIW, WPSI)
  requiresMetalNumber: boolean; // True if metal number mandatory
  isActive: boolean; // Soft enable/disable
  remarks?: string; // Optional admin notes
  createdAt?: Timestamp; // serverTimestamp()
  updatedAt?: Timestamp; // serverTimestamp()
}

export interface PendingRegistration {
  id?: string;
  kgid: string;
  email: string;
  name: string;
  mobile1: string;
  mobile2?: string;
  rank?: string;
  district: string;
  station: string;
  pin: string;
  createdAt?: Timestamp;
}

export interface NotificationQueue {
  id?: string;
  title: string;
  body: string;
  targetType: "SINGLE" | "STATION" | "DISTRICT" | "ADMIN" | "ALL";
  targetKgid?: string;
  targetDistrict?: string;
  targetStation?: string;
  status?: string;
  sentCount?: number;
  failedCount?: number;
  createdAt?: Timestamp;
}

export interface Document {
  id?: string;
  // Support both field name formats (admin panel and mobile app)
  name?: string;
  title?: string; // Mobile app format
  url: string;
  URL?: string; // Mobile app format
  type?: string;
  fileType?: string; // Mobile app format
  category?: string; // Mobile app format
  description?: string; // Mobile app format
  uploadedBy?: string; // Mobile app format
  uploadedDate?: string; // Mobile app format
  fileId?: string; // Mobile app format
  size?: number;
  createdAt?: Timestamp;
  // Helper to get the display name
  getDisplayName?: () => string;
  // Helper to get the display URL
  getDisplayUrl?: () => string;
  // Helper to get the display type
  getDisplayType?: () => string;
}

export interface GalleryImage {
  id?: string;
  imageUrl: string;
  title?: string;
  createdAt?: Timestamp;
}

export interface UsefulLink {
  id?: string;
  name: string;
  playStoreUrl?: string;
  apkUrl?: string;
  iconUrl?: string;
  createdAt?: Timestamp;
  updatedAt?: Timestamp;
}

// Generic CRUD functions
const createDoc = async <T>(
  collectionName: string,
  data: Omit<T, "id">
): Promise<string> => {
  if (typeof window === "undefined" || !db) {
    throw new Error("Firestore not initialized (server-side or not available)");
  }
  const docRef = await addDoc(collection(db, collectionName), {
    ...data,
    createdAt: Timestamp.now(),
  });
  return docRef.id;
};

export const getDocument = async <T>(
  collectionName: string,
  id: string
): Promise<T | null> => {
  if (typeof window === "undefined" || !db) {
    console.warn("Firestore not initialized (server-side or not available)");
    return null;
  }
  const docRef = doc(db, collectionName, id);
  const docSnap = await getDoc(docRef);
  if (docSnap.exists()) {
    return { id: docSnap.id, ...docSnap.data() } as T;
  }
  return null;
};

export const getDocuments = async <T>(
  collectionName: string,
  constraints: QueryConstraint[] = []
): Promise<T[]> => {
  if (typeof window === "undefined" || !db) {
    console.warn("Firestore not initialized (server-side or not available)");
    return [];
  }
  const q = query(collection(db, collectionName), ...constraints);
  const querySnapshot = await getDocs(q);
  return querySnapshot.docs.map(
    (doc) => ({ id: doc.id, ...doc.data() } as T)
  );
};

export const updateDocument = async <T>(
  collectionName: string,
  id: string,
  data: Partial<T>
): Promise<void> => {
  if (typeof window === "undefined" || !db) {
    throw new Error("Firestore not initialized (server-side or not available)");
  }
  const docRef = doc(db, collectionName, id);
  await updateDoc(docRef, {
    ...data,
    updatedAt: Timestamp.now(),
  });
};

export const deleteDocument = async (
  collectionName: string,
  id: string
): Promise<void> => {
  if (typeof window === "undefined" || !db) {
    throw new Error("Firestore not initialized (server-side or not available)");
  }
  const docRef = doc(db, collectionName, id);
  await deleteDoc(docRef);
};

// Helpers
const sanitizeRankPayload = (data: Partial<Rank>) => {
  const payload: any = { ...data };
  // Firestore does not allow undefined
  Object.keys(payload).forEach((key) => {
    if (payload[key] === undefined) {
      delete payload[key];
    }
  });
  // Normalize aliases
  if (!payload.aliases) {
    payload.aliases = [];
  }
  // Default staffType
  if (!payload.staffType) {
    payload.staffType = "POLICE";
  }
  // If ministerial, force no equivalent_rank requirement and no metal number
  if (payload.staffType === "MINISTERIAL") {
    payload.equivalent_rank = payload.equivalent_rank ?? "";
    payload.requiresMetalNumber = false;
  }
  // Normalize remarks: remove empty string -> null
  if (payload.remarks === "") {
    payload.remarks = null;
  }
  return payload;
};

// Employee functions
export const getEmployees = async (): Promise<Employee[]> => {
  const employees = await getDocuments<Employee>("employees", [orderBy("name")]);
  
  // Remove duplicates based on kgid (primary key)
  // Keep the first occurrence of each kgid
  const uniqueEmployees = new Map<string, Employee>();
  employees.forEach(employee => {
    const kgid = employee.kgid?.trim().toLowerCase();
    if (kgid && !uniqueEmployees.has(kgid)) {
      uniqueEmployees.set(kgid, employee);
    } else if (kgid) {
      console.warn(`Duplicate employee found with kgid: ${employee.kgid}. Keeping first occurrence.`);
    }
  });
  
  const deduplicatedEmployees = Array.from(uniqueEmployees.values());
  
  // Normalize photo URLs for better image loading
  return deduplicatedEmployees.map(employee => {
    if (employee.photoUrl || employee.photoUrlFromGoogle) {
      const photoUrl = employee.photoUrl || employee.photoUrlFromGoogle || "";
      // Normalize Google Drive URLs to use CDN for better performance
      if (photoUrl.includes("drive.google.com")) {
        let fileId: string | null = null;
        
        // Extract file ID from various URL formats
        const match1 = photoUrl.match(/[?&]id=([-\w]{25,})/);
        if (match1) fileId = match1[1];
        
        if (!fileId) {
          const match2 = photoUrl.match(/\/file\/d\/([-\w]{25,})/);
          if (match2) fileId = match2[1];
        }
        
        // Convert to Google Image CDN if we found a file ID
        if (fileId && !photoUrl.includes("lh3.googleusercontent.com")) {
          return {
            ...employee,
            photoUrl: `https://lh3.googleusercontent.com/d/${fileId}`,
            photoUrlFromGoogle: employee.photoUrlFromGoogle ? `https://lh3.googleusercontent.com/d/${fileId}` : undefined,
          };
        }
      }
    }
    return employee;
  });
};

export const getEmployee = async (id: string): Promise<Employee | null> => {
  return getDocument<Employee>("employees", id);
};

export const createEmployee = async (data: Omit<Employee, "id">): Promise<string> => {
  // Compute displayRank: rank + " " + metalNumber (if both exist)
  const displayRank = data.rank && data.metalNumber
    ? `${data.rank} ${data.metalNumber}`
    : data.rank || undefined;

  return createDoc<Employee>("employees", {
    ...data,
    displayRank,
    createdAt: Timestamp.now(),
    updatedAt: Timestamp.now(),
  });
};

export const updateEmployee = async (
  id: string,
  data: Partial<Employee>
): Promise<void> => {
  // Recompute displayRank if rank or metalNumber is being updated
  let updateData = { ...data };
  if (data.rank !== undefined || data.metalNumber !== undefined) {
    // Get current employee data to compute displayRank correctly
    const current = await getEmployee(id);
    const rank = data.rank ?? current?.rank;
    const metalNumber = data.metalNumber ?? current?.metalNumber;
    
    updateData.displayRank = rank && metalNumber
      ? `${rank} ${metalNumber}`
      : rank || undefined;
  }
  
  return updateDocument<Employee>("employees", id, updateData);
};

export const deleteEmployee = async (id: string): Promise<void> => {
  return deleteDocument("employees", id);
};

// Officer functions
export const getOfficers = async (): Promise<Officer[]> => {
  const officers = await getDocuments<Officer>("officers", [orderBy("name")]);
  // Map cfd field to agid if cfd exists and agid doesn't
  return officers.map(officer => ({
    ...officer,
    agid: officer.agid || officer.cfd || undefined,
  }));
};

export const getOfficer = async (id: string): Promise<Officer | null> => {
  return getDocument<Officer>("officers", id);
};

export const createOfficer = async (data: Omit<Officer, "id">): Promise<string> => {
  return createDoc<Officer>("officers", {
    ...data,
    createdAt: Timestamp.now(),
  });
};

export const updateOfficer = async (
  id: string,
  data: Partial<Officer>
): Promise<void> => {
  return updateDocument<Officer>("officers", id, data);
};

export const deleteOfficer = async (id: string): Promise<void> => {
  return deleteDocument("officers", id);
};

// District functions
export const getDistricts = async (): Promise<District[]> => {
  try {
    // Get all districts and sort client-side to avoid composite index requirement
    const allDistricts = await getDocuments<District>("districts", []);
    
    // Filter active districts and sort by name
    const activeDistricts = allDistricts
      .filter((d) => d.isActive !== false) // Include districts where isActive is true or undefined
      .sort((a, b) => (a.name || "").localeCompare(b.name || ""));
    
    return activeDistricts.length > 0 ? activeDistricts : allDistricts.sort((a, b) => (a.name || "").localeCompare(b.name || ""));
  } catch (error) {
    console.error("Error fetching districts:", error);
    // Return empty array on error to prevent crashes
    return [];
  }
};

export const createDistrict = async (data: Omit<District, "id">): Promise<string> => {
  return createDoc<District>("districts", { ...data, isActive: true });
};

export const updateDistrict = async (
  id: string,
  data: Partial<District>
): Promise<void> => {
  return updateDocument<District>("districts", id, data);
};

export const deleteDistrict = async (id: string): Promise<void> => {
  return deleteDocument("districts", id);
};

// Station functions
export const getStations = async (district?: string): Promise<Station[]> => {
  try {
    if (district) {
      // Try to get stations filtered by district with ordering
      try {
        const filteredStations = await getDocuments<Station>("stations", [
          where("district", "==", district),
          orderBy("name"),
        ]);
        return filteredStations;
      } catch (error) {
        // If composite index is missing, get all and filter client-side
        console.warn("Error with filtered query, trying client-side filter:", error);
        const allStations = await getDocuments<Station>("stations", []);
        return allStations
          .filter((s) => s.district === district)
          .sort((a, b) => (a.name || "").localeCompare(b.name || ""));
      }
    } else {
      // Get all stations
      try {
        return await getDocuments<Station>("stations", [orderBy("name")]);
      } catch (error) {
        console.warn("Error with ordered query, trying without order:", error);
        const allStations = await getDocuments<Station>("stations", []);
        return allStations.sort((a, b) => (a.name || "").localeCompare(b.name || ""));
      }
    }
  } catch (error) {
    console.error("Error fetching stations:", error);
    return [];
  }
};

export const createStation = async (data: Omit<Station, "id">): Promise<string> => {
  return createDoc<Station>("stations", { ...data, isActive: true });
};

export const updateStation = async (
  id: string,
  data: Partial<Station>
): Promise<void> => {
  return updateDocument<Station>("stations", id, data);
};

export const deleteStation = async (id: string): Promise<void> => {
  return deleteDocument("stations", id);
};

// Rank functions
export const getRanks = async (): Promise<Rank[]> => {
  try {
    // Get all ranks from rankMaster collection
    const allRanks = await getDocuments<Rank>("rankMaster", []);
    
    // Add rank_id from document ID if not present
    const ranksWithId = allRanks.map(rank => ({
      ...rank,
      rank_id: rank.rank_id || (rank as any).id || "",
    }));
    
    // Filter active ranks and sort by seniority_order
    const activeRanks = ranksWithId
      .filter((r) => r.isActive !== false) // Include ranks where isActive is true or undefined
      .sort((a, b) => {
        // Sort by seniority_order (1 = highest, lower numbers first)
        if (a.seniority_order !== undefined && b.seniority_order !== undefined) {
          return a.seniority_order - b.seniority_order;
        }
        if (a.seniority_order !== undefined) return -1;
        if (b.seniority_order !== undefined) return 1;
        // Otherwise sort by rank_label
        return (a.rank_label || "").localeCompare(b.rank_label || "");
      });
    
    return activeRanks.length > 0 ? activeRanks : ranksWithId.sort((a, b) => {
      if (a.seniority_order !== undefined && b.seniority_order !== undefined) {
        return a.seniority_order - b.seniority_order;
      }
      return (a.rank_label || "").localeCompare(b.rank_label || "");
    });
  } catch (error) {
    console.error("Error fetching ranks:", error);
    // Return empty array on error to prevent crashes
    return [];
  }
};

export const createRank = async (data: Rank): Promise<string> => {
  if (typeof window === "undefined" || !db) {
    throw new Error("Firestore not initialized (server-side or not available)");
  }
  if (!data.rank_id) {
    throw new Error("rank_id is required");
  }
  const payload = sanitizeRankPayload(data);
  // Use rank_id as document ID
  const docRef = doc(db, "rankMaster", data.rank_id);
  await setDoc(docRef, {
    ...payload,
    createdAt: Timestamp.now(),
    updatedAt: Timestamp.now(),
  });
  return data.rank_id;
};

export const updateRank = async (
  rank_id: string,
  data: Partial<Omit<Rank, "rank_id" | "createdAt">>
): Promise<void> => {
  if (typeof window === "undefined" || !db) {
    throw new Error("Firestore not initialized (server-side or not available)");
  }
  const docRef = doc(db, "rankMaster", rank_id);
  const payload = sanitizeRankPayload(data);
  await updateDoc(docRef, {
    ...payload,
    updatedAt: Timestamp.now(),
  });
};

export const deleteRank = async (rank_id: string): Promise<void> => {
  return deleteDocument("rankMaster", rank_id);
};

// Helper function to get rank by name (checks equivalent_rank and aliases)
export const getRankByName = async (rankName: string): Promise<Rank | null> => {
  try {
    const ranks = await getRanks();
    return ranks.find(r => 
      r.equivalent_rank === rankName || 
      r.aliases?.includes(rankName) ||
      r.rank_id === rankName
    ) || null;
  } catch (error) {
    console.error("Error finding rank by name:", error);
    return null;
  }
};

// Pending Registrations
export const getPendingRegistrations = async (): Promise<PendingRegistration[]> => {
  try {
    return await getDocuments<PendingRegistration>("pending_registrations", [
      orderBy("createdAt", "desc"),
    ]);
  } catch (error: any) {
    // Handle common Firestore errors gracefully
    if (error?.code === "failed-precondition") {
      // Missing index - try without orderBy
      console.warn("Firestore index missing for pending_registrations, fetching without orderBy");
      try {
        return await getDocuments<PendingRegistration>("pending_registrations", []);
      } catch (fallbackError) {
        console.error("Error fetching pending registrations (fallback):", fallbackError);
        return [];
      }
    }
    console.error("Error fetching pending registrations:", error);
    return [];
  }
};

export const approveRegistration = async (
  registrationId: string,
  registration: PendingRegistration
): Promise<void> => {
  // Create employee from registration
  await createDoc<Employee>("employees", {
    kgid: registration.kgid,
    name: registration.name,
    email: registration.email,
    mobile1: registration.mobile1,
    mobile2: registration.mobile2,
    rank: registration.rank,
    district: registration.district,
    station: registration.station,
    pin: registration.pin,
    isApproved: true,
    isAdmin: false,
  });

  // Delete pending registration
  await deleteDocument("pending_registrations", registrationId);
};

export const rejectRegistration = async (registrationId: string): Promise<void> => {
  await deleteDocument("pending_registrations", registrationId);
};

// Notifications
export const createNotification = async (
  data: Omit<NotificationQueue, "id">,
  isAdmin: boolean = false
): Promise<string> => {
  // Use different collection for admin notifications
  const collectionName = isAdmin ? "admin_notifications" : "notifications_queue";
  return createDoc<NotificationQueue>(collectionName, {
    ...data,
    status: "pending",
    createdAt: Timestamp.now(),
  });
};

// Documents API URL (same as mobile app)
const DOCUMENTS_API_URL = "https://script.google.com/macros/s/AKfycby-7jOc_naI1_XDVzG1qAGvNc9w3tIU4ZwmCFGUUCLdg0_DEJh7oouF8a9iy5E93-p9zg/exec";

// Documents
export const getDocumentsList = async (): Promise<Document[]> => {
  try {
    console.log("Fetching documents from Firestore + Apps Script...");
    
    // Fetch Firestore docs first (Firebase path)
    let firestoreDocs: Document[] = [];
    try {
      // Try with orderBy first
      try {
        console.log("🔍 Fetching documents from Firestore with orderBy('createdAt', 'desc')...");
        firestoreDocs = await getDocuments<Document>("documents", [orderBy("createdAt", "desc")]);
        if (firestoreDocs.length > 0) {
          console.log(`✅ Found ${firestoreDocs.length} documents in Firestore (with orderBy)`);
          console.log("✅ Firestore documents sample:", JSON.stringify(firestoreDocs[0], null, 2));
        } else {
          console.log("ℹ️ No documents found in Firestore collection 'documents' (with orderBy)");
          // Try without orderBy as fallback (in case there are docs but index is missing)
          console.log("🔍 Trying without orderBy as fallback...");
          try {
            firestoreDocs = await getDocuments<Document>("documents", []);
            // Sort manually
            firestoreDocs.sort((a, b) => {
              const aDate = a.createdAt?.toDate ? a.createdAt.toDate().getTime() : (a.createdAt instanceof Timestamp ? a.createdAt.toMillis() : 0);
              const bDate = b.createdAt?.toDate ? b.createdAt.toDate().getTime() : (b.createdAt instanceof Timestamp ? b.createdAt.toMillis() : 0);
              return bDate - aDate;
            });
            if (firestoreDocs.length > 0) {
              console.log(`✅ Found ${firestoreDocs.length} documents in Firestore (without orderBy)`);
              console.log("✅ Firestore documents sample:", JSON.stringify(firestoreDocs[0], null, 2));
            } else {
              console.log("ℹ️ No documents found in Firestore collection 'documents' (without orderBy either)");
            }
          } catch (fallbackError: any) {
            console.error("❌ Error fetching without orderBy:", fallbackError?.message);
          }
        }
      } catch (orderByError: any) {
        // If orderBy fails (missing index), try without it
        if (orderByError?.code === "failed-precondition") {
          console.warn("⚠️ Firestore index missing for 'createdAt', fetching without orderBy...");
          firestoreDocs = await getDocuments<Document>("documents", []);
          // Sort manually
          firestoreDocs.sort((a, b) => {
            const aDate = a.createdAt?.toDate ? a.createdAt.toDate().getTime() : (a.createdAt instanceof Timestamp ? a.createdAt.toMillis() : 0);
            const bDate = b.createdAt?.toDate ? b.createdAt.toDate().getTime() : (b.createdAt instanceof Timestamp ? b.createdAt.toMillis() : 0);
            return bDate - aDate;
          });
          if (firestoreDocs.length > 0) {
            console.log(`✅ Found ${firestoreDocs.length} documents in Firestore (without orderBy)`);
            console.log("✅ Firestore documents sample:", JSON.stringify(firestoreDocs[0], null, 2));
          } else {
            console.log("ℹ️ No documents found in Firestore collection 'documents' (without orderBy)");
          }
        } else {
          throw orderByError; // Re-throw if it's not an index error
        }
      }
    } catch (error: any) {
      console.error("❌ Error fetching from Firestore:", error?.message || "Unknown");
      console.error("❌ Error code:", error?.code);
      console.error("❌ Error details:", error);
    }
    
    // Fetch from Apps Script API via Next.js proxy (Drive/Sheet path)
    let apiDocs: any[] = [];
    try {
      console.log("Fetching from Apps Script API via proxy...");
      const response = await fetch("/api/documents", {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
        cache: "no-store",
      });
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const body = await response.json();
      console.log("API response type:", Array.isArray(body) ? "array" : typeof body);
      console.log("API response length:", Array.isArray(body) ? body.length : "not an array");
      
      if (Array.isArray(body)) {
        apiDocs = body;
      } else if (body && typeof body === "object") {
        if (body.error) {
          console.error("API returned error:", body.error);
        }
        const data = body.data || body.documents || body.items || [];
        if (Array.isArray(data)) {
          apiDocs = data;
        }
      }
    } catch (apiError: any) {
      console.warn("Error fetching from Apps Script API:", apiError?.message || apiError);
    }
    
    // Deduplicate, preferring Firestore entries (Firebase uploads)
    const combinedMap = new Map<string, Document>();
    // Use a more specific key that includes source to avoid conflicts
    const keyFor = (doc: any, isFirestore: boolean = false) => {
      // For Firestore docs, prefer fileId or id
      if (isFirestore && (doc.fileId || doc.id)) {
        return `firestore_${doc.fileId || doc.id}`;
      }
      // For API docs, use fileId or URL
      if (!isFirestore && doc.fileId) {
        return `gdrive_${doc.fileId}`;
      }
      // Fallback to URL-based key
      const url = doc.url || doc.URL;
      if (url) {
        const source = url.includes("storage.googleapis.com") || url.includes("firebasestorage.app") ? "firebase" : "gdrive";
        return `${source}_${url}`;
      }
      // Last resort: title + source
      return `${isFirestore ? "firestore" : "gdrive"}_${doc.title || doc.name || doc.id || Math.random().toString(36).slice(2)}`;
    };
    
    // Add Firestore docs first (they take precedence)
    firestoreDocs.forEach((doc) => {
      const key = keyFor(doc, true);
      combinedMap.set(key, doc);
      console.log(`📦 Added Firestore doc: ${doc.title} (key: ${key})`);
    });
    
    // Add API docs only if they don't conflict
    apiDocs.forEach((doc) => {
      const key = keyFor(doc, false);
      if (!combinedMap.has(key)) {
        combinedMap.set(key, doc as Document);
        console.log(`📦 Added API doc: ${doc.title || doc.Title} (key: ${key})`);
      } else {
        console.log(`⚠️ Skipped duplicate API doc: ${doc.title || doc.Title} (key: ${key})`);
      }
    });
    
    const merged = Array.from(combinedMap.values());
    
    // Normalize field names
    const normalized = merged
      .map((doc: any) => {
        if (doc.Delete && doc.Delete.toString().toLowerCase() === "deleted") {
          return null;
        }
        const normalized: Document = {
          ...doc,
          name: doc.name || doc.Title || doc.title || "Untitled Document",
          url: doc.url || doc.URL || "",
          type: doc.type || doc.fileType || doc.Category || doc.category || "",
          title: doc.Title || doc.title || doc.name,
          URL: doc.URL || doc.url,
          fileType: doc.fileType,
          category: doc.Category || doc.category,
          description: doc.Description || doc.description,
          uploadedBy: doc.UploadedBy || doc.uploadedBy,
          uploadedDate: doc.UploadedDate || doc.uploadedDate,
          fileId: doc.fileId,
          createdAt:
            doc.createdAt ||
            (doc.UploadedDate
              ? typeof doc.UploadedDate === "string"
                ? Timestamp.fromDate(new Date(doc.UploadedDate))
                : doc.UploadedDate instanceof Date
                  ? Timestamp.fromDate(doc.UploadedDate)
                  : doc.UploadedDate
              : undefined),
        };
        return normalized;
      })
      .filter((doc): doc is Document => !!doc);
    
    normalized.sort((a, b) => {
      const aDate = a.createdAt?.toDate ? a.createdAt.toDate().getTime() : a.createdAt instanceof Timestamp ? a.createdAt.toMillis() : 0;
      const bDate = b.createdAt?.toDate ? b.createdAt.toDate().getTime() : b.createdAt instanceof Timestamp ? b.createdAt.toMillis() : 0;
      return bDate - aDate;
    });
    
    console.log(`Returning ${normalized.length} normalized documents (merged Firestore + Apps Script)`);
    return normalized;
  } catch (error: any) {
    console.error("Error fetching documents:", error);
    console.error("Error details:", {
      message: error?.message,
      code: error?.code,
      stack: error?.stack
    });
    return [];
  }
};

export const createDocument = async (data: Omit<Document, "id">): Promise<string> => {
  return createDoc<Document>("documents", data);
};

// Gallery API URL (same Apps Script as Documents)
const GALLERY_API_URL = DOCUMENTS_API_URL; // Same script, different action

// Gallery: merge Firebase (Storage metadata in Firestore) + Apps Script
export const getGalleryImages = async (): Promise<GalleryImage[]> => {
  try {
    console.log("Fetching gallery images (Firebase + Apps Script)...");

    // Fetch Firebase (Firestore) entries first
    let firestoreImages: GalleryImage[] = [];
    try {
      firestoreImages = await getDocuments<GalleryImage>("gallery", []);
      if (firestoreImages.length > 0) {
        console.log(`Found ${firestoreImages.length} images in Firestore`);
      }
    } catch (err: any) {
      console.warn("Error fetching gallery images from Firestore:", err?.message || err);
    }

    // Fetch Apps Script images
    let normalizedApi: GalleryImage[] = [];
    try {
      console.log("Fetching from Apps Script API via proxy...");
      const response = await fetch("/api/gallery", {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
        cache: "no-store",
      });
      
      if (response.ok) {
        const apiImages = await response.json();
        console.log("Gallery API response type:", Array.isArray(apiImages) ? "array" : typeof apiImages);
        console.log("Gallery API response length:", Array.isArray(apiImages) ? apiImages.length : "not an array");
        
        if (apiImages && typeof apiImages === 'object' && !Array.isArray(apiImages)) {
          if (apiImages.error) {
            console.error("Gallery API returned error:", apiImages.error);
          } else {
            const data = apiImages.data || apiImages.images || apiImages.items || [];
            if (Array.isArray(data) && data.length > 0) {
              normalizedApi = normalizeGalleryImages(data);
            }
          }
        } else if (Array.isArray(apiImages)) {
          normalizedApi = normalizeGalleryImages(apiImages);
        } else {
          console.warn("Gallery API response is not an array or object with data");
        }
      } else {
        console.error("Gallery API HTTP error:", response.status);
      }
    } catch (err: any) {
      console.warn("Error fetching gallery images from Apps Script:", err?.message || err);
    }

    // Combine Firestore + API, preferring Firestore for duplicates by imageUrl
    const combinedMap = new Map<string, GalleryImage>();
    firestoreImages.forEach((img) => {
      if (img.imageUrl) combinedMap.set(img.imageUrl, img);
    });
    normalizedApi.forEach((img) => {
      if (img.imageUrl && !combinedMap.has(img.imageUrl)) {
        combinedMap.set(img.imageUrl, img);
      }
    });

    const combined = Array.from(combinedMap.values());
    combined.sort((a, b) => {
      const aDate = a.createdAt?.toDate ? a.createdAt.toDate().getTime() : 0;
      const bDate = b.createdAt?.toDate ? b.createdAt.toDate().getTime() : 0;
      return bDate - aDate;
    });

    console.log(`Returning ${combined.length} gallery images (merged)`);
    return combined;
  } catch (error: any) {
    console.error("Error fetching gallery images:", error);
    return [];
  }
}

// Helper function to normalize gallery images from Apps Script format
function normalizeGalleryImages(images: any[]): GalleryImage[] {
  if (!images || images.length === 0) {
    return [];
  }
  
  console.log(`Processing ${images.length} gallery images...`);
  if (images.length > 0) {
    console.log("First raw image before normalization:", JSON.stringify(images[0], null, 2));
  }
  
  // Map images to normalize field names from Apps Script format
  // Apps Script returns dynamic headers from sheet (Title, URL, UploadedBy, UploadedDate, Description, Delete, etc.)
  // GalleryImage expects: id, imageUrl, createdAt
  const normalized = images.map((img: any) => {
      // Filter out deleted images (check all possible delete column names/positions)
      const deleteValue = img.Delete || img.delete || img["Delete"] || (img[6] !== undefined ? img[6] : null);
      if (deleteValue && deleteValue.toString().toLowerCase() === "deleted") {
        return null; // Will filter these out
      }
      
      // Get image URL from various possible field names
      // Gallery API already returns imageUrl field, but check all variations
      let imageUrl = img.imageUrl || img.URL || img.url || img["URL"] || img.image || "";
      
      // Skip if no valid image URL
      if (!imageUrl || imageUrl.trim() === "") {
        console.warn("Skipping image with no URL:", img);
        return null;
      }
      
      // Fix Google Drive URL format if needed
      // The API sometimes returns https://drive.google.com/uc?id=FILE_ID
      // But we need https://drive.google.com/uc?export=view&id=FILE_ID for proper image display
      if (imageUrl.includes("drive.google.com/uc")) {
        // Extract file ID from various URL formats
        let fileId: string | null = null;
        
        // Pattern 1: /uc?id=FILE_ID
        const match1 = imageUrl.match(/\/uc\?id=([-\w]{25,})/);
        if (match1) fileId = match1[1];
        
        // Pattern 2: /uc?export=view&id=FILE_ID (already correct)
        if (!fileId) {
          const match2 = imageUrl.match(/\/uc\?export=view&id=([-\w]{25,})/);
          if (match2) fileId = match2[1];
        }
        
        // Pattern 3: /file/d/FILE_ID/ (full Drive URL)
        if (!fileId) {
          const match3 = imageUrl.match(/\/file\/d\/([-\w]{25,})/);
          if (match3) fileId = match3[1];
        }
        
        // If we found a file ID, convert to proper image URL format
        // Try Google Image CDN first (fastest, no redirects)
        // Fallback to Drive thumbnail API if CDN doesn't work
        if (fileId) {
          // Use Google Image CDN - correct format: https://lh3.googleusercontent.com/d/FILE_ID
          // This is the recommended format for Drive images (without =w1000)
          imageUrl = `https://lh3.googleusercontent.com/d/${fileId}`;
        }
      }
      
      // Get date from various possible field names (Gallery API uses uploadedDate)
      const dateValue = img.uploadedDate || img.UploadedDate || img["Uploaded Date"] || img.createdAt || img["UploadedDate"] || img.date;
      let createdAt: Timestamp | undefined = undefined;
      
      if (dateValue) {
        try {
          if (dateValue instanceof Date) {
            createdAt = Timestamp.fromDate(dateValue);
          } else if (typeof dateValue === 'string') {
            // Try parsing the date string
            const parsedDate = new Date(dateValue);
            if (!isNaN(parsedDate.getTime())) {
              createdAt = Timestamp.fromDate(parsedDate);
            }
          } else if (dateValue.toDate) {
            // Already a Firestore Timestamp
            createdAt = dateValue;
          }
        } catch (e) {
          console.warn("Could not parse date:", dateValue, e);
        }
      }
      
      // Normalize field names - support both admin panel and mobile app formats
      // Gallery API returns dynamic headers from sheet (Title, URL, UploadedBy, UploadedDate, Description, Delete)
      const title = img.title || img.Title || img["Title"] || img["title"] || "";
      const normalized: GalleryImage = {
        // Ensure imageUrl is set (check all possible field names)
        imageUrl: imageUrl,
        // Preserve title if available
        title: title,
        // Use title as ID if no id exists, or generate one
        id: img.id || title || `img-${Date.now()}-${Math.random()}`,
        createdAt: createdAt,
      };
      
      // Log for debugging
      if (!normalized.imageUrl) {
        console.warn("Normalized image missing imageUrl:", normalized);
      }
      
      return normalized;
    }).filter((img): img is GalleryImage => !!(img && img.imageUrl && img.imageUrl.trim() !== "")); // Remove null entries and images without URLs
    
    // Sort by date (newest first)
    normalized.sort((a, b) => {
      const aDate = a.createdAt?.toDate ? a.createdAt.toDate().getTime() : 0;
      const bDate = b.createdAt?.toDate ? b.createdAt.toDate().getTime() : 0;
      return bDate - aDate;
    });
    
    console.log(`Returning ${normalized.length} normalized gallery images`);
    return normalized;
}

export const createGalleryImage = async (
  data: Omit<GalleryImage, "id">
): Promise<string> => {
  return createDoc<GalleryImage>("gallery", data);
};

// Delete gallery image via Apps Script API
export const deleteGalleryImage = async (title: string, userEmail: string): Promise<void> => {
  try {
    const response = await fetch("/api/gallery/delete", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        title: title,
        userEmail: userEmail,
      }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ error: "Unknown error" }));
      throw new Error(errorData.error || `Failed to delete image: ${response.status}`);
    }

    const data = await response.json();
    if (!data.success) {
      throw new Error(data.error || "Failed to delete image");
    }
  } catch (error: any) {
    console.error("Error deleting gallery image:", error);
    throw new Error(`Failed to delete gallery image: ${error?.message || "Unknown error"}`);
  }
};

// Useful Links
export const getUsefulLinks = async (): Promise<UsefulLink[]> => {
  return getDocuments<UsefulLink>("useful_links", [orderBy("name")]);
};

export const createUsefulLink = async (
  data: Omit<UsefulLink, "id">
): Promise<string> => {
  return createDoc<UsefulLink>("useful_links", data);
};

// Statistics
export const getEmployeeStats = async () => {
  const employees = await getEmployees();
  const districts = await getDistricts();
  const stations = await getStations();

  const byDistrict: Record<string, number> = {};
  const byStation: Record<string, number> = {};
  const byRank: Record<string, number> = {};

  employees.forEach((emp) => {
    if (emp.district) {
      byDistrict[emp.district] = (byDistrict[emp.district] || 0) + 1;
    }
    if (emp.station) {
      byStation[emp.station] = (byStation[emp.station] || 0) + 1;
    }
    if (emp.rank) {
      byRank[emp.rank] = (byRank[emp.rank] || 0) + 1;
    }
  });

  return {
    total: employees.length,
    approved: employees.filter((e) => e.isApproved).length,
    pending: employees.filter((e) => !e.isApproved).length,
    byDistrict,
    byStation,
    byRank,
    districtsCount: districts.length,
    stationsCount: stations.length,
  };
};

