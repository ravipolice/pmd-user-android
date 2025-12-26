"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import {
  getEmployee,
  updateEmployee,
  getDistricts,
  getStations,
  getRanks,
  District,
  Station,
  Rank,
} from "@/lib/firebase/firestore";
import { BLOOD_GROUPS } from "@/lib/constants";

export default function EditEmployeePage() {
  const router = useRouter();
  const [employeeId, setEmployeeId] = useState<string>("");
  const [mounted, setMounted] = useState(false);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [districts, setDistricts] = useState<District[]>([]);
  const [stations, setStations] = useState<Station[]>([]);
  const [ranks, setRanks] = useState<Rank[]>([]);
  const [selectedDistrict, setSelectedDistrict] = useState("");

  const [formData, setFormData] = useState({
    kgid: "",
    name: "",
    email: "",
    mobile1: "",
    mobile2: "",
    rank: "",
    metalNumber: "",
    district: "",
    station: "",
    bloodGroup: "",
    photoUrl: "",
    isAdmin: false,
    isApproved: true,
  });

  useEffect(() => {
    // Mark as mounted (client-side only)
    setMounted(true);
    
    // Get ID from URL query parameter
    const params = new URLSearchParams(window.location.search);
    const id = params.get("id") || "";
    if (!id) {
      alert("No employee ID provided");
      router.push("/employees");
      return;
    }
    setEmployeeId(id);
  }, [router]);

  useEffect(() => {
    if (!employeeId) return;
    loadDistricts();
    loadRanks();
    loadEmployee();
  }, [employeeId]);

  useEffect(() => {
    // Only auto-load stations when district changes via user interaction
    // Don't reload if we're in the middle of loading employee data
    // Also don't reload if stations are already loaded for this district
    if (selectedDistrict && !loading) {
      const currentStationDistrict = stations[0]?.district;
      // Only reload if stations are for a different district or empty
      if (stations.length === 0 || (currentStationDistrict && currentStationDistrict !== selectedDistrict)) {
        loadStations(selectedDistrict);
      }
    } else if (!selectedDistrict && !loading) {
      // Only clear stations if not loading
      setStations([]);
    }
  }, [selectedDistrict, loading]);

  // Ensure station value is preserved when stations are loaded
  useEffect(() => {
    if (stations.length > 0 && formData.station) {
      // Check if the current station exists in the stations list
      const stationExists = stations.some(s => s.name === formData.station);
      if (!stationExists && formData.station) {
        // If station doesn't exist, add it to the list
        setStations(prev => {
          // Check if it's already been added
          if (!prev.some(s => s.name === formData.station)) {
            return [...prev, {
              id: "current",
              name: formData.station,
              district: selectedDistrict || ""
            }];
          }
          return prev;
        });
      }
    }
  }, [stations, formData.station, selectedDistrict]);

  const loadEmployee = async () => {
    if (!employeeId) return;
    try {
      const employee = await getEmployee(employeeId);
      if (!employee) {
        alert("Employee not found");
        router.push("/employees");
        return;
      }

      // Preserve exact values as they are stored in the database
      const employeeStation = employee.station ?? "";
      const employeeDistrict = employee.district ?? "";
      const employeeBloodGroup = employee.bloodGroup ?? "";

      // Load stations first if district exists, then set form data
      if (employeeDistrict) {
        // Load stations for the district before setting form data
        const districtStations = await getStations(employeeDistrict);
        
        // Normalize station names for comparison (trim and case-insensitive)
        const normalizedEmployeeStation = employeeStation.trim();
        const stationExists = districtStations.some(s => 
          s.name.trim().toLowerCase() === normalizedEmployeeStation.toLowerCase()
        );
        
        // If the employee's station doesn't exist in the stations list, add it
        // This ensures the station value is always displayed, even if it was removed from the list
        if (normalizedEmployeeStation && !stationExists) {
          districtStations.push({ 
            id: "current", 
            name: normalizedEmployeeStation,
            district: employeeDistrict 
          });
        }
        
        // Set stations first, then set selectedDistrict
        setStations(districtStations);
        setSelectedDistrict(employeeDistrict);
      }

      // Now set form data with all values exactly as stored
      // Preserve exact values - don't convert undefined/null to empty string for optional fields
      setFormData({
        kgid: employee.kgid ?? "",
        name: employee.name ?? "",
        email: employee.email ?? "",
        mobile1: employee.mobile1 ?? "",
        mobile2: employee.mobile2 ?? "",
        rank: employee.rank ?? "",
        metalNumber: employee.metalNumber ?? "",
        district: employeeDistrict,
        station: employeeStation,
        bloodGroup: employeeBloodGroup, // Preserve "??" exactly as stored
        photoUrl: employee.photoUrl ?? "",
        isAdmin: employee.isAdmin ?? false,
        isApproved: employee.isApproved !== undefined ? employee.isApproved : true,
      });
    } catch (error) {
      console.error("Error loading employee:", error);
      alert("Failed to load employee");
      router.push("/employees");
    } finally {
      setLoading(false);
    }
  };

  const loadDistricts = async () => {
    try {
      const data = await getDistricts();
      setDistricts(data);
    } catch (error) {
      console.error("Error loading districts:", error);
    }
  };

  const loadRanks = async () => {
    try {
      const data = await getRanks();
      setRanks(data);
    } catch (error) {
      console.error("Error loading ranks:", error);
    }
  };

  const getSelectedRank = (rankName: string): Rank | undefined => {
    return ranks.find(r => 
      r.equivalent_rank === rankName || 
      r.aliases?.includes(rankName) ||
      r.rank_id === rankName
    );
  };

  const requiresMetalNumber = (rankName: string): boolean => {
    const rank = getSelectedRank(rankName);
    return rank?.requiresMetalNumber || false;
  };

  const loadStations = async (district: string) => {
    try {
      const data = await getStations(district);
      setStations(data);
    } catch (error) {
      console.error("Error loading stations:", error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!employeeId) {
      alert("No employee ID provided");
      return;
    }

    // Validate metal number for ranks that require it
    if (
      formData.rank &&
      requiresMetalNumber(formData.rank) &&
      !formData.metalNumber?.trim()
    ) {
      alert(`Metal number is required for rank: ${formData.rank}`);
      return;
    }

    // Validate mobile number format
    if (formData.mobile1 && formData.mobile1.length !== 10) {
      alert("Mobile number must be 10 digits");
      return;
    }

    setSaving(true);

    try {
      await updateEmployee(employeeId, formData);
      router.push("/employees");
    } catch (error) {
      console.error("Error updating employee:", error);
      alert("Failed to update employee");
    } finally {
      setSaving(false);
    }
  };

  if (!mounted || loading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-lg text-slate-400">Loading...</div>
      </div>
    );
  }

  return (
    <div className="p-6">
      <h1 className="mb-6 text-3xl font-bold text-slate-100">Edit Employee</h1>

      <form onSubmit={handleSubmit} className="rounded-lg bg-dark-card border border-dark-border p-6 shadow-lg" style={{ overflow: 'visible' }}>
        <h2 className="mb-4 text-xl font-semibold text-slate-100">Employee Details</h2>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3" style={{ overflow: 'visible' }}>
          {/* Row 1: Name */}
          <div>
            <label className="block text-sm font-medium text-slate-400">
              Name *
            </label>
            <input
              type="text"
              required
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-400/50"
            />
          </div>

          {/* Row 2: Email */}
          <div>
            <label className="block text-sm font-medium text-slate-400">
              Email *
            </label>
            <input
              type="email"
              required
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-400/50"
            />
          </div>

          {/* Row 3: Mobile 1 */}
          <div>
            <label className="block text-sm font-medium text-slate-400">
              Mobile 1 *
            </label>
            <input
              type="tel"
              required
              value={formData.mobile1}
              onChange={(e) => {
                // Only allow digits, max 10
                const value = e.target.value.replace(/\D/g, '').slice(0, 10);
                setFormData({ ...formData, mobile1: value });
              }}
              className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-400/50"
            />
          </div>

          {/* Row 4: Mobile 2 */}
          <div>
            <label className="block text-sm font-medium text-slate-400">
              Mobile 2 (Optional)
            </label>
            <input
              type="tel"
              value={formData.mobile2}
              onChange={(e) => {
                // Only allow digits, max 10
                const value = e.target.value.replace(/\D/g, '').slice(0, 10);
                setFormData({ ...formData, mobile2: value });
              }}
              className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-400/50"
            />
          </div>

          {/* Row 5: KGID, Rank, Metal Number (all in same row) */}
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            <div>
              <label className="block text-sm font-medium text-slate-400">
                KGID *
              </label>
              <input
                type="text"
                required
                value={formData.kgid}
                onChange={(e) => {
                  // Only allow digits
                  const value = e.target.value.replace(/\D/g, '');
                  setFormData({ ...formData, kgid: value });
                }}
                className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-400/50"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-400">
                Rank *
              </label>
              <select
                required
                value={formData.rank}
                onChange={(e) => {
                  const newRank = e.target.value;
                  // Clear metal number if rank changes to one that doesn't require it
                  const shouldClearMetal =
                    formData.metalNumber &&
                    (!newRank || !requiresMetalNumber(newRank));
                  setFormData({
                    ...formData,
                    rank: newRank,
                    metalNumber: shouldClearMetal ? "" : formData.metalNumber,
                  });
                }}
                className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-400/50"
              >
                <option value="">Select Rank</option>
                {ranks.map((rank) => (
                  <option key={rank.rank_id} value={rank.equivalent_rank}>
                    {rank.rank_label} ({rank.equivalent_rank})
                  </option>
                ))}
              </select>
            </div>

            {formData.rank &&
              requiresMetalNumber(formData.rank) && (
                <div>
                  <label className="block text-sm font-medium text-slate-400">
                    Metal Number *
                  </label>
                  <input
                    type="text"
                    required
                    value={formData.metalNumber}
                    onChange={(e) =>
                      setFormData({ ...formData, metalNumber: e.target.value })
                    }
                    className={`mt-1 block w-full rounded-md border px-3 py-2 focus:outline-none focus:ring-2 ${
                      formData.metalNumber?.trim()
                        ? "border-dark-border focus:border-primary-400 focus:ring-primary-400/50"
                        : "border-amber-300 focus:border-amber-500 focus:ring-amber-500"
                    }`}
                  />
                  <p className={`mt-1 text-xs font-medium ${
                    formData.metalNumber?.trim()
                      ? "text-slate-500"
                      : "text-amber-600"
                  }`}>
                    {formData.metalNumber?.trim() 
                      ? "✓ Metal number provided" 
                      : "⚠ Required for this rank"}
                  </p>
                </div>
              )}
          </div>

          {/* Row 6: District */}
          <div className="relative" style={{ zIndex: 10 }}>
            <label className="block text-sm font-medium text-slate-400">
              District *
            </label>
            <select
              required
              value={selectedDistrict}
              onChange={(e) => {
                setSelectedDistrict(e.target.value);
                setFormData({ ...formData, district: e.target.value, station: "" });
              }}
              className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-400/50"
              style={{ zIndex: 1000, position: 'relative' }}
            >
              <option value="">Select District</option>
              {districts.map((d) => (
                <option key={d.id} value={d.name} style={{ backgroundColor: 'white', color: 'black' }}>
                  {d.name}
                </option>
              ))}
            </select>
          </div>

          {/* Row 7: Station and Blood Group (both in same row) */}
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="relative" style={{ zIndex: 10 }}>
              <label className="block text-sm font-medium text-slate-400">
                Station *
              </label>
              <select
                required
                key={`station-${stations.length}-${formData.station}`}
                value={formData.station}
                onChange={(e) => setFormData({ ...formData, station: e.target.value })}
                disabled={!selectedDistrict}
                className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-400/50 disabled:bg-dark-accent-light disabled:text-slate-500"
                style={{ zIndex: 1000, position: 'relative' }}
              >
                <option value="">
                  {selectedDistrict ? "Select Station" : "Select District First"}
                </option>
                {stations.map((s) => (
                  <option key={s.id || s.name} value={s.name}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-400">
                Blood Group
              </label>
              <select
                value={formData.bloodGroup}
                onChange={(e) => setFormData({ ...formData, bloodGroup: e.target.value })}
                className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-400/50"
              >
                <option value="">Select Blood Group</option>
                {BLOOD_GROUPS.map((bg) => (
                  <option key={bg} value={bg}>
                    {bg}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Row 8: Photo URL */}
          <div>
            <label className="block text-sm font-medium text-slate-400">
              Photo URL
            </label>
            <input
              type="url"
              value={formData.photoUrl}
              onChange={(e) => setFormData({ ...formData, photoUrl: e.target.value })}
              className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-400/50"
            />
          </div>
        </div>

        <div className="mt-6 flex items-center gap-4">
          <label className="flex items-center">
            <input
              type="checkbox"
              checked={formData.isAdmin}
              onChange={(e) => setFormData({ ...formData, isAdmin: e.target.checked })}
              className="mr-2 rounded border-dark-border bg-dark-sidebar text-primary-600 focus:ring-primary-500"
            />
            <span className="text-sm text-slate-400">Is Admin</span>
          </label>
          <label className="flex items-center">
            <input
              type="checkbox"
              checked={formData.isApproved}
              onChange={(e) => setFormData({ ...formData, isApproved: e.target.checked })}
              className="mr-2 rounded border-dark-border bg-dark-sidebar text-primary-600 focus:ring-primary-500"
            />
            <span className="text-sm text-slate-400">Is Approved</span>
          </label>
        </div>

        <div className="mt-6 flex gap-4">
          <button
            type="submit"
            disabled={saving}
            className="rounded-lg bg-primary-600 px-6 py-2 text-white transition-colors hover:bg-primary-700 disabled:opacity-50"
          >
            {saving ? "Saving..." : "Update Employee"}
          </button>
          <button
            type="button"
            onClick={() => router.back()}
            className="rounded-lg border border-dark-border px-6 py-2 text-slate-400 transition-colors hover:bg-dark-sidebar hover:text-slate-100"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}

