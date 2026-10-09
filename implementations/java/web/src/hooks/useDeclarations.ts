import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { errorMessage, isAbortError } from '../api/client';
import { subscribeDeclarations } from '../api/subscription';
import { createDeclarationsApi } from '../api/declarations';
import type { DeclarationData, DeclarationResult, TradeDeclaration } from '../types/declaration';

export function useDeclarations(token: string, selectedId: string | null) {
  const api = useMemo(() => createDeclarationsApi(token), [token]);
  const [declarations, setDeclarations] = useState<TradeDeclaration[]>([]);
  const [selectedDeclaration, setSelectedDeclaration] = useState<TradeDeclaration | null>(null);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [readError, setReadError] = useState<string | null>(null);
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
    setReadError(null);
    if (!token) {
      setLoading(false);
      return;
    }

    const controller = new AbortController();
    let disposed = false;
    setLoading(true);

    async function load() {
      try {
        const [records, detail] = await Promise.all([
          api.list(controller.signal),
          selectedId ? api.get(selectedId, controller.signal) : Promise.resolve(null),
        ]);
        if (!disposed) {
          setDeclarations(records);
          setSelectedDeclaration(detail);
          setReadError(null);
        }
      } catch (error) {
        if (!disposed && !isAbortError(error)) setReadError(errorMessage(error));
      } finally {
        if (!disposed) {
          setLoading(false);
        }
      }
    }
    void load();
    return () => {
      disposed = true;
      controller.abort();
    };
  }, [api, selectedId, token, refreshVersion]);

  // One connection per token; selecting a row does not reopen the stream.
  useEffect(() => {
    if (!token) return;
    const controller = new AbortController();
    void subscribeDeclarations(token, controller.signal, refresh, setReadError);
    return () => controller.abort();
  }, [token, refresh]);

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
    error: mutationError ?? readError,
    createDraft,
    submitSelected,
    refresh,
  };
}
