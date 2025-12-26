"use client";

import { useState } from "react";
import { Shield } from "lucide-react";

interface LogoProps {
  size?: "sm" | "md" | "lg";
  className?: string;
}

export function Logo({ size = "md", className = "" }: LogoProps) {
  const [imageError, setImageError] = useState(false);

  const sizeClasses = {
    sm: "h-10 w-10",
    md: "h-20 w-20",
    lg: "h-32 w-32",
  };

  const iconSizes = {
    sm: "h-5 w-5",
    md: "h-10 w-10",
    lg: "h-16 w-16",
  };

  if (imageError) {
    return (
      <div
        className={`flex ${sizeClasses[size]} items-center justify-center rounded-full bg-primary-600 ${className}`}
      >
        <Shield className={`${iconSizes[size]} text-white`} />
      </div>
    );
  }

  return (
    <img
      src="/logo.png"
      alt="PMD Logo"
      className={`${sizeClasses[size]} object-contain ${className}`}
      onError={() => {
        console.log(`Logo load error (${size}): local logo not found`);
        setImageError(true);
      }}
      onLoad={() => {
        console.log(`Logo loaded successfully (${size})`);
      }}
    />
  );
}

