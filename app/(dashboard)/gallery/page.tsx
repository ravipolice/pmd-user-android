"use client";

import { useEffect, useState } from "react";
import { getGalleryImages, createGalleryImage, deleteGalleryImage, GalleryImage } from "@/lib/firebase/firestore";
import { Plus, Image as ImageIcon, Upload, Link as LinkIcon, Trash2 } from "lucide-react";
import { useAuth } from "@/components/providers/AuthProvider";

type UploadMethod = "url" | "file";
type UploadTarget = "gdrive" | "firebase";

export default function GalleryPage() {
  const { user } = useAuth();
  const [images, setImages] = useState<GalleryImage[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [uploadMethod, setUploadMethod] = useState<UploadMethod>("file");
  const [uploadTarget, setUploadTarget] = useState<UploadTarget>("gdrive");
  const [formData, setFormData] = useState({ 
    imageUrl: "", 
    title: "", 
    description: "",
    category: "Gallery"
  });
  const [uploadFileState, setUploadFileState] = useState<File | null>(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [deleting, setDeleting] = useState<string | null>(null);

  useEffect(() => {
    loadImages();
  }, []);

  const loadImages = async () => {
    try {
      console.log("Loading gallery images...");
      const data = await getGalleryImages();
      console.log("Loaded gallery images:", data);
      console.log("Number of images:", data.length);
      if (data.length > 0) {
        console.log("First image:", data[0]);
        console.log("First image URL:", data[0].imageUrl);
      }
      setImages(data);
      if (data.length === 0) {
        console.warn("No gallery images found. Check Apps Script API response.");
      }
    } catch (error) {
      console.error("Error loading images:", error);
      alert("Failed to load gallery images. Please check the console for details.");
    } finally {
      setLoading(false);
    }
  };

  // Determine image source (Firebase Storage or Google Drive)
  const getImageSource = (image: GalleryImage): "firebase" | "gdrive" => {
    const url = image.imageUrl || "";
    
    // Check if it's Firebase Storage
    if (url.includes("storage.googleapis.com") || 
        url.includes("firebasestorage.app") ||
        (image as any).storagePath) {
      return "firebase";
    }
    
    // Check if it's Google Drive
    if (url.includes("drive.google.com") || 
        url.includes("lh3.googleusercontent.com") ||
        url.includes("googleusercontent.com")) {
      return "gdrive";
    }
    
    // Default to gdrive if URL format is unclear (for backward compatibility)
    return "gdrive";
  };

  // Convert file to base64
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

  // Get MIME type from file extension
  const getMimeTypeFromExtension = (filename: string): string => {
    const extension = filename.split('.').pop()?.toLowerCase();
    const mimeTypes: Record<string, string> = {
      jpg: "image/jpeg",
      jpeg: "image/jpeg",
      png: "image/png",
      gif: "image/gif",
      webp: "image/webp",
      bmp: "image/bmp",
    };
    return mimeTypes[extension || ""] || "image/jpeg";
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      // Validate image file
      if (!file.type.startsWith("image/")) {
        alert("Please select an image file");
        return;
      }
      setUploadFileState(file);
      // Auto-fill title if empty
      if (!formData.title) {
        const nameWithoutExt = file.name.replace(/\.[^/.]+$/, "");
        setFormData({ ...formData, title: nameWithoutExt });
      }
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);

    try {
      if (uploadMethod === "file") {
        if (!uploadFileState) {
          alert("Please select an image file to upload");
          return;
        }

        setUploadProgress(0);

        const safeName = (formData.title || uploadFileState.name)
          .toLowerCase()
          .replace(/[^a-z0-9]+/g, "_")
          .replace(/^_+|_+$/g, "") || "image";
        const extension = uploadFileState.name.split(".").pop() || "jpg";

        // Simulate upload progress to 90% while work happens
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
          const mimeType = uploadFileState.type || getMimeTypeFromExtension(uploadFileState.name);

          const response = await fetch("/api/gallery/upload-firebase", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
            },
            body: JSON.stringify({
              title: formData.title.trim() || uploadFileState.name.replace(/\.[^/.]+$/, ""),
              fileBase64: base64,
              mimeType: mimeType,
              category: formData.category.trim() || "Gallery",
              description: formData.description.trim() || "",
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

          // Reload images to show the newly uploaded one
          await loadImages();
        } else {
          // Upload to Google Drive via Apps Script API
          const base64 = await fileToBase64(uploadFileState);
          const mimeType = uploadFileState.type || getMimeTypeFromExtension(uploadFileState.name);

          const response = await fetch("/api/gallery/upload", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
            },
            body: JSON.stringify({
              title: formData.title.trim() || uploadFileState.name.replace(/\.[^/.]+$/, ""),
              fileBase64: base64,
              mimeType: mimeType,
              category: formData.category.trim() || "Gallery",
              description: formData.description.trim() || "",
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

        setFormData({ imageUrl: "", title: "", description: "", category: "Gallery" });
        setUploadFileState(null);
        setUploadProgress(0);
        setShowForm(false);
        await loadImages();
      } else {
        // URL upload (existing functionality)
        await createGalleryImage({
          imageUrl: formData.imageUrl.trim(),
        });
        setFormData({ imageUrl: "", title: "", description: "", category: "Gallery" });
        setShowForm(false);
        await loadImages();
      }
    } catch (error: any) {
      console.error("Error uploading image:", error);
      alert(`Failed to upload image: ${error?.message || "Unknown error"}`);
    } finally {
      setSubmitting(false);
      setUploadProgress(0);
    }
  };

  const handleDelete = async (image: GalleryImage) => {
    if (!user?.email) {
      alert("You must be logged in to delete images");
      return;
    }

    const imageTitle = image.title || image.id || "this image";
    if (!confirm(`Are you sure you want to delete "${imageTitle}"?`)) {
      return;
    }

    setDeleting(image.id || imageTitle);
    try {
      // Use title for deletion (Apps Script uses title as identifier)
      await deleteGalleryImage(imageTitle, user.email);
      await loadImages(); // Reload images after deletion
    } catch (error: any) {
      console.error("Error deleting image:", error);
      alert(`Failed to delete image: ${error?.message || "Unknown error"}`);
    } finally {
      setDeleting(null);
    }
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
        <h1 className="text-3xl font-bold text-gray-900">Gallery</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          className="flex items-center gap-2 rounded-lg bg-primary-600 px-4 py-2 text-white transition-colors hover:bg-primary-700"
        >
          <Plus className="h-5 w-5" />
          Add Image
        </button>
      </div>

      {showForm && (
        <div className="mb-6 rounded-lg bg-white p-6 shadow-md">
          <h2 className="mb-4 text-xl font-semibold">Add New Image</h2>
          
          {/* Upload Method Tabs */}
          <div className="mb-4 flex gap-2 border-b border-gray-200">
            <button
              type="button"
              onClick={() => {
                setUploadMethod("file");
                  setUploadFileState(null);
                setFormData({ ...formData, imageUrl: "" });
              }}
              className={`flex items-center gap-2 px-4 py-2 font-medium transition-colors ${
                uploadMethod === "file"
                  ? "border-b-2 border-primary-600 text-primary-600"
                  : "text-gray-500 hover:text-gray-700"
              }`}
            >
              <Upload className="h-4 w-4" />
              Upload File
            </button>
            <button
              type="button"
              onClick={() => {
                setUploadMethod("url");
                setUploadFileState(null);
              }}
              className={`flex items-center gap-2 px-4 py-2 font-medium transition-colors ${
                uploadMethod === "url"
                  ? "border-b-2 border-primary-600 text-primary-600"
                  : "text-gray-500 hover:text-gray-700"
              }`}
            >
              <LinkIcon className="h-4 w-4" />
              Image URL
            </button>
          </div>

          <form onSubmit={handleSubmit}>
            {uploadMethod === "file" ? (
              <>
                <div className="mb-4">
                  <label className="block text-sm font-medium text-gray-800 mb-2">
                    Upload To
                  </label>
                  <div className="flex gap-4 text-sm">
                    <label className="inline-flex items-center gap-2 text-gray-800">
                      <input
                        type="radio"
                        name="gallery-upload-target"
                        value="gdrive"
                        checked={uploadTarget === "gdrive"}
                        onChange={() => setUploadTarget("gdrive")}
                      />
                      <span>Google Drive (Apps Script)</span>
                    </label>
                    <label className="inline-flex items-center gap-2 text-gray-800">
                      <input
                        type="radio"
                        name="gallery-upload-target"
                        value="firebase"
                        checked={uploadTarget === "firebase"}
                        onChange={() => setUploadTarget("firebase")}
                      />
                      <span>Firebase Storage</span>
                    </label>
                  </div>
                  <p className="mt-1 text-xs text-gray-600">
                    Drive (Apps Script) keeps images in the existing sheet/Drive path. Firebase stores the file in Storage and metadata in Firestore.
                  </p>
                </div>
                <div className="mb-4">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Select Image File *
                  </label>
                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleFileSelect}
                    className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:bg-primary-50 file:text-primary-700 hover:file:bg-primary-100"
                  />
                  {uploadFileState && (
                    <p className="mt-2 text-sm text-gray-600">
                      Selected: {uploadFileState.name} ({(uploadFileState.size / 1024 / 1024).toFixed(2)} MB)
                    </p>
                  )}
                  {uploadProgress > 0 && uploadProgress < 100 && (
                    <div className="mt-2">
                      <div className="h-2 w-full bg-gray-200 rounded-full overflow-hidden">
                        <div
                          className="h-full bg-primary-600 transition-all duration-300"
                          style={{ width: `${uploadProgress}%` }}
                        />
                      </div>
                      <p className="mt-1 text-xs text-gray-500">{uploadProgress}% uploaded</p>
                    </div>
                  )}
                </div>
                <div className="mb-4">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Title
                  </label>
                  <input
                    type="text"
                    value={formData.title}
                    onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                    placeholder="Image title (optional)"
                    className="block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
                <div className="mb-4">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Description
                  </label>
                  <textarea
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    placeholder="Image description (optional)"
                    rows={3}
                    className="block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
              </>
            ) : (
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Image URL *
                </label>
                <input
                  type="url"
                  required
                  value={formData.imageUrl}
                  onChange={(e) => setFormData({ ...formData, imageUrl: e.target.value })}
                  placeholder="https://example.com/image.jpg"
                  className="block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
              </div>
            )}
            
            <div className="mt-4 flex gap-4">
              <button
                type="submit"
                disabled={submitting || (uploadMethod === "file" && !uploadFileState)}
                className="rounded-lg bg-primary-600 px-6 py-2 text-white transition-colors hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {submitting ? (uploadMethod === "file" ? "Uploading..." : "Saving...") : "Add Image"}
              </button>
              <button
                type="button"
                onClick={() => {
                  setShowForm(false);
                  setFormData({ imageUrl: "", title: "", description: "", category: "Gallery" });
                  setUploadFileState(null);
                  setUploadProgress(0);
                }}
                className="rounded-lg border border-gray-300 px-6 py-2 text-gray-700 transition-colors hover:bg-gray-50"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
        {images.map((image) => (
          <div
            key={image.id}
            className="group relative overflow-hidden rounded-lg bg-white shadow-md transition-shadow hover:shadow-lg"
          >
            <div className="aspect-square w-full overflow-hidden bg-gray-100 relative">
              <img
                src={image.imageUrl || ""}
                alt={image.title || image.id || "Gallery image"}
                className="h-full w-full object-cover transition-transform group-hover:scale-105"
                loading="lazy"
                referrerPolicy="no-referrer"
                onError={(e) => {
                  const img = e.currentTarget;
                  // Prevent infinite error loop - only show placeholder once
                  if (!img.dataset.failed) {
                    img.dataset.failed = "1";
                    // Extract file ID from current URL (CDN or Drive format)
                    let fileId: string | null = null;
                    
                    // Try to extract from CDN URL: lh3.googleusercontent.com/d/FILE_ID
                    if (image.imageUrl?.includes("lh3.googleusercontent.com")) {
                      const cdnMatch = image.imageUrl.match(/\/d\/([-\w]{25,})/);
                      if (cdnMatch) fileId = cdnMatch[1];
                    }
                    
                    // Try to extract from Drive URL: drive.google.com/uc?id=FILE_ID
                    if (!fileId && image.imageUrl?.includes("drive.google.com")) {
                      const driveMatch = image.imageUrl.match(/[?&]id=([-\w]{25,})/);
                      if (driveMatch) fileId = driveMatch[1];
                    }
                    
                    // Try fallback URLs if we have a file ID
                    if (fileId) {
                      if (!img.dataset.retried) {
                        // First retry: Try Drive thumbnail API
                        img.dataset.retried = "1";
                        img.src = `https://drive.google.com/thumbnail?id=${fileId}&sz=w1000`;
                        return;
                      } else if (!img.dataset.retried2) {
                        // Second retry: Try standard Drive view URL
                        img.dataset.retried2 = "1";
                        img.src = `https://drive.google.com/uc?export=view&id=${fileId}`;
                        return;
                      }
                    }
                    
                    // Final fallback: placeholder
                    img.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='300' height='300'%3E%3Crect fill='%23ddd' width='300' height='300'/%3E%3Ctext fill='%23999' font-family='sans-serif' font-size='20' dy='10.5' font-weight='bold' x='50%25' y='50%25' text-anchor='middle'%3EImage%3C/text%3E%3C/svg%3E";
                  }
                }}
                onLoad={() => {
                  // Only log in development to avoid console spam
                  if (process.env.NODE_ENV === 'development') {
                    console.log("✅ Image loaded:", image.title || image.id);
                  }
                }}
              />
              {/* Source badge - always visible */}
              {(() => {
                const source = getImageSource(image);
                return (
                  <div className="absolute top-2 left-2">
                    <span
                      className={`inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-semibold shadow-lg ${
                        source === "firebase"
                          ? "bg-blue-600 text-white"
                          : "bg-green-600 text-white"
                      }`}
                      title={source === "firebase" ? "Stored in Firebase Storage" : "Stored in Google Drive"}
                    >
                      {source === "firebase" ? "🔥 Firebase" : "📁 Drive"}
                    </span>
                  </div>
                );
              })()}
              
              {/* Delete button - appears on hover */}
              <button
                onClick={() => handleDelete(image)}
                disabled={deleting === (image.id || image.title)}
                className="absolute top-2 right-2 p-2 bg-red-600 text-white rounded-full opacity-0 group-hover:opacity-100 transition-opacity hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg"
                title="Delete image"
              >
                <Trash2 className="h-4 w-4" />
              </button>
              {/* Image title overlay - appears on hover */}
              {image.title && (
                <div className="absolute bottom-0 left-0 right-0 bg-black bg-opacity-60 text-white p-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <p className="text-sm font-medium truncate">{image.title}</p>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
      {images.length === 0 && (
        <div className="rounded-lg bg-white p-12 text-center shadow-md">
          <ImageIcon className="mx-auto h-12 w-12 text-gray-400" />
          <p className="mt-4 text-lg text-gray-500">No images found</p>
        </div>
      )}
    </div>
  );
}

