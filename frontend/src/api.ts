import type { FilterResponse, ImpulseFilterRequest } from "./types";

const jsonHeaders = (userId: number) => ({
  "Content-Type": "application/json",
  "X-user-id": String(userId),
});

async function parseJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `HTTP ${response.status}`);
  }

  return response.json() as Promise<T>;
}

export async function getFilters(userId: number): Promise<FilterResponse[]> {
  const response = await fetch("/api/filters", {
    headers: {
      "X-user-id": String(userId),
    },
  });

  return parseJson<FilterResponse[]>(response);
}

export async function createImpulseFilter(
  userId: number,
  payload: ImpulseFilterRequest,
): Promise<FilterResponse> {
  const response = await fetch("/api/filters/IMPULSE", {
    method: "POST",
    headers: jsonHeaders(userId),
    body: JSON.stringify(payload),
  });

  return parseJson<FilterResponse>(response);
}

export async function deleteImpulseFilter(userId: number, filterId: number): Promise<void> {
  const response = await fetch(`/api/filters/IMPULSE/${filterId}`, {
    method: "DELETE",
    headers: {
      "X-user-id": String(userId),
    },
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `HTTP ${response.status}`);
  }
}
