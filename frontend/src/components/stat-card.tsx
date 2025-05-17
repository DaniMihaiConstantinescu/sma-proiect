"use client";

import { Card, CardContent } from "@/components/ui/card";

interface StatCardProps {
  value: number;
  label: string;
  onClick: () => void;
  isSelected: boolean;
}

export default function StatCard({
  value,
  label,
  onClick,
  isSelected,
}: StatCardProps) {
  return (
    <Card
      className={`w-full cursor-pointer transition-all ${
        isSelected ? "ring-2 ring-primary" : "hover:shadow-md"
      }`}
      onClick={onClick}
    >
      <CardContent className="flex flex-col items-center justify-center p-6">
        <span className="text-6xl font-bold">{value}</span>
        <span className="text-lg text-muted-foreground mt-2">{label}</span>
      </CardContent>
    </Card>
  );
}
