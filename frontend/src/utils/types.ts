export type InfrastructureItem = {
  id: string;
  childrenIds: string[];
  capacity: number;
  parentId: string;
};

export type Category = "reverseProxies" | "loadBalancers" | "nodes" | null;

export interface Log {
  timestamp: string;
  instance: string;
  description: string;
}
