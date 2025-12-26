"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import {
  LayoutDashboard,
  Users,
  UserCheck,
  MapPin,
  Building2,
  Bell,
  FileText,
  Image as ImageIcon,
  Link as LinkIcon,
  Upload,
  LogOut,
  Shield,
  Award,
} from "lucide-react";
import { signOut } from "@/lib/firebase/auth";
import { useAuth } from "@/components/providers/AuthProvider";
import { getPendingRegistrations } from "@/lib/firebase/firestore";
import { cn } from "@/lib/utils";
import { Logo } from "@/components/common/Logo";

const navigation = [
  { name: "Dashboard", href: "/", icon: LayoutDashboard },
  { name: "Employees", href: "/employees", icon: Users },
  { name: "Pending Approvals", href: "/approvals", icon: UserCheck },
  { name: "Officers", href: "/officers", icon: Shield },
  { name: "Ranks", href: "/ranks", icon: Award },
  { name: "Districts", href: "/districts", icon: MapPin },
  { name: "Stations", href: "/stations", icon: Building2 },
  { name: "Notifications", href: "/notifications", icon: Bell },
  { name: "Documents", href: "/documents", icon: FileText },
  { name: "Gallery", href: "/gallery", icon: ImageIcon },
  { name: "Useful Links", href: "/links", icon: LinkIcon },
  { name: "CSV Upload", href: "/upload", icon: Upload },
];

export function Sidebar() {
  const pathname = usePathname();
  const { user } = useAuth();
  const [pendingCount, setPendingCount] = useState<number>(0);

  // Fetch pending registrations count
  useEffect(() => {
    const fetchPendingCount = async () => {
      try {
        const registrations = await getPendingRegistrations();
        setPendingCount(registrations.length);
      } catch (error: any) {
        // Silently handle errors - don't break the sidebar if pending count fails
        // Common causes: Firestore rules, missing index, or network issues
        if (error?.code !== "permission-denied" && error?.code !== "unavailable") {
          console.warn("Error fetching pending count:", error?.message || error);
        }
        // Set count to 0 on error so badge doesn't show incorrectly
        setPendingCount(0);
      }
    };

    fetchPendingCount();
    // Refresh count every 30 seconds
    const interval = setInterval(fetchPendingCount, 30000);
    return () => clearInterval(interval);
  }, []);

  const handleSignOut = async () => {
    try {
      await signOut();
      window.location.href = "/login";
    } catch (error) {
      console.error("Error signing out:", error);
    }
  };

  return (
    <div className="flex h-screen w-64 flex-col bg-dark-sidebar text-dark-text border-r border-dark-border">
      <div className="flex flex-col items-center justify-center border-b border-dark-border px-4 py-6 bg-gradient-to-br from-purple-900/20 to-blue-900/20">
        <Logo size="md" className="mb-4" />
        <h1 className="text-lg font-bold bg-gradient-to-r from-purple-400 to-blue-400 bg-clip-text text-transparent">PMD Admin</h1>
        <p className="mt-1 text-xs text-dark-text-secondary">Admin Panel</p>
      </div>
      <nav className="flex-1 space-y-1 px-2 py-4 overflow-y-auto">
        {navigation.map((item) => {
          const isActive = pathname === item.href;
          const showBadge = item.name === "Pending Approvals" && pendingCount > 0;
          return (
            <Link
              key={item.name}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-all relative group",
                isActive
                  ? "bg-gradient-to-r from-purple-600 to-blue-600 text-white shadow-lg shadow-purple-500/50"
                  : "text-dark-text-secondary hover:bg-dark-card hover:text-dark-text"
              )}
            >
              <item.icon className={cn(
                "h-5 w-5 transition-colors",
                isActive ? "text-white" : "text-dark-text-secondary group-hover:text-purple-400"
              )} />
              <span className="flex-1">{item.name}</span>
              {showBadge && (
                <span className={cn(
                  "flex items-center justify-center min-w-[20px] h-5 px-1.5 rounded-full text-xs font-bold",
                  isActive
                    ? "bg-white text-purple-600"
                    : "bg-red-500 text-white"
                )}>
                  {pendingCount > 99 ? "99+" : pendingCount}
                </span>
              )}
            </Link>
          );
        })}
      </nav>
      <div className="border-t border-dark-border p-4 bg-dark-card">
        <div className="mb-2 text-sm text-dark-text-secondary">
          {user?.email}
        </div>
        <button
          onClick={handleSignOut}
          className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-dark-text-secondary transition-colors hover:bg-dark-card hover:text-dark-text"
        >
          <LogOut className="h-5 w-5" />
          Sign Out
        </button>
      </div>
    </div>
  );
}

