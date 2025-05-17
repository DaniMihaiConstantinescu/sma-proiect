import { Category } from "@/utils/types";
import { useState } from "react";
import StatCard from "@/components/stat-card";
import StatsTable from "@/components/stats-table";
import useWebSocketHook from "./useWebSocket";

export default function Home() {
  const { reverseProxies, loadBalancers, nodes, sendMessage } =
    useWebSocketHook();

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

      <div className="mt-6 text-center">
        <button
          className="bg-blue-500 text-white px-4 py-2 rounded"
          onClick={() =>
            sendMessage({
              type: "ping",
              timestamp: Date.now(),
            })
          }
        >
          Send Ping to Server
        </button>
      </div>
    </main>
  );
}
