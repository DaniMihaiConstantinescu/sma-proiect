export type InfrastructureItem = {
  id: string;
  childrenIds: string[];
  capacity: number;
  parentId: string;
};

export type Category = "reverseProxies" | "loadBalancers" | "nodes" | null
