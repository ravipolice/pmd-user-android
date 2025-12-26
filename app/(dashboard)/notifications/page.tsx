"use client";

import { useState, useEffect } from "react";
import { createNotification, getDistricts, getStations, District, Station } from "@/lib/firebase/firestore";
import { Send, Users, Shield } from "lucide-react";

type TabType = "admin" | "users";

export default function NotificationsPage() {
  const [activeTab, setActiveTab] = useState<TabType>("admin");
  const [loading, setLoading] = useState(false);
  const [districts, setDistricts] = useState<District[]>([]);
  const [stations, setStations] = useState<Station[]>([]);
  const [selectedDistrict, setSelectedDistrict] = useState("");

  // Admin notification form data
  const [adminFormData, setAdminFormData] = useState({
    title: "",
    body: "",
  });

  // User notification form data
  const [userFormData, setUserFormData] = useState({
    title: "",
    body: "",
    targetType: "ALL" as "SINGLE" | "STATION" | "DISTRICT" | "ALL",
    targetKgid: "",
    targetDistrict: "",
    targetStation: "",
  });

  useEffect(() => {
    loadDistricts();
  }, []);

  useEffect(() => {
    if (selectedDistrict && (userFormData.targetType === "STATION" || userFormData.targetType === "DISTRICT")) {
      loadStations(selectedDistrict);
    } else {
      setStations([]);
    }
  }, [selectedDistrict, userFormData.targetType]);

  const loadDistricts = async () => {
    try {
      const data = await getDistricts();
      setDistricts(data);
    } catch (error) {
      console.error("Error loading districts:", error);
    }
  };

  const loadStations = async (district: string) => {
    try {
      const data = await getStations(district);
      setStations(data);
    } catch (error) {
      console.error("Error loading stations:", error);
    }
  };

  const handleAdminSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Validate form
    if (!adminFormData.title.trim() || !adminFormData.body.trim()) {
      alert("Please fill in both title and message fields");
      return;
    }
    
    setLoading(true);

    try {
      const notificationData: any = {
        title: adminFormData.title.trim(),
        body: adminFormData.body.trim(),
        targetType: "ADMIN",
      };

      console.log("📤 Creating admin notification:", notificationData);
      const notificationId = await createNotification(notificationData, true); // true = admin notification
      console.log("✅ Admin notification created with ID:", notificationId);
      
      alert("Admin notification queued successfully! It will be sent shortly.");
      setAdminFormData({
        title: "",
        body: "",
      });
    } catch (error: any) {
      console.error("❌ Error creating admin notification:", error);
      console.error("❌ Error details:", {
        message: error?.message,
        code: error?.code,
        stack: error?.stack
      });
      alert(`Failed to create admin notification: ${error?.message || "Unknown error"}`);
    } finally {
      setLoading(false);
    }
  };

  const handleUserSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Validate form
    if (!userFormData.title.trim() || !userFormData.body.trim()) {
      alert("Please fill in both title and message fields");
      return;
    }
    
    setLoading(true);

    try {
      const notificationData: any = {
        title: userFormData.title.trim(),
        body: userFormData.body.trim(),
        targetType: userFormData.targetType,
      };

      // Add target-specific fields
      if (userFormData.targetType === "SINGLE") {
        if (!userFormData.targetKgid?.trim()) {
          alert("Please enter a KGID for single user notification");
          setLoading(false);
          return;
        }
        notificationData.targetKgid = userFormData.targetKgid.trim();
      } else if (userFormData.targetType === "STATION") {
        if (!userFormData.targetDistrict || !userFormData.targetStation) {
          alert("Please select both district and station");
          setLoading(false);
          return;
        }
        notificationData.targetDistrict = userFormData.targetDistrict;
        notificationData.targetStation = userFormData.targetStation;
      } else if (userFormData.targetType === "DISTRICT") {
        if (!userFormData.targetDistrict) {
          alert("Please select a district");
          setLoading(false);
          return;
        }
        notificationData.targetDistrict = userFormData.targetDistrict;
      }

      console.log("📤 Creating user notification:", notificationData);
      const notificationId = await createNotification(notificationData, false); // false = user notification
      console.log("✅ User notification created with ID:", notificationId);
      
      alert("User notification queued successfully! It will be sent shortly.");
      setUserFormData({
        title: "",
        body: "",
        targetType: "ALL",
        targetKgid: "",
        targetDistrict: "",
        targetStation: "",
      });
      setSelectedDistrict("");
      setStations([]);
    } catch (error: any) {
      console.error("❌ Error creating user notification:", error);
      console.error("❌ Error details:", {
        message: error?.message,
        code: error?.code,
        stack: error?.stack
      });
      alert(`Failed to create user notification: ${error?.message || "Unknown error"}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-6">
      <h1 className="mb-6 text-3xl font-bold text-gray-900">Notifications</h1>

      {/* Tabs */}
      <div className="mb-6 border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          <button
            onClick={() => setActiveTab("admin")}
            className={`flex items-center gap-2 border-b-2 px-1 py-4 text-sm font-medium ${
              activeTab === "admin"
                ? "border-primary-500 text-primary-600"
                : "border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700"
            }`}
          >
            <Shield className="h-5 w-5" />
            Send to Admins
          </button>
          <button
            onClick={() => setActiveTab("users")}
            className={`flex items-center gap-2 border-b-2 px-1 py-4 text-sm font-medium ${
              activeTab === "users"
                ? "border-primary-500 text-primary-600"
                : "border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700"
            }`}
          >
            <Users className="h-5 w-5" />
            Send to Users
          </button>
        </nav>
      </div>

      {/* Admin Notification Form */}
      {activeTab === "admin" && (
        <form onSubmit={handleAdminSubmit} className="rounded-lg bg-white p-6 shadow-md">
          <div className="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-700">
            <Shield className="h-5 w-5 text-primary-600" />
            <span>Send Notification to Admins</span>
          </div>
          <div className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-700">
                Title *
              </label>
              <input
                type="text"
                required
                value={adminFormData.title}
                onChange={(e) => setAdminFormData({ ...adminFormData, title: e.target.value })}
                className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Message *
              </label>
              <textarea
                required
                rows={4}
                value={adminFormData.body}
                onChange={(e) => setAdminFormData({ ...adminFormData, body: e.target.value })}
                className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="flex items-center gap-2 rounded-lg bg-primary-600 px-6 py-2 text-white transition-colors hover:bg-primary-700 disabled:opacity-50"
            >
              <Send className="h-5 w-5" />
              {loading ? "Sending..." : "Send to Admins"}
            </button>
          </div>
        </form>
      )}

      {/* User Notification Form */}
      {activeTab === "users" && (
        <form onSubmit={handleUserSubmit} className="rounded-lg bg-white p-6 shadow-md">
          <div className="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-700">
            <Users className="h-5 w-5 text-primary-600" />
            <span>Send Notification to Users</span>
          </div>
          <div className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-700">
                Title *
              </label>
              <input
                type="text"
                required
                value={userFormData.title}
                onChange={(e) => setUserFormData({ ...userFormData, title: e.target.value })}
                className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Message *
              </label>
              <textarea
                required
                rows={4}
                value={userFormData.body}
                onChange={(e) => setUserFormData({ ...userFormData, body: e.target.value })}
                className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Target Audience *
              </label>
                <select
                  required
                  value={userFormData.targetType}
                  onChange={(e) =>
                    setUserFormData({
                      ...userFormData,
                      targetType: e.target.value as any,
                    })
                  }
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                >
                <option value="ALL">All Users</option>
                <option value="DISTRICT">By District</option>
                <option value="STATION">By Station</option>
                <option value="SINGLE">Single User (KGID)</option>
              </select>
            </div>

            {userFormData.targetType === "SINGLE" && (
              <div>
                <label className="block text-sm font-medium text-gray-700">
                  KGID *
                </label>
                <input
                  type="text"
                  required
                  value={userFormData.targetKgid}
                  onChange={(e) =>
                    setUserFormData({ ...userFormData, targetKgid: e.target.value })
                  }
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 placeholder-gray-500 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
              </div>
            )}

            {userFormData.targetType === "DISTRICT" && (
              <div>
                <label className="block text-sm font-medium text-gray-700">
                  District *
                </label>
                <select
                  required
                  value={selectedDistrict}
                  onChange={(e) => {
                    setSelectedDistrict(e.target.value);
                    setUserFormData({ ...userFormData, targetDistrict: e.target.value });
                  }}
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                >
                  <option value="">Select District</option>
                  {districts.map((d) => (
                    <option key={d.id} value={d.name}>
                      {d.name}
                    </option>
                  ))}
                </select>
              </div>
            )}

            {userFormData.targetType === "STATION" && (
              <>
                <div>
                  <label className="block text-sm font-medium text-gray-700">
                    District *
                  </label>
                  <select
                    required
                    value={selectedDistrict}
                    onChange={(e) => {
                      setSelectedDistrict(e.target.value);
                      setUserFormData({ 
                        ...userFormData, 
                        targetDistrict: e.target.value,
                        targetStation: "" 
                      });
                    }}
                    className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                  >
                    <option value="">Select District</option>
                    {districts.map((d) => (
                      <option key={d.id} value={d.name}>
                        {d.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">
                    Station *
                  </label>
                  <select
                    required
                    value={userFormData.targetStation}
                    onChange={(e) =>
                      setUserFormData({ ...userFormData, targetStation: e.target.value })
                    }
                    disabled={!selectedDistrict}
                    className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500 disabled:bg-gray-100 disabled:text-gray-500"
                  >
                    <option value="">
                      {selectedDistrict ? "Select Station" : "Select District First"}
                    </option>
                    {stations.map((s) => (
                      <option key={s.id} value={s.name}>
                        {s.name}
                      </option>
                    ))}
                  </select>
                </div>
              </>
            )}

            <button
              type="submit"
              disabled={loading}
              className="flex items-center gap-2 rounded-lg bg-primary-600 px-6 py-2 text-white transition-colors hover:bg-primary-700 disabled:opacity-50"
            >
              <Send className="h-5 w-5" />
              {loading ? "Sending..." : "Send to Users"}
            </button>
          </div>
        </form>
      )}
    </div>
  );
}

