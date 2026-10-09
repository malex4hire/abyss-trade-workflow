import type { AuditEntry } from '../types/declaration';

export function AuditTrail({ entries }: { entries: AuditEntry[] }) {
  return (
    <>
      <h3>Audit trail</h3>
      <ol>
        {entries.map((entry, index) => (
          <li key={`${entry.at}-${entry.action}-${index}`}>
            <strong>{entry.action.replaceAll('_', ' ')}</strong>
            <small>{new Date(entry.at).toLocaleString()}</small>
            {entry.detail && <p>{entry.detail}</p>}
          </li>
        ))}
      </ol>
    </>
  );
}
