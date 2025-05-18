"use client";

import type { Category } from "@/utils/types";
import { useState } from "react";
import StatCard from "@/components/stat-card";
import StatsTable from "@/components/stats-table";
import useWebSocketHook from "./useWebSocket";
import InfrastructureDiagram from "@/components/infrastructure-diagram";
import { LogsDialog } from "@/components/logs-dialog";

export default function Home() {
  const { reverseProxies, loadBalancers, nodes } = useWebSocketHook();

  const [selectedCategory, setSelectedCategory] = useState<Category>(null);

  const sampleLogs = [
    {
      timestamp: new Date(Date.now() - 1000 * 60 * 5).toISOString(),
      instance: "server-01",
      description: "Application started successfully",
    },
    {
      timestamp: new Date(Date.now() - 1000 * 60 * 10).toISOString(),
      instance: "db-primary",
      description: "Database connection established",
    },
    {
      timestamp: new Date(Date.now() - 1000 * 60 * 15).toISOString(),
      instance: "server-01",
      description: "User authentication failed: Invalid credentials",
    },
    {
      timestamp: new Date(Date.now() - 1000 * 60 * 30).toISOString(),
      instance: "cache-01",
      description: "Cache invalidation completed",
    },
    {
      timestamp: new Date(Date.now() - 1000 * 60 * 45).toISOString(),
      instance: "server-02",
      description: "Memory usage above 80% threshold",
    },
  ];

  return (
    <main className="container mx-auto py-12 px-4">
      <h1 className="text-3xl font-bold mb-8 text-center">
        Infrastructure Overview
      </h1>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10">
        <StatCard
          value={reverseProxies.length}
          label="Reverse Proxies"
          onClick={() => setSelectedCategory("reverseProxies")}
          isSelected={selectedCategory === "reverseProxies"}
        />
        <StatCard
          value={loadBalancers.length}
          label="Load Balancers"
          onClick={() => setSelectedCategory("loadBalancers")}
          isSelected={selectedCategory === "loadBalancers"}
        />
        <StatCard
          value={nodes.length}
          label="Nodes"
          onClick={() => setSelectedCategory("nodes")}
          isSelected={selectedCategory === "nodes"}
        />
      </div>

      <div className="flex gap-1 mb-2">
        <InfrastructureDiagram
          reverseProxies={reverseProxies}
          loadBalancers={loadBalancers}
          nodes={nodes}
        />
        <LogsDialog logs={sampleLogs} />
      </div>

      <StatsTable
        selectedCategory={selectedCategory}
        reverseProxies={reverseProxies}
        loadBalancers={loadBalancers}
        nodes={nodes}
      />
    </main>
  );
}
