import { InfrastructureItem } from "@/utils/types";
import { useEffect, useRef, useState } from "react";

type MessagePayload = {
  type: "newNode" | "newLoadBalancer" | "newReverseProxy";
  data: InfrastructureItem;
};

export default function useWebSocketHook() {
  const [reverseProxies, setReverseProxies] = useState<InfrastructureItem[]>(
    []
  );
  const [loadBalancers, setLoadBalancers] = useState<InfrastructureItem[]>([]);
  const [nodes, setNodes] = useState<InfrastructureItem[]>([]);

  const ws = useRef<WebSocket | null>(null);

  useEffect(() => {
    ws.current = new WebSocket("ws://localhost:8887");

    ws.current.onopen = () => {
      console.log("[WebSocket] Connected");
    };

    ws.current.onmessage = (event) => {
      try {
        const message: MessagePayload = JSON.parse(event.data);

        switch (message.type) {
          case "newNode":
            setNodes((prev) => [...prev, message.data]);
            setLoadBalancers((prev) => {
              return prev.map((item) =>
                item.id === message.data.parentId
                  ? {
                      ...item,
                      childrenIds: [...item.childrenIds, message.data.id],
                    }
                  : item
              );
            });
            break;
          case "newLoadBalancer":
            setLoadBalancers((prev) => [...prev, message.data]);
            setReverseProxies((prev) => {
              return prev.map((item) =>
                item.id === message.data.parentId
                  ? {
                      ...item,
                      childrenIds: [...item.childrenIds, message.data.id],
                    }
                  : item
              );
            });
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
