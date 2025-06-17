export type InfrastructureItem = {
  id: string;
  childrenIds: string[];
  capacity: number;
  parentId: string;
  resourceType?: string;
};

export type Category = "reverseProxies" | "loadBalancers" | "nodes" | null;

export enum InstanceType {
  WEBSOCKET_SERVER = "WEBSOCKET_SERVER",
  GATEWAY = "GATEWAY",
  REVERSE_PROXY = "REVERSE_PROXY",
  LOAD_BALANCER = "LOAD_BALANCER",
  NODE = "NODE",
}

export interface Log {
  timestamp: string;
  instance: string;
  description: string;
  instanceType: InstanceType;
}
