import { useState } from 'react';
import { AccessTokenInput } from './components/AccessTokenInput';
import { DeclarationForm } from './components/DeclarationForm';
import { DeclarationList } from './components/DeclarationList';
import { DeclarationDetails } from './components/DeclarationDetails';
import { useDeclarations } from './hooks/useDeclarations';
import type { DeclarationData } from './types/declaration';

const TOKEN_STORAGE_KEY = 'trade-token';

export function App() {
  const [token, setToken] = useState(() => sessionStorage.getItem(TOKEN_STORAGE_KEY) ?? '');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const workflow = useDeclarations(token, selectedId);

  function changeToken(value: string) {
    sessionStorage.setItem(TOKEN_STORAGE_KEY, value);
    setToken(value);
    setSelectedId(null);
  }

  async function saveDraft(data: DeclarationData) {
    const result = await workflow.createDraft(data);
    if (result) setSelectedId(result.id);
    return result;
  }

  return (
    <>
      <header>
        <b>ABYSS APPLIED</b>
        <span>Trade workflow demonstration</span>
      </header>
      <main>
        <div className="intro">
          <small>FEDERAL CIVILIAN · ENGINEERING DEMO</small>
          <h1>Trade declaration portal</h1>
          <p>One submission. A traceable journey from draft to decision.</p>
        </div>
        <aside>Synthetic data only · Demonstration rules · No connection to CBP or State</aside>
        <AccessTokenInput token={token} disabled={workflow.busy} onChange={changeToken} />
        {workflow.error && (
          <div role="alert" className="error">
            {workflow.error}
          </div>
        )}
        <div className="grid">
          <DeclarationForm disabled={workflow.busy || !token} onSave={saveDraft} />
          <section>
            <small>02 · SUBMIT & TRACK</small>
            <h2>
              Declarations <span className="count">{workflow.declarations.length}</span>
            </h2>
            <DeclarationList
              declarations={workflow.declarations}
              selectedId={selectedId}
              loading={workflow.loading}
              onSelect={setSelectedId}
            />
            {selectedId && workflow.loading && !workflow.selectedDeclaration && (
              <p>Loading declaration…</p>
            )}
            {workflow.selectedDeclaration && (
              <DeclarationDetails
                declaration={workflow.selectedDeclaration}
                busy={workflow.busy}
                onSubmit={() => void workflow.submitSelected()}
              />
            )}
          </section>
        </div>
        <footer>Go API + worker · PostgreSQL · Kafka · React · OpenAPI</footer>
      </main>
    </>
  );
}
