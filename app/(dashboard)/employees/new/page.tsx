"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { createEmployee, getDistricts, getStations, getRanks, District, Station, Rank } from "@/lib/firebase/firestore";
import { BLOOD_GROUPS } from "@/lib/constants";

export default function NewEmployeePage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
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
    loadDistricts();
    loadRanks();
  }, []);

  useEffect(() => {
    if (selectedDistrict) {
      loadStations(selectedDistrict);
    } else {
      setStations([]);
    }
  }, [selectedDistrict]);

  const loadDistricts = async () => {
    try {
      const data = await getDistricts();
      console.log("Loaded districts:", data);
      setDistricts(data);
      if (data.length === 0) {
        console.warn("No districts found. Make sure districts exist in Firestore.");
      }
    } catch (error) {
      console.error("Error loading districts:", error);
      alert("Failed to load districts. Please check the console for details.");
    }
  };

  const loadStations = async (district: string) => {
    try {
      console.log("Loading stations for district:", district);
      const data = await getStations(district);
      console.log("Loaded stations:", data);
      setStations(data);
      if (data.length === 0) {
        console.warn(`No stations found for district: ${district}`);
      }
    } catch (error) {
      console.error("Error loading stations:", error);
      alert(`Failed to load stations for ${district}. Please check the console for details.`);
    }
  };

  const loadRanks = async () => {
    try {
      const data = await getRanks();
      console.log("Loaded ranks:", data);
      setRanks(data);
      if (data.length === 0) {
        console.warn("No ranks found. Make sure ranks exist in Firestore.");
      }
    } catch (error) {
      console.error("Error loading ranks:", error);
      alert("Failed to load ranks. Please check the console for details.");
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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
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

    setLoading(true);

    try {
      await createEmployee(formData);
      router.push("/employees");
    } catch (error) {
      console.error("Error creating employee:", error);
      alert("Failed to create employee");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-6">
      <h1 className="mb-6 text-3xl font-bold text-slate-100">Add New Employee</h1>

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

          {/* Row 5: KGID */}
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

          {/* Row 6: Rank */}
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

          {/* Row 7: Metal Number (conditional) */}
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
                  className="mt-1 block w-full rounded-md bg-dark-sidebar border border-red-400 px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-red-500 focus:outline-none focus:ring-2 focus:ring-red-500/50"
                />
                <p className="mt-1 text-xs text-red-400 font-medium">
                  ⚠ Required for this rank
                </p>
              </div>
            )}

          {/* Row 8: District */}
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
                <option key={d.id} value={d.name}>
                  {d.name}
                </option>
              ))}
            </select>
          </div>

          {/* Row 9: Station */}
          <div className="relative" style={{ zIndex: 10 }}>
            <label className="block text-sm font-medium text-slate-400">
              Station *
            </label>
            <select
              required
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
                <option key={s.id} value={s.name}>
                  {s.name}
                </option>
              ))}
            </select>
          </div>

          {/* Row 10: Blood Group */}
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

          {/* Row 11: Photo URL */}
          <div>
            <label className="block text-sm font-medium text-slate-400">
              Photo URL
            </label>
            <input
              type="url"
              value={formData.photoUrl}
              onChange={(e) => setFormData({ ...formData, photoUrl: e.target.value })}
              className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-400/50"
              placeholder="https://..."
            />
          </div>

          {/* Row 12: Checkboxes */}
          <div className="flex gap-6">
            <div className="flex items-center">
              <input
                type="checkbox"
                id="isAdmin"
                checked={formData.isAdmin}
                onChange={(e) => setFormData({ ...formData, isAdmin: e.target.checked })}
                className="h-4 w-4 rounded border-dark-border bg-dark-sidebar text-primary-600 focus:ring-primary-500"
              />
              <label htmlFor="isAdmin" className="ml-2 text-sm font-medium text-slate-400">
                Is Admin
              </label>
            </div>
            <div className="flex items-center">
              <input
                type="checkbox"
                id="isApproved"
                checked={formData.isApproved}
                onChange={(e) => setFormData({ ...formData, isApproved: e.target.checked })}
                className="h-4 w-4 rounded border-dark-border bg-dark-sidebar text-primary-600 focus:ring-primary-500"
              />
              <label htmlFor="isApproved" className="ml-2 text-sm font-medium text-slate-400">
                Is Approved
              </label>
            </div>
          </div>
        </div>

        <div className="mt-6 flex gap-4">
          <button
            type="submit"
            disabled={loading}
            className="rounded-lg bg-primary-600 px-6 py-2 text-white transition-colors hover:bg-primary-700 disabled:opacity-50"
          >
            {loading ? "Saving..." : "Save Employee"}
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

