"use client";

import { useEffect, useState } from "react";
import { getRanks, createRank, updateRank, deleteRank, Rank } from "@/lib/firebase/firestore";
import { Plus, Edit, Trash2 } from "lucide-react";

type ColumnKey = "rank_id" | "rank_label" | "staffType" | "equivalent_rank" | "category" | "requiresMetal" | "seniority_order" | "status" | "actions";

const defaultColumnWidths: Record<ColumnKey, number> = {
  rank_id: 140,
  rank_label: 280,
  staffType: 140,
  equivalent_rank: 150,
  category: 150,
  requiresMetal: 120,
  seniority_order: 120,
  status: 120,
  actions: 120,
};

export default function RanksPage() {
  const [ranks, setRanks] = useState<Rank[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingRankId, setEditingRankId] = useState<string | null>(null);
  const [formData, setFormData] = useState({ 
    rank_id: "",
    rank_label: "",
    staffType: "POLICE",
    category: "BOTH",
    equivalent_rank: "",
    seniority_order: "",
    aliases: "",
    requiresMetalNumber: false,
    isActive: true,
    remarks: ""
  });
  const [submitting, setSubmitting] = useState(false);
  const [columnWidths, setColumnWidths] = useState<Record<ColumnKey, number>>(() => {
    if (typeof window !== "undefined") {
      const saved = localStorage.getItem("rankColumnWidths");
      return saved ? { ...defaultColumnWidths, ...JSON.parse(saved) } : defaultColumnWidths;
    }
    return defaultColumnWidths;
  });
  const [resizingColumn, setResizingColumn] = useState<ColumnKey | null>(null);

  useEffect(() => {
    loadRanks();
  }, []);

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
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (rank: Rank) => {
    setEditingRankId(rank.rank_id);
    setFormData({
      rank_id: rank.rank_id,
      rank_label: rank.rank_label,
      staffType: rank.staffType || "POLICE",
      category: rank.category,
      equivalent_rank: rank.equivalent_rank,
      seniority_order: rank.seniority_order.toString(),
      aliases: rank.aliases?.join(", ") || "",
      requiresMetalNumber: rank.requiresMetalNumber,
      isActive: rank.isActive,
      remarks: rank.remarks || ""
    });
    setShowForm(true);
  };

  const handleDelete = async (rank_id: string, rank_label: string) => {
    if (!confirm(`Are you sure you want to delete "${rank_label}"?`)) return;
    try {
      await deleteRank(rank_id);
      await loadRanks();
    } catch (error) {
      console.error("Error deleting rank:", error);
      alert("Failed to delete rank");
    }
  };

  const handleCancel = () => {
    setShowForm(false);
    setEditingRankId(null);
    setFormData({ 
      rank_id: "",
      rank_label: "",
      staffType: "POLICE",
      category: "BOTH",
      equivalent_rank: "",
      seniority_order: "",
      aliases: "",
      requiresMetalNumber: false,
      isActive: true,
      remarks: ""
    });
  };

  const handleMouseDown = (e: React.MouseEvent, column: ColumnKey) => {
    e.preventDefault();
    setResizingColumn(column);
    const startX = e.pageX;
    const startWidth = columnWidths[column];

    const handleMouseMove = (e: MouseEvent) => {
      const diff = e.pageX - startX;
      const newWidth = Math.max(50, startWidth + diff);
      setColumnWidths((prev) => {
        const updated = { ...prev, [column]: newWidth };
        if (typeof window !== "undefined") {
          localStorage.setItem("rankColumnWidths", JSON.stringify(updated));
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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);

    try {
      // Parse aliases from comma-separated string
      const aliasesArray = formData.aliases
        .split(",")
        .map(a => a.trim())
        .filter(a => a.length > 0);

      const staffType = formData.staffType as "POLICE" | "MINISTERIAL";

      // Validation: equivalent_rank required only for POLICE
      if (staffType === "POLICE" && !formData.equivalent_rank.trim()) {
        alert("Equivalent Rank is required for POLICE staff.");
        setSubmitting(false);
        return;
      }

      const rankData: Rank = {
        rank_id: formData.rank_id.trim(),
        rank_label: formData.rank_label.trim(),
        staffType,
        category: formData.category,
        equivalent_rank: staffType === "POLICE" ? formData.equivalent_rank.trim() : "",
        seniority_order: parseInt(formData.seniority_order, 10),
        aliases: aliasesArray,
        requiresMetalNumber: staffType === "POLICE" ? formData.requiresMetalNumber : false,
        isActive: formData.isActive,
        remarks: formData.remarks.trim() || undefined,
      };

      if (editingRankId) {
        // Update: don't include rank_id in update data
        const { rank_id, ...updateData } = rankData;
        await updateRank(rank_id, updateData);
      } else {
        await createRank(rankData);
      }
      handleCancel();
      await loadRanks();
    } catch (error) {
      console.error("Error saving rank:", error);
      alert(`Failed to ${editingRankId ? "update" : "create"} rank: ${error instanceof Error ? error.message : "Unknown error"}`);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center bg-gradient-dark">
        <div className="text-lg text-slate-100-secondary">Loading...</div>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-3xl font-bold bg-gradient-to-r from-purple-400 to-blue-400 bg-clip-text text-transparent">Ranks</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          className="flex items-center gap-2 rounded-lg bg-gradient-to-r from-purple-600 to-blue-600 px-4 py-2 text-white transition-all hover:shadow-lg hover:shadow-purple-500/50"
        >
          <Plus className="h-5 w-5" />
          Add Rank
        </button>
      </div>

      {showForm && (
        <div className="mb-6 rounded-lg bg-dark-card border border-dark-border p-6 shadow-lg">
          <h2 className="mb-4 text-xl font-semibold text-slate-100">
            {editingRankId ? "Edit Rank" : "Add New Rank"}
          </h2>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
              <div>
                <label className="block text-sm font-medium text-slate-100-secondary">
                  Rank ID * {editingRankId && <span className="text-xs text-slate-500">(Immutable)</span>}
                </label>
                <input
                  type="text"
                  required
                  disabled={!!editingRankId}
                  value={formData.rank_id}
                  onChange={(e) => setFormData({ ...formData, rank_id: e.target.value.toUpperCase().replace(/\s+/g, "_") })}
                  className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-purple-500 focus:outline-none focus:ring-2 focus:ring-purple-500/50 disabled:opacity-50 disabled:cursor-not-allowed"
                  placeholder="e.g., DG_IGP, DSP, PSI"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-100-secondary">
                  Staff Type *
                </label>
                <select
                  required
                  value={formData.staffType}
                  onChange={(e) => setFormData({ ...formData, staffType: e.target.value as "POLICE" | "MINISTERIAL", requiresMetalNumber: e.target.value === "POLICE" ? formData.requiresMetalNumber : false })}
                  className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 focus:border-purple-500 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                >
                  <option value="POLICE">POLICE</option>
                  <option value="MINISTERIAL">MINISTERIAL</option>
                </select>
                {formData.staffType === "MINISTERIAL" && (
                  <p className="mt-1 text-xs text-slate-500">Equivalent rank not applicable; metal number disabled.</p>
                )}
              </div>
              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-slate-100-secondary">
                  Rank Label *
                </label>
                <input
                  type="text"
                  required
                  value={formData.rank_label}
                  onChange={(e) => setFormData({ ...formData, rank_label: e.target.value })}
                  className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-purple-500 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                  placeholder="e.g., Director General & Inspector General of Police"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-100-secondary">
                  Equivalent Rank *
                </label>
                <input
                  type="text"
                  required={formData.staffType === "POLICE"}
                  disabled={formData.staffType === "MINISTERIAL"}
                  value={formData.equivalent_rank}
                  onChange={(e) => setFormData({ ...formData, equivalent_rank: e.target.value.toUpperCase() })}
                  className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-purple-500 focus:outline-none focus:ring-2 focus:ring-purple-500/50 disabled:bg-dark-border/40 disabled:text-slate-500"
                  placeholder="e.g., DG, DSP, PSI"
                />
                {formData.staffType === "MINISTERIAL" && (
                  <p className="mt-1 text-xs text-slate-500">Not required for ministerial staff.</p>
                )}
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-100-secondary">
                  Category *
                </label>
                <select
                  required
                  value={formData.category}
                  onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                  className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 focus:border-purple-500 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                >
                  <option value="BOTH">BOTH</option>
                  <option value="DISTRICT">DISTRICT</option>
                  <option value="COMMISSIONERATE">COMMISSIONERATE</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-100-secondary">
                  Seniority Order *
                </label>
                <input
                  type="number"
                  required
                  min="1"
                  value={formData.seniority_order}
                  onChange={(e) => setFormData({ ...formData, seniority_order: e.target.value })}
                  className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-purple-500 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                  placeholder="1 = highest"
                />
              </div>
              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-slate-100-secondary">
                  Aliases (comma-separated)
                </label>
                <input
                  type="text"
                  value={formData.aliases}
                  onChange={(e) => setFormData({ ...formData, aliases: e.target.value })}
                  className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-purple-500 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                  placeholder="e.g., DYSP, SDPO, PSIW, WPSI"
                />
                <p className="mt-1 text-xs text-slate-500">
                  Separate multiple aliases with commas
                </p>
              </div>
              <div className="flex items-end">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={formData.staffType === "POLICE" ? formData.requiresMetalNumber : false}
                    onChange={(e) => setFormData({ ...formData, requiresMetalNumber: e.target.checked })}
                    disabled={formData.staffType !== "POLICE"}
                    className="w-5 h-5 rounded border-dark-border bg-dark-sidebar text-purple-600 focus:ring-purple-500 focus:ring-2 disabled:opacity-50"
                  />
                  <span className="text-sm font-medium text-slate-100-secondary">
                    Requires Metal Number
                  </span>
                </label>
              </div>
              <div className="flex items-end">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={formData.isActive}
                    onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
                    className="w-5 h-5 rounded border-dark-border bg-dark-sidebar text-purple-600 focus:ring-purple-500 focus:ring-2"
                  />
                  <span className="text-sm font-medium text-slate-100-secondary">
                    Is Active
                  </span>
                </label>
              </div>
              <div className="md:col-span-3">
                <label className="block text-sm font-medium text-slate-100-secondary">
                  Remarks
                </label>
                <textarea
                  value={formData.remarks}
                  onChange={(e) => setFormData({ ...formData, remarks: e.target.value })}
                  className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-purple-500 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                  rows={2}
                  placeholder="Optional admin notes"
                />
              </div>
            </div>
            <div className="mt-4 flex gap-4">
              <button
                type="submit"
                disabled={submitting}
                className="rounded-lg bg-gradient-to-r from-purple-600 to-blue-600 px-6 py-2 text-white transition-all hover:shadow-lg hover:shadow-purple-500/50 disabled:opacity-50"
              >
                {submitting ? "Saving..." : "Save Rank"}
              </button>
              <button
                type="button"
                onClick={handleCancel}
                className="rounded-lg border border-dark-border px-6 py-2 text-slate-100-secondary transition-colors hover:bg-dark-sidebar hover:text-slate-100"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="overflow-x-auto rounded-lg bg-dark-card border border-dark-border shadow-lg" style={{ overflowX: 'scroll', WebkitOverflowScrolling: 'touch' }}>
        <table className="w-full" style={{ tableLayout: 'fixed' }}>
          <thead className="bg-dark-sidebar border-b border-dark-border">
            <tr>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-100-secondary relative"
                style={{ width: columnWidths.rank_id }}
              >
                Rank ID
                <div
                  onMouseDown={(e) => handleMouseDown(e, "rank_id")}
                  style={{ cursor: resizingColumn === "rank_id" ? "col-resize" : "col-resize" }}
                  className="absolute top-0 right-0 w-1 h-full cursor-col-resize hover:bg-purple-500"
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-100-secondary relative"
                style={{ width: columnWidths.rank_label }}
              >
                Rank Label
                <div
                  onMouseDown={(e) => handleMouseDown(e, "rank_label")}
                  style={{ cursor: resizingColumn === "rank_label" ? "col-resize" : "col-resize" }}
                  className="absolute top-0 right-0 w-1 h-full cursor-col-resize hover:bg-purple-500"
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-100-secondary relative"
                style={{ width: columnWidths.staffType }}
              >
                Staff
                <div
                  onMouseDown={(e) => handleMouseDown(e, "staffType")}
                  style={{ cursor: resizingColumn === "staffType" ? "col-resize" : "col-resize" }}
                  className="absolute top-0 right-0 w-1 h-full cursor-col-resize hover:bg-purple-500"
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-100-secondary relative"
                style={{ width: columnWidths.equivalent_rank }}
              >
                Equivalent
                <div
                  onMouseDown={(e) => handleMouseDown(e, "equivalent_rank")}
                  style={{ cursor: resizingColumn === "equivalent_rank" ? "col-resize" : "col-resize" }}
                  className="absolute top-0 right-0 w-1 h-full cursor-col-resize hover:bg-purple-500"
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-100-secondary relative"
                style={{ width: columnWidths.category }}
              >
                Category
                <div
                  onMouseDown={(e) => handleMouseDown(e, "category")}
                  style={{ cursor: resizingColumn === "category" ? "col-resize" : "col-resize" }}
                  className="absolute top-0 right-0 w-1 h-full cursor-col-resize hover:bg-purple-500"
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-100-secondary relative"
                style={{ width: columnWidths.requiresMetal }}
              >
                Metal #
                <div
                  onMouseDown={(e) => handleMouseDown(e, "requiresMetal")}
                  style={{ cursor: resizingColumn === "requiresMetal" ? "col-resize" : "col-resize" }}
                  className="absolute top-0 right-0 w-1 h-full cursor-col-resize hover:bg-purple-500"
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-100-secondary relative"
                style={{ width: columnWidths.seniority_order }}
              >
                Order
                <div
                  onMouseDown={(e) => handleMouseDown(e, "seniority_order")}
                  style={{ cursor: resizingColumn === "seniority_order" ? "col-resize" : "col-resize" }}
                  className="absolute top-0 right-0 w-1 h-full cursor-col-resize hover:bg-purple-500"
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-100-secondary relative"
                style={{ width: columnWidths.status }}
              >
                Status
                <div
                  onMouseDown={(e) => handleMouseDown(e, "status")}
                  style={{ cursor: resizingColumn === "status" ? "col-resize" : "col-resize" }}
                  className="absolute top-0 right-0 w-1 h-full cursor-col-resize hover:bg-purple-500"
                />
              </th>
              <th 
                className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-slate-100-secondary relative"
                style={{ width: columnWidths.actions }}
              >
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-dark-border bg-dark-card">
            {ranks.map((rank) => (
              <tr key={rank.rank_id} className="hover:bg-dark-sidebar transition-colors">
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-100-secondary overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.rank_id }}>
                  {rank.rank_id}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-slate-100 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.rank_label }}>
                  {rank.rank_label}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-100-secondary overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.staffType }}>
                  {rank.staffType || "POLICE"}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-100-secondary overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.equivalent_rank }}>
                  {rank.equivalent_rank}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-100-secondary overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.category }}>
                  {rank.category}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-100-secondary overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.requiresMetal }}>
                  {rank.requiresMetalNumber ? (
                    <span className="inline-flex rounded-full bg-amber-500/20 px-2 text-xs font-semibold text-amber-400">
                      Yes
                    </span>
                  ) : (
                    <span className="text-slate-500">No</span>
                  )}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-100-secondary overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.seniority_order }}>
                  {rank.seniority_order}
                </td>
                <td className="whitespace-nowrap px-6 py-4 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.status }}>
                  {rank.isActive ? (
                    <span className="inline-flex rounded-full bg-green-500/20 px-2 text-xs font-semibold text-green-400">
                      Active
                    </span>
                  ) : (
                    <span className="inline-flex rounded-full bg-red-500/20 px-2 text-xs font-semibold text-red-400">
                      Inactive
                    </span>
                  )}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-right text-sm font-medium">
                  <div className="flex items-center justify-end gap-2">
                    <button
                      onClick={() => handleEdit(rank)}
                      className="text-purple-400 hover:text-purple-300 transition-colors"
                      title="Edit"
                    >
                      <Edit className="h-5 w-5" />
                    </button>
                    <button
                      onClick={() => handleDelete(rank.rank_id, rank.rank_label)}
                      className="text-red-400 hover:text-red-300 transition-colors"
                      title="Delete"
                    >
                      <Trash2 className="h-5 w-5" />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {ranks.length === 0 && (
          <div className="py-12 text-center text-slate-100-secondary">
            No ranks found
          </div>
        )}
      </div>
    </div>
  );
}
