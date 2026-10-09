import { createApiClient } from './client';
import type { DeclarationData, DeclarationResult, TradeDeclaration } from '../types/declaration';

export function createDeclarationsApi(token: string) {
  const request = createApiClient(token);
  const declarationPath = (id: string) => `/declarations/${encodeURIComponent(id)}`;

  return {
    list: (signal?: AbortSignal) => request<TradeDeclaration[]>('/declarations', { signal }),
    get: (id: string, signal?: AbortSignal) =>
      request<TradeDeclaration>(declarationPath(id), { signal }),
    createDraft: (data: DeclarationData, signal?: AbortSignal) =>
      request<DeclarationResult>('/declarations', { method: 'POST', body: data, signal }),
    submit: (id: string, signal?: AbortSignal) =>
      request<DeclarationResult>(`${declarationPath(id)}/submit`, { method: 'POST', signal }),
  };
}

export type DeclarationsApi = ReturnType<typeof createDeclarationsApi>;
