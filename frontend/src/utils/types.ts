export type InfrastructureItem = {
  id: string;
  childrenIds: string[];
  capacity: number;
};

export type Category = "reverseProxies" | "loadBalancers" | "nodes" | null
