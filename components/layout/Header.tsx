"use client";

import { useAuth } from "@/components/providers/AuthProvider";
import { Logo } from "@/components/common/Logo";

export function Header() {
  const { user } = useAuth();

  return (
    <header className="border-b bg-white shadow-sm">
      <div className="flex h-16 items-center justify-between px-6">
        <div className="flex items-center gap-3">
          <Logo size="sm" />
          <h2 className="text-lg font-semibold text-gray-900">
            Police Mobile Directory
          </h2>
        </div>
        <div className="flex items-center gap-4">
          {user && (
            <div className="text-sm text-gray-600">
              {user.email}
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

