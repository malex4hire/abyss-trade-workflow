import { act, cleanup, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { useDeclarations } from './useDeclarations';
import type { TradeDeclaration } from '../types/declaration';

const api = vi.hoisted(() => ({
  list: vi.fn(),
  get: vi.fn(),
  createDraft: vi.fn(),
  submit: vi.fn(),
}));
vi.mock('../api/declarations', () => ({ createDeclarationsApi: () => api }));

function record(id: string): TradeDeclaration {
  return {
    id,
    status: 'draft',
    reason: '',
    createdAt: '2026-10-08T00:00:00Z',
    data: {
      importer: 'Demo',
      originCountry: 'CA',
      commodityCode: '847130',
      declaredValueCents: 100,
      currency: 'USD',
      shipmentReference: id,
    },
  };
}

beforeEach(() => {
  vi.useFakeTimers();
  vi.resetAllMocks();
  api.list.mockResolvedValue([]);
  api.get.mockImplementation(async (id: string) => record(id));
});

afterEach(() => {
  cleanup();
  vi.useRealTimers();
});

it('does not overlap polls while a previous request is pending', async () => {
  let resolve!: (records: TradeDeclaration[]) => void;
  api.list.mockImplementationOnce(
    () =>
      new Promise((done) => {
        resolve = done;
      }),
  );
  renderHook(() => useDeclarations('token', null));
  await act(async () => {
    await vi.advanceTimersByTimeAsync(6000);
  });
  expect(api.list).toHaveBeenCalledTimes(1);
  await act(async () => {
    resolve([]);
  });
  await act(async () => {
    await vi.advanceTimersByTimeAsync(2000);
  });
  expect(api.list).toHaveBeenCalledTimes(2);
});

it('ignores a stale detail response after the user selects another declaration', async () => {
  let resolveOld!: (value: TradeDeclaration) => void;
  api.get.mockImplementationOnce(
    () =>
      new Promise((done) => {
        resolveOld = done;
      }),
  );
  const { result, rerender } = renderHook(({ id }) => useDeclarations('token', id), {
    initialProps: { id: 'old' },
  });
  const oldSignal = api.get.mock.calls[0][1] as AbortSignal;
  await act(async () => {
    rerender({ id: 'new' });
  });
  expect(oldSignal.aborted).toBe(true);
  expect(result.current.selectedDeclaration?.id).toBe('new');
  await act(async () => {
    resolveOld(record('old'));
  });
  expect(result.current.selectedDeclaration?.id).toBe('new');
});

it('cancels polling when the component unmounts', async () => {
  const { unmount } = renderHook(() => useDeclarations('token', 'one'));
  await act(async () => {});
  const signal = api.list.mock.calls[0][0] as AbortSignal;
  unmount();
  expect(signal.aborted).toBe(true);
  await act(async () => {
    await vi.advanceTimersByTimeAsync(4000);
  });
  expect(api.list).toHaveBeenCalledTimes(1);
});
