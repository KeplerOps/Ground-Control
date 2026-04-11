const API_BASE = "/api/v1";

export class ApiError extends Error {
  constructor(
    public status: number,
    public detail: string,
  ) {
    super(detail);
    this.name = "ApiError";
  }
}

async function handleError(response: Response): Promise<never> {
  const errorBody = await response.json().catch(() => ({}));
  const body = errorBody as { detail?: string; error?: { message?: string } };
  throw new ApiError(
    response.status,
    body.detail ?? body.error?.message ?? `Request failed: ${response.status}`,
  );
}

export async function apiFetch<T>(
  path: string,
  options?: {
    params?: Record<string, string | undefined>;
    method?: string;
    body?: unknown;
    headers?: Record<string, string>;
  },
): Promise<T> {
  const url = new URL(`${API_BASE}${path}`, window.location.origin);

  if (options?.params) {
    for (const [key, value] of Object.entries(options.params)) {
      if (value !== undefined) {
        url.searchParams.set(key, value);
      }
    }
  }

  const init: RequestInit = {
    method: options?.method ?? "GET",
    headers: {},
  };

  if (options?.body) {
    init.headers = {
      ...init.headers,
      "Content-Type": "application/json",
    };
    init.body = JSON.stringify(options.body);
  }

  if (options?.headers) {
    init.headers = {
      ...init.headers,
      ...options.headers,
    };
  }

  const response = await fetch(url.toString(), init);

  if (!response.ok) {
    await handleError(response);
  }

  return response.json() as Promise<T>;
}

export async function apiDelete(
  path: string,
  options?: { params?: Record<string, string | undefined> },
): Promise<void> {
  const url = new URL(`${API_BASE}${path}`, window.location.origin);

  if (options?.params) {
    for (const [key, value] of Object.entries(options.params)) {
      if (value !== undefined) {
        url.searchParams.set(key, value);
      }
    }
  }

  const response = await fetch(url.toString(), {
    method: "DELETE",
  });

  if (!response.ok) {
    await handleError(response);
  }
}

export async function apiUpload<T>(
  path: string,
  formData: FormData,
  options?: {
    params?: Record<string, string | undefined>;
    headers?: Record<string, string>;
  },
): Promise<T> {
  const url = new URL(`${API_BASE}${path}`, window.location.origin);

  if (options?.params) {
    for (const [key, value] of Object.entries(options.params)) {
      if (value !== undefined) {
        url.searchParams.set(key, value);
      }
    }
  }

  const response = await fetch(url.toString(), {
    method: "POST",
    headers: options?.headers,
    body: formData,
  });

  if (!response.ok) {
    await handleError(response);
  }

  return response.json() as Promise<T>;
}
