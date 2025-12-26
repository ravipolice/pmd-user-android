"use client";

import { useEffect, useState } from "react";
import { getEmployees, deleteEmployee, Employee } from "@/lib/firebase/firestore";
import { Plus, Trash2, Edit, Search, ChevronUp, ChevronDown } from "lucide-react";
import Link from "next/link";
import { formatDate } from "@/lib/utils";

type SortField = "kgid" | "name" | "rank" | "email" | "mobile1" | "mobile2" | "bloodGroup" | "district" | "station" | "isApproved";
type SortDirection = "asc" | "desc";

type ColumnKey = "photo" | "kgid" | "name" | "rank" | "email" | "mobile1" | "mobile2" | "bloodGroup" | "district" | "station" | "status" | "actions";

const defaultColumnWidths: Record<ColumnKey, number> = {
  photo: 80,
  kgid: 120,
  name: 200,
  rank: 100,
  email: 200,
  mobile1: 120,
  mobile2: 120,
  bloodGroup: 120,
  district: 150,
  station: 150,
  status: 100,
  actions: 100,
};

export default function EmployeesPage() {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [sortField, setSortField] = useState<SortField>("name");
  const [sortDirection, setSortDirection] = useState<SortDirection>("asc");
  const [columnWidths, setColumnWidths] = useState<Record<ColumnKey, number>>(() => {
    if (typeof window !== "undefined") {
      const saved = localStorage.getItem("employeeColumnWidths");
      return saved ? { ...defaultColumnWidths, ...JSON.parse(saved) } : defaultColumnWidths;
    }
    return defaultColumnWidths;
  });
  const [resizingColumn, setResizingColumn] = useState<ColumnKey | null>(null);

  useEffect(() => {
    loadEmployees();
  }, []);

  const loadEmployees = async () => {
    try {
      const data = await getEmployees();
      setEmployees(data);
    } catch (error) {
      console.error("Error loading employees:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Are you sure you want to delete this employee?")) return;
    try {
      await deleteEmployee(id);
      await loadEmployees();
    } catch (error) {
      console.error("Error deleting employee:", error);
      alert("Failed to delete employee");
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
          localStorage.setItem("employeeColumnWidths", JSON.stringify(updated));
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

  const filteredEmployees = employees
    .filter(
      (emp) =>
        emp.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        emp.kgid.toLowerCase().includes(searchTerm.toLowerCase()) ||
        emp.email?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        emp.mobile1?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        emp.mobile2?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        emp.rank?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        emp.displayRank?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        emp.bloodGroup?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        emp.district.toLowerCase().includes(searchTerm.toLowerCase()) ||
        emp.station.toLowerCase().includes(searchTerm.toLowerCase())
    )
    .sort((a, b) => {
      let aValue: string | number | boolean = "";
      let bValue: string | number | boolean = "";

      switch (sortField) {
        case "kgid":
          aValue = a.kgid || "";
          bValue = b.kgid || "";
          break;
        case "name":
          aValue = a.name || "";
          bValue = b.name || "";
          break;
        case "rank":
          aValue = a.displayRank || a.rank || "";
          bValue = b.displayRank || b.rank || "";
          break;
        case "email":
          aValue = a.email || "";
          bValue = b.email || "";
          break;
        case "mobile1":
          aValue = a.mobile1 || "";
          bValue = b.mobile1 || "";
          break;
        case "mobile2":
          aValue = a.mobile2 || "";
          bValue = b.mobile2 || "";
          break;
        case "bloodGroup":
          aValue = a.bloodGroup || "";
          bValue = b.bloodGroup || "";
          break;
        case "district":
          aValue = a.district || "";
          bValue = b.district || "";
          break;
        case "station":
          aValue = a.station || "";
          bValue = b.station || "";
          break;
        case "isApproved":
          aValue = a.isApproved ? 1 : 0;
          bValue = b.isApproved ? 1 : 0;
          break;
      }

      if (typeof aValue === "string" && typeof bValue === "string") {
        const comparison = aValue.localeCompare(bValue);
        return sortDirection === "asc" ? comparison : -comparison;
      } else {
        const comparison = (aValue as number) - (bValue as number);
        return sortDirection === "asc" ? comparison : -comparison;
      }
    });

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center bg-gradient-dark">
        <div className="text-lg text-slate-400">Loading...</div>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-3xl font-bold bg-gradient-to-r from-purple-400 to-blue-400 bg-clip-text text-transparent">Employees</h1>
        <Link
          href="/employees/new"
          className="flex items-center gap-2 rounded-lg bg-gradient-to-r from-purple-600 to-blue-600 px-4 py-2 text-white transition-all hover:shadow-lg hover:shadow-purple-500/50"
        >
          <Plus className="h-5 w-5" />
          Add Employee
        </Link>
      </div>

      <div className="mb-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search employees..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full rounded-lg bg-dark-card border border-dark-border py-2 pl-10 pr-4 text-slate-100 placeholder-slate-400 focus:border-purple-500 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
          />
        </div>
      </div>

      <div className="overflow-x-auto rounded-lg bg-dark-card border border-dark-border shadow-lg" style={{ overflowX: 'scroll', WebkitOverflowScrolling: 'touch' }}>
        <table className="w-full" style={{ tableLayout: 'fixed', minWidth: '1200px' }}>
          <thead className="bg-dark-sidebar border-b border-dark-border">
            <tr>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-400 relative"
                style={{ width: columnWidths.photo }}
              >
                Photo
                <div
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-purple-500 bg-transparent"
                  onMouseDown={(e) => handleMouseDown(e, "photo")}
                  style={{ cursor: resizingColumn === "photo" ? "col-resize" : "col-resize" }}
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-400 cursor-pointer hover:bg-dark-card select-none relative"
                onClick={() => handleSort("kgid")}
                style={{ width: columnWidths.kgid }}
              >
                <div className="flex items-center gap-1">
                  KGID
                  {sortField === "kgid" && (
                    sortDirection === "asc" ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />
                  )}
                </div>
                <div
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-purple-500 bg-transparent"
                  onMouseDown={(e) => {
                    e.stopPropagation();
                    handleMouseDown(e, "kgid");
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
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-purple-500 bg-transparent"
                  onMouseDown={(e) => {
                    e.stopPropagation();
                    handleMouseDown(e, "name");
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
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-purple-500 bg-transparent"
                  onMouseDown={(e) => {
                    e.stopPropagation();
                    handleMouseDown(e, "rank");
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
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-purple-500 bg-transparent"
                  onMouseDown={(e) => {
                    e.stopPropagation();
                    handleMouseDown(e, "email");
                  }}
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-400 cursor-pointer hover:bg-dark-card select-none relative"
                onClick={() => handleSort("mobile1")}
                style={{ width: columnWidths.mobile1 }}
              >
                <div className="flex items-center gap-1">
                  Mobile 1
                  {sortField === "mobile1" && (
                    sortDirection === "asc" ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />
                  )}
                </div>
                <div
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-purple-500 bg-transparent"
                  onMouseDown={(e) => {
                    e.stopPropagation();
                    handleMouseDown(e, "mobile1");
                  }}
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-400 cursor-pointer hover:bg-dark-card select-none relative"
                onClick={() => handleSort("mobile2")}
                style={{ width: columnWidths.mobile2 }}
              >
                <div className="flex items-center gap-1">
                  Mobile 2
                  {sortField === "mobile2" && (
                    sortDirection === "asc" ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />
                  )}
                </div>
                <div
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-purple-500 bg-transparent"
                  onMouseDown={(e) => {
                    e.stopPropagation();
                    handleMouseDown(e, "mobile2");
                  }}
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-400 cursor-pointer hover:bg-dark-card select-none relative"
                onClick={() => handleSort("bloodGroup")}
                style={{ width: columnWidths.bloodGroup }}
              >
                <div className="flex items-center gap-1">
                  Blood Group
                  {sortField === "bloodGroup" && (
                    sortDirection === "asc" ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />
                  )}
                </div>
                <div
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-purple-500 bg-transparent"
                  onMouseDown={(e) => {
                    e.stopPropagation();
                    handleMouseDown(e, "bloodGroup");
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
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-purple-500 bg-transparent"
                  onMouseDown={(e) => {
                    e.stopPropagation();
                    handleMouseDown(e, "district");
                  }}
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-400 cursor-pointer hover:bg-dark-card select-none relative"
                onClick={() => handleSort("station")}
                style={{ width: columnWidths.station }}
              >
                <div className="flex items-center gap-1">
                  Station
                  {sortField === "station" && (
                    sortDirection === "asc" ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />
                  )}
                </div>
                <div
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-purple-500 bg-transparent"
                  onMouseDown={(e) => {
                    e.stopPropagation();
                    handleMouseDown(e, "station");
                  }}
                />
              </th>
              <th 
                className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-400 cursor-pointer hover:bg-dark-card select-none relative"
                onClick={() => handleSort("isApproved")}
                style={{ width: columnWidths.status }}
              >
                <div className="flex items-center gap-1">
                  Status
                  {sortField === "isApproved" && (
                    sortDirection === "asc" ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />
                  )}
                </div>
                <div
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-purple-500 bg-transparent"
                  onMouseDown={(e) => {
                    e.stopPropagation();
                    handleMouseDown(e, "status");
                  }}
                />
              </th>
              <th 
                className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500 relative"
                style={{ width: columnWidths.actions }}
              >
                Actions
                <div
                  className="absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-purple-500 bg-transparent"
                  onMouseDown={(e) => handleMouseDown(e, "actions")}
                />
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-dark-border bg-dark-card">
            {filteredEmployees.map((employee) => (
              <tr key={employee.id} className="hover:bg-dark-sidebar transition-colors">
                <td className="whitespace-nowrap px-6 py-4">
                  <div className="flex items-center">
                    {employee.photoUrl || employee.photoUrlFromGoogle ? (
                      <img
                        src={employee.photoUrl || employee.photoUrlFromGoogle || ""}
                        alt={employee.name}
                        className="h-12 w-12 rounded-full object-cover"
                        loading="lazy"
                        referrerPolicy="no-referrer"
                        onError={(e) => {
                          const img = e.currentTarget;
                          if (!img.dataset.failed) {
                            img.dataset.failed = "1";
                            const photoUrl = employee.photoUrl || employee.photoUrlFromGoogle || "";
                            
                            // Try to extract file ID from Google Drive URL
                            let fileId: string | null = null;
                            
                            // Pattern 1: lh3.googleusercontent.com/d/FILE_ID
                            if (photoUrl.includes("lh3.googleusercontent.com")) {
                              const cdnMatch = photoUrl.match(/\/d\/([-\w]{25,})/);
                              if (cdnMatch) fileId = cdnMatch[1];
                            }
                            
                            // Pattern 2: drive.google.com/uc?id=FILE_ID or /uc?export=view&id=FILE_ID
                            if (!fileId && photoUrl.includes("drive.google.com")) {
                              const driveMatch = photoUrl.match(/[?&]id=([-\w]{25,})/);
                              if (driveMatch) fileId = driveMatch[1];
                            }
                            
                            // Pattern 3: /file/d/FILE_ID/
                            if (!fileId && photoUrl.includes("/file/d/")) {
                              const fileMatch = photoUrl.match(/\/file\/d\/([-\w]{25,})/);
                              if (fileMatch) fileId = fileMatch[1];
                            }
                            
                            // Try fallback URLs if we have a file ID
                            if (fileId) {
                              if (!img.dataset.retried) {
                                // First retry: Try Drive thumbnail API
                                img.dataset.retried = "1";
                                img.src = `https://drive.google.com/thumbnail?id=${fileId}&sz=w200`;
                                return;
                              } else if (!img.dataset.retried2) {
                                // Second retry: Try standard Drive view URL
                                img.dataset.retried2 = "1";
                                img.src = `https://drive.google.com/uc?export=view&id=${fileId}`;
                                return;
                              }
                            }
                            
                            // Final fallback: placeholder with initial
                            img.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='48' height='48'%3E%3Ccircle cx='24' cy='24' r='24' fill='%23e5e7eb'/%3E%3Ctext x='50%25' y='50%25' font-family='Arial' font-size='18' fill='%239ca3af' text-anchor='middle' dominant-baseline='middle'%3E" + (employee.name.charAt(0).toUpperCase() || "?") + "%3C/text%3E%3C/svg%3E";
                          }
                        }}
                      />
                    ) : (
                      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-dark-sidebar text-slate-400 font-semibold">
                        {employee.name.charAt(0).toUpperCase() || "?"}
                      </div>
                    )}
                  </div>
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-slate-100 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.kgid }}>
                  {employee.kgid}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-100 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.name }}>
                  {employee.name}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-400 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.rank }}>
                  {employee.displayRank || employee.rank || "N/A"}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-400 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.email }}>
                  {employee.email || "N/A"}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-400 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.mobile1 }}>
                  {employee.mobile1}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-400 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.mobile2 }}>
                  {employee.mobile2 || "N/A"}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-400 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.bloodGroup }}>
                  {employee.bloodGroup || "N/A"}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-400 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.district }}>
                  {employee.district}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-400 overflow-hidden text-ellipsis" style={{ maxWidth: columnWidths.station }}>
                  {employee.station}
                </td>
                <td className="whitespace-nowrap px-6 py-4">
                  <span
                    className={`inline-flex rounded-full px-2 text-xs font-semibold ${
                      employee.isApproved
                        ? "bg-green-100 text-green-800"
                        : "bg-yellow-100 text-yellow-800"
                    }`}
                  >
                    {employee.isApproved ? "Approved" : "Pending"}
                  </span>
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-right text-sm font-medium">
                  <div className="flex items-center justify-end gap-2">
                    <Link
                      href={`/employees/edit?id=${employee.id}`}
                      className="text-purple-400 hover:text-purple-300 transition-colors"
                    >
                      <Edit className="h-5 w-5" />
                    </Link>
                    <button
                      onClick={() => handleDelete(employee.id!)}
                      className="text-red-400 hover:text-red-300 transition-colors"
                    >
                      <Trash2 className="h-5 w-5" />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {filteredEmployees.length === 0 && (
          <div className="py-12 text-center text-slate-400">
            No employees found
          </div>
        )}
      </div>
    </div>
  );
}

