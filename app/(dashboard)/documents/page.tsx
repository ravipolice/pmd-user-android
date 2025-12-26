"use client";

import { useEffect, useState } from "react";
import { createDocument } from "@/lib/firebase/firestore";
import { fetchDocuments, deleteDocument, getPreviewUrl, getDownloadUrl, DocumentItem } from "@/lib/services/documents.service";
import { useAuth } from "@/components/providers/AuthProvider";
import { Plus, FileText, ExternalLink, Eye, Download, Trash2, Upload, Link as LinkIcon } from "lucide-react";
import { formatDateTime } from "@/lib/utils";

type TabType = "url" | "upload";
type UploadTarget = "gdrive" | "firebase";

export default function DocumentsPage() {
  const { user } = useAuth();
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<TabType>("url");
  const [uploadTarget, setUploadTarget] = useState<UploadTarget>("gdrive");
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({ name: "", url: "", type: "", category: "", description: "" });
  const [uploadFileState, setUploadFileState] = useState<File | null>(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [deleting, setDeleting] = useState<string | null>(null);

  useEffect(() => {
    loadDocuments();
  }, []);

  const loadDocuments = async () => {
    try {
      console.log("Loading documents...");
      // ✅ Use the service - normalization happens here
      const data = await fetchDocuments();
      console.log("Loaded documents:", data);
      console.log("Number of documents:", data.length);
      if (data.length > 0) {
        console.log("First document sample:", JSON.stringify(data[0], null, 2));
      }
      setDocuments(data);
      if (data.length === 0) {
        console.warn("No documents found. Make sure documents exist in the Google Sheet.");
        console.warn("Check browser console for detailed error messages.");
      }
    } catch (error: any) {
      console.error("Error loading documents:", error);
      console.error("Error details:", {
        message: error?.message,
        code: error?.code,
        stack: error?.stack
      });
      alert(`Failed to load documents: ${error?.message || "Unknown error"}. Please check the console for details.`);
    } finally {
      setLoading(false);
    }
  };

  const handleUrlSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);

    try {
      // For URL-based documents, we can either:
      // 1. Store in Firestore (for admin panel display)
      // 2. Or create a placeholder entry in Google Sheet
      // Since the app reads from Google Sheet, we'll store in Firestore for admin panel
      // and optionally sync to sheet if needed
      
      await createDocument({
        name: formData.name.trim(),
        title: formData.name.trim(),
        url: formData.url.trim(),
        URL: formData.url.trim(),
        type: formData.type.trim() || undefined,
        fileType: formData.type.trim() || undefined,
        category: formData.category.trim() || undefined,
        description: formData.description.trim() || undefined,
        uploadedBy: user?.email || undefined,
        uploadedDate: new Date().toISOString(),
      });
      setFormData({ name: "", url: "", type: "", category: "", description: "" });
      setShowForm(false);
      await loadDocuments();
    } catch (error) {
      console.error("Error creating document:", error);
      alert("Failed to create document");
    } finally {
      setSubmitting(false);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setUploadFileState(file);
      // Auto-fill name if empty
      if (!formData.name) {
        setFormData({ ...formData, name: file.name.replace(/\.[^/.]+$/, "") });
      }
      // Auto-detect file type
      const extension = file.name.split('.').pop()?.toUpperCase();
      if (extension && !formData.type) {
        setFormData({ ...formData, type: extension });
      }
    }
  };

  const handleUploadSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadFileState) {
      alert("Please select a file to upload");
      return;
    }

    setSubmitting(true);
    setUploadProgress(0);

    try {
      const safeName = (formData.name || uploadFileState.name)
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, "_")
        .replace(/^_+|_+$/g, "") || "document";
      const extension = uploadFileState.name.split(".").pop() || "bin";
      const mimeType = uploadFileState.type || getMimeTypeFromExtension(uploadFileState.name);

      // Simulate upload progress up to 90% while work is in flight
      const progressInterval = setInterval(() => {
        setUploadProgress((prev) => {
          if (prev >= 90) {
            clearInterval(progressInterval);
            return 90;
          }
          return prev + 10;
        });
      }, 200);

      if (uploadTarget === "firebase") {
        // Upload to Firebase via server-side API (avoids CORS issues)
        const base64 = await fileToBase64(uploadFileState);

        const response = await fetch("/api/documents/upload-firebase", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            title: formData.name.trim() || uploadFileState.name.replace(/\.[^/.]+$/, ""),
            fileBase64: base64,
            mimeType: mimeType,
            category: formData.category.trim() || undefined,
            description: formData.description.trim() || undefined,
            userEmail: user?.email || "admin@pmd.com",
          }),
        });

        clearInterval(progressInterval);
        setUploadProgress(100);

        if (!response.ok) {
          const errorData = await response.json().catch(() => ({ error: "Unknown error" }));
          throw new Error(errorData.error || `HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        
        if (!result.success) {
          throw new Error(result.error || "Upload failed");
        }

        console.log("✅ Firebase upload successful:", result);
        console.log("✅ Uploaded document URL:", result.url);
        console.log("✅ Uploaded document fileId:", result.fileId);

        // 🔥 IMPORTANT: Register the document in Google Sheet via Apps Script
        // This makes Google Sheet the single master index for all documents
        try {
          console.log("📝 Registering document in Google Sheet...");
          const registerResponse = await fetch("/api/documents/upload", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
            },
            body: JSON.stringify({
              action: "upload",
              title: formData.name.trim() || uploadFileState.name.replace(/\.[^/.]+$/, ""),
              category: formData.category.trim() || "",
              description: formData.description.trim() || "",
              externalUrl: result.url, // 🔥 Firebase Storage URL - key to registration
              userEmail: user?.email || "admin@pmd.com",
            }),
          });

          if (!registerResponse.ok) {
            const errorData = await registerResponse.json().catch(() => ({ error: "Unknown error" }));
            console.warn("⚠️ Failed to register in Google Sheet:", errorData.error);
            // Don't throw - file is already uploaded to Firebase
          } else {
            const registerResult = await registerResponse.json();
            if (registerResult.success) {
              console.log("✅ Document registered in Google Sheet");
            } else {
              console.warn("⚠️ Google Sheet registration returned success=false:", registerResult.error);
            }
          }
        } catch (registerError: any) {
          console.warn("⚠️ Error registering in Google Sheet (non-critical):", registerError.message);
          // Don't throw - file is already uploaded to Firebase
        }

        // Clear form
        setFormData({ name: "", url: "", type: "", category: "", description: "" });
        setUploadFileState(null);
        setUploadProgress(0);
        setShowForm(false);
        
        // Reload documents to show the newly uploaded one
        await loadDocuments();
      } else {
        const base64 = await fileToBase64(uploadFileState);
        const response = await fetch("/api/documents/upload", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            action: "upload",
            title: formData.name.trim(),
            fileBase64: base64,
            mimeType: mimeType,
            category: formData.category.trim() || undefined,
            description: formData.description.trim() || undefined,
            userEmail: user?.email || "admin@pmd.com",
          }),
        });

        clearInterval(progressInterval);
        setUploadProgress(100);

        if (!response.ok) {
          const errorData = await response.json().catch(() => ({ error: "Unknown error" }));
          throw new Error(errorData.error || `HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        if (!result.success) {
          throw new Error(result.error || "Upload failed");
        }
      }

      setFormData({ name: "", url: "", type: "", category: "", description: "" });
      setUploadFileState(null);
      setUploadProgress(0);
      setShowForm(false);
      await loadDocuments();
    } catch (error: any) {
      console.error("Error uploading document:", error);
      alert(`Failed to upload document: ${error?.message || "Unknown error"}`);
    } finally {
      setSubmitting(false);
      setUploadProgress(0);
    }
  };

  const fileToBase64 = (file: File): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        const result = reader.result as string;
        // Remove data URI prefix if present
        const base64 = result.includes(",") ? result.split(",")[1] : result;
        resolve(base64);
      };
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  };

  const getMimeTypeFromExtension = (filename: string): string => {
    const extension = filename.split('.').pop()?.toLowerCase();
    const mimeTypes: Record<string, string> = {
      pdf: "application/pdf",
      doc: "application/msword",
      docx: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      xls: "application/vnd.ms-excel",
      xlsx: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      ppt: "application/vnd.ms-powerpoint",
      pptx: "application/vnd.openxmlformats-officedocument.presentationml.presentation",
      jpg: "image/jpeg",
      jpeg: "image/jpeg",
      png: "image/png",
      gif: "image/gif",
      txt: "text/plain",
    };
    return mimeTypes[extension || ""] || "application/octet-stream";
  };

  const handleDelete = async (doc: DocumentItem) => {
    if (!user?.email) {
      alert("You must be logged in to delete documents");
      return;
    }

    if (!confirm(`Are you sure you want to delete "${doc.title}"?`)) {
      return;
    }

    setDeleting(doc.title);
    try {
      // Pass fileId if available (required for Apps Script delete)
      await deleteDocument(doc.title, user.email, doc.fileId);
      await loadDocuments(); // Reload documents after deletion
    } catch (error: any) {
      console.error("Error deleting document:", error);
      alert(`Failed to delete document: ${error?.message || "Unknown error"}`);
    } finally {
      setDeleting(null);
    }
  };

  const handlePreview = (doc: DocumentItem) => {
    const previewUrl = getPreviewUrl(doc.url);
    window.open(previewUrl, "_blank", "noopener,noreferrer");
  };

  const handleDownload = (doc: DocumentItem) => {
    const downloadUrl = getDownloadUrl(doc.url);
    // Create a temporary anchor element to trigger download
    const link = document.createElement("a");
    link.href = downloadUrl;
    link.download = doc.title || "document";
    link.target = "_blank";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-lg text-gray-600">Loading...</div>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-3xl font-bold text-gray-900">Documents</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          className="flex items-center gap-2 rounded-lg bg-primary-600 px-4 py-2 text-white transition-colors hover:bg-primary-700"
        >
          <Plus className="h-5 w-5" />
          Add Document
        </button>
      </div>

      {showForm && (
        <div className="mb-6 rounded-lg bg-white p-6 shadow-md">
          <h2 className="mb-4 text-xl font-semibold">Add New Document</h2>
          
          {/* Tabs */}
          <div className="mb-6 border-b border-gray-200">
            <div className="flex gap-4">
              <button
                type="button"
                onClick={() => {
                  setActiveTab("url");
      setUploadFileState(null);
                  setFormData({ name: "", url: "", type: "", category: "", description: "" });
                }}
                className={`flex items-center gap-2 px-4 py-2 font-medium transition-colors ${
                  activeTab === "url"
                    ? "border-b-2 border-primary-600 text-primary-600"
                    : "text-gray-500 hover:text-gray-700"
                }`}
              >
                <LinkIcon className="h-4 w-4" />
                Add by URL
              </button>
              <button
                type="button"
                onClick={() => {
                  setActiveTab("upload");
                  setFormData({ name: "", url: "", type: "", category: "", description: "" });
                }}
                className={`flex items-center gap-2 px-4 py-2 font-medium transition-colors ${
                  activeTab === "upload"
                    ? "border-b-2 border-primary-600 text-primary-600"
                    : "text-gray-500 hover:text-gray-700"
                }`}
              >
                <Upload className="h-4 w-4" />
                Upload File
              </button>
            </div>
          </div>

          {/* URL Tab Form */}
          {activeTab === "url" && (
            <form onSubmit={handleUrlSubmit}>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div>
                  <label className="block text-sm font-medium text-gray-700">
                    Name *
                  </label>
                  <input
                    type="text"
                    required
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">
                    URL *
                  </label>
                  <input
                    type="url"
                    required
                    value={formData.url}
                    onChange={(e) => setFormData({ ...formData, url: e.target.value })}
                    className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">
                    Type
                  </label>
                  <input
                    type="text"
                    value={formData.type}
                    onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                    placeholder="e.g., PDF, DOCX"
                    className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">
                    Category
                  </label>
                  <input
                    type="text"
                    value={formData.category}
                    onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                    placeholder="Optional category"
                    className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-gray-700">
                    Description
                  </label>
                  <textarea
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    placeholder="Optional description"
                    rows={3}
                    className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
              </div>
              <div className="mt-4 flex gap-4">
                <button
                  type="submit"
                  disabled={submitting}
                  className="rounded-lg bg-primary-600 px-6 py-2 text-white transition-colors hover:bg-primary-700 disabled:opacity-50"
                >
                  {submitting ? "Saving..." : "Save Document"}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setShowForm(false);
                    setFormData({ name: "", url: "", type: "", category: "", description: "" });
                  }}
                  className="rounded-lg border border-gray-300 px-6 py-2 text-gray-700 transition-colors hover:bg-gray-50"
                >
                  Cancel
                </button>
              </div>
            </form>
          )}

          {/* Upload Tab Form */}
          {activeTab === "upload" && (
            <form onSubmit={handleUploadSubmit}>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700">
                  Upload To
                </label>
                <div className="mt-2 flex gap-4 text-sm">
                  <label className="inline-flex items-center gap-2 text-gray-800">
                    <input
                      type="radio"
                      name="upload-target"
                      value="gdrive"
                      checked={uploadTarget === "gdrive"}
                      onChange={() => setUploadTarget("gdrive")}
                    />
                    <span>Google Drive (Apps Script)</span>
                  </label>
                  <label className="inline-flex items-center gap-2">
                    <input
                      type="radio"
                      name="upload-target"
                      value="firebase"
                      checked={uploadTarget === "firebase"}
                      onChange={() => setUploadTarget("firebase")}
                    />
                    <span className="text-gray-800">Firebase Storage</span>
                  </label>
                </div>
                <p className="mt-1 text-xs text-gray-600">
                  Drive (Apps Script) keeps documents in the existing sheet/Drive path. Firebase stores the file in Storage and metadata in Firestore.
                </p>
              </div>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div>
                  <label className="block text-sm font-medium text-gray-700">
                    Name *
                  </label>
                  <input
                    type="text"
                    required
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">
                    File *
                  </label>
              <input
                type="file"
                required
                onChange={handleFileSelect}
                className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm file:mr-4 file:rounded-md file:border-0 file:bg-primary-50 file:px-4 file:py-2 file:text-primary-700 hover:file:bg-primary-100 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
              {uploadFileState && (
                <p className="mt-1 text-sm text-gray-500">
                  Selected: {uploadFileState.name} ({(uploadFileState.size / 1024 / 1024).toFixed(2)} MB)
                </p>
              )}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">
                    Type
                  </label>
                  <input
                    type="text"
                    value={formData.type}
                    onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                    placeholder="Auto-detected from file"
                    className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">
                    Category
                  </label>
                  <input
                    type="text"
                    value={formData.category}
                    onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                    placeholder="Optional category"
                    className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-gray-700">
                    Description
                  </label>
                  <textarea
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    placeholder="Optional description"
                    rows={3}
                    className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
              </div>
              {uploadProgress > 0 && uploadProgress < 100 && (
                <div className="mt-4">
                  <div className="mb-1 flex justify-between text-sm text-gray-600">
                    <span>Uploading...</span>
                    <span>{uploadProgress}%</span>
                  </div>
                  <div className="h-2 w-full overflow-hidden rounded-full bg-gray-200">
                    <div
                      className="h-full bg-primary-600 transition-all duration-300"
                      style={{ width: `${uploadProgress}%` }}
                    />
                  </div>
                </div>
              )}
              <div className="mt-4 flex gap-4">
                <button
                  type="submit"
                disabled={submitting || !uploadFileState}
                  className="rounded-lg bg-primary-600 px-6 py-2 text-white transition-colors hover:bg-primary-700 disabled:opacity-50"
                >
                  {submitting ? "Uploading..." : "Upload Document"}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setShowForm(false);
                    setFormData({ name: "", url: "", type: "", category: "", description: "" });
                    setUploadFileState(null);
                    setUploadProgress(0);
                  }}
                  className="rounded-lg border border-gray-300 px-6 py-2 text-gray-700 transition-colors hover:bg-gray-50"
                >
                  Cancel
                </button>
              </div>
            </form>
          )}
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
        {documents.map((doc, idx) => (
          <div
            key={doc.id || doc.fileId || idx}
            className="rounded-lg bg-white p-6 shadow-md"
          >
            <div className="mb-4 flex items-start justify-between">
              <div className="flex items-center gap-3">
                <FileText className="h-8 w-8 text-primary-600" />
                <div className="flex-1">
                  {/* ✅ Use normalized fields directly */}
                  <div className="flex items-center gap-2 flex-wrap">
                    <h3 className="font-semibold text-gray-900">{doc.title}</h3>
                    {/* Source indicator badge */}
                    {(() => {
                      const url = doc.url || "";
                      const isFirebase = url.includes("storage.googleapis.com") || url.includes("firebasestorage.app");
                      const isGoogleDrive = url.includes("drive.google.com");
                      
                      if (isFirebase) {
                        return (
                          <span className="inline-flex items-center rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-800" title="Stored in Firebase Storage">
                            🔥 Firebase
                          </span>
                        );
                      } else if (isGoogleDrive) {
                        return (
                          <span className="inline-flex items-center rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-800" title="Stored in Google Drive">
                            📁 Google Drive
                          </span>
                        );
                      }
                      return (
                        <span className="inline-flex items-center rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-800" title="Unknown source">
                          ❓ Unknown
                        </span>
                      );
                    })()}
                  </div>
                  {doc.category && (
                    <p className="text-sm text-gray-500">{doc.category}</p>
                  )}
                  {doc.description && (
                    <p className="mt-1 text-sm text-gray-400">{doc.description}</p>
                  )}
                </div>
              </div>
            </div>
            {doc.uploadedDate && (
              <p className="mb-2 text-xs text-gray-500">
                Uploaded on: {doc.uploadedDate ? formatDateTime(new Date(doc.uploadedDate)) : "Unknown date"}
              </p>
            )}
            {doc.uploadedBy && (
              <p className="mb-4 text-xs text-gray-500">
                Uploaded by: {doc.uploadedBy}
              </p>
            )}
            <div className="flex flex-wrap gap-2">
              <button
                onClick={() => handlePreview(doc)}
                className="flex items-center gap-2 rounded-md bg-blue-50 px-3 py-2 text-sm text-blue-700 transition-colors hover:bg-blue-100"
                title="Preview document"
              >
                <Eye className="h-4 w-4" />
                Preview
              </button>
              <button
                onClick={() => handleDownload(doc)}
                className="flex items-center gap-2 rounded-md bg-green-50 px-3 py-2 text-sm text-green-700 transition-colors hover:bg-green-100"
                title="Download document"
              >
                <Download className="h-4 w-4" />
                Download
              </button>
              <button
                onClick={() => handleDelete(doc)}
                disabled={deleting === doc.title}
                className="flex items-center gap-2 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700 transition-colors hover:bg-red-100 disabled:opacity-50"
                title="Delete document"
              >
                <Trash2 className="h-4 w-4" />
                {deleting === doc.title ? "Deleting..." : "Delete"}
              </button>
              <a
                href={doc.url}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-2 rounded-md bg-gray-50 px-3 py-2 text-sm text-gray-700 transition-colors hover:bg-gray-100"
                title="Open in new tab"
              >
                <ExternalLink className="h-4 w-4" />
                Open
              </a>
            </div>
          </div>
        ))}
      </div>
      {documents.length === 0 && (
        <div className="rounded-lg bg-white p-12 text-center shadow-md">
          <p className="text-lg text-gray-500">No documents found</p>
        </div>
      )}
    </div>
  );
}

