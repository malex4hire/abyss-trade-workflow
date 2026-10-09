import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { errorMessage, isAbortError } from '../api/client';
import { createDeclarationsApi } from '../api/declarations';
import type { DeclarationData, DeclarationResult, TradeDeclaration } from '../types/declaration';

const POLL_INTERVAL_MS = 2000;

export function useDeclarations(token: string, selectedId: string | null) {
  const api = useMemo(() => createDeclarationsApi(token), [token]);
  const [declarations, setDeclarations] = useState<TradeDeclaration[]>([]);
  const [selectedDeclaration, setSelectedDeclaration] = useState<TradeDeclaration | null>(null);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [pollError, setPollError] = useState<string | null>(null);
  const [mutationError, setMutationError] = useState<string | null>(null);
  const [refreshVersion, setRefreshVersion] = useState(0);
  const mutationControllers = useRef(new Set<AbortController>());
  const mutationInFlight = useRef(false);

  const refresh = useCallback(() => setRefreshVersion((version) => version + 1), []);

  // Replacing the token or unmounting cancels mutations from the old session.
  // Aborting a browser request cannot roll back a server transaction already
  // committed; create-request idempotency remains an explicit future decision.
  useEffect(() => {
    const controllers = mutationControllers.current;
    mutationInFlight.current = false;
    setBusy(false);
    setMutationError(null);
    setDeclarations([]);
    return () => {
      for (const controller of controllers) controller.abort();
      controllers.clear();
    };
  }, [api]);

  useEffect(() => {
    setSelectedDeclaration(null);
    setPollError(null);
    if (!token) {
      setLoading(false);
      return;
    }

    const controller = new AbortController();
    let timer: ReturnType<typeof setTimeout> | undefined;
    let disposed = false;
    setLoading(true);

    async function poll() {
      try {
        const [records, detail] = await Promise.all([
          api.list(controller.signal),
          selectedId ? api.get(selectedId, controller.signal) : Promise.resolve(null),
        ]);
        if (!disposed) {
          setDeclarations(records);
          setSelectedDeclaration(detail);
          setPollError(null);
        }
      } catch (error) {
        if (!disposed && !isAbortError(error)) setPollError(errorMessage(error));
      } finally {
        if (!disposed) {
          setLoading(false);
          // Schedule after completion so slow requests never overlap.
          timer = setTimeout(() => void poll(), POLL_INTERVAL_MS);
        }
      }
    }
    void poll();
    return () => {
      disposed = true;
      controller.abort();
      clearTimeout(timer);
    };
  }, [api, selectedId, token, refreshVersion]);

  async function mutate(
    operation: (signal: AbortSignal) => Promise<DeclarationResult>,
  ): Promise<DeclarationResult | undefined> {
    if (!token || mutationInFlight.current) return;
    const controller = new AbortController();
    mutationControllers.current.add(controller);
    mutationInFlight.current = true;
    setBusy(true);
    setMutationError(null);
    try {
      const result = await operation(controller.signal);
      if (controller.signal.aborted) return;
      refresh();
      return result;
    } catch (error) {
      if (!controller.signal.aborted && !isAbortError(error)) setMutationError(errorMessage(error));
      return undefined;
    } finally {
      mutationControllers.current.delete(controller);
      if (!controller.signal.aborted) {
        mutationInFlight.current = false;
        setBusy(false);
      }
    }
  }

  function createDraft(data: DeclarationData) {
    return mutate((signal) => api.createDraft(data, signal));
  }

  function submitSelected() {
    if (!selectedId) return Promise.resolve(undefined);
    return mutate((signal) => api.submit(selectedId, signal));
  }

  return {
    declarations,
    selectedDeclaration,
    loading,
    busy,
    error: mutationError ?? pollError,
    createDraft,
    submitSelected,
    refresh,
  };
}
