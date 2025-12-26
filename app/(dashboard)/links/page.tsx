"use client";

import { useEffect, useState } from "react";
import { getUsefulLinks, createUsefulLink, UsefulLink } from "@/lib/firebase/firestore";
import { uploadFile } from "@/lib/firebase/storage";
import { Timestamp } from "firebase/firestore";
import { Plus, ExternalLink, Upload, Link as LinkIcon } from "lucide-react";

type UploadMethod = "url" | "file";

export default function LinksPage() {
  const [links, setLinks] = useState<UsefulLink[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [uploadMethod, setUploadMethod] = useState<UploadMethod>("url");
  const [formData, setFormData] = useState({
    name: "",
    playStoreUrl: "",
    apkUrl: "",
    iconUrl: "",
  });
  const [apkFile, setApkFile] = useState<File | null>(null);
  const [iconFile, setIconFile] = useState<File | null>(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    loadLinks();
  }, []);

  const loadLinks = async () => {
    try {
      const data = await getUsefulLinks();
      setLinks(data);
    } catch (error) {
      console.error("Error loading links:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setUploadProgress(0);

    try {
      let finalApkUrl = formData.apkUrl.trim() || undefined;
      let finalIconUrl = formData.iconUrl.trim() || undefined;

      // Upload APK file if provided (matching mobile app logic)
      if (!finalApkUrl && apkFile) {
        setUploadProgress(25);
        const safeName = formData.name
          .toLowerCase()
          .replace(/[^a-z0-9]+/g, "_")
          .replace(/^_+|_+$/g, "") || "link";
        const fileName = `${safeName}_${Date.now()}_${Math.random().toString(36).substring(7)}.apk`;
        const storagePath = `useful_links/apks/${fileName}`;
        
        try {
          finalApkUrl = await uploadFile(storagePath, apkFile);
          console.log("APK uploaded successfully:", finalApkUrl);
          setUploadProgress(50);
        } catch (error: any) {
          console.error("APK upload failed:", error);
          throw new Error("Failed to upload APK file. Please check your internet connection and try again.");
        }
      }

      // Upload icon image if provided (matching mobile app logic)
      if (!finalIconUrl && iconFile) {
        setUploadProgress(75);
        const safeName = formData.name
          .toLowerCase()
          .replace(/[^a-z0-9]+/g, "_")
          .replace(/^_+|_+$/g, "") || "link";
        const fileName = `${safeName}_${Date.now()}_${Math.random().toString(36).substring(7)}.jpg`;
        const storagePath = `useful_links/icons/${fileName}`;
        
        try {
          finalIconUrl = await uploadFile(storagePath, iconFile);
          console.log("Icon uploaded successfully:", finalIconUrl);
        } catch (error: any) {
          console.warn("Icon upload failed, continuing without icon:", error);
          // Don't throw - icon is optional
        }
      }

      // Validation: Must have either playStoreUrl OR apkUrl (matching mobile app logic)
      const hasPlayStoreUrl = formData.playStoreUrl.trim().length > 0;
      const hasApkUrl = !!finalApkUrl;
      
      if (!hasPlayStoreUrl && !hasApkUrl) {
        throw new Error("Provide either Play Store URL OR APK file/URL");
      }

      setUploadProgress(90);

      // Save to Firestore with timestamps (matching mobile app logic)
      const now = Timestamp.now();
      await createUsefulLink({
        name: formData.name.trim(),
        playStoreUrl: hasPlayStoreUrl ? formData.playStoreUrl.trim() : undefined,
        apkUrl: finalApkUrl,
        iconUrl: finalIconUrl,
        createdAt: now,
        updatedAt: now,
      });

      setUploadProgress(100);
      
      // Reset form
      setFormData({ name: "", playStoreUrl: "", apkUrl: "", iconUrl: "" });
      setApkFile(null);
      setIconFile(null);
      setUploadProgress(0);
      setShowForm(false);
      await loadLinks();
    } catch (error: any) {
      console.error("Error creating link:", error);
      alert(error?.message || "Failed to create link");
    } finally {
      setSubmitting(false);
      setUploadProgress(0);
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
        <h1 className="text-3xl font-bold text-gray-900">Useful Links</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          className="flex items-center gap-2 rounded-lg bg-primary-600 px-4 py-2 text-white transition-colors hover:bg-primary-700"
        >
          <Plus className="h-5 w-5" />
          Add Link
        </button>
      </div>

      {showForm && (
        <div className="mb-6 rounded-lg bg-white p-6 shadow-md">
          <h2 className="mb-4 text-xl font-semibold">Add New Link</h2>
          
          {/* Upload Method Tabs */}
          <div className="mb-4 flex gap-2 border-b border-gray-200">
            <button
              type="button"
              onClick={() => {
                setUploadMethod("url");
                setApkFile(null);
                setIconFile(null);
              }}
              className={`flex items-center gap-2 px-4 py-2 font-medium transition-colors ${
                uploadMethod === "url"
                  ? "border-b-2 border-primary-600 text-primary-600"
                  : "text-gray-500 hover:text-gray-700"
              }`}
            >
              <LinkIcon className="h-4 w-4" />
              URLs
            </button>
            <button
              type="button"
              onClick={() => {
                setUploadMethod("file");
                setFormData({ ...formData, apkUrl: "", iconUrl: "" });
              }}
              className={`flex items-center gap-2 px-4 py-2 font-medium transition-colors ${
                uploadMethod === "file"
                  ? "border-b-2 border-primary-600 text-primary-600"
                  : "text-gray-500 hover:text-gray-700"
              }`}
            >
              <Upload className="h-4 w-4" />
              Upload Files
            </button>
          </div>

          <form onSubmit={handleSubmit}>
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
                  Play Store URL
                </label>
                <input
                  type="url"
                  value={formData.playStoreUrl}
                  onChange={(e) => setFormData({ ...formData, playStoreUrl: e.target.value })}
                  placeholder="https://play.google.com/store/apps/..."
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
              </div>
              
              {uploadMethod === "file" ? (
                <>
                  <div>
                    <label className="block text-sm font-medium text-gray-700">
                      APK File *
                    </label>
                    <input
                      type="file"
                      accept=".apk"
                      onChange={(e) => {
                        const file = e.target.files?.[0];
                        if (file) {
                          setApkFile(file);
                          // Auto-fill name if empty
                          if (!formData.name) {
                            const nameWithoutExt = file.name.replace(/\.apk$/i, "");
                            setFormData({ ...formData, name: nameWithoutExt });
                          }
                        }
                      }}
                      className="mt-1 block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:bg-primary-50 file:text-primary-700 hover:file:bg-primary-100"
                    />
                    {apkFile && (
                      <p className="mt-2 text-sm text-gray-600">
                        Selected: {apkFile.name} ({(apkFile.size / 1024 / 1024).toFixed(2)} MB)
                      </p>
                    )}
                    <p className="mt-1 text-xs text-gray-500">
                      Or provide APK URL below if file is already hosted
                    </p>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700">
                      Icon Image
                    </label>
                    <input
                      type="file"
                      accept="image/*"
                      onChange={(e) => {
                        const file = e.target.files?.[0];
                        if (file) {
                          if (!file.type.startsWith("image/")) {
                            alert("Please select an image file");
                            return;
                          }
                          setIconFile(file);
                        }
                      }}
                      className="mt-1 block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:bg-primary-50 file:text-primary-700 hover:file:bg-primary-100"
                    />
                    {iconFile && (
                      <p className="mt-2 text-sm text-gray-600">
                        Selected: {iconFile.name} ({(iconFile.size / 1024).toFixed(2)} KB)
                      </p>
                    )}
                    <p className="mt-1 text-xs text-gray-500">
                      Or provide Icon URL below if image is already hosted
                    </p>
                  </div>
                </>
              ) : (
                <>
                  <div>
                    <label className="block text-sm font-medium text-gray-700">
                      APK URL
                    </label>
                    <input
                      type="url"
                      value={formData.apkUrl}
                      onChange={(e) => setFormData({ ...formData, apkUrl: e.target.value })}
                      placeholder="https://example.com/app.apk"
                      className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700">
                      Icon URL
                    </label>
                    <input
                      type="url"
                      value={formData.iconUrl}
                      onChange={(e) => setFormData({ ...formData, iconUrl: e.target.value })}
                      placeholder="https://example.com/icon.png"
                      className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                    />
                  </div>
                </>
              )}
            </div>
            
            {/* Upload Progress */}
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
                disabled={submitting || (uploadMethod === "file" && !apkFile && !formData.apkUrl.trim())}
                className="rounded-lg bg-primary-600 px-6 py-2 text-white transition-colors hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {submitting ? "Saving..." : "Save Link"}
              </button>
              <button
                type="button"
                onClick={() => {
                  setShowForm(false);
                  setFormData({ name: "", playStoreUrl: "", apkUrl: "", iconUrl: "" });
                  setApkFile(null);
                  setIconFile(null);
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

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
        {links.map((link) => (
          <div
            key={link.id}
            className="rounded-lg bg-white p-6 shadow-md transition-shadow hover:shadow-lg"
          >
            <div className="mb-4 flex items-center gap-4">
              {/* App Icon/Logo */}
              {link.iconUrl ? (
                <div className="flex-shrink-0">
                  <img
                    src={link.iconUrl}
                    alt={link.name}
                    className="h-16 w-16 rounded-lg object-cover border border-gray-200"
                    loading="lazy"
                    referrerPolicy="no-referrer"
                    onError={(e) => {
                      const img = e.currentTarget;
                      if (!img.dataset.failed) {
                        img.dataset.failed = "1";
                        // Try fallback to Drive URL if CDN failed
                        if (link.iconUrl?.includes("lh3.googleusercontent.com")) {
                          const fileIdMatch = link.iconUrl.match(/\/d\/([-\w]{25,})/);
                          if (fileIdMatch) {
                            const fileId = fileIdMatch[1];
                            img.src = `https://drive.google.com/thumbnail?id=${fileId}&sz=w200`;
                            return;
                          }
                        }
                        // Final fallback: placeholder
                        img.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='64' height='64'%3E%3Crect fill='%23e5e7eb' width='64' height='64' rx='8'/%3E%3Ctext fill='%239ca3af' font-family='sans-serif' font-size='24' font-weight='bold' x='50%25' y='50%25' text-anchor='middle' dy='.3em'%3EApp%3C/text%3E%3C/svg%3E";
                      }
                    }}
                  />
                </div>
              ) : (
                <div className="flex-shrink-0">
                  <div className="flex h-16 w-16 items-center justify-center rounded-lg bg-gray-100 border border-gray-200">
                    <span className="text-2xl font-bold text-gray-400">
                      {link.name.charAt(0).toUpperCase()}
                    </span>
                  </div>
                </div>
              )}
              
              {/* App Name */}
              <div className="flex-1 min-w-0">
                <h3 className="text-lg font-semibold text-gray-900 truncate">
                  {link.name}
                </h3>
              </div>
            </div>
            
            {/* Links */}
            <div className="space-y-2">
              {link.playStoreUrl && (
                <a
                  href={link.playStoreUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-2 rounded-md bg-blue-50 px-3 py-2 text-sm text-blue-700 transition-colors hover:bg-blue-100"
                >
                  <ExternalLink className="h-4 w-4" />
                  Play Store
                </a>
              )}
              {link.apkUrl && (
                <a
                  href={link.apkUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-2 rounded-md bg-green-50 px-3 py-2 text-sm text-green-700 transition-colors hover:bg-green-100"
                >
                  <ExternalLink className="h-4 w-4" />
                  Download APK
                </a>
              )}
            </div>
          </div>
        ))}
      </div>
      {links.length === 0 && (
        <div className="rounded-lg bg-white p-12 text-center shadow-md">
          <p className="text-lg text-gray-500">No links found</p>
        </div>
      )}
    </div>
  );
}

