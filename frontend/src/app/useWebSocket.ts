import { InfrastructureItem, Log } from "@/utils/types";
import { useEffect, useRef, useState } from "react";

type MessagePayload = {
  type:
    | "newNode"
    | "newLoadBalancer"
    | "newReverseProxy"
    | "deleteNode"
    | "log";
  data: InfrastructureItem | Log;
};

export default function useWebSocketHook() {
  const [reverseProxies, setReverseProxies] = useState<InfrastructureItem[]>(
    []
  );
  const [loadBalancers, setLoadBalancers] = useState<InfrastructureItem[]>([]);
  const [nodes, setNodes] = useState<InfrastructureItem[]>([]);

  const [logs, setLogs] = useState<Log[]>([]);

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
          case "newNode": {
            const newNode = message.data as InfrastructureItem;
            // add to nodes list
            setNodes((prev) => [...prev, newNode]);
            // register under parent LB
            setLoadBalancers((prev) =>
              prev.map((item) =>
                item.id === newNode.parentId
                  ? {
                      ...item,
                      childrenIds: [...item.childrenIds, newNode.id],
                    }
                  : item
              )
            );
            break;
          }

          case "newLoadBalancer": {
            const newLB = message.data as InfrastructureItem;
            setLoadBalancers((prev) => [...prev, newLB]);
            setReverseProxies((prev) =>
              prev.map((item) =>
                item.id === newLB.parentId
                  ? {
                      ...item,
                      childrenIds: [...item.childrenIds, newLB.id],
                    }
                  : item
              )
            );
            break;
          }

          case "newReverseProxy": {
            const newProxy = message.data as InfrastructureItem;
            setReverseProxies((prev) => [...prev, newProxy]);
            break;
          }

          case "deleteNode": {
            const deleted = message.data as InfrastructureItem;

            console.log("deleted: ", deleted);

            setNodes((prev) => prev.filter((n) => n.id !== deleted.id));
            setLoadBalancers((prev) =>
              prev.map((lb) =>
                lb.id === deleted.parentId
                  ? {
                      ...lb,
                      childrenIds: lb.childrenIds.filter(
                        (id) => id !== deleted.id
                      ),
                    }
                  : lb
              )
            );
            break;
          }

          case "log": {
            const newLog = message.data as Log;
            setLogs((prev) => [...prev, newLog]);
            break;
          }

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
    logs,
  };
}
