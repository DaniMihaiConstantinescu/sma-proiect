"use client";

import StatCard from "@/components/stat-card";
import StatsTable from "@/components/stats-table";
import { Category } from "@/utils/types";
import { useState } from "react";

type InfrastructureItem = {
  id: string;
  childrenIds: string[];
  capacity: number;
};

export default function Home() {
  const [reverseProxies, setReverseProxies] = useState<InfrastructureItem[]>([
    { id: "rp-1", childrenIds: ["lb-1", "lb-2"], capacity: 100 },
    { id: "rp-2", childrenIds: ["lb-3"], capacity: 80 },
  ]);

  const [loadBalancers, setLoadBalancers] = useState<InfrastructureItem[]>([
    { id: "lb-1", childrenIds: ["node-1", "node-2"], capacity: 50 },
    { id: "lb-2", childrenIds: ["node-3"], capacity: 30 },
    { id: "lb-3", childrenIds: ["node-4", "node-5"], capacity: 60 },
  ]);

  const [nodes, setNodes] = useState<InfrastructureItem[]>([
    { id: "node-1", childrenIds: [], capacity: 20 },
    { id: "node-2", childrenIds: [], capacity: 25 },
    { id: "node-3", childrenIds: [], capacity: 10 },
    { id: "node-4", childrenIds: [], capacity: 15 },
    { id: "node-5", childrenIds: [], capacity: 18 },
  ]);

  const [selectedCategory, setSelectedCategory] = useState<Category>(null);

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

      <StatsTable
        selectedCategory={selectedCategory}
        reverseProxies={reverseProxies}
        loadBalancers={loadBalancers}
        nodes={nodes}
      />
    </main>
  );
}
