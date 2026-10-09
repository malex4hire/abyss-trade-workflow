import { afterEach, expect, it, vi } from 'vitest';
import { ApiError, createApiClient } from './client';

afterEach(() => vi.unstubAllGlobals());

it('sends token and serializes the payload through the same-origin proxy', async () => {
  const fetch = vi
    .fn()
    .mockResolvedValue(new Response(JSON.stringify({ id: 'one' }), { status: 201 }));
  vi.stubGlobal('fetch', fetch);
  const request = createApiClient('demo-token');
  await request('/declarations', { method: 'POST', body: { importer: 'Demo' } });
  expect(fetch).toHaveBeenCalledWith(
    '/api/declarations',
    expect.objectContaining({
      headers: { Authorization: 'Bearer demo-token', 'Content-Type': 'application/json' },
      body: '{"importer":"Demo"}',
    }),
  );
});

it('preserves an API error reason and HTTP status', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(new Response('{"error":"Denied"}', { status: 401 })),
  );
  const request = createApiClient('wrong');
  await expect(request('/declarations')).rejects.toMatchObject({ message: 'Denied', status: 401 });
});

it('reports a non-JSON proxy response clearly', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(new Response('<html>Unavailable</html>', { status: 502 })),
  );
  await expect(createApiClient('token')('/declarations')).rejects.toBeInstanceOf(ApiError);
});
