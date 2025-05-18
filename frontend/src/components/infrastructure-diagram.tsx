import {
  Dialog,
  DialogTrigger,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import type { InfrastructureItem } from "@/utils/types";
import { useEffect, useState } from "react";
import { AnimatedTree, Data } from "react-tree-graph";
import "react-tree-graph/dist/style.css";

interface InfrastructureDiagramProps {
  reverseProxies: InfrastructureItem[];
  loadBalancers: InfrastructureItem[];
  nodes: InfrastructureItem[];
}

export default function InfrastructureDiagramDialog({
  reverseProxies,
  loadBalancers,
  nodes,
}: InfrastructureDiagramProps) {
  const [treeData, setTreeData] = useState<Data | null>(null);

  useEffect(() => {
    console.log(treeData);
  }, [treeData]);

  useEffect(() => {
    if (
      reverseProxies.length === 0 &&
      loadBalancers.length === 0 &&
      nodes.length === 0
    ) {
      return;
    }

    const root: Data = {
      name: "Gateway",
      children: [],
      textProps: { className: "text-base font-bold" },
      gProps: { className: "root-node" },
    };

    const rpNodes = reverseProxies.map((rp) => {
      const rpNode: Data = {
        name: rp.id,
        children: [],
        textProps: { className: "text-sm font-semibold text-blue-700" },
        gProps: { className: "rp-node" },
        pathProps: { className: "path-to-rp" },
      };
      const childLoadBalancers = loadBalancers.filter((lb) =>
        rp.childrenIds.includes(lb.id)
      );
      rpNode.children = childLoadBalancers.map((lb) => {
        const lbNode: Data = {
          name: lb.id,
          children: [],
          textProps: { className: "text-sm font-medium text-amber-600" },
          gProps: { className: "lb-node" },
          pathProps: { className: "path-to-lb" },
        };
        const childNodes = nodes.filter((node) =>
          lb.childrenIds.includes(node.id)
        );
        lbNode.children = childNodes.map((node) => ({
          name: node.id,
          id: node.id,
          capacity: node.capacity,
          textProps: { className: "text-sm text-green-600" },
          gProps: { className: "node-node" },
          pathProps: { className: "path-to-node" },
        }));
        return lbNode;
      });
      return rpNode;
    });

    root.children = rpNodes;
    setTreeData(root);
  }, [reverseProxies, loadBalancers, nodes]);

  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button variant="outline">Show Infrastructure Diagram</Button>
      </DialogTrigger>
      <DialogContent className="w-[85%] !max-w-none">
        <DialogHeader>
          <DialogTitle>Infrastructure Diagram</DialogTitle>
        </DialogHeader>
        <div className="h-[500px] w-full">
          {treeData ? (
            <AnimatedTree
              data={treeData}
              height={480}
              width={800}
              svgProps={{ className: "w-full h-full" }}
              margins={{ top: 40, bottom: 40, left: 120, right: 120 }}
              gProps={{
                onClick: (e, node) => console.log("Clicked node:", node),
              }}
            />
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
