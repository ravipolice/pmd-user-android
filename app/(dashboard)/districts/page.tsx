"use client";

import { useEffect, useState } from "react";
import { getDistricts, createDistrict, updateDistrict, deleteDistrict, District } from "@/lib/firebase/firestore";
import { Plus, Edit, Trash2 } from "lucide-react";

type ColumnKey = "name" | "range" | "status" | "actions";

const defaultColumnWidths: Record<ColumnKey, number> = {
  name: 250,
  range: 200,
  status: 120,
  actions: 120,
};

export default function DistrictsPage() {
  const [districts, setDistricts] = useState<District[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formData, setFormData] = useState({ name: "", range: "" });
  const [submitting, setSubmitting] = useState(false);
  const [columnWidths, setColumnWidths] = useState<Record<ColumnKey, number>>(() => {
    if (typeof window !== "undefined") {
      const saved = localStorage.getItem("districtColumnWidths");
      return saved ? { ...defaultColumnWidths, ...JSON.parse(saved) } : defaultColumnWidths;
    }
    return defaultColumnWidths;
  });
  const [resizingColumn, setResizingColumn] = useState<ColumnKey | null>(null);

  useEffect(() => {
    loadDistricts();
  }, []);

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
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (district: District) => {
    setEditingId(district.id || null);
    setFormData({
      name: district.name,
      range: district.range || "",
    });
    setShowForm(true);
  };

  const handleDelete = async (id: string, name: string) => {
    if (!confirm(`Are you sure you want to delete "${name}"?`)) return;
    try {
      await deleteDistrict(id);
      await loadDistricts();
    } catch (error) {
      console.error("Error deleting district:", error);
      alert("Failed to delete district");
    }
  };

  const handleCancel = () => {
    setShowForm(false);
    setEditingId(null);
    setFormData({ name: "", range: "" });
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
          localStorage.setItem("districtColumnWidths", JSON.stringify(updated));
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
      if (editingId) {
        await updateDistrict(editingId, {
          name: formData.name.trim(),
          range: formData.range.trim() || undefined,
        });
      } else {
        await createDistrict({
          name: formData.name.trim(),
          range: formData.range.trim() || undefined,
        });
      }
      handleCancel();
      await loadDistricts();
    } catch (error) {
      console.error("Error saving district:", error);
      alert(`Failed to ${editingId ? "update" : "create"} district`);
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
        <h1 className="text-3xl font-bold bg-gradient-to-r from-purple-400 to-blue-400 bg-clip-text text-transparent">Districts</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          className="flex items-center gap-2 rounded-lg bg-gradient-to-r from-purple-600 to-blue-600 px-4 py-2 text-white transition-all hover:shadow-lg hover:shadow-purple-500/50"
        >
          <Plus className="h-5 w-5" />
          Add District
        </button>
      </div>

      {showForm && (
        <div className="mb-6 rounded-lg bg-dark-card border border-dark-border p-6 shadow-lg">
          <h2 className="mb-4 text-xl font-semibold text-slate-100">
            {editingId ? "Edit District" : "Add New District"}
          </h2>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-slate-100-secondary">
                  Name *
                </label>
                <input
                  type="text"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-purple-500 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-100-secondary">
                  Range
                </label>
                <input
                  type="text"
                  value={formData.range}
                  onChange={(e) => setFormData({ ...formData, range: e.target.value })}
                  className="mt-1 block w-full rounded-md bg-dark-sidebar border border-dark-border px-3 py-2 text-slate-100 placeholder-slate-400 focus:border-purple-500 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                />
              </div>
            </div>
            <div className="mt-4 flex gap-4">
              <button
                type="submit"
                disabled={submitting}
                className="rounded-lg bg-gradient-to-r from-purple-600 to-blue-600 px-6 py-2 text-white transition-all hover:shadow-lg hover:shadow-purple-500/50 disabled:opacity-50"
              >
                {submitting ? "Saving..." : "Save District"}
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
                style={{ width: columnWidths.name }}
              >
                Name
                <div
                  onMouseDown={(e) => handleMouseDown(e, "name")}
                  style={{ cursor: resizingColumn === "name" ? "col-resize" : "col-resize" }}
                  className="absolute top-0 right-0 w-1 h-full cursor-col-resize hover:bg-purple-500"
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-100-secondary relative"
                style={{ width: columnWidths.range }}
              >
                Range
                <div
                  onMouseDown={(e) => handleMouseDown(e, "range")}
                  style={{ cursor: resizingColumn === "range" ? "col-resize" : "col-resize" }}
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
            {districts.map((district) => (
              <tr key={district.id} className="hover:bg-dark-sidebar transition-colors">
                <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-slate-100 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.name }}>
                  {district.name}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-100-secondary overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.range }}>
                  {district.range || "N/A"}
                </td>
                <td className="whitespace-nowrap px-6 py-4 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.status }}>
                  <span className="inline-flex rounded-full bg-green-500/20 px-2 text-xs font-semibold text-green-400">
                    Active
                  </span>
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-right text-sm font-medium">
                  <div className="flex items-center justify-end gap-2">
                    <button
                      onClick={() => handleEdit(district)}
                      className="text-purple-400 hover:text-purple-300 transition-colors"
                      title="Edit"
                    >
                      <Edit className="h-5 w-5" />
                    </button>
                    <button
                      onClick={() => handleDelete(district.id!, district.name)}
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
        {districts.length === 0 && (
          <div className="py-12 text-center text-slate-100-secondary">
            No districts found
          </div>
        )}
      </div>
    </div>
  );
}

