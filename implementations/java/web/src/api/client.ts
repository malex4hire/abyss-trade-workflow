export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export function isAbortError(error: unknown): boolean {
  return error instanceof Error && error.name === 'AbortError';
}

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'An unexpected error occurred';
}

// All browser requests use the same-origin Nginx proxy. Host-port changes do
// not alter the internal API address configured in nginx.conf.
export function createApiClient(token: string) {
  return async function request<T>(
    path: string,
    options: { method?: 'GET' | 'POST'; body?: unknown; signal?: AbortSignal } = {},
  ): Promise<T> {
    const response = await fetch(`/api${path}`, {
      method: options.method ?? 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      signal: options.signal,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    });

    let body: unknown;
    try {
      body = await response.json();
    } catch (error) {
      if (isAbortError(error)) throw error;
      throw new ApiError('The server returned an invalid response', response.status);
    }
    if (!response.ok) {
      const message =
        typeof body === 'object' &&
        body !== null &&
        'error' in body &&
        typeof body.error === 'string'
          ? body.error
          : `Request failed (${response.status})`;
      throw new ApiError(message, response.status);
    }
    // TypeScript checks call sites. Runtime response-schema validation is a
    // separate production concern; these types mirror the OpenAPI contract.
    return body as T;
  };
}
