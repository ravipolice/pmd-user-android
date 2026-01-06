"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import {
  getOfficer,
  updateOfficer,
  getDistricts,
  getStations,
  getRanks,
  District,
  Station,
  Rank,
} from "@/lib/firebase/firestore";

export default function EditOfficerPage() {
  const router = useRouter();
  const [officerId, setOfficerId] = useState<string>("");
  const [mounted, setMounted] = useState(false);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [districts, setDistricts] = useState<District[]>([]);
  const [stations, setStations] = useState<Station[]>([]);
  const [ranks, setRanks] = useState<Rank[]>([]);
  const [selectedDistrict, setSelectedDistrict] = useState("");

  const [formData, setFormData] = useState({
    agid: "",
    rank: "",
    name: "",
    mobile: "",
    email: "",
    landline: "",
    district: "",
    office: "",
  });

  const loadOfficer = async () => {
    if (!officerId) return;
    try {
      const officer = await getOfficer(officerId);
      if (!officer) {
        alert("Officer not found");
        router.push("/officers");
        return;
      }

      setFormData({
        agid: officer.agid || officer.cfd || "",
        rank: officer.rank || "",
        name: officer.name || "",
        mobile: officer.mobile ? officer.mobile.toUpperCase() : "",
        email: officer.email || "",
        landline: officer.landline || "",
        district: officer.district || "",
        office: (officer as any).station || officer.office || "",
      });

      // Set selected district to load stations
      if (officer.district) {
        setSelectedDistrict(officer.district);
      }
    } catch (error) {
      console.error("Error loading officer:", error);
      alert("Failed to load officer");
      router.push("/officers");
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

  const loadStations = async (district: string) => {
    try {
      const data = await getStations(district);
      setStations(data);
    } catch (error) {
      console.error("Error loading stations:", error);
    }
  };

  useEffect(() => {
    // Mark as mounted (client-side only)
    setMounted(true);
    
    // Get ID from URL query parameter
    const params = new URLSearchParams(window.location.search);
    const id = params.get("id") || "";
    if (!id) {
      alert("No officer ID provided");
      router.push("/officers");
      return;
    }
    setOfficerId(id);
  }, [router]);

  useEffect(() => {
    if (!officerId) return;
    const loadData = async () => {
      await loadDistricts();
      await loadRanks();
      await loadOfficer();
    };
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [officerId]);

  useEffect(() => {
    if (selectedDistrict) {
      loadStations(selectedDistrict);
    } else {
      setStations([]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedDistrict]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!officerId) {
      alert("No officer ID provided");
      return;
    }

    // Validate mobile number format - allow "NM" or 10 digits
    const mobileUpper = formData.mobile.trim().toUpperCase();
    if (mobileUpper && mobileUpper !== "NM" && mobileUpper.length !== 10) {
      alert("Mobile number must be 10 digits or 'NM' if not provided by government");
      return;
    }

    setSaving(true);

    try {
      await updateOfficer(officerId, {
        agid: formData.agid.trim() || undefined,
        rank: formData.rank.trim(),
        name: formData.name.trim(),
        mobile: mobileUpper || undefined,
        email: formData.email.trim() || undefined,
        landline: formData.landline.trim() || undefined,
        district: formData.district,
        office: formData.office.trim() || undefined,
      });
      router.push("/officers");
    } catch (error) {
      console.error("Error updating officer:", error);
      alert("Failed to update officer");
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
      <h1 className="mb-6 text-3xl font-bold text-slate-100">Edit Officer</h1>

      <form onSubmit={handleSubmit} className="rounded-lg bg-dark-card border border-dark-border p-6 shadow-lg">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          <div>
            <label className="block text-sm font-medium text-slate-400">
              AGID
            </label>
            <input
              type="text"
              value={formData.agid}
              onChange={(e) => setFormData({ ...formData, agid: e.target.value })}
              className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-400/50"
              placeholder="Auto-generated or enter manually"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-400">
              Rank *
            </label>
            <select
              required
              value={formData.rank}
              onChange={(e) => setFormData({ ...formData, rank: e.target.value })}
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

          <div>
            <label className="block text-sm font-medium text-slate-400">
              Mobile *
            </label>
            <input
              type="text"
              required
              value={formData.mobile}
              onChange={(e) => {
                let value = e.target.value.toUpperCase();
                // If user is typing "NM", allow it
                if (value.startsWith("N") || value.startsWith("NM")) {
                  // Allow "N" or "NM" only, no digits after
                  value = value.replace(/[^NM]/g, "");
                  if (value.length > 2) value = "NM";
                } else {
                  // Allow digits only, max 10
                  value = value.replace(/[^0-9]/g, "").slice(0, 10);
                }
                setFormData({ ...formData, mobile: value });
              }}
              placeholder="Enter 10-digit number or 'NM'"
              maxLength={10}
              className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-400/50"
            />
            <p className="mt-1 text-xs text-slate-500">
              Enter 10-digit mobile number or "NM" if not provided by government
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-400">
              Email
            </label>
            <input
              type="email"
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-400/50"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-400">
              Landline
            </label>
            <input
              type="tel"
              value={formData.landline}
              onChange={(e) => setFormData({ ...formData, landline: e.target.value })}
              className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-400/50"
              placeholder="e.g., 08151-123456"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-400">
              District *
            </label>
            <select
              required
              value={selectedDistrict}
              onChange={(e) => {
                setSelectedDistrict(e.target.value);
                setFormData({ ...formData, district: e.target.value, office: "" });
              }}
              className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-400/50"
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
            <label className="block text-sm font-medium text-slate-400">
              Station
            </label>
            <select
              value={formData.office}
              onChange={(e) => setFormData({ ...formData, office: e.target.value })}
              disabled={!selectedDistrict}
              className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-400/50 disabled:bg-dark-accent-light disabled:text-slate-500"
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
        </div>

        <div className="mt-6 flex gap-4">
          <button
            type="submit"
            disabled={saving}
            className="rounded-lg bg-primary-600 px-6 py-2 text-white transition-colors hover:bg-primary-700 disabled:opacity-50"
          >
            {saving ? "Saving..." : "Update Officer"}
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

