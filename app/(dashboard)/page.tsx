"use client";

import { useEffect, useState } from "react";
import { getEmployeeStats } from "@/lib/firebase/firestore";
import {
  Users,
  UserCheck,
  MapPin,
  Building2,
  TrendingUp,
} from "lucide-react";

export default function Dashboard() {
  const [stats, setStats] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadStats = async () => {
      try {
        // Only load stats on client side
        if (typeof window === "undefined") {
          setLoading(false);
          return;
        }
        const data = await getEmployeeStats();
        setStats(data);
      } catch (error) {
        console.error("Error loading stats:", error);
        // Set default stats on error
        setStats({
          total: 0,
          approved: 0,
          pending: 0,
          byDistrict: {},
          byStation: {},
          byRank: {},
          districtsCount: 0,
          stationsCount: 0,
        });
      } finally {
        setLoading(false);
      }
    };
    loadStats();
  }, []);

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center bg-gradient-dark">
        <div className="text-lg text-slate-100-secondary">Loading...</div>
      </div>
    );
  }

  const statCards = [
    {
      title: "Total Employees",
      value: stats?.total || 0,
      icon: Users,
      gradient: "from-blue-500 to-blue-600",
    },
    {
      title: "Approved",
      value: stats?.approved || 0,
      icon: UserCheck,
      gradient: "from-green-500 to-emerald-600",
    },
    {
      title: "Pending",
      value: stats?.pending || 0,
      icon: TrendingUp,
      gradient: "from-yellow-500 to-orange-500",
    },
    {
      title: "Districts",
      value: stats?.districtsCount || 0,
      icon: MapPin,
      gradient: "from-purple-500 to-purple-600",
    },
    {
      title: "Stations",
      value: stats?.stationsCount || 0,
      icon: Building2,
      gradient: "from-indigo-500 to-blue-600",
    },
  ];

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold bg-gradient-to-r from-purple-400 to-blue-400 bg-clip-text text-transparent">Dashboard</h1>
        <p className="mt-2 text-slate-100-secondary">Welcome back! Here&apos;s your overview.</p>
      </div>
      
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        {statCards.map((card) => (
          <div
            key={card.title}
            className="rounded-lg bg-dark-card border border-dark-border p-6 shadow-lg hover:shadow-xl transition-shadow"
          >
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-100-secondary">
                  {card.title}
                </p>
                <p className="mt-2 text-3xl font-bold text-slate-100">
                  {card.value}
                </p>
              </div>
              <div className={`rounded-full p-3 bg-gradient-to-br ${card.gradient} shadow-lg`}>
                <card.icon className="h-6 w-6 text-white" />
              </div>
            </div>
          </div>
        ))}
      </div>

      {stats && (
        <div className="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-2">
          <div className="rounded-lg bg-dark-card border border-dark-border p-6 shadow-lg">
            <h2 className="mb-4 text-xl font-semibold text-slate-100">Employees by District</h2>
            <div className="space-y-2">
              {Object.entries(stats.byDistrict || {})
                .sort(([, a], [, b]) => (b as number) - (a as number))
                .slice(0, 10)
                .map(([district, count]) => (
                  <div
                    key={district}
                    className="flex items-center justify-between py-2 border-b border-dark-border last:border-0"
                  >
                    <span className="text-slate-100-secondary">{district}</span>
                    <span className="font-semibold text-slate-100">
                      {count as number}
                    </span>
                  </div>
                ))}
            </div>
          </div>

          <div className="rounded-lg bg-dark-card border border-dark-border p-6 shadow-lg">
            <h2 className="mb-4 text-xl font-semibold text-slate-100">Top Ranks</h2>
            <div className="space-y-2">
              {Object.entries(stats.byRank || {})
                .sort(([, a], [, b]) => (b as number) - (a as number))
                .slice(0, 10)
                .map(([rank, count]) => (
                  <div
                    key={rank}
                    className="flex items-center justify-between py-2 border-b border-dark-border last:border-0"
                  >
                    <span className="text-slate-100-secondary">{rank}</span>
                    <span className="font-semibold text-slate-100">
                      {count as number}
                    </span>
                  </div>
                ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

