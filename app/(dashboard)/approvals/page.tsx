"use client";

import { useEffect, useState } from "react";
import {
  getPendingRegistrations,
  approveRegistration,
  rejectRegistration,
  PendingRegistration,
} from "@/lib/firebase/firestore";
import { Check, X, RefreshCw } from "lucide-react";
import { formatDateTime } from "@/lib/utils";

export default function ApprovalsPage() {
  const [registrations, setRegistrations] = useState<PendingRegistration[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadRegistrations();
  }, []);

  const loadRegistrations = async () => {
    try {
      const data = await getPendingRegistrations();
      setRegistrations(data);
    } catch (error) {
      console.error("Error loading registrations:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (registration: PendingRegistration) => {
    if (!registration.id) return;
    if (!confirm(`Approve registration for ${registration.name}?`)) return;

    try {
      await approveRegistration(registration.id, registration);
      await loadRegistrations();
    } catch (error) {
      console.error("Error approving registration:", error);
      alert("Failed to approve registration");
    }
  };

  const handleReject = async (id: string, name: string) => {
    if (!confirm(`Reject registration for ${name}?`)) return;

    try {
      await rejectRegistration(id);
      await loadRegistrations();
    } catch (error) {
      console.error("Error rejecting registration:", error);
      alert("Failed to reject registration");
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
        <h1 className="text-3xl font-bold text-gray-900">
          Pending Approvals ({registrations.length})
        </h1>
        <button
          onClick={loadRegistrations}
          className="flex items-center gap-2 rounded-lg border border-gray-300 px-4 py-2 text-gray-700 transition-colors hover:bg-gray-50"
        >
          <RefreshCw className="h-5 w-5" />
          Refresh
        </button>
      </div>

      {registrations.length === 0 ? (
        <div className="rounded-lg bg-white p-12 text-center shadow-md">
          <p className="text-lg text-gray-500">No pending registrations</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
          {registrations.map((reg) => (
            <div
              key={reg.id}
              className="rounded-lg bg-white p-6 shadow-md"
            >
              <div className="mb-4">
                <h3 className="text-lg font-semibold text-gray-900">
                  {reg.name}
                </h3>
                <p className="text-sm text-gray-500">{reg.email}</p>
              </div>

              <div className="space-y-2 text-sm">
                <div>
                  <span className="font-medium text-gray-700">KGID:</span>{" "}
                  <span className="text-gray-900">{reg.kgid}</span>
                </div>
                <div>
                  <span className="font-medium text-gray-700">Mobile:</span>{" "}
                  <span className="text-gray-900">{reg.mobile1}</span>
                </div>
                <div>
                  <span className="font-medium text-gray-700">Rank:</span>{" "}
                  <span className="text-gray-900">{reg.rank || "N/A"}</span>
                </div>
                <div>
                  <span className="font-medium text-gray-700">District:</span>{" "}
                  <span className="text-gray-900">{reg.district}</span>
                </div>
                <div>
                  <span className="font-medium text-gray-700">Station:</span>{" "}
                  <span className="text-gray-900">{reg.station}</span>
                </div>
                {reg.createdAt && (
                  <div>
                    <span className="font-medium text-gray-700">Created:</span>{" "}
                    <span className="text-gray-900">
                      {formatDateTime(reg.createdAt.toDate())}
                    </span>
                  </div>
                )}
              </div>

              <div className="mt-6 flex gap-2">
                <button
                  onClick={() => handleApprove(reg)}
                  className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-green-600 px-4 py-2 text-white transition-colors hover:bg-green-700"
                >
                  <Check className="h-4 w-4" />
                  Approve
                </button>
                <button
                  onClick={() => handleReject(reg.id!, reg.name)}
                  className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-white transition-colors hover:bg-red-700"
                >
                  <X className="h-4 w-4" />
                  Reject
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}



