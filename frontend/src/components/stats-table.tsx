import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Category, InfrastructureItem } from "@/utils/types";
import { Progress } from "./ui/progress";

interface StatsTableProps {
  selectedCategory: Category;
  reverseProxies: InfrastructureItem[];
  loadBalancers: InfrastructureItem[];
  nodes: InfrastructureItem[];
}

export default function StatsTable({
  selectedCategory,
  reverseProxies,
  loadBalancers,
  nodes,
}: StatsTableProps) {
  const getSelectedData = () => {
    switch (selectedCategory) {
      case "reverseProxies":
        return reverseProxies;
      case "loadBalancers":
        return loadBalancers;
      case "nodes":
        return nodes;
      default:
        return [];
    }
  };

  const getCategoryLabel = () => {
    switch (selectedCategory) {
      case "reverseProxies":
        return "Reverse Proxies";
      case "loadBalancers":
        return "Load Balancers";
      case "nodes":
        return "Nodes";
      default:
        return "";
    }
  };

  return (
    <div className="bg-card rounded-lg p-6 border">
      {selectedCategory ? (
        <>
          <h2 className="text-2xl font-semibold mb-4">
            {getCategoryLabel()} Details
          </h2>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                {selectedCategory === "nodes" && <TableHead>Load</TableHead>}
                {selectedCategory !== "nodes" && (
                  <TableHead>Connected To</TableHead>
                )}
              </TableRow>
            </TableHeader>
            <TableBody>
              {getSelectedData().map((item) => (
                <TableRow key={item.id}>
                  <TableCell className="font-medium">{item.id}</TableCell>

                  {selectedCategory === "nodes" && (
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <span className="text-xs text-muted-foreground">
                          {item.capacity}%
                        </span>
                        <Progress
                          value={item.capacity}
                          max={100}
                          className={`h-2 ${
                            item.capacity > 80
                              ? "bg-red-200"
                              : item.capacity > 50
                              ? "bg-amber-200"
                              : "bg-green-200"
                          }`}
                        />
                      </div>
                    </TableCell>
                  )}
                  {selectedCategory !== "nodes" && (
                    <TableCell>
                      {item.childrenIds.join(", ") || "None"}
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </>
      ) : (
        <div className="text-center py-10 text-muted-foreground">
          <p className="text-xl">Select a category to view details</p>
        </div>
      )}
    </div>
  );
}
