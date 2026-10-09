import { expect, it, vi } from 'vitest';
import { subscribeDeclarations } from './subscription';

it('handles split SSE frames, ignores heartbeats and cancels on abort', async () => {
  const controller = new AbortController();
  const changed = vi.fn(() => {
    if (changed.mock.calls.length === 2) controller.abort();
  });
  const errors = vi.fn();
  const encoder = new TextEncoder();
  const body = new ReadableStream<Uint8Array>({
    start(stream) {
      stream.enqueue(encoder.encode('event: reset\ndata: {}\n\nevent: heart'));
      stream.enqueue(encoder.encode('beat\ndata: {}\n\nevent: changed\ndata: {}\n\n'));
    },
  });
  const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(body));
  try {
    await subscribeDeclarations('secret', controller.signal, changed, errors);
    expect(changed).toHaveBeenCalledTimes(2);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock.mock.calls[0][1]?.headers).toEqual({
      Authorization: 'Bearer secret',
      Accept: 'text/event-stream',
    });
  } finally {
    fetchMock.mockRestore();
  }
});

it('does not repeatedly retry invalid credentials', async () => {
  const fetchMock = vi
    .spyOn(globalThis, 'fetch')
    .mockResolvedValue(new Response('', { status: 401 }));
  const error = vi.fn();
  try {
    await subscribeDeclarations('bad', new AbortController().signal, vi.fn(), error);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(error).toHaveBeenCalledWith('Subscription failed (401)');
  } finally {
    fetchMock.mockRestore();
  }
});
