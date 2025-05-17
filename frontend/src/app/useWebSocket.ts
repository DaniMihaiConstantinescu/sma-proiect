import { useEffect, useRef, useState } from "react";

type InfrastructureItem = {
  id: string;
  childrenIds: string[];
  capacity: number;
};

type MessagePayload = {
  type: "newNode" | "newLoadBalancer" | "newReverseProxy";
  data: InfrastructureItem;
};

export default function useWebSocketHook() {
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

  const ws = useRef<WebSocket | null>(null);

  useEffect(() => {
    ws.current = new WebSocket("ws://localhost:8080");

    ws.current.onopen = () => {
      console.log("[WebSocket] Connected");
      ws.current?.send("Hello from client");
    };

    ws.current.onmessage = (event) => {
      try {
        const message: MessagePayload = JSON.parse(event.data);

        switch (message.type) {
          case "newNode":
            setNodes((prev) => [...prev, message.data]);
            break;
          case "newLoadBalancer":
            setLoadBalancers((prev) => [...prev, message.data]);
            break;
          case "newReverseProxy":
            setReverseProxies((prev) => [...prev, message.data]);
            break;
          default:
            console.warn("Unknown message type:", message.type);
        }
      } catch (err) {
        console.error("[WebSocket] Failed to parse:", err);
      }
    };

    ws.current.onclose = () => console.log("[WebSocket] Connection closed");
    ws.current.onerror = (err) => console.error("[WebSocket] Error", err);

    return () => ws.current?.close();
  }, []);

  const sendMessage = (payload: object) => {
    if (ws.current?.readyState === WebSocket.OPEN) {
      ws.current.send(JSON.stringify(payload));
    }
  };

  return {
    reverseProxies,
    loadBalancers,
    nodes,
    sendMessage,
  };
}
