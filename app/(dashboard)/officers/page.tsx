"use client";

import { useEffect, useState } from "react";
import { getOfficers, createOfficer, deleteOfficer, Officer, getRanks, Rank } from "@/lib/firebase/firestore";
import { Plus, Trash2, Edit, ChevronUp, ChevronDown } from "lucide-react";
import { getDistricts, getStations, District, Station } from "@/lib/firebase/firestore";
import Link from "next/link";

type SortField = "rank" | "agid" | "name" | "mobile" | "email" | "landline" | "district" | "office";
type SortDirection = "asc" | "desc";
type ColumnKey = "rank" | "agid" | "name" | "mobile" | "email" | "landline" | "district" | "office" | "actions";

const defaultOfficerColumnWidths: Record<ColumnKey, number> = {
  rank: 100,
  agid: 120,
  name: 200,
  mobile: 120,
  email: 200,
  landline: 120,
  district: 150,
  office: 150,
  actions: 100,
};

export default function OfficersPage() {
  const [officers, setOfficers] = useState<Officer[]>([]);
  const [districts, setDistricts] = useState<District[]>([]);
  const [stations, setStations] = useState<Station[]>([]);
  const [ranks, setRanks] = useState<Rank[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [selectedDistrict, setSelectedDistrict] = useState("");
  const [sortField, setSortField] = useState<SortField>("name");
  const [sortDirection, setSortDirection] = useState<SortDirection>("asc");
  const [columnWidths, setColumnWidths] = useState<Record<ColumnKey, number>>(() => {
    if (typeof window !== "undefined") {
      const saved = localStorage.getItem("officerColumnWidths");
      return saved ? { ...defaultOfficerColumnWidths, ...JSON.parse(saved) } : defaultOfficerColumnWidths;
    }
    return defaultOfficerColumnWidths;
  });
  const [resizingColumn, setResizingColumn] = useState<ColumnKey | null>(null);
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
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    if (selectedDistrict) {
      loadStations(selectedDistrict);
    } else {
      setStations([]);
    }
  }, [selectedDistrict]);

  const loadData = async () => {
    try {
      const [officersData, districtsData, ranksData] = await Promise.all([
        getOfficers(),
        getDistricts(),
        getRanks(),
      ]);
      console.log("Loaded districts:", districtsData);
      setOfficers(officersData);
      setDistricts(districtsData);
      setRanks(ranksData);
      if (districtsData.length === 0) {
        console.warn("No districts found. Make sure districts exist in Firestore.");
      }
    } catch (error) {
      console.error("Error loading data:", error);
      alert("Failed to load data. Please check the console for details.");
    } finally {
      setLoading(false);
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

  const handleDelete = async (id: string) => {
    if (!confirm("Are you sure you want to delete this officer?")) return;
    try {
      await deleteOfficer(id);
      await loadData();
    } catch (error) {
      console.error("Error deleting officer:", error);
      alert("Failed to delete officer");
    }
  };

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection(sortDirection === "asc" ? "desc" : "asc");
    } else {
      setSortField(field);
      setSortDirection("asc");
    }
  };

  const handleMouseDown = (e: React.MouseEvent, column: ColumnKey) => {
    e.preventDefault();
    setResizingColumn(column);
    const startX = e.pageX;
    const startWidth = columnWidths[column];

    const handleMouseMove = (e: MouseEvent) => {
      const diff = e.pageX - startX;
      const newWidth = Math.max(50, startWidth + diff); // Minimum width of 50px
      setColumnWidths((prev) => {
        const updated = { ...prev, [column]: newWidth };
        if (typeof window !== "undefined") {
          localStorage.setItem("officerColumnWidths", JSON.stringify(updated));
        }
        return updated;
      });
    };

    const handleMouseUp = () => {
      setResizingColumn(null);
      document.removeEventListener("mousemove", handleMouseMove);
      document.removeEventListener("mouseup", handleMouseUp);
    };

    document.addEventListener("mousemove", handleMouseMove);
    document.addEventListener("mouseup", handleMouseUp);
  };

  const sortedOfficers = [...officers].sort((a, b) => {
    let aValue: string = "";
    let bValue: string = "";

    switch (sortField) {
      case "rank":
        aValue = a.rank || "";
        bValue = b.rank || "";
        break;
      case "agid":
        aValue = a.agid || "";
        bValue = b.agid || "";
        break;
      case "name":
        aValue = a.name || "";
        bValue = b.name || "";
        break;
      case "mobile":
        aValue = a.mobile || "";
        bValue = b.mobile || "";
        break;
      case "email":
        aValue = a.email || "";
        bValue = b.email || "";
        break;
      case "landline":
        aValue = a.landline || "";
        bValue = b.landline || "";
        break;
      case "district":
        aValue = a.district || "";
        bValue = b.district || "";
        break;
      case "office":
        aValue = (a as any).station || a.office || "";
        bValue = (b as any).station || b.office || "";
        break;
    }

    const comparison = aValue.localeCompare(bValue);
    return sortDirection === "asc" ? comparison : -comparison;
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Validate mobile number format - allow "NM" or 10 digits
    const mobileUpper = formData.mobile.trim().toUpperCase();
    if (mobileUpper && mobileUpper !== "NM" && mobileUpper.length !== 10) {
      alert("Mobile number must be 10 digits or 'NM' if not provided by government");
      return;
    }
    
    setSubmitting(true);

    try {
      await createOfficer({
        agid: formData.agid.trim() || undefined,
        rank: formData.rank.trim(),
        name: formData.name.trim(),
        mobile: mobileUpper || undefined,
        email: formData.email.trim() || undefined,
        landline: formData.landline.trim() || undefined,
        district: formData.district,
        office: formData.office.trim() || undefined,
      });
      setFormData({
        agid: "",
        rank: "",
        name: "",
        mobile: "",
        email: "",
        landline: "",
        district: "",
        office: "",
      });
      setSelectedDistrict("");
      setStations([]);
      setShowForm(false);
      await loadData();
    } catch (error) {
      console.error("Error creating officer:", error);
      alert("Failed to create officer");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-lg text-slate-400">Loading...</div>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-3xl font-bold text-slate-100">Officers</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          className="flex items-center gap-2 rounded-lg bg-primary-600 px-4 py-2 text-white transition-colors hover:bg-primary-700"
        >
          <Plus className="h-5 w-5" />
          Add Officer
        </button>
      </div>

      {showForm && (
        <div className="mb-6 rounded-lg bg-dark-card border border-dark-border p-6 shadow-lg">
          <h2 className="mb-4 text-xl font-semibold text-slate-100">Add New Officer</h2>
          <form onSubmit={handleSubmit}>
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
                  <option value="">Select Station</option>
                  {stations.map((s) => (
                    <option key={s.id} value={s.name}>
                      {s.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <div className="mt-4 flex gap-4">
              <button
                type="submit"
                disabled={submitting}
                className="rounded-lg bg-primary-600 px-6 py-2 text-white transition-colors hover:bg-primary-700 disabled:opacity-50"
              >
                {submitting ? "Saving..." : "Save Officer"}
              </button>
              <button
                type="button"
                onClick={() => {
                  setShowForm(false);
                  setFormData({
                    agid: "",
                    rank: "",
                    name: "",
                    mobile: "",
                    email: "",
                    landline: "",
                    district: "",
                    office: "",
                  });
                  setSelectedDistrict("");
                  setStations([]);
                }}
                className="rounded-lg border border-dark-border px-6 py-2 text-slate-400 transition-colors hover:bg-dark-sidebar hover:text-slate-100"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="overflow-x-auto rounded-lg bg-dark-card border border-dark-border shadow-lg" style={{ overflowX: 'scroll', WebkitOverflowScrolling: 'touch' }}>
        <table className="w-full" style={{ tableLayout: 'fixed', minWidth: '1200px' }}>
          <thead className="bg-dark-sidebar border-b border-dark-border">
            <tr>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-400 cursor-pointer hover:bg-dark-card select-none relative"
                onClick={() => handleSort("agid")}
                style={{ width: columnWidths.agid }}
              >
                <div className="flex items-center gap-1">
                  AGID
                  {sortField === "agid" && (
                    sortDirection === "asc" ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />
                  )}
                </div>
                <div
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-primary-500 bg-transparent"
                  onMouseDown={(e) => {
                    e.stopPropagation();
                    handleMouseDown(e, "agid");
                  }}
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-400 cursor-pointer hover:bg-dark-card select-none relative"
                onClick={() => handleSort("rank")}
                style={{ width: columnWidths.rank }}
              >
                <div className="flex items-center gap-1">
                  Rank
                  {sortField === "rank" && (
                    sortDirection === "asc" ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />
                  )}
                </div>
                <div
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-primary-500 bg-transparent"
                  onMouseDown={(e) => {
                    e.stopPropagation();
                    handleMouseDown(e, "rank");
                  }}
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-400 cursor-pointer hover:bg-dark-card select-none relative"
                onClick={() => handleSort("name")}
                style={{ width: columnWidths.name }}
              >
                <div className="flex items-center gap-1">
                  Name
                  {sortField === "name" && (
                    sortDirection === "asc" ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />
                  )}
                </div>
                <div
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-primary-500 bg-transparent"
                  onMouseDown={(e) => {
                    e.stopPropagation();
                    handleMouseDown(e, "name");
                  }}
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-400 cursor-pointer hover:bg-dark-card select-none relative"
                onClick={() => handleSort("mobile")}
                style={{ width: columnWidths.mobile }}
              >
                <div className="flex items-center gap-1">
                  Mobile
                  {sortField === "mobile" && (
                    sortDirection === "asc" ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />
                  )}
                </div>
                <div
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-primary-500 bg-transparent"
                  onMouseDown={(e) => {
                    e.stopPropagation();
                    handleMouseDown(e, "mobile");
                  }}
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-400 cursor-pointer hover:bg-dark-card select-none relative"
                onClick={() => handleSort("email")}
                style={{ width: columnWidths.email }}
              >
                <div className="flex items-center gap-1">
                  Email
                  {sortField === "email" && (
                    sortDirection === "asc" ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />
                  )}
                </div>
                <div
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-primary-500 bg-transparent"
                  onMouseDown={(e) => {
                    e.stopPropagation();
                    handleMouseDown(e, "email");
                  }}
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-400 cursor-pointer hover:bg-dark-card select-none relative"
                onClick={() => handleSort("landline")}
                style={{ width: columnWidths.landline }}
              >
                <div className="flex items-center gap-1">
                  Landline
                  {sortField === "landline" && (
                    sortDirection === "asc" ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />
                  )}
                </div>
                <div
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-primary-500 bg-transparent"
                  onMouseDown={(e) => {
                    e.stopPropagation();
                    handleMouseDown(e, "landline");
                  }}
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-400 cursor-pointer hover:bg-dark-card select-none relative"
                onClick={() => handleSort("district")}
                style={{ width: columnWidths.district }}
              >
                <div className="flex items-center gap-1">
                  District
                  {sortField === "district" && (
                    sortDirection === "asc" ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />
                  )}
                </div>
                <div
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-primary-500 bg-transparent"
                  onMouseDown={(e) => {
                    e.stopPropagation();
                    handleMouseDown(e, "district");
                  }}
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-400 cursor-pointer hover:bg-dark-card select-none relative"
                onClick={() => handleSort("office")}
                style={{ width: columnWidths.office }}
              >
                <div className="flex items-center gap-1">
                  Station
                  {sortField === "office" && (
                    sortDirection === "asc" ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />
                  )}
                </div>
                <div
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-primary-500 bg-transparent"
                  onMouseDown={(e) => {
                    e.stopPropagation();
                    handleMouseDown(e, "office");
                  }}
                />
              </th>
              <th 
                className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-slate-400 relative"
                style={{ width: columnWidths.actions }}
              >
                Actions
                <div
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-primary-500 bg-transparent"
                  onMouseDown={(e) => handleMouseDown(e, "actions")}
                />
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-dark-border bg-dark-card">
            {sortedOfficers.map((officer) => (
              <tr key={officer.id} className="hover:bg-dark-sidebar transition-colors">
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-100 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.agid }}>
                  {officer.agid || "N/A"}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-slate-100">
                  {officer.rank}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-100 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.name }}>
                  {officer.name}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-400 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.mobile }}>
                  {officer.mobile}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-400 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.email }}>
                  {officer.email || "N/A"}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-400 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.landline }}>
                  {officer.landline || "N/A"}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-400 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.district }}>
                  {officer.district}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-400 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.office }}>
                  {(officer as any).station || officer.office || "N/A"}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-right text-sm font-medium">
                  <div className="flex items-center justify-end gap-2">
                    <Link
                      href={`/officers/edit?id=${officer.id}`}
                      className="text-primary-400 hover:text-primary-300"
                    >
                      <Edit className="h-5 w-5" />
                    </Link>
                    <button
                      onClick={() => handleDelete(officer.id!)}
                      className="text-red-400 hover:text-red-300"
                    >
                      <Trash2 className="h-5 w-5" />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {sortedOfficers.length === 0 && (
          <div className="py-12 text-center text-slate-400">
            No officers found
          </div>
        )}
      </div>
    </div>
  );
}

