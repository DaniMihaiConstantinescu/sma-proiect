import React, { useEffect, useState } from "react";
import {
  Dialog,
  DialogTrigger,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import type { InfrastructureItem } from "@/utils/types";

import ReactFlow, {
  Background,
  Controls,
  Node,
  Edge,
  Position,
} from "reactflow";
import "reactflow/dist/style.css";
import dagre from "dagre";

interface InfrastructureDiagramProps {
  reverseProxies: InfrastructureItem[];
  loadBalancers: InfrastructureItem[];
  nodes: InfrastructureItem[];
}

// dagre config
const dagreGraph = new dagre.graphlib.Graph();
dagreGraph.setDefaultEdgeLabel(() => ({}));
const NODE_WIDTH = 200;
const NODE_HEIGHT = 75;
const SMALL_NODE_HEIGHT = 50;

type Direction = "TB" | "LR";

function getLayoutedElements(
  nodes: Node<{ label: string }>[],
  edges: Edge[],
  direction: Direction = "TB"
): { nodes: Node<{ label: string }>[]; edges: Edge[] } {
  const isHorizontal = direction === "LR";
  dagreGraph.setGraph({ rankdir: direction });

  nodes.forEach((node) => {
    dagreGraph.setNode(node.id, { width: NODE_WIDTH, height: NODE_HEIGHT });
  });
  edges.forEach((edge) => {
    dagreGraph.setEdge(edge.source, edge.target);
  });

  dagre.layout(dagreGraph);

  const layoutedNodes = nodes.map((node) => {
    const { x, y } = dagreGraph.node(node.id);
    node.sourcePosition = (isHorizontal ? "right" : "bottom") as Position;
    node.targetPosition = (isHorizontal ? "left" : "top") as Position;
    node.position = {
      x: x - NODE_WIDTH / 2,
      y: y - NODE_HEIGHT / 2,
    };
    return node;
  });

  return { nodes: layoutedNodes, edges };
}

export default function InfrastructureDiagramDialog({
  reverseProxies,
  loadBalancers,
  nodes,
}: InfrastructureDiagramProps) {
  const [direction, setDirection] = useState<Direction>("LR");
  const [layout, setLayout] = useState<{
    nodes: Node<{ label: string }>[];
    edges: Edge[];
  }>({ nodes: [], edges: [] });

  useEffect(() => {
    if (
      reverseProxies.length === 0 &&
      loadBalancers.length === 0 &&
      nodes.length === 0
    ) {
      setLayout({ nodes: [], edges: [] });
      return;
    }

    const rfNodes: Node<{ label: string }>[] = [];
    const rfEdges: Edge[] = [];

    // Gateway root
    rfNodes.push({
      id: "gateway",
      data: { label: "Gateway" },
      position: { x: 0, y: 0 },
      style: {
        width: NODE_WIDTH,
        height: SMALL_NODE_HEIGHT,
        fontWeight: "bold",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        textAlign: "center",
      },
    });

    // Reverse Proxies
    reverseProxies.forEach((rp) => {
      rfNodes.push({
        id: rp.id,
        data: { label: rp.id },
        position: { x: 0, y: 0 },
        style: {
          width: NODE_WIDTH,
          height: SMALL_NODE_HEIGHT,
          borderColor: "#1E3A8A",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          textAlign: "center",
        },
      });
      rfEdges.push({
        id: `e-gateway-${rp.id}`,
        source: "gateway",
        target: rp.id,
        animated: true,
      });

      // Load Balancers under each RP
      const childLBs = loadBalancers.filter((lb) =>
        rp.childrenIds.includes(lb.id)
      );
      childLBs.forEach((lb) => {
        rfNodes.push({
          id: lb.id,
          data: { label: lb.id },
          position: { x: 0, y: 0 },
          style: {
            width: NODE_WIDTH,
            height: NODE_HEIGHT,
            borderColor: "#B45309",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            textAlign: "center",
          },
        });
        rfEdges.push({
          id: `e-${rp.id}-${lb.id}`,
          source: rp.id,
          target: lb.id,
          animated: true,
        });

        // Nodes under each LB
        const childNodes = nodes.filter((n) => lb.childrenIds.includes(n.id));
        childNodes.forEach((nd) => {
          rfNodes.push({
            id: nd.id,
            data: { label: `${nd.id} (${nd.capacity})` },
            position: { x: 0, y: 0 },
            style: {
              width: NODE_WIDTH,
              height: NODE_HEIGHT,
              borderColor: "#047857",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              textAlign: "center",
            },
          });
          rfEdges.push({
            id: `e-${lb.id}-${nd.id}`,
            source: lb.id,
            target: nd.id,
            animated: true,
          });
        });
      });
    });

    const { nodes: layoutedNodes, edges: layoutedEdges } = getLayoutedElements(
      rfNodes,
      rfEdges,
      direction
    );

    setLayout({ nodes: layoutedNodes, edges: layoutedEdges });
  }, [reverseProxies, loadBalancers, nodes, direction]);

  const toggleDirection = () => setDirection((d) => (d === "TB" ? "LR" : "TB"));

  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button variant="outline">Show Infrastructure Diagram</Button>
      </DialogTrigger>
      <DialogContent className="w-[85%] !max-w-none">
        <DialogHeader className="flex justify-between items-center">
          <DialogTitle>Infrastructure Diagram</DialogTitle>
          <Button size="sm" variant="ghost" onClick={toggleDirection}>
            {direction === "TB" ? "Horizontal" : "Vertical"}
          </Button>
        </DialogHeader>
        <div className="h-[600px] w-full">
          {layout.nodes.length > 0 ? (
            <ReactFlow
              nodes={layout.nodes}
              edges={layout.edges}
              fitView
              nodesDraggable={false}
              nodesConnectable={false}
              zoomOnScroll
              panOnScroll
            >
              <Background />
              <Controls />
            </ReactFlow>
          ) : (
            <div className="flex items-center justify-center h-full">
              Loading diagram...
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
