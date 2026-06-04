export type Direction = "UP" | "DOWN";

export type ImpulseFilterRequest = {
  exchange: string[];
  market: string[];
  blackList: string[];
  action: "IMPULSE";
  timeWindow: number;
  direction: Direction;
  percent: number;
  volume24h: number;
};

export type FilterResponse = ImpulseFilterRequest & {
  id: number;
};

export type AlertCreatedEvent = {
  filterId: number;
  subscribers: number[];
  exchange: string[];
  market: string[];
  symbol: string;
  timestampNs: number;
};

export type StreamState = "idle" | "connecting" | "connected" | "error";
